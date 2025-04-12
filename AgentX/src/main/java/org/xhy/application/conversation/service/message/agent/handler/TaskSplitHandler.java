package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.template.AgentPromptTemplates;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.task.model.TaskEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 任务拆分处理器
 * 负责将复杂任务拆分为可管理的子任务
 */
@Component
public class TaskSplitHandler extends AbstractAgentHandler {

    public TaskSplitHandler(
            LLMServiceFactory llmServiceFactory,
            TaskManager taskManager,
            ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService) {
        super(llmServiceFactory, taskManager, conversationDomainService, contextDomainService);
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
        
        try {
            // 获取流式模型客户端
            StreamingChatLanguageModel streamingClient = getStreamingClient(context);
            
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
                        conversationDomainService.insertBathMessage(Arrays.asList(
                                context.getUserMessageEntity(), 
                                context.getLlmMessageEntity()));
                        
                        // 更新上下文
                        List<String> activeMessages = context.getChatContext().getContextEntity().getActiveMessages();
                        activeMessages.add(context.getUserMessageEntity().getId());
                        activeMessages.add(context.getLlmMessageEntity().getId());
                        contextDomainService.insertOrUpdate(context.getChatContext().getContextEntity());
                        
                        // 转换到任务拆分完成状态
                        context.transitionTo(AgentWorkflowState.TASK_SPLIT_COMPLETED);
                        
                        splitTaskFuture.complete(true);



                    } catch (Exception e) {
                        context.handleError(e);
                        splitTaskFuture.complete(false);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    context.handleError(error);
                    splitTaskFuture.complete(false);
                }
            });
        } catch (Exception e) {
            context.handleError(e);
        }
    }
    
    /**
     * 构建任务拆分请求
     */
    private <T> ChatRequest buildSplitTaskRequest(AgentWorkflowContext<T> context) {
        List<ChatMessage> messages = new ArrayList<>();
        
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