package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.UserToolEntity;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.service.UserToolDomainService;
import org.xhy.infrastructure.config.MCPGatewayProperties;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;
import org.xhy.interfaces.dto.tool.request.QueryToolRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentToolManager.class);

    private final UserToolDomainService userToolDomainService;
    private final ToolDomainService toolDomainService;
    private final MCPGatewayProperties mcpGatewayProperties;
    private final MCPGatewayService mcpGatewayService;

    public AgentToolManager(UserToolDomainService userToolDomainService, ToolDomainService toolDomainService,
            MCPGatewayProperties mcpGatewayProperties, MCPGatewayService mcpGatewayService) {
        this.userToolDomainService = userToolDomainService;
        this.toolDomainService = toolDomainService;
        this.mcpGatewayProperties = mcpGatewayProperties;
        this.mcpGatewayService = mcpGatewayService;
    }

    public ToolProvider createToolProvider(String userId) {
        List<ToolEntity> availableTools = getAvailableTools(userId);
        if (availableTools.isEmpty()) {
            return null;
        }

        List<McpClient> mcpClients = new ArrayList<>();
        for (ToolEntity tool : availableTools) {
            McpClient mcpClient = createClientWithRetry(tool);
            if (mcpClient != null) {
                mcpClients.add(mcpClient);
            }
        }

        if (mcpClients.isEmpty()) {
            return null;
        }
        return McpToolProvider.builder().mcpClients(mcpClients).build();
    }

    public List<ToolEntity> getAvailableTools(String userId) {
        if (userId == null || userId.isBlank() || isBlank(mcpGatewayProperties.getBaseUrl())
                || isBlank(mcpGatewayProperties.getApiKey())) {
            return List.of();
        }

        QueryToolRequest queryToolRequest = new QueryToolRequest();
        queryToolRequest.setPage(1);
        queryToolRequest.setPageSize(Integer.MAX_VALUE);

        List<UserToolEntity> installedTools = userToolDomainService.listByUserId(userId, queryToolRequest).getRecords();
        if (installedTools == null || installedTools.isEmpty()) {
            return List.of();
        }

        List<ToolEntity> toolEntities = new ArrayList<>();
        for (UserToolEntity installedTool : installedTools) {
            try {
                ToolEntity toolEntity = toolDomainService.getTool(installedTool.getToolId());
                if (toolEntity.getInstallCommand() != null && !toolEntity.getInstallCommand().isEmpty()) {
                    toolEntities.add(toolEntity);
                }
            } catch (Exception ignored) {
                // Skip broken tool records so one bad install does not block all tool usage.
            }
        }

        return toolEntities;
    }

    private McpClient createClientWithRetry(ToolEntity tool) {
        String toolName = extractToolName(tool.getInstallCommand());
        if (isBlank(toolName)) {
            return null;
        }

        String toolUrl = buildToolSseUrl(toolName);
        try {
            return createClient(toolUrl);
        } catch (RuntimeException firstException) {
            logger.warn("连接 MCP 工具失败，尝试重新部署。toolId={}, toolName={}, url={}", tool.getId(), toolName, toolUrl,
                    firstException);
            try {
                boolean deploySuccess = mcpGatewayService.deployTool(JsonUtils.toJsonString(tool.getInstallCommand()));
                if (!deploySuccess) {
                    logger.warn("重新部署 MCP 工具失败，跳过该工具。toolId={}, toolName={}", tool.getId(), toolName);
                    return null;
                }
                return createClient(toolUrl);
            } catch (Exception secondException) {
                logger.error("重新部署后仍无法连接 MCP 工具，跳过该工具。toolId={}, toolName={}, url={}", tool.getId(), toolName,
                        toolUrl, secondException);
                return null;
            }
        }
    }

    private McpClient createClient(String toolUrl) {
        McpTransport transport = new HttpMcpTransport.Builder().sseUrl(toolUrl).logRequests(true).logResponses(true)
                .timeout(Duration.ofHours(1)).build();
        return new DefaultMcpClient.Builder().transport(transport).build();
    }

    private String extractToolName(Map<String, Object> installCommand) {
        if (installCommand == null || installCommand.isEmpty()) {
            return null;
        }

        Object mcpServersValue = installCommand.get("mcpServers");
        if (!(mcpServersValue instanceof Map<?, ?> mcpServers) || mcpServers.isEmpty()) {
            return null;
        }

        for (Object key : mcpServers.keySet()) {
            if (key instanceof String toolName && !toolName.isBlank()) {
                return toolName;
            }
        }

        return null;
    }

    private String buildToolSseUrl(String toolName) {
        String baseUrl = mcpGatewayProperties.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/" + toolName + "/sse/sse?api_key=" + mcpGatewayProperties.getApiKey();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
