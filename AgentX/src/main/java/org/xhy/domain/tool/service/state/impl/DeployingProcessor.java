package org.xhy.domain.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.external_services.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.util.Map;

/** 工具部署处理器 */
public class DeployingProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DeployingProcessor.class);

    private final MCPGatewayService mcpGatewayService;

    /** 构造函数，注入MCPGatewayService
     * 
     * @param mcpGatewayService MCP网关服务 */
    public DeployingProcessor(MCPGatewayService mcpGatewayService) {
        this.mcpGatewayService = mcpGatewayService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.DEPLOYING;
    }

    @Override
    public void process(ToolEntity tool) {
        logger.info("工具ID: {} 进入DEPLOYING状态，开始部署。", tool.getId());
        try {
            // 获取安装命令
            Map<String, Object> installCommand = tool.getInstallCommand();
            if (installCommand == null || installCommand.isEmpty()) {
                throw new BusinessException("工具ID: " + tool.getId() + " 安装命令为空，无法部署。");
            }
            String installCommandJson = JsonUtils.toJsonString(installCommand);

            // 调用MCPGatewayService进行部署
            boolean deploySuccess = mcpGatewayService.deployTool(installCommandJson);

            if (deploySuccess) {
                logger.info("工具部署成功，工具ID: {}", tool.getId());
                // ToolStateService will handle the status transition to FETCHING_TOOLS
            } else {
                // MCPGatewayService.deployTool should throw BusinessException on API errors,
                // so this else block might only be reached if the API returned success: false
                // or if there's a specific condition for non-exception failure.
                // For now, assuming deployTool throws exception on API issues, this might be
                // redundant.
                // Re-throwing a BusinessException for clarity if needed.
                logger.error("工具部署失败 (API returned non-success status)，工具ID: {}", tool.getId());
                throw new BusinessException("MCP Gateway部署返回非成功状态。");
            }
        } catch (BusinessException e) {
            // Catch BusinessException from MCPGatewayService or internal checks
            logger.error("部署工具 {} (ID: {}) 失败: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw e; // Re-throw BusinessException to be caught by ToolStateService
        } catch (Exception e) {
            // Catch any unexpected exceptions
            logger.error("部署工具 {} (ID: {}) 过程中发生意外错误: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw new BusinessException("部署工具过程中发生意外错误: " + e.getMessage(), e); // Wrap unexpected exceptions
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }
}