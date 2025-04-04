package org.xhy.application.conversation.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.xhy.domain.conversation.model.ContextEntity;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ChatCompletionHandler;
import org.xhy.domain.llm.service.CompletionCallback;
import org.xhy.infrastructure.sse.SseEmitterFactory;

/**
 * 聊天完成回调适配器
 * 负责连接基础设施层和领域层，协调完成回调和消息处理
 */
@Component
public class ChatCompletionCallbackAdapter implements CompletionCallback {

    private final SseEmitterFactory sseEmitterFactory;
    private final ChatCompletionHandler chatCompletionHandler;
    
    private SseEmitter emitter;
    private MessageEntity userMessageEntity;
    private MessageEntity llmMessageEntity;
    private ContextEntity contextEntity;
    private String providerName;
    private String modelId;

    public ChatCompletionCallbackAdapter(
            SseEmitterFactory sseEmitterFactory,
            ChatCompletionHandler chatCompletionHandler) {
        this.sseEmitterFactory = sseEmitterFactory;
        this.chatCompletionHandler = chatCompletionHandler;
    }
    
    /**
     * 初始化回调上下文
     */
    public void initialize(
            SseEmitter emitter,
            MessageEntity userMessageEntity,
            MessageEntity llmMessageEntity,
            ContextEntity contextEntity,
            String providerName,
            String modelId) {
        this.emitter = emitter;
        this.userMessageEntity = userMessageEntity;
        this.llmMessageEntity = llmMessageEntity;
        this.contextEntity = contextEntity;
        this.providerName = providerName;
        this.modelId = modelId;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        // 发送部分响应
        sseEmitterFactory.sendPartialResponse(emitter, partialResponse, providerName, modelId);
    }

    @Override
    public void onCompleteResponse(String completeResponse, Integer inputTokenCount, Integer outputTokenCount) {
        try {
            // 发送完成响应
            sseEmitterFactory.sendCompleteResponse(emitter, providerName, modelId);
            
            // 委托给领域服务处理业务逻辑
            chatCompletionHandler.handleCompletion(
                    userMessageEntity,
                    llmMessageEntity,
                    contextEntity,
                    inputTokenCount,
                    outputTokenCount,
                    completeResponse);
        } catch (Exception e) {
            onError(e);
        }
    }

    @Override
    public void onError(Throwable error) {
        // 发送错误响应
        sseEmitterFactory.sendErrorResponse(emitter, "处理响应时发生错误: " + error.getMessage());
    }
} 