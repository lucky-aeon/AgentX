package org.xhy.application.tool.service.state.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.application.container.service.ReviewContainerService;
import org.xhy.application.tool.service.state.AppToolStateProcessor;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;

import java.util.List;
import java.util.Map;

/** 应用层获取工具列表处理器
 * 
 * 职责： 1. 从审核容器获取工具定义列表 2. 调用MCPGatewayService和ReviewContainerService 3. 将工具定义存储到工具实体 4. 转换到手动审核状态 */
public class AppFetchingToolsProcessor implements AppToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AppFetchingToolsProcessor.class);

    private final MCPGatewayService mcpGatewayService;
    private final ReviewContainerService reviewContainerService;

    /** 构造函数，注入依赖服务
     * 
     * @param mcpGatewayService MCP网关服务
     * @param reviewContainerService 审核容器服务 */
    public AppFetchingToolsProcessor(MCPGatewayService mcpGatewayService,
            ReviewContainerService reviewContainerService) {
        this.mcpGatewayService = mcpGatewayService;
        this.reviewContainerService = reviewContainerService;
    }

    @Override
    public ToolStatus getStatus() {
        return ToolStatus.FETCHING_TOOLS;
    }

    @Override
    public void process(ToolEntity tool) {
        logger.info("工具ID: {} 进入FETCHING_TOOLS状态，开始从审核容器获取工具列表。", tool.getId());

        try {
            // 添加延时以确保部署完成（TODO: 后续可优化为轮询检查部署状态）
            Thread.sleep(10000);

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

            // 存储MCP服务器名称到工具实体
            tool.setMcpServerName(toolName);

            // 获取审核容器连接信息
            logger.info("获取审核容器连接信息用于工具 {} 的审核", toolName);
            ReviewContainerService.ReviewContainerConnection reviewConnection = reviewContainerService
                    .getReviewContainerConnection();

            logger.info("从审核容器 {}:{} 获取工具 {} 的列表", reviewConnection.getIpAddress(), reviewConnection.getPort(),
                    toolName);

            // 调用MCPGatewayService从审核容器获取工具列表
            List<ToolDefinition> toolDefinitions = mcpGatewayService.listToolsFromReviewContainer(toolName,
                    reviewConnection.getIpAddress(), reviewConnection.getPort());

            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                logger.info("从审核容器获取工具列表成功，数量: {}, 工具: {}", toolDefinitions.size(), toolName);

                // 将获取到的工具定义列表设置到ToolEntity中
                tool.setToolList(toolDefinitions);
            } else {
                logger.warn("从审核容器获取工具列表失败或为空: {}", toolName);
                throw new BusinessException("从审核容器获取工具列表失败或为空");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            logger.error("获取工具列表过程中被中断: tool={}", tool.getName(), e);
            throw new BusinessException("获取工具列表过程中被中断: " + e.getMessage(), e);
        } catch (BusinessException e) {
            logger.error("从审核容器获取工具列表失败 {} (ID: {}): {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw e; // 重新抛出BusinessException
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