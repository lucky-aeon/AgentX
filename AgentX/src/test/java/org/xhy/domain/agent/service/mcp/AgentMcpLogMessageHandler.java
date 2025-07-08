package org.xhy.domain.agent.service.mcp;

import dev.langchain4j.mcp.client.logging.McpLogMessage;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;

public class AgentMcpLogMessageHandler implements McpLogMessageHandler {
    @Override
    public void handleLogMessage(McpLogMessage message) {
        System.out.println(message.toString());
    }
}
