package org.xhy.domain.conversation.handler;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.domain.conversation.model.MessageEntity;
import org.xhy.domain.conversation.service.ContextDomainService;
import org.xhy.domain.conversation.service.ConversationDomainService;
import org.xhy.infrastructure.llm.LLMServiceFactory;
import org.xhy.infrastructure.transport.MessageTransport;

import java.util.List;

/**
 * React消息处理器
 * 用于支持工具调用的对话模式
 * 目前为预留实现，后续将完善MCP工具调用
 */
@Component(value = "reactMessageHandler")
public class AgentMessageHandler extends ChatMessageHandler {

    public AgentMessageHandler(
            ConversationDomainService conversationDomainService,
            ContextDomainService contextDomainService,
            LLMServiceFactory llmServiceFactory) {
        super(conversationDomainService, contextDomainService, llmServiceFactory);
    }

    @Override
    public <T> T handleChat(ChatEnvironment environment, MessageTransport<T> messageTransport) {
        // 创建用户消息实体
        MessageEntity userMessageEntity = this.createUserMessage(environment);

        // 创建LLM消息实体
        MessageEntity llmMessageEntity = createLlmMessage(environment);

        // 创建连接
        T connection = messageTransport.createConnection(CONNECTION_TIMEOUT);

        // 准备LLM请求
        dev.langchain4j.model.chat.request.ChatRequest llmRequest = prepareLlmRequest(environment);

        // 获取LLM客户端
        ChatLanguageModel strandClient = llmServiceFactory.getStrandClient(
                environment.getProvider(), environment.getModel());

        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://124.220.234.136:8006/time/sse")
                .logRequests(true) // if you want to see the traffic in the log
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        Agent agent = AiServices.builder(Agent.class)
                .chatLanguageModel(strandClient)
                .toolProvider(toolProvider)
                .build();
        String response = agent.chat("Summarize the last 3 commits of the LangChain4j GitHub repository");
        System.out.println("RESPONSE: " + response);
        return connection;
    }

    /**
     * 预留的工具调用方法
     *
     * @param toolName   工具名称
     * @param parameters 工具参数
     * @return 工具调用结果
     */
    protected Object invokeExternalTool(String toolName, Object parameters) {
        // 预留接口，未来将实现MCP工具调用
        return null;
    }
} 