package org.xhy.domain.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;

import java.util.List;
import java.util.Map;

public class FetchingToolsProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FetchingToolsProcessor.class);
    private final MCPGatewayService mcpGatewayService;

    public FetchingToolsProcessor(MCPGatewayService mcpGatewayService) {
        this.mcpGatewayService = mcpGatewayService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }

    @Override
    public void process(ToolEntity tool) {
        try {
            // 部署后反查 MCP Gateway，自动拿到这个工具真正暴露了哪些能力定义
            Map<String, Object> installCommand = tool.getInstallCommand();
            if (installCommand == null || installCommand.isEmpty()) {
                throw new BusinessException("安装命令为空");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) installCommand.get("mcpServers");
            if (mcpServers == null || mcpServers.isEmpty()) {
                throw new BusinessException("工具 ID: " + tool.getId() + " 的安装命令中 mcpServers 为空");
            }

            String toolName = mcpServers.keySet().iterator().next();
            if (toolName == null || toolName.isEmpty()) {
                throw new BusinessException("工具 ID: " + tool.getId() + " 无法从安装命令中获取工具名称");
            }

            List<ToolDefinition> toolDefinitions = mcpGatewayService.listTools(toolName);
            // toolList 不是前端手填，而是平台自动发现出的结果
            tool.setToolList(toolDefinitions);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("获取工具列表过程中发生意外错误: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }
}
