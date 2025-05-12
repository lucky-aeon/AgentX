package org.xhy.domain.agent.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import okhttp3.OkHttpClient;
import org.xhy.domain.agent.service.AgentStandTest;
import org.xhy.infrastructure.utils.JsonUtils;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;

public class MCPStandTest {

    public static void main(String[] args) throws Exception {
        // 1) 要监听的 SSE 地址列表
        List<String> sseUrls = List.of( "http://localhost:8005/surge/sse/sse?api_key=123456");

        // 用于并行监听的订阅器和 McpClient 列表
        List<RawSseSubscriber> subscribers = new ArrayList<>();
        List<McpClient> mcpClients = new ArrayList<>();

        // 2) 为每个 URL 分别创建拦截器、订阅器和 McpClient
        for (String url : sseUrls) {
            // 每个 URL 单独一个 IdToToolInterceptor
            IdToToolInterceptor idMap = new IdToToolInterceptor();

            // 2.2) 构造 HttpMcpTransport 并注入拦截器
            HttpMcpTransport transport = new HttpMcpTransport.Builder().sseUrl(url).timeout(Duration.ofHours(1))
                    .logRequests(false).logResponses(true).build();

            // 通过反射替换 OkHttpClient，加入 interceptor
            Field clientField = HttpMcpTransport.class.getDeclaredField("client");
            clientField.setAccessible(true);
            OkHttpClient origClient = (OkHttpClient) clientField.get(transport);
            OkHttpClient hookedClient = origClient.newBuilder().addInterceptor(idMap).build();
            clientField.set(transport, hookedClient);

            // 2.3) 用这个 transport 构造 McpClient
            McpClient client = new DefaultMcpClient.Builder().transport(transport)
                    .logHandler(new AgentMcpLogMessageHandler()).build();
            mcpClients.add(client);
            
            // 获取工具列表
            List<ToolSpecification> toolSpecifications = client.listTools();
            
            // 使用ToolSpecificationConverter转换为DTO对象
            List<ToolSpecificationConverter.ToolDto> toolDtos = ToolSpecificationConverter.convert(toolSpecifications);

            System.out.println(JsonUtils.toJsonString(toolDtos));
        }

        // 3) 构造 OpenAI 聊天模型
        OpenAiChatModel model = OpenAiChatModel.builder().apiKey(System.getenv("API_KEY")).modelName("gpt-4o-mini")
                .baseUrl("https://api.ttapi.io/v1").logRequests(true).timeout(Duration.ofHours(1)).logResponses(true)
                .build();

        // 4) 把多个 McpClient 注入到同一个 ToolProvider
        ToolProvider toolProvider = McpToolProvider.builder().mcpClients(mcpClients).build();

        // 5) 构造聊天记忆并初始化 Agent
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder().maxMessages(1000)
                .chatMemoryStore(new InMemoryChatMemoryStore()) // 历史消息
                .build();
        memory.add(new SystemMessage(
                "你是一个用于创建网站的助手，你的作用是根据用户的要求生成对应的 html，css，js代码并且写入对应目录下，保证结构目录清晰。创建完对应的文件后执行对应的部署工具进行部署，上下文信息：1.部署账号密码为：xx / xxx.目录为：etc/proxy/code/xhy。"));

        var agent = AiServices.builder(AgentStandTest.class).chatLanguageModel(model).chatMemory(memory)
                .toolProvider(toolProvider).build();

        // 6) 调用 Agent，一次生成并部署页面
        AiMessage reply = agent.chat("程序员风格 html 页面，有以下信息：1.社区：code.xhyovo.cn，注册人数 400+。2.b 站：xhyovo");
        System.out.println("最终回复： " + (reply.hasToolExecutionRequests() ? "有工具调用" : reply.text()));

        // 7) 等待 SSE 输出刷完，再关闭所有订阅
        Thread.sleep(5_000);
        subscribers.forEach(RawSseSubscriber::close);
    }
}
