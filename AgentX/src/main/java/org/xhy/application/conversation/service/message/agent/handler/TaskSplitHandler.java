package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.service.InfoRequirementService;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 任务拆分处理器
 * 负责将复杂任务拆分为可管理的子任务
 */
@Component
public class TaskSplitHandler extends AbstractAgentHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskSplitHandler.class);
    private final InfoRequirementService infoRequirementService;

    public TaskSplitHandler(
            LLMServiceFactory llmServiceFactory,
            TaskManager taskManager,
            ContextDomainService contextDomainService,
            InfoRequirementService infoRequirementService,
            MessageDomainService messageDomainService
            ) {
        super(llmServiceFactory, taskManager, contextDomainService,messageDomainService);
        this.infoRequirementService = infoRequirementService;
    }
    
    @Override
    protected boolean shouldHandle(AgentWorkflowEvent event) {
        return event.getToState() == AgentWorkflowState.TASK_SPLITTING;
    }
    
    @Override
    protected void transitionToNextState(AgentWorkflowContext<?> context) {
        // 任务拆分阶段不需要立即转换状态，在处理完成后转换
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected <T> void processEvent(AgentWorkflowContext<?> contextObj) {
        AgentWorkflowContext<T> context = (AgentWorkflowContext<T>) contextObj;
        
        // 首先检查信息是否完整
        infoRequirementService.checkInfoAndWaitIfNeeded(context)
            .thenAccept(infoComplete -> {
                if (infoComplete) {
                    // 信息完整，执行任务拆分
                    log.info("信息完整，开始执行任务拆分");
                    doTaskSplitting(context);
                }
                // 如果信息不完整，checkInfoAndWaitIfNeeded已经处理了提示和上下文保存
            });
        
        // 设置为true以阻止父类自动调用transitionToNextState
        // 我们将在doTaskSplitting的回调中手动处理状态转换
        this.setBreak(true);
    }
    
    /**
     * 执行实际的任务拆分逻辑
     */
    private <T> void doTaskSplitting(AgentWorkflowContext<T> context) {
        try {

            // 获取流式模型客户端
            StreamingChatModel streamingClient = getStreamingClient(context);

            // 构建任务拆分请求
            ChatRequest splitTaskRequest = buildSplitTaskRequest(context);

            
            // 不阻塞，使用Future跟踪任务拆分完成
            CompletableFuture<Boolean> splitTaskFuture = new CompletableFuture<>();
            
            // 流式处理任务拆分响应
            streamingClient.doChat(splitTaskRequest, new StreamingChatResponseHandler() {
                StringBuilder taskSplitResult = new StringBuilder();
                
                @Override
                public void onPartialResponse(String partialResponse) {
                    // 累积响应结果
                    taskSplitResult.append(partialResponse);
                    
                    // 发送流式响应给前端
                    context.sendMessage(partialResponse, MessageType.TEXT);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        // 设置LLM消息内容和token数
                        TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();
                        Integer outputTokenCount = tokenUsage.outputTokenCount();
                        
                        String fullResponse = completeResponse.aiMessage().text();
                        context.getLlmMessageEntity().setContent(fullResponse);
                        context.getLlmMessageEntity().setTokenCount(outputTokenCount);
                        context.getLlmMessageEntity().setMessageType(MessageType.TEXT);
                        
                        // 分割任务描述
                        List<String> tasks = splitTaskDescriptions(fullResponse);
                        
                        if (tasks.isEmpty()) {
                            context.handleError(new RuntimeException("任务拆分失败，未能识别子任务"));
                            splitTaskFuture.complete(false);
                            return;
                        }
                        
                        // 为每个子任务创建实体
                        for (String task : tasks) {
                            TaskEntity subTask = taskManager.createSubTask(
                                    task,
                                    context.getParentTask().getId(),
                                    context.getChatContext());
                            
                            // 添加到上下文
                            context.addSubTask(task, subTask);
                        }
                        
                        context.sendEndMessage(MessageType.TASK_SPLIT_FINISH);
                        
                        // 保存用户消息和LLM消息，并更新上下文
                        saveMessageAndUpdateContext(Collections.singletonList(context.getLlmMessageEntity()),context.getChatContext());

                        // 转换到任务拆分完成状态
                        context.transitionTo(AgentWorkflowState.TASK_SPLIT_COMPLETED);
                        
                        splitTaskFuture.complete(true);
                    } catch (Exception e) {
                        log.error("任务拆分处理响应失败", e);
                        context.handleError(e);
                        splitTaskFuture.complete(false);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    log.error("任务拆分失败", error);
                    context.handleError(error);
                    splitTaskFuture.complete(false);
                }
            });
        } catch (Exception e) {
            log.error("执行任务拆分失败", e);
            context.handleError(e);
        }
    }
    
    /**
     * 构建任务拆分请求
     */
    private <T> ChatRequest buildSplitTaskRequest(AgentWorkflowContext<T> context) {
        List<ChatMessage> messages = new ArrayList<>();
        for (MessageEntity messageEntity : context.getChatContext().getMessageHistory()) {
            String content = messageEntity.getContent();
            if (messageEntity.getRole() == Role.SYSTEM){
                messages.add(new SystemMessage(content));
            }else if (messageEntity.getRole() == Role.USER){
                messages.add(new UserMessage(content));
            }else {
                messages.add(new AiMessage(content));
            }
        }
        // 添加系统提示词
        messages.add(new SystemMessage(AgentPromptTemplates.getDecompositionPrompt()));
        
        // 添加用户消息
        messages.add(new UserMessage(context.getChatContext().getUserMessage()));
        
        return buildChatRequest(context, messages);
    }
    
    /**
     * 将大模型返回的文本分割为子任务列表
     */
    private List<String> splitTaskDescriptions(String text) {
        List<String> tasks = new ArrayList<>();
        
        // 简单的任务分割逻辑，基于行号和可能的标记如"任务1"，"1."等
        // 实际项目中可能需要更复杂的解析逻辑
        String[] lines = text.split("\n");
        StringBuilder currentTask = new StringBuilder();
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过空行
            if (line.isEmpty()) {
                continue;
            }
            
            // 检测新任务的开始（基于常见模式）
            boolean isNewTask = line.matches("^\\d+\\..*") || // "1. 任务描述"
                               line.matches("^任务\\s*\\d+.*") || // "任务1: 描述"
                               line.matches("^子任务\\s*\\d+.*"); // "子任务1: 描述"
            
            if (isNewTask && currentTask.length() > 0) {
                // 保存之前的任务
                tasks.add(currentTask.toString().trim());
                currentTask = new StringBuilder();
            }
            
            currentTask.append(line).append("\n");
        }
        
        // 添加最后一个任务
        if (currentTask.length() > 0) {
            tasks.add(currentTask.toString().trim());
        }
        
        return tasks;
    }
} 