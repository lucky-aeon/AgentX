package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.container.service.ContainerAppService;
import org.xhy.application.container.service.McpGatewayService;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.infrastructure.exception.BusinessException;

import java.util.List;

/** 容器集成应用服务 - 用于对话Agent流程 */
@Service
public class ContainerIntegrationAppService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerIntegrationAppService.class);

    private final ContainerAppService containerAppService;
    private final McpGatewayService mcpGatewayService;

    public ContainerIntegrationAppService(ContainerAppService containerAppService, 
                                        McpGatewayService mcpGatewayService) {
        this.containerAppService = containerAppService;
        this.mcpGatewayService = mcpGatewayService;
    }

    /** 检查用户容器状态（对话Agent流程第一步）
     * 
     * @param userId 用户ID
     * @return 容器状态检查结果 */
    public ContainerCheckResult checkUserContainerStatus(String userId) {
        try {
            logger.info("开始检查用户容器状态: {}", userId);

            // 1. 检查用户是否有容器
            ContainerDTO container = containerAppService.getUserContainer(userId);
            if (container == null) {
                logger.info("用户没有容器，需要创建: {}", userId);
                return new ContainerCheckResult(false, "用户容器不存在", false, null);
            }

            // 2. 检查容器健康状态
            ContainerAppService.ContainerHealthStatus healthStatus = 
                    containerAppService.checkUserContainerHealth(userId);

            if (!healthStatus.isHealthy()) {
                logger.warn("用户容器不健康: {} - {}", userId, healthStatus.getMessage());
                return new ContainerCheckResult(true, healthStatus.getMessage(), false, container);
            }

            // 3. 检查MCP网关是否可用
            McpGatewayService.ContainerHealthResult gatewayHealth = 
                    mcpGatewayService.checkAndCreateUserContainer(userId);

            if (!gatewayHealth.isHealthy()) {
                logger.warn("MCP网关不可用: {} - {}", userId, gatewayHealth.getMessage());
                return new ContainerCheckResult(true, gatewayHealth.getMessage(), false, container);
            }

            logger.info("用户容器状态正常: {}", userId);
            return new ContainerCheckResult(true, "容器状态正常", true, container);

        } catch (Exception e) {
            logger.error("检查用户容器状态失败: {}", userId, e);
            return new ContainerCheckResult(false, "容器状态检查失败: " + e.getMessage(), false, null);
        }
    }

    /** 创建用户容器（如果不存在）
     * 
     * @param userId 用户ID
     * @return 创建结果 */
    public ContainerCreationResult createUserContainerIfNeeded(String userId) {
        try {
            logger.info("开始为用户创建容器: {}", userId);

            ContainerDTO container = containerAppService.createUserContainer(userId);
            
            logger.info("用户容器创建成功: {} - {}", userId, container.getId());
            return new ContainerCreationResult(true, "容器创建成功", container);

        } catch (Exception e) {
            logger.error("创建用户容器失败: {}", userId, e);
            return new ContainerCreationResult(false, "容器创建失败: " + e.getMessage(), null);
        }
    }

    /** 部署工具到用户容器工作区
     * 
     * @param userId 用户ID
     * @param toolConfigs 工具配置列表
     * @return 部署结果 */
    public ToolDeploymentResult deployToolsToUserWorkspace(String userId, 
                                                          List<McpGatewayService.ToolConfig> toolConfigs) {
        try {
            logger.info("开始部署工具到用户容器: {} - {} 个工具", userId, toolConfigs.size());

            McpGatewayService.ToolDeploymentResult deployResult = 
                    mcpGatewayService.deployToolsToUserContainer(userId, toolConfigs);

            if (deployResult.isSuccess()) {
                logger.info("工具部署成功: {} - 成功: {}, 失败: {}", 
                           userId, deployResult.getSuccessCount(), deployResult.getFailedCount());
            } else {
                logger.warn("工具部署部分失败: {} - 成功: {}, 失败: {}", 
                           userId, deployResult.getSuccessCount(), deployResult.getFailedCount());
            }

            return new ToolDeploymentResult(
                    deployResult.isSuccess(),
                    deployResult.getMessage(),
                    deployResult.getSuccessCount(),
                    deployResult.getFailedCount(),
                    deployResult.getErrors()
            );

        } catch (Exception e) {
            logger.error("部署工具到用户容器失败: {}", userId, e);
            return new ToolDeploymentResult(false, "工具部署失败: " + e.getMessage(), 0, 0, List.of(e.getMessage()));
        }
    }

    /** 获取用户容器中的工具状态
     * 
     * @param userId 用户ID
     * @return 工具状态列表 */
    public List<McpGatewayService.ToolStatus> getUserContainerToolsStatus(String userId) {
        try {
            return mcpGatewayService.getDeployedToolsStatus(userId);
        } catch (Exception e) {
            logger.error("获取用户容器工具状态失败: {}", userId, e);
            throw new BusinessException("获取工具状态失败: " + e.getMessage());
        }
    }

    /** 检查工具是否需要部署到用户容器
     * 
     * @param userId 用户ID
     * @param requiredTools 需要的工具列表
     * @return 需要部署的工具配置 */
    public List<McpGatewayService.ToolConfig> checkToolsNeedDeployment(String userId, 
                                                                       List<String> requiredTools) {
        try {
            List<McpGatewayService.ToolStatus> deployedTools = getUserContainerToolsStatus(userId);
            
            // 这里应该实现具体的逻辑来判断哪些工具需要部署
            // 为简化，这里返回空列表，实际应该根据部署状态进行比较
            
            logger.info("检查工具部署状态: 用户={}, 需要={}, 已部署={}", 
                       userId, requiredTools.size(), deployedTools.size());
            
            // TODO: 实现具体的工具对比逻辑
            return List.of();

        } catch (Exception e) {
            logger.error("检查工具部署状态失败: {}", userId, e);
            throw new BusinessException("检查工具状态失败: " + e.getMessage());
        }
    }

    /** 容器状态检查结果 */
    public static class ContainerCheckResult {
        private final boolean containerExists;
        private final String message;
        private final boolean healthy;
        private final ContainerDTO container;

        public ContainerCheckResult(boolean containerExists, String message, boolean healthy, ContainerDTO container) {
            this.containerExists = containerExists;
            this.message = message;
            this.healthy = healthy;
            this.container = container;
        }

        public boolean isContainerExists() {
            return containerExists;
        }

        public String getMessage() {
            return message;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public ContainerDTO getContainer() {
            return container;
        }
    }

    /** 容器创建结果 */
    public static class ContainerCreationResult {
        private final boolean success;
        private final String message;
        private final ContainerDTO container;

        public ContainerCreationResult(boolean success, String message, ContainerDTO container) {
            this.success = success;
            this.message = message;
            this.container = container;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ContainerDTO getContainer() {
            return container;
        }
    }

    /** 工具部署结果 */
    public static class ToolDeploymentResult {
        private final boolean success;
        private final String message;
        private final int successCount;
        private final int failedCount;
        private final List<String> errors;

        public ToolDeploymentResult(boolean success, String message, int successCount, int failedCount, List<String> errors) {
            this.success = success;
            this.message = message;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}