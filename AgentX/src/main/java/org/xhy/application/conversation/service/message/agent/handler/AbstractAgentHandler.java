package org.xhy.application.conversation.service.message.agent.handler;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.xhy.application.conversation.service.handler.content.ChatContext;
import org.xhy.application.conversation.service.message.agent.event.AgentEventHandler;
import org.xhy.application.conversation.service.message.agent.event.AgentWorkflowEvent;
import org.xhy.application.conversation.service.message.agent.manager.TaskManager;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowContext;
import org.xhy.application.conversation.service.message.agent.workflow.AgentWorkflowState;
import org.xhy.domain.conversation.constant.MessageType;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.domain.conversation.service.MessageDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;

import java.util.List;

/**
 * 代理事件处理器抽象基类
 * 提供通用功能和模板方法，减少子类中的重复代码
 */
public abstract class AbstractAgentHandler implements AgentEventHandler {

    private Boolean isBreak = false;

    protected final LLMServiceFactory llmServiceFactory;
    protected final TaskManager taskManager;
    protected final ContextDomainService contextDomainService;
    protected final MessageDomainService messageDomainService;
    
    protected AbstractAgentHandler(
            LLMServiceFactory llmServiceFactory,
            TaskManager taskManager,
            ContextDomainService contextDomainService, MessageDomainService messageDomainService) {
        this.llmServiceFactory = llmServiceFactory;
        this.taskManager = taskManager;
        this.contextDomainService = contextDomainService;
        this.messageDomainService = messageDomainService;
    }
    
    /**
     * 统一事件处理入口（模板方法）
     */
    @Override
    public final void handle(AgentWorkflowEvent event) {
        // 检查是否应该处理此事件
        if (!shouldHandle(event)) {
            return;
        }
        
        // 获取上下文并处理事件
        AgentWorkflowContext<?> context = event.getContext();
        processEvent(context);
        transitionToNextState(context);
    }
    
    /**
     * 检查是否应该处理此事件
     * 子类必须实现此方法以定义它们感兴趣的事件
     */
    protected abstract boolean shouldHandle(AgentWorkflowEvent event);
    
    /**
     * 转换到下一个状态
     * 子类必须实现此方法以定义状态转换
     */
    protected abstract void transitionToNextState(AgentWorkflowContext<?> context);
    
    /**
     * 处理具体的事件逻辑
     * 子类必须实现此方法以定义具体处理步骤
     */
    protected abstract <T> void processEvent(AgentWorkflowContext<?> context);


    /**
     * 保存消息并且更新消息到上下文
     */
    protected void saveMessageAndUpdateContext(List<MessageEntity> messageEntities, ChatContext chatContext){
        messageDomainService.saveMessageAndUpdateContext(messageEntities, chatContext.getContextEntity());
        chatContext.getMessageHistory().addAll(messageEntities);
    }

    /**
     * 创建消息实体
     * 通用方法，创建特定类型的消息实体
     */
    protected <T> MessageEntity createMessageEntity(
            AgentWorkflowContext<T> context, 
            MessageType messageType, 
            String content,
            Integer tokenCount) {
        MessageEntity message = new MessageEntity();
        message.setRole(context.getLlmMessageEntity().getRole());
        message.setSessionId(context.getChatContext().getSessionId());
        message.setModel(context.getChatContext().getModel().getModelId());
        message.setProvider(context.getChatContext().getProvider().getId());
        message.setMessageType(messageType);
        message.setContent(content);
        message.setTokenCount(tokenCount != null ? tokenCount : 0);
        return message;
    }
    
    /**
     * 获取流式模型客户端
     */
    protected <T> StreamingChatLanguageModel getStreamingClient(AgentWorkflowContext<T> context) {
        return llmServiceFactory.getStreamingClient(
                context.getChatContext().getProvider(), 
                context.getChatContext().getModel());
    }

    /**
     * 获取标准模型客户端
     */
    protected <T> ChatLanguageModel getStrandClient(AgentWorkflowContext<T> context) {
        return llmServiceFactory.getStrandClient(
                context.getChatContext().getProvider(),
                context.getChatContext().getModel());
    }
    
    /**
     * 构建聊天请求的通用方法
     */
    protected <T> ChatRequest buildChatRequest(
            AgentWorkflowContext<T> context, 
            List<ChatMessage> messages) {
        ChatRequest.Builder requestBuilder = new ChatRequest.Builder();
        
        // 构建请求参数
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(context.getChatContext().getModel().getModelId());
        parameters.topP(context.getChatContext().getLlmModelConfig().getTopP())
                .temperature(context.getChatContext().getLlmModelConfig().getTemperature());
                
        requestBuilder.messages(messages);
        requestBuilder.parameters(parameters.build());
        
        return requestBuilder.build();
    }

    public Boolean getBreak() {
        return isBreak;
    }

    public void setBreak(Boolean aBreak) {
        isBreak = aBreak;
    }
}