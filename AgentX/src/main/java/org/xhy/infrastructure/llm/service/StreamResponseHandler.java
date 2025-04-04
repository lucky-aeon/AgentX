package org.xhy.infrastructure.llm.service;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.stereotype.Component;
import org.xhy.domain.llm.model.LLMRequest;
import org.xhy.domain.llm.service.CompletionCallback;
import org.xhy.infrastructure.llm.adapter.LangChain4jAdapter;

/**
 * 流式响应处理器
 * 负责处理LLM的流式回复，使用适配器隔离外部依赖
 */
@Component
public class StreamResponseHandler {

    private final LangChain4jAdapter adapter;
    
    public StreamResponseHandler(LangChain4jAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * 处理LLM的流式响应
     *
     * @param chatStreamClient 聊天流客户端
     * @param llmRequest       LLM请求
     * @param callback         完成回调接口
     */
    public void handleStreamResponse(
            StreamingChatLanguageModel chatStreamClient,
            LLMRequest llmRequest,
            CompletionCallback callback) {
        
        // 委托给适配器处理
        adapter.doStreamingChat(chatStreamClient, llmRequest, callback);
    }
} 