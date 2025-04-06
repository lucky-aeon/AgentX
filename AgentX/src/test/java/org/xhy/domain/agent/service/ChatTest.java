package org.xhy.domain.agent.service;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

import static java.lang.Thread.sleep;

public class ChatTest {
    public static void main(String[] args) throws InterruptedException {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("API_KEY"))
                .modelName("Qwen/QwQ-32B")
                .baseUrl("https://api.siliconflow.cn/v1")
                .logRequests(true)
                .logResponses(true)
                .build();


        AgentTest bot = AiServices.builder(AgentTest.class)
                .streamingChatLanguageModel(model)
                .build();

        TokenStream tokenStream = bot.chat("你是谁");
        tokenStream.onPartialResponse((String partialResponse) -> System.out.println("流式：" + partialResponse))
                .onCompleteResponse((ChatResponse response) -> {
                    System.out.println("已完成" + response);})
                .onError((Throwable error) -> error.printStackTrace())
                .start();
        while (true){
            sleep(1000);
        }
    }
}
