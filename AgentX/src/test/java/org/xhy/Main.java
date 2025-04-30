// package org.xhy;
//
// import dev.langchain4j.model.chat.StreamingChatLanguageModel;
// import dev.langchain4j.model.chat.response.ChatResponse;
// import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
// import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
// import dev.langchain4j.service.AiServices;
// import dev.langchain4j.service.TokenStream;
//
// import java.util.Map;
// import java.util.concurrent.CompletableFuture;
//
// interface StreamingAssistant {
//
// }
//
// public class Main {
// public static void main(String[] args) {
// StreamingChatLanguageModel chatModel = OpenAiStreamingChatModel.builder()
// .apiKey("sk-gdfpoouhufulfqrxetonlzzfobqdnwedeefaxdxvgvqidpzu")
// .modelName("Qwen/QwQ-32B")
// .baseUrl("https://api.siliconflow.cn/v1")
// .build();
// CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
//
// chatModel.chat("你是谁呀", new StreamingChatResponseHandler() {
//
// @Override
// public void onPartialResponse(String partialResponse) {
// System.out.print(partialResponse);
// }
//
// @Override
// public void onCompleteResponse(ChatResponse completeResponse) {
// futureChatResponse.complete(completeResponse);
// }
//
// @Override
// public void onError(Throwable error) {
// futureChatResponse.completeExceptionally(error);
// }
// });
//
// futureChatResponse.join();
// }
// }
//
