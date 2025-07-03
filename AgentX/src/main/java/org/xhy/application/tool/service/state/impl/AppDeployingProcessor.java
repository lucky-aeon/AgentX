package org.xhy.application.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.container.service.ReviewContainerService;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.utils.JsonUtils;

import java.util.Map;

/** 应用层工具部署处理器
 * 
 * 职责： 1. 调用MCPGatewayService进行工具部署 2. 处理部署结果 3. 转换到下一状态（获取工具列表） */
public class AppDeployingProcessor implements AppToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppDeployingProcessor.class);

    private final MCPGatewayService mcpGatewayService;
    private final ReviewContainerService reviewContainerService;

    /** 构造函数，注入MCPGatewayService
     * 
     * @param mcpGatewayService MCP网关服务 */
    public AppDeployingProcessor(MCPGatewayService mcpGatewayService, ReviewContainerService reviewContainerService) {
        this.mcpGatewayService = mcpGatewayService;
        this.reviewContainerService = reviewContainerService;
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

            ReviewContainerService.ReviewContainerConnection reviewConnection = reviewContainerService
                    .getReviewContainerConnection();
            boolean deploySuccess = mcpGatewayService.deployTool(installCommandJson, reviewConnection.getIpAddress(),
                    reviewConnection.getPort());

            if (deploySuccess) {
                logger.info("工具部署成功，工具ID: {}", tool.getId());
            } else {
                logger.error("工具部署失败 (API返回非成功状态)，工具ID: {}", tool.getId());
                throw new BusinessException("MCP Gateway部署返回非成功状态。");
            }
        } catch (BusinessException e) {
            logger.error("部署工具 {} (ID: {}) 失败: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw e; // 重新抛出BusinessException
        } catch (Exception e) {
            logger.error("部署工具 {} (ID: {}) 过程中发生意外错误: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw new BusinessException("部署工具过程中发生意外错误: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }
}