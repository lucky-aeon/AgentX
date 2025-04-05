package org.xhy.infrastructure.llm.adapter;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import org.springframework.stereotype.Component;
import org.xhy.domain.llm.model.LLMRequest;
import org.xhy.domain.llm.service.CompletionCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j适配器
 * 负责将领域模型转换为LangChain4j模型，屏蔽外部库细节
 */
@Component
public class LangChain4jAdapter {
    
    /**
     * 将领域请求模型转换为LangChain4j请求模型
     *
     * @param llmRequest 领域请求模型
     * @return LangChain4j请求模型
     */
    public ChatRequest toExternalRequest(LLMRequest llmRequest) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        
        // 转换消息
        for (LLMRequest.LLMMessage message : llmRequest.getMessages()) {
            switch (message.getType()) {
                case USER:
                    chatMessages.add(new UserMessage(message.getContent()));
                    break;
                case SYSTEM:
                    chatMessages.add(new SystemMessage(message.getContent()));
                    break;
                case ASSISTANT:
                    chatMessages.add(new AiMessage(message.getContent()));
                    break;
            }
        }
        
        // 转换参数
        LLMRequest.LLMRequestParameters params = llmRequest.getParameters();
        OpenAiChatRequestParameters.Builder parameters = new OpenAiChatRequestParameters.Builder();
        parameters.modelName(params.getModelId())
                 .temperature(params.getTemperature())
                 .topP(params.getTopP());
        
        // 构建请求
        return new ChatRequest.Builder()
                .messages(chatMessages)
                .parameters(parameters.build())
                .build();
    }
    
    /**
     * 执行流式请求
     *
     * @param client 流式聊天语言模型
     * @param llmRequest 领域请求模型
     * @param callback 完成回调
     */
    public void doStreamingChat(
            StreamingChatLanguageModel client, 
            LLMRequest llmRequest,
            CompletionCallback callback) {
            
        // 转换为外部请求
        ChatRequest externalRequest = toExternalRequest(llmRequest);
        
        // 执行请求
        client.doChat(externalRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                callback.onPartialResponse(partialResponse);
            }
            
            @Override
            public void onCompleteResponse(ChatResponse response) {
                String content = response.aiMessage().text();
                Integer inputTokens = response.metadata().tokenUsage().inputTokenCount();
                Integer outputTokens = response.metadata().tokenUsage().outputTokenCount();
                
                callback.onCompleteResponse(content, inputTokens, outputTokens);
            }
            
            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }
} 