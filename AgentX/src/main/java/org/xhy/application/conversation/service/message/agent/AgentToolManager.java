package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.PresetParameter;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Agent工具管理器 负责创建和管理工具提供者 */
@Component
public class AgentToolManager {

    private final MCPGatewayService mcpGatewayService;

    public AgentToolManager(MCPGatewayService mcpGatewayService) {
        this.mcpGatewayService = mcpGatewayService;
    }

    /** 创建工具提供者
     *
     * @param mcpServerNames 工具URL列表
     * @return 工具提供者实例，如果工具列表为空则返回null */
    public ToolProvider createToolProvider(List<String> mcpServerNames,
            Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        if (mcpServerNames == null || mcpServerNames.isEmpty()) {
            return null;
        }

        List<McpClient> mcpClients = new ArrayList<>();

        for (String mcpServerName : mcpServerNames) {

            String sseUrl = this.mcpGatewayService.getSSEUrl(mcpServerName);
            McpTransport transport = new HttpMcpTransport.Builder().sseUrl(sseUrl).logRequests(true).logResponses(true)
                    .timeout(Duration.ofHours(1)).build();

            McpClient mcpClient = new DefaultMcpClient.Builder().transport(transport).build();

            /** 预先设置参数 */
            if (toolPresetParams != null && toolPresetParams.containsKey(mcpServerName)) {
                List<PresetParameter> presetParameters = new ArrayList<>();
                for (String key : toolPresetParams.keySet()) {
                    toolPresetParams.get(key).forEach((k, v) -> {
                        presetParameters.add(new PresetParameter(k, JsonUtils.toJsonString(v)));
                    });
                }
                mcpClient.presetParameters(presetParameters);
            }
            mcpClients.add(mcpClient);
        }

        return McpToolProvider.builder().mcpClients(mcpClients).build();
    }

    /** 获取可用的工具列表
     *
     * @return 工具URL列表 */
    public List<String> getAvailableTools(ChatContext chatContext) {
        return chatContext.getMcpServerNames();
    }
}