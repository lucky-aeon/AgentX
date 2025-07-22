package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecutor;
import org.xhy.application.conversation.service.message.agent.tool.RagToolSpecification;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.amazonaws.auth.policy.Policy.fromJson;

/** 动态RAG工具测试 */
public class DynamicRagToolTest {

    public static void main(String[] args) {

        // 使用新的RAG工具规范和执行器
        List<String> knowledgeBaseNames = Arrays.asList("Java知识库", "Spring框架文档");
        ToolSpecification ragToolSpec = RagToolSpecification.createToolSpecification(knowledgeBaseNames);

        // 创建模拟的RAG工具执行器
        ToolExecutor ragToolExecutor = (toolExecutionRequest, memoryId) -> {
            System.out.println("执行RAG工具搜索，参数: " + toolExecutionRequest.arguments());

            // 模拟RAG搜索结果
            return "根据查询在知识库中找到以下相关内容：\n\n" + "【文档片段 1】\n" + "Java是一种面向对象的编程语言，具有跨平台、安全性高、性能优良等特点。\n"
                    + "来源：Java基础教程.pdf\n\n" + "【文档片段 2】\n" + "Spring框架是Java企业级应用开发的主流框架，提供了依赖注入、AOP等核心功能。\n"
                    + "来源：Spring框架指南.md\n\n" + "以上内容来自知识库，请基于这些信息回答用户问题。";
        };

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://124.220.234.136:8005/file-system/sse/sse?api_key=123456").logRequests(true)
                .logResponses(true).timeout(Duration.ofHours(1)).build();

        McpClient mcpClient = new DefaultMcpClient.Builder().transport(transport).build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder().modelName("Qwen/Qwen3-30B-A3B")
                .baseUrl("https://api.siliconflow.cn/v1").apiKey("sk-").logRequests(true).logResponses(true).build();

        // 创建MCP工具提供者
        McpToolProvider mcpProvider = McpToolProvider.builder().mcpClients(mcpClient).build();

        // 创建RAG工具映射
        Map<ToolSpecification, ToolExecutor> ragTools = Map.of(ragToolSpec, ragToolExecutor);

        // 创建复合工具提供者，同时支持MCP工具和RAG工具

        // 创建Agent，使用复合工具提供者
        Agent assistant = AiServices.builder(Agent.class).chatModel(chatModel).tools(ragTools).toolProvider(mcpProvider)
                .build();

        // 测试RAG工具调用
        System.out.println("=== 测试RAG工具集成 ===");
        AiMessage aiMessage = assistant.chat("请搜索Java和Spring相关的知识");
        System.out.println("Agent响应: " + aiMessage.text());

        while (true) {

        }
    }
}