package org.xhy.application.tool.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.domain.tool.constant.ToolStatus;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.domain.tool.service.ToolStateDomainService;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.github.GitHubService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JsonUtils;
import org.xhy.domain.tool.model.config.ToolDefinition;

import java.util.List;
import java.util.Map;

/** 工具状态应用服务 - 协调工具状态处理与外部服务 */
@Service
public class ToolStateAppService {

    private static final Logger logger = LoggerFactory.getLogger(ToolStateAppService.class);

    private final ToolStateDomainService toolStateDomainService;
    private final MCPGatewayService mcpGatewayService;
    private final GitHubService gitHubService;

    public ToolStateAppService(ToolStateDomainService toolStateDomainService, MCPGatewayService mcpGatewayService,
            GitHubService gitHubService) {
        this.toolStateDomainService = toolStateDomainService;
        this.mcpGatewayService = mcpGatewayService;
        this.gitHubService = gitHubService;
    }

    /** 处理工具部署 */
    public void processDeploying(ToolEntity tool) {
        try {
            logger.info("开始部署工具: {}", tool.getName());

            String installCommandJson = JsonUtils.toJsonString(tool.getInstallCommand());

            // 调用MCPGatewayService进行部署
            boolean success = mcpGatewayService.deployTool(installCommandJson);

            if (success) {
                logger.info("工具部署成功，转换状态到获取工具列表: {}", tool.getName());
                // MCPGatewayService.deployTool should throw BusinessException on API errors,
                // If it doesn't throw, we consider it successful and transition to fetching tools
                toolStateDomainService.transitionToStatus(tool, ToolStatus.FETCHING_TOOLS);
            } else {
                logger.error("工具部署失败，状态保持为部署中: {}", tool.getName());
                throw new BusinessException("工具部署失败");
            }
        } catch (Exception e) {
            logger.error("工具部署过程中发生异常: tool={}", tool.getName(), e);
            // Catch BusinessException from MCPGatewayService or internal checks
            toolStateDomainService.transitionToStatus(tool, ToolStatus.FAILED);
            throw new BusinessException("工具部署失败: " + e.getMessage());
        }
    }

    /** 处理工具列表获取 */
    public void processFetchingTools(ToolEntity tool) {
        try {
            logger.info("开始获取工具列表: {}", tool.getName());

            // 调用MCPGatewayService获取工具列表
            List<ToolDefinition> toolDefinitions = mcpGatewayService.listTools(tool.getMcpServerName());

            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                logger.info("工具列表获取成功，数量: {}, 工具: {}", toolDefinitions.size(), tool.getName());

                // 将工具定义存储到工具实体（如果需要）
                String toolDefinitionsJson = JsonUtils.toJsonString(toolDefinitions);
                tool.setToolList(toolDefinitions);

                // 转换状态到已通过
                toolStateDomainService.transitionToStatus(tool, ToolStatus.APPROVED);
            } else {
                logger.warn("工具列表获取失败或为空: {}", tool.getName());
                toolStateDomainService.transitionToStatus(tool, ToolStatus.FAILED);
                throw new BusinessException("工具列表获取失败或为空");
            }
        } catch (Exception e) {
            logger.error("获取工具列表过程中发生异常: tool={}", tool.getName(), e);
            toolStateDomainService.transitionToStatus(tool, ToolStatus.FAILED);
            throw new BusinessException("工具列表获取失败: " + e.getMessage());
        }
    }

    /** 委托给Domain层处理状态转换 */
    public void processToolState(ToolEntity tool) {
        ToolStatus currentStatus = tool.getStatus();

        switch (currentStatus) {
            case DEPLOYING :
                processDeploying(tool);
                break;
            case FETCHING_TOOLS :
                processFetchingTools(tool);
                break;
            default :
                // 其他状态由Domain层直接处理
                toolStateDomainService.processToolState(tool);
                break;
        }
    }
}