package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
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
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 任务汇总处理器
 * 负责将所有子任务结果汇总为最终结果
 */
@Component
public class SummarizeHandler extends AbstractAgentHandler {

    public SummarizeHandler(
            LLMServiceFactory llmServiceFactory,
            TaskManager taskManager,
            ContextDomainService contextDomainService,
            MessageDomainService messageDomainService) {
        super(llmServiceFactory, taskManager, contextDomainService,messageDomainService);
    }
    
    @Override
    protected boolean shouldHandle(AgentWorkflowEvent event) {
        return event.getToState() == AgentWorkflowState.TASK_EXECUTED;
    }
    
    @Override
    protected void transitionToNextState(AgentWorkflowContext<?> context) {
        context.transitionTo(AgentWorkflowState.SUMMARIZING);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected <T> void processEvent(AgentWorkflowContext<?> contextObj) {
        AgentWorkflowContext<T> context = (AgentWorkflowContext<T>) contextObj;
        
        try {
            // 创建汇总消息实体
            MessageEntity summaryMessageEntity = createMessageEntity(
                    context, MessageType.TEXT, null, 0);
            
            // 获取任务结果字符串
            String taskSummary = context.buildTaskSummary();
            
            // 获取流式模型客户端
            StreamingChatModel streamingClient = getStreamingClient(context);

            // 构建汇总请求
            ChatRequest summaryRequest = buildSummaryRequest(context, taskSummary);
            
            // 执行汇总
            streamingClient.doChat(summaryRequest, new StreamingChatResponseHandler() {
                StringBuilder fullSummary = new StringBuilder();
                
                @Override
                public void onPartialResponse(String partialResponse) {
                    fullSummary.append(partialResponse);
                    
                    // 发送流式响应给前端
                    context.sendMessage(partialResponse, MessageType.TEXT);
                }
                
                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    try {
                        // 设置LLM消息内容和token数
                        TokenUsage tokenUsage = completeResponse.metadata().tokenUsage();
                        Integer outputTokenCount = tokenUsage.outputTokenCount();
                        
                        String summary = completeResponse.aiMessage().text();
                        summaryMessageEntity.setContent(summary);
                        summaryMessageEntity.setTokenCount(outputTokenCount);


                        saveMessageAndUpdateContext(Collections.singletonList(summaryMessageEntity),context.getChatContext());
                        // 更新父任务为完成状态
                        taskManager.completeTask(context.getParentTask(), summary);
                        
                        // 发送最终完成消息
                        context.sendEndMessage(MessageType.TEXT);
                        context.completeConnection();
                        
                        // 转换到完成状态
                        context.transitionTo(AgentWorkflowState.COMPLETED);
                        
                    } catch (Exception e) {
                        context.handleError(e);
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    context.handleError(error);
                }
            });
            
        } catch (Exception e) {
            context.handleError(e);
        }
    }
    
    /**
     * 构建汇总请求
     */
    private ChatRequest buildSummaryRequest(AgentWorkflowContext<?> context, String taskResults) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统提示词
        messages.add(new SystemMessage(AgentPromptTemplates.getSummaryPrompt(taskResults)));
        
        // 添加用户消息
        messages.add(new UserMessage("请基于上述子任务结果提供总结"));
        
        return buildChatRequest(context, messages);
    }
} 
