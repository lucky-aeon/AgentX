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
            // 全局工具：使用审核容器
            return buildReviewContainerSSEUrl(mcpServerName);
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

    /** 构建审核容器工具SSE URL */
    private String buildReviewContainerSSEUrl(String mcpServerName) {
        try {
            logger.info("准备审核容器工具连接: tool={}", mcpServerName);

            // 1. 确保审核容器就绪（自动创建和启动）
            ContainerDTO containerInfo = ensureReviewContainerReady();

            // 2. 构建容器SSE URL
            String sseUrl = mcpGatewayService.buildUserContainerUrl(mcpServerName, containerInfo.getIpAddress(),
                    containerInfo.getExternalPort());

            logger.info("审核容器工具连接就绪: tool={}, url={}", mcpServerName, maskSensitiveInfo(sseUrl));
            return sseUrl;

        } catch (Exception e) {
            logger.error("构建审核容器SSE URL失败: tool={}", mcpServerName, e);
            throw new BusinessException("无法连接全局工具：" + e.getMessage());
        }
    }

    /** 确保用户容器就绪（自动创建和启动） */
    private ContainerDTO ensureUserContainerReady(String userId) {
        try {
            // ContainerAppService.getUserContainer() 已经包含自动创建和启动逻辑
            ContainerDTO userContainer = containerAppService.getUserContainer(userId);

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

    /** 确保审核容器就绪（自动创建和启动） */
    private ContainerDTO ensureReviewContainerReady() {
        try {
            // ContainerAppService.getReviewContainer() 已经包含自动创建和启动逻辑
            ContainerDTO reviewContainer = containerAppService.getOrCreateReviewContainer();

            // 最终验证容器状态
            if (!isContainerHealthy(reviewContainer)) {
                throw new BusinessException("审核容器准备失败，状态异常: " + reviewContainer.getStatus());
            }

            return reviewContainer;
        } catch (Exception e) {
            logger.error("准备审核容器失败", e);
            throw new BusinessException("审核容器准备失败: " + e.getMessage());
        }
    }

    /** 部署全局工具到审核容器 */
    private void deployGlobalTool(ContainerDTO container, String toolName) {
        try {
            // 对于全局工具，暂时跳过部署步骤，因为全局工具应该已经在审核容器中预安装
            logger.debug("全局工具 {} 连接到审核容器，跳过部署步骤（应已预安装）", toolName);

        } catch (Exception e) {
            logger.warn("处理审核容器内全局工具时出错: tool={}, error={}", toolName, e.getMessage());
            // 不抛出异常，避免影响主流程
        }
    }

    /** 屏蔽敏感信息 */
    private String maskSensitiveInfo(String url) {
        if (url == null)
            return null;
        return url.replaceAll("api_key=[^&]*", "api_key=***");
    }
}