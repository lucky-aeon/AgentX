package org.xhy.application.chat.service;

import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatService {
    

    public SseEmitter streamChat(String message) {
        SseEmitter emitter = new SseEmitter();

        StreamingChatLanguageModel chatModel = OpenAiStreamingChatModel.builder()
                .apiKey("sk-gdfpoouhufulfqrxetonlzzfobqdnwedeefaxdxvgvqidpzu")
                .modelName("Qwen/QwQ-32B")
                .baseUrl("https://api.siliconflow.cn/v1")
                .build();
        chatModel.chat(message, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.println(partialResponse);
                try {
                    emitter.send(partialResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("end: " + completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                System.out.println(error.getMessage());
            }
        });


        return emitter;
    }
} 