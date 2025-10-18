package org.xhy.application.conversation.service.message.agent;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.PresetParameter;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import org.springframework.stereotype.Component;
import org.xhy.application.conversation.service.McpUrlProviderService;
import org.xhy.application.conversation.service.handler.context.ChatContext;
import org.xhy.application.conversation.service.message.builtin.ToolDefinition;
import org.xhy.domain.agent.model.AgentEntity;
import org.xhy.domain.agent.service.AgentDomainService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.time.Duration;
import java.util.*;

/** Agent工具管理器 负责创建和管理工具提供者 */
@Component
public class AgentToolManager {

    private final McpUrlProviderService mcpUrlProviderService;
    private final AgentDomainService agentDomainService;

    public AgentToolManager(McpUrlProviderService mcpUrlProviderService, AgentDomainService agentDomainService) {
        this.mcpUrlProviderService = mcpUrlProviderService;
        this.agentDomainService = agentDomainService;
    }

    /** 创建工具提供者（支持全局/用户隔离工具自动识别）
     *
     * @return 工具提供者实例，如果工具列表为空则返回null */
    public ToolProvider createToolProvider(ChatContext chatContext) {
        // 仅构建 MCP ToolProvider（子Agent工具通过 BuiltInToolProvider 提供并由 BuiltInToolRegistry 注入）
        ToolProvider mcpProvider = null;
        List<String> mcpServerNames = chatContext.getMcpServerNames();
        Map<String, Map<String, Map<String, String>>> toolPresetParams = chatContext.getAgent().getToolPresetParams();
        String userId = chatContext.getUserId();

        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgentToolManager.class);
        logger.debug("🔧 [创建工具提供者] MCP服务器列表: {}, UserId: {}", mcpServerNames, userId);

        if (mcpServerNames != null && !mcpServerNames.isEmpty()) {
            List<McpClient> mcpClients = new ArrayList<>();
            for (String mcpServerName : mcpServerNames) {
                String sseUrl = mcpUrlProviderService.getMcpToolUrl(mcpServerName, userId);
                McpTransport transport = new HttpMcpTransport.Builder().sseUrl(sseUrl).logRequests(true)
                        .logResponses(true).timeout(Duration.ofHours(1)).build();
                McpClient mcpClient = new DefaultMcpClient.Builder().transport(transport).build();

                if (toolPresetParams != null && toolPresetParams.containsKey(mcpServerName)) {
                    List<PresetParameter> presetParameters = new ArrayList<>();
                    for (String key : toolPresetParams.keySet()) {
                        toolPresetParams.get(key).forEach((k, v) -> presetParameters
                                .add(new PresetParameter(k, JsonUtils.toJsonString(v))));
                    }
                    mcpClient.presetParameters(presetParameters);
                }
                mcpClients.add(mcpClient);
            }
            mcpProvider = McpToolProvider.builder().mcpClients(mcpClients).build();
            logger.debug("🔧 [工具提供者已创建] MCP客户端数量: {}", mcpClients.size());
        } else {
            logger.debug("🔧 [工具提供者] MCP服务器列表为空，返回null");
        }

        return mcpProvider;
    }

    /** 获取可用的工具列表
     *
     * @return 工具URL列表 */
    public List<String> getAvailableTools(ChatContext chatContext) {
        return chatContext.getMcpServerNames();
    }

    /**
     * 为当前Agent的“关联子Agent”创建工具定义与执行器（每个子Agent一个工具）。
     * 工具名示例：call_<agentName>，参数：message (required)
     */
    public Map<ToolSpecification, ToolExecutor> createLinkedAgentTools(AgentEntity agent, String userId) {
        Map<ToolSpecification, ToolExecutor> tools = new LinkedHashMap<>();
        if (agent == null) return tools;

        List<String> linkedIds = agent.getLinkedAgentIds();
        if (linkedIds == null || linkedIds.isEmpty()) return tools;

        for (String linkedId : linkedIds) {
            try {
                AgentEntity linked = agentDomainService.getAgentById(linkedId);
                if (linked == null || !Boolean.TRUE.equals(linked.getEnabled())) {
                    continue;
                }

                String toolName = "call_" + Optional.ofNullable(linked.getName()).orElse(linked.getId());
                String desc = "调用子Agent: " + linked.getName() + "，将消息转交给该Agent并返回其输出";

                ToolDefinition def = ToolDefinition.builder().name(toolName).description(desc)
                        .addRequiredStringParameter("message", "要发送给子Agent的消息内容").build();

                ToolSpecification spec = def.toSpecification();

                ToolExecutor exec = (request, memoryId) -> {
                    // 解析参数（简单 JSON 提取，延后替换为更稳健解析）
                    String args = request.arguments();
                    String msg = extractMessage(args);
                    if (msg == null || msg.trim().isEmpty()) {
                        return "❌ 子Agent调用失败：缺少参数 message";
                    }
                    // 暂时返回占位结果；后续步骤接入 ConversationAppService 同会话流式调用并事件回显
                    return "[子Agent " + linked.getName() + "] 接收消息: " + msg;
                };

                tools.put(spec, exec);

            } catch (Exception ignore) {
            }
        }

        return tools;
    }

    private String extractMessage(String argumentsJson) {
        try {
            // 非严格解析，尽量稳健；后续可使用 ObjectMapper
            if (argumentsJson == null) return null;
            String s = argumentsJson.trim();
            int i = s.indexOf("\"message\"");
            if (i < 0) return null;
            int colon = s.indexOf(":", i);
            if (colon < 0) return null;
            int startQuote = s.indexOf('"', colon + 1);
            if (startQuote < 0) return null;
            int endQuote = s.indexOf('"', startQuote + 1);
            if (endQuote < 0) return null;
            return s.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }
}
