package org.xhy.domain.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.container.service.ReviewContainerService;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.domain.tool.service.state.ToolStateProcessor;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;

import java.util.List;
import java.util.Map;

/** 获取工具列表处理器 */
public class FetchingToolsProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FetchingToolsProcessor.class);

    private final MCPGatewayService mcpGatewayService;
    private final ReviewContainerService reviewContainerService;

    /** 构造函数，注入依赖服务
     * 
     * @param mcpGatewayService MCP网关服务
     * @param reviewContainerService 审核容器服务 */
    public FetchingToolsProcessor(MCPGatewayService mcpGatewayService, ReviewContainerService reviewContainerService) {
        this.mcpGatewayService = mcpGatewayService;
        this.reviewContainerService = reviewContainerService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }

    @Override
    public void process(ToolEntity tool) {
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("工具ID: {} 进入FETCHING_TOOLS状态，开始从审核容器获取工具列表。", tool.getId());
        try {
            // 从installCommand中获取工具名称
            Map<String, Object> installCommand = tool.getInstallCommand();
            if (installCommand == null || installCommand.isEmpty()) {
                throw new BusinessException("安装命令为空");
            }

            // 解析mcpServers中的第一个key作为工具名称
            @SuppressWarnings("unchecked")
            Map<String, Object> mcpServers = (Map<String, Object>) installCommand.get("mcpServers");
            if (mcpServers == null || mcpServers.isEmpty()) {
                throw new BusinessException("工具ID: " + tool.getId() + " 安装命令中mcpServers为空。");
            }

            // 获取第一个key作为工具名称
            String toolName = mcpServers.keySet().iterator().next();
            if (toolName == null || toolName.isEmpty()) {
                throw new BusinessException("工具ID: " + tool.getId() + " 无法从安装命令中获取工具名称。");
            }

            // 获取审核容器连接信息
            logger.info("获取审核容器连接信息用于工具 {} 的审核", toolName);
            ReviewContainerService.ReviewContainerConnection reviewConnection = reviewContainerService
                    .getReviewContainerConnection();

            logger.info("从审核容器 {}:{} 获取工具 {} 的列表", reviewConnection.getIpAddress(), reviewConnection.getPort(),
                    toolName);

            // 调用MCPGatewayService从审核容器获取工具列表
            List<ToolDefinition> toolDefinitions = mcpGatewayService.listToolsFromReviewContainer(toolName,
                    reviewConnection.getIpAddress(), reviewConnection.getPort());

            // 将获取到的工具定义列表设置到ToolEntity中
            tool.setToolList(toolDefinitions);

            logger.info("成功从审核容器获取到工具 {} 的列表，共 {} 个定义。", toolName,
                    toolDefinitions != null ? toolDefinitions.size() : 0);

        } catch (BusinessException e) {
            logger.error("从审核容器获取工具列表失败 {} (ID: {}): {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("从审核容器获取工具列表 {} (ID: {}) 过程中发生意外错误: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw new BusinessException("从审核容器获取工具列表过程中发生意外错误: " + e.getMessage(), e);
        }
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }
}