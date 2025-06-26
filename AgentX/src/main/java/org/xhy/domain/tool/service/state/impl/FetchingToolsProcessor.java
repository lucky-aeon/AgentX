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

/** 获取工具列表处理器 */
public class FetchingToolsProcessor implements ToolStateProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FetchingToolsProcessor.class);

    private final MCPGatewayService mcpGatewayService;

    /** 构造函数，注入MCPGatewayService
     * 
     * @param mcpGatewayService MCP网关服务 */
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
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info("工具ID: {} 进入FETCHING_TOOLS状态，开始获取工具列表。", tool.getId());
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

            // 重试机制获取工具列表
            List<ToolDefinition> toolDefinitions = fetchToolsWithRetry(toolName, tool.getId());

            // 将获取到的工具定义列表设置到ToolEntity中
            tool.setToolList(toolDefinitions);

            logger.info("成功获取到工具 {} 的列表，共 {} 个定义。", toolName, toolDefinitions != null ? toolDefinitions.size() : 0);

        } catch (BusinessException e) {
            logger.error("获取工具列表失败 {} (ID: {}): {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("获取工具列表 {} (ID: {}) 过程中发生意外错误: {}", tool.getName(), tool.getId(), e.getMessage(), e);
            throw new BusinessException("获取工具列表过程中发生意外错误: " + e.getMessage(), e); // Wrap unexpected exceptions
        }
    }

    /** 重试机制获取工具列表
     * 
     * @param toolName 工具名称
     * @param toolId 工具ID
     * @return 工具定义列表
     * @throws BusinessException 当所有重试都失败时抛出异常 */
    private List<ToolDefinition> fetchToolsWithRetry(String toolName, String toolId) {
        int maxAttempts = 3;
        int currentAttempt = 1;
        Exception lastException = null;

        while (currentAttempt <= maxAttempts) {
            try {
                logger.info("第 {} 次尝试从MCP Gateway获取工具 {} 的列表", currentAttempt, toolName);
                return mcpGatewayService.listTools(toolName);
            } catch (Exception e) {
                lastException = e;
                logger.warn("第 {} 次获取工具 {} 列表失败: {}", currentAttempt, toolName, e.getMessage());
                
                if (currentAttempt < maxAttempts) {
                    try {
                        // 重试间隔递增: 第1次失败后等待1秒，第2次失败后等待2秒
                        long delayMs = currentAttempt * 1000L;
                        logger.info("等待 {} 毫秒后进行第 {} 次重试", delayMs, currentAttempt + 1);
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException("重试过程中被中断", ie);
                    }
                }
                currentAttempt++;
            }
        }

        // 所有重试都失败，抛出异常
        String errorMessage = String.format("获取工具 %s (ID: %s) 列表失败，已重试 %d 次", toolName, toolId, maxAttempts);
        logger.error(errorMessage);
        throw new BusinessException(errorMessage, lastException);
    }

    @Override
    public ToolStatus getNextStatus() {
        return ToolStatus.MANUAL_REVIEW;
    }
}