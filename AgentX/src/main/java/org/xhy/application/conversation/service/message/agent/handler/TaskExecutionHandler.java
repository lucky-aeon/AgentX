package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.agent.Agent;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.AgentToolManager;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.task.constant.TaskStatus;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.infrastructure.llm.LLMServiceFactory;

import java.util.Collections;
import java.util.Map;

/**
 * 任务执行处理器
 * 处理子任务的执行逻辑
 */
@Component
public class TaskExecutionHandler extends AbstractAgentHandler {
    private final AgentToolManager toolManager;

    public TaskExecutionHandler(
            LLMServiceFactory llmServiceFactory,
            AgentToolManager toolManager,
            TaskManager taskManager,
            ContextDomainService contextDomainService,
            MessageDomainService messageDomainService) {
        super(llmServiceFactory, taskManager, contextDomainService,messageDomainService);
        this.toolManager = toolManager;
    }
    
    @Override
    protected boolean shouldHandle(AgentWorkflowEvent event) {
        return event.getToState() == AgentWorkflowState.TASK_SPLIT_COMPLETED;
    }
    
    @Override
    protected void transitionToNextState(AgentWorkflowContext<?> context) {
        context.transitionTo(AgentWorkflowState.TASK_EXECUTING);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected <T> void processEvent(AgentWorkflowContext<?> contextObj) {
        AgentWorkflowContext<T> context = (AgentWorkflowContext<T>) contextObj;
        
        try {
            // 获取工具提供者
            ToolProvider toolProvider = toolManager.createToolProvider(toolManager.getAvailableTools());
            
            // 依次执行每个子任务
            while (context.hasNextTask()) {
                String taskName = context.getNextTask();
                if (taskName == null) {
                    break;
                }
                
                TaskEntity subTask = context.getSubTaskMap().get(taskName);
                executeSubTask(context, subTask, taskName, toolProvider);
                
                // 更新父任务进度
                taskManager.updateTaskProgress(
                        context.getParentTask(), 
                        context.getCompletedTaskCount(), 
                        context.getTotalTaskCount());
            }
            
            // 所有子任务执行完成，转换到任务执行完成状态
            context.transitionTo(AgentWorkflowState.TASK_EXECUTED);
            
        } catch (Exception e) {
            context.handleError(e);
        }
    }
    
    /**
     * 执行单个子任务
     */
    private <T> void executeSubTask(
            AgentWorkflowContext<T> context, 
            TaskEntity subTask, 
            String taskName,
            ToolProvider toolProvider) {
        
        try {
            String taskId = subTask.getId();
            // 更新任务状态为进行中
            taskManager.updateTaskStatus(subTask, TaskStatus.IN_PROGRESS);
            
            // 保存执行消息
            MessageEntity taskCallMessageEntity = createMessageEntity(
                    context, MessageType.TASK_EXEC, taskName, 0);
            messageDomainService.saveMessage(Collections.singletonList(taskCallMessageEntity));
            
            // 通知前端当前执行的任务
            context.sendEndMessage(taskName, MessageType.TASK_EXEC);
            
            // 通知前端任务状态
            context.sendEndWithTaskIdMessage(taskId,MessageType.TASK_STATUS_TO_LOADING);
            
            // 获取用户原始请求
            String userRequest = context.getChatContext().getUserMessage();
            
            // 获取之前子任务的结果
            Map<String, String> previousTaskResults = context.getTaskResults();
            
            // 构建任务提示词
            String taskPrompt = AgentPromptTemplates.getTaskExecutionPrompt(
                    userRequest,
                    taskName,
                    previousTaskResults);
            
            // 执行子任务
            ChatLanguageModel model = llmServiceFactory.getStrandClient(
                    context.getChatContext().getProvider(), 
                    context.getChatContext().getModel());
            
            // 创建Agent服务
            Agent agent = AiServices.builder(Agent.class)
                    .chatLanguageModel(model)
                    .toolProvider(toolProvider)
                    .build();
            
            // 执行任务，直接使用完整提示词
            AiMessage aiMessage = agent.chat(taskPrompt);
            
            // 处理工具调用
            if (aiMessage.hasToolExecutionRequests()) {
                handleToolCalls(aiMessage, context);
            }
            
            // 获取任务结果
            String taskResult = aiMessage.text();
            
            // 保存子任务结果
            context.addTaskResult(taskName, taskResult);
            taskManager.completeTask(subTask, taskResult);
            
            // 通知前端任务完成
            context.sendEndWithTaskIdMessage(taskId, MessageType.TASK_STATUS_TO_FINISH);
            
        } catch (Exception e) {
            // 处理子任务执行异常，但不影响其他子任务执行
            subTask.updateStatus(TaskStatus.FAILED);
            subTask.setTaskResult("执行失败: " + e.getMessage());
            taskManager.updateTaskStatus(subTask, TaskStatus.FAILED);
            
            // 记录错误并继续
            context.sendEndMessage("任务 '" + taskName + "' 执行失败: " + e.getMessage(), MessageType.TEXT);
            
            // 为了工作流继续，我们仍然增加已完成任务计数
            context.addTaskResult(taskName, "执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理工具调用
     */
    private <T> void handleToolCalls(AiMessage aiMessage, AgentWorkflowContext<T> context) {
        // 创建工具调用消息实体
        MessageEntity toolCallMessageEntity = createMessageEntity(
                context, MessageType.TOOL_CALL, null, 0);
        StringBuilder toolCallsContent = new StringBuilder("工具调用:\n");
        
        aiMessage.toolExecutionRequests().forEach(toolExecutionRequest -> {
            String toolName = toolExecutionRequest.name();
            toolCallsContent.append("- ").append(toolName).append("\n");
            
            // 通知前端工具调用
            context.sendEndMessage(toolName, MessageType.TOOL_CALL);
        });
        
        // 设置工具调用内容并保存
        toolCallMessageEntity.setContent(toolCallsContent.toString());
        messageDomainService.saveMessage(Collections.singletonList(toolCallMessageEntity));
        
        // 更新上下文
        context.getChatContext().getContextEntity().getActiveMessages().add(toolCallMessageEntity.getId());
    }
} 