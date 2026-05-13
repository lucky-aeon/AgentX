package org.xhy.domain.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.util.Map;

public class DeployingProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DeployingProcessor.class);
    private final MCPGatewayService mcpGatewayService;

    public DeployingProcessor(MCPGatewayService mcpGatewayService) {
        this.mcpGatewayService = mcpGatewayService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.DEPLOYING;
    }

    @Override
    public void process(ToolEntity tool) {
        logger.info("工具 ID: {} 进入 DEPLOYING 状态，开始部署。", tool.getId());
        try {
            // installCommand 是结构化部署配置，这里会整体转成 JSON 发给 MCP Gateway
            Map<String, Object> installCommand = tool.getInstallCommand();
            if (installCommand == null || installCommand.isEmpty()) {
                throw new BusinessException("工具 ID: " + tool.getId() + " 的安装命令为空，无法部署");
            }
            String installCommandJson = JsonUtils.toJsonString(installCommand);

            boolean deploySuccess = mcpGatewayService.deployTool(installCommandJson);
            if (!deploySuccess) {
                throw new BusinessException("MCP Gateway 部署返回了非成功状态");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("部署工具过程中发生意外错误: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }
}
