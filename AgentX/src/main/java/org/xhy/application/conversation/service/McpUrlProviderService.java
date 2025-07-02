package org.xhy.application.conversation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.container.service.ContainerAppService;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.tool.service.ToolDomainService;
import org.xhy.domain.tool.model.ToolEntity;
import org.xhy.infrastructure.mcp_gateway.MCPGatewayService;
import org.xhy.infrastructure.exception.BusinessException;
import org.xhy.infrastructure.utils.JsonUtils;

import java.util.Map;

/** MCP URL提供服务 负责协调容器管理和URL构建 */
@Service
public class McpUrlProviderService {

    private static final Logger logger = LoggerFactory.getLogger(McpUrlProviderService.class);

    private final MCPGatewayService mcpGatewayService;
    private final ContainerAppService containerAppService;
    private final ToolDomainService toolDomainService;

    public McpUrlProviderService(MCPGatewayService mcpGatewayService, ContainerAppService containerAppService,
            ToolDomainService toolDomainService) {
        this.mcpGatewayService = mcpGatewayService;
        this.containerAppService = containerAppService;
        this.toolDomainService = toolDomainService;
    }

    /** 智能获取SSE URL：自动判断工具类型并选择连接策略
     * 
     * @param mcpServerName 工具服务名称
     * @param userId 用户ID（可选，用户工具必需）
     * @return 对应的SSE连接URL */
    public String getSSEUrl(String mcpServerName, String userId) {
        // 1. 自动判断工具类型
        boolean isGlobalTool = isGlobalTool(mcpServerName, userId);

        if (isGlobalTool) {
            // 全局工具：使用yml配置的全局Gateway
            return mcpGatewayService.buildGlobalSSEUrl(mcpServerName);
        }

        // 用户工具：需要用户容器
        return buildUserContainerSSEUrl(mcpServerName, userId);
    }

    /** 获取MCP工具的SSE URL（包含容器自动创建和启动）
     * 
     * @param mcpServerName 工具服务名称
     * @param userId 用户ID
     * @return SSE连接URL */
    public String getMcpToolUrl(String mcpServerName, String userId) {
        try {
            return getSSEUrl(mcpServerName, userId);
        } catch (Exception e) {
            logger.error("获取MCP工具URL失败: userId={}, tool={}", userId, mcpServerName, e);
            throw new BusinessException("无法连接工具：" + mcpServerName + " - " + e.getMessage());
        }
    }

    /** 判断是否为全局工具 */
    private boolean isGlobalTool(String mcpServerName, String userId) {
        try {
            ToolEntity tool = toolDomainService.getToolByServerNameForUsage(mcpServerName, userId);
            return tool != null && tool.isGlobal();
        } catch (Exception e) {
            logger.warn("无法判断工具类型，默认为用户工具: {}", mcpServerName, e);
            return false; // 默认为用户工具，需要用户容器
        }
    }

    /** 构建用户容器工具SSE URL */
    private String buildUserContainerSSEUrl(String mcpServerName, String userId) {
        try {
            logger.info("准备用户容器工具连接: userId={}, tool={}", userId, mcpServerName);

            // 1. 确保用户容器就绪（自动创建和启动）
            ContainerDTO containerInfo = ensureUserContainerReady(userId);

            // 2. 构建容器SSE URL
            String sseUrl = mcpGatewayService.buildUserContainerUrl(mcpServerName, containerInfo.getIpAddress(),
                    containerInfo.getExternalPort());

            // 3. 部署工具
            deployTool(containerInfo, mcpServerName, userId);

            logger.info("用户容器工具连接就绪: userId={}, url={}", userId, maskSensitiveInfo(sseUrl));
            return sseUrl;

        } catch (Exception e) {
            logger.error("构建用户容器SSE URL失败: userId={}, tool={}", userId, mcpServerName, e);
            throw new BusinessException("无法连接用户工具：" + e.getMessage());
        }
    }

    /** 确保用户容器就绪（自动创建和启动） */
    private ContainerDTO ensureUserContainerReady(String userId) {
        try {
            // ContainerAppService.getUserContainer() 已经包含自动创建和启动逻辑
            ContainerDTO userContainer = containerAppService.getUserContainer(userId);

            // 如果容器正在创建中，等待其完成
            if (ContainerStatus.CREATING.equals(userContainer.getStatus())) {
                logger.info("容器正在创建中，等待完成: userId={}", userId);
                userContainer = waitForUserContainerReady(userContainer.getId(), userId);
            }

            // 最终验证容器状态
            if (!isContainerHealthy(userContainer)) {
                throw new BusinessException("用户容器准备失败，状态异常: " + userContainer.getStatus());
            }

            return userContainer;
        } catch (Exception e) {
            logger.error("准备用户容器失败: userId={}", userId, e);
            throw new BusinessException("用户容器准备失败: " + e.getMessage());
        }
    }

    /** 检查容器是否健康 */
    private boolean isContainerHealthy(ContainerDTO container) {
        if (container == null) {
            return false;
        }

        // 检查容器状态是否为运行中
        boolean isRunning = ContainerStatus.RUNNING.equals(container.getStatus());

        // 检查必要的网络信息是否存在
        boolean hasNetworkInfo = container.getIpAddress() != null && container.getExternalPort() != null;

        return isRunning && hasNetworkInfo;
    }

    /** 部署工具到用户容器 */
    private void deployTool(ContainerDTO container, String toolName, String userId) {
        try {
            ToolEntity tool = toolDomainService.getToolByServerNameForUsage(toolName, userId);
            if (tool == null) {
                logger.warn("无法找到工具定义: {}", toolName);
                return;
            }

            String installCommandJson = convertInstallCommand(tool.getInstallCommand());
            mcpGatewayService.deployTool(installCommandJson, container.getIpAddress(), container.getExternalPort());

            Thread.sleep(1000L);
            logger.debug("工具 {} 部署请求已发送到用户容器", toolName);

        } catch (Exception e) {
            logger.warn("部署容器内工具失败: tool={}, error={}", toolName, e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    /** 将工具安装命令转换为JSON字符串 */
    private String convertInstallCommand(Map<String, Object> installCommand) {
        try {
            return JsonUtils.toJsonString(installCommand);
        } catch (Exception e) {
            throw new BusinessException("转换安装命令失败: " + e.getMessage());
        }
    }

    /** 等待用户容器准备就绪
     * 
     * @param containerId 容器ID
     * @param userId 用户ID
     * @return 包含完整网络信息的容器DTO
     * @throws BusinessException 如果等待超时或容器创建失败 */
    private ContainerDTO waitForUserContainerReady(String containerId, String userId) {
        int maxRetries = 30; // 最多等待30秒
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(1000); // 等待1秒

                ContainerDTO container = containerAppService.getUserContainer(userId);

                // 检查容器是否处于错误状态
                if (ContainerStatus.ERROR.equals(container.getStatus())) {
                    logger.error("容器创建失败: containerId={}, status={}", containerId, container.getStatus());
                    throw new BusinessException("容器创建失败，请检查Docker环境");
                }

                // 检查容器是否已经健康运行
                if (isContainerHealthy(container)) {
                    logger.info("用户容器准备就绪: userId={}, containerId={}, ip={}, port={}", userId, containerId, 
                            container.getIpAddress(), container.getExternalPort());
                    return container;
                }

                retryCount++;
                logger.debug("等待用户容器准备就绪: userId={}, containerId={}, retry={}/{}, status={}, ip={}", 
                        userId, containerId, retryCount, maxRetries, container.getStatus(), container.getIpAddress());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("等待用户容器准备被中断");
            } catch (BusinessException e) {
                // 如果是业务异常，直接抛出
                throw e;
            } catch (Exception e) {
                logger.warn("检查用户容器状态时出错: userId={}, retry={}", userId, retryCount, e);
                retryCount++;
            }
        }

        // 超时后尝试获取最新状态
        try {
            ContainerDTO container = containerAppService.getUserContainer(userId);
            logger.warn("用户容器等待超时，当前状态: userId={}, containerId={}, status={}, ip={}, port={}", 
                    userId, containerId, container.getStatus(), container.getIpAddress(), container.getExternalPort());
        } catch (Exception e) {
            logger.error("获取用户容器最终状态失败: userId={}", userId, e);
        }

        throw new BusinessException("用户容器创建超时，请稍后重试或检查Docker环境");
    }

    /** 屏蔽敏感信息 */
    private String maskSensitiveInfo(String url) {
        if (url == null)
            return null;
        return url.replaceAll("api_key=[^&]*", "api_key=***");
    }
}