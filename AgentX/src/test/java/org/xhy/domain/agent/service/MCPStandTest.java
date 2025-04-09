package org.xhy.domain.agent.service;

import dev.langchain4j.data.message.AiMessage;
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

public class MCPStandTest {

    public static void main(String[] args) throws Exception {

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("API_KEY"))
                .modelName("Qwen/QwQ-32B")
                .baseUrl("https://api.siliconflow.cn/v1")
                .logRequests(true)
                .logResponses(true)
                .build();
        // npx -y @smithery/cli@latest run @smithery-ai/github --key def1f067-c5b7-443f-af21-2a80c5f176d9
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://127.0.0.1:8006/time/sse")
                .logRequests(true) // if you want to see the traffic in the log
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        AgentStandTest agent = AiServices.builder(AgentStandTest.class)
                .streamingChatLanguageModel(model)
                .toolProvider(toolProvider)
                .build();

        AiMessage aiMessage = agent.chat("获取时区");
        if (aiMessage.hasToolExecutionRequests()) {
            aiMessage.toolExecutionRequests().forEach(toolExecutionRequest -> {
                String toolName = toolExecutionRequest.name();
                System.out.println(toolName);
            });
        }else {
            System.out.println(aiMessage.text());
        }


    }

}
