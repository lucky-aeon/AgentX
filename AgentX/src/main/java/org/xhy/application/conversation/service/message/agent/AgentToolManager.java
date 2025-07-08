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
import org.xhy.application.conversation.service.McpUrlProviderService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Agent工具管理器 负责创建和管理工具提供者 */
@Component
public class AgentToolManager {

    private final McpUrlProviderService mcpUrlProviderService;

    public AgentToolManager(McpUrlProviderService mcpUrlProviderService) {
        this.mcpUrlProviderService = mcpUrlProviderService;
    }

    /** 创建工具提供者（支持全局/用户隔离工具自动识别）
     *
     * @param mcpServerNames 工具服务名列表
     * @param toolPresetParams 工具预设参数
     * @param userId 用户ID（关键参数：用于用户隔离工具）
     * @return 工具提供者实例，如果工具列表为空则返回null */
    public ToolProvider createToolProvider(List<String> mcpServerNames,
            Map<String, Map<String, Map<String, String>>> toolPresetParams, String userId) {
        if (mcpServerNames == null || mcpServerNames.isEmpty()) {
            return null;
        }

        List<McpClient> mcpClients = new ArrayList<>();

        for (String mcpServerName : mcpServerNames) {
            String sseUrl = mcpUrlProviderService.getMcpToolUrl(mcpServerName, userId);
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

    /** 兼容性方法：保持向后兼容
     *
     * @param mcpServerNames 工具服务名列表
     * @param toolPresetParams 工具预设参数
     * @return 工具提供者实例，如果工具列表为空则返回null */
    public ToolProvider createToolProvider(List<String> mcpServerNames,
            Map<String, Map<String, Map<String, String>>> toolPresetParams) {
        return createToolProvider(mcpServerNames, toolPresetParams, null);
    }

    /** 获取可用的工具列表
     *
     * @return 工具URL列表 */
    public List<String> getAvailableTools(ChatContext chatContext) {
        return chatContext.getMcpServerNames();
    }
}