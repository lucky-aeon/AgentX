package org.xhy.application.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.application.container.dto.ContainerDTO;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.infrastructure.exception.BusinessException;

/** 审核容器服务
 * 
 * 负责管理工具审核所需的容器环境 */
@Service
public class ReviewContainerService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewContainerService.class);

    private final ContainerAppService containerAppService;

    public ReviewContainerService(ContainerAppService containerAppService) {
        this.containerAppService = containerAppService;
    }

    /** 获取审核容器连接信息
     * 
     * @return 审核容器连接信息
     * @throws BusinessException 如果审核容器不可用 */
    public ReviewContainerConnection getReviewContainerConnection() {
        try {
            // 获取或创建审核容器（现在会等待容器完全准备就绪）
            ContainerDTO reviewContainer = containerAppService.getOrCreateReviewContainer();

            // 二次验证容器信息完整性（防御性编程）
            if (reviewContainer.getIpAddress() == null || reviewContainer.getExternalPort() == null) {
                logger.error("审核容器网络信息不完整: ip={}, port={}, status={}", reviewContainer.getIpAddress(),
                        reviewContainer.getExternalPort(), reviewContainer.getStatus());
                throw new BusinessException("审核容器网络配置不完整，容器状态: " + reviewContainer.getStatus());
            }

            logger.info("获取审核容器连接信息: {}:{} ({})", reviewContainer.getIpAddress(), reviewContainer.getExternalPort(),
                    reviewContainer.getStatus());

            return new ReviewContainerConnection(reviewContainer.getIpAddress(), reviewContainer.getExternalPort(),
                    reviewContainer.getId(), reviewContainer.getName());

        } catch (BusinessException e) {
            // 重新抛出业务异常，保持原始错误信息
            throw e;
        } catch (Exception e) {
            logger.error("获取审核容器连接信息失败", e);
            throw new BusinessException("审核容器服务异常: " + e.getMessage());
        }
    }

    /** 检查审核容器健康状态
     * 
     * @return 健康检查结果 */
    public ReviewContainerHealthStatus checkReviewContainerHealth() {
        try {
            ContainerDTO reviewContainer = containerAppService.getOrCreateReviewContainer();

            // 检查容器基本状态
            if (!isContainerHealthy(reviewContainer)) {
                return new ReviewContainerHealthStatus(false, "审核容器状态异常: " + reviewContainer.getStatus(),
                        reviewContainer);
            }

            // 可以添加更多健康检查逻辑，比如网络连通性测试

            return new ReviewContainerHealthStatus(true, "审核容器健康", reviewContainer);

        } catch (Exception e) {
            logger.error("审核容器健康检查失败", e);
            return new ReviewContainerHealthStatus(false, "健康检查失败: " + e.getMessage(), null);
        }
    }

    /** 重新创建审核容器
     * 
     * @return 新的审核容器信息 */
    public ContainerDTO recreateReviewContainer() {
        logger.info("重新创建审核容器");

        try {
            // 这里可以添加删除旧容器的逻辑（如果需要）
            // 目前直接创建新的审核容器
            return containerAppService.createReviewContainer();

        } catch (Exception e) {
            logger.error("重新创建审核容器失败", e);
            throw new BusinessException("重新创建审核容器失败: " + e.getMessage());
        }
    }

    /** 检查容器是否健康 */
    private boolean isContainerHealthy(ContainerDTO container) {
        if (container == null) {
            return false;
        }

        // 检查容器状态是否为运行中
        boolean isRunning = "RUNNING".equals(container.getStatus());

        // 检查必要的网络信息是否存在
        boolean hasNetworkInfo = container.getIpAddress() != null && container.getExternalPort() != null;

        return isRunning && hasNetworkInfo;
    }

    /** 审核容器连接信息 */
    public static class ReviewContainerConnection {
        private final String ipAddress;
        private final Integer port;
        private final String containerId;
        private final String containerName;

        public ReviewContainerConnection(String ipAddress, Integer port, String containerId, String containerName) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.containerId = containerId;
            this.containerName = containerName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public Integer getPort() {
            return port;
        }

        public String getContainerId() {
            return containerId;
        }

        public String getContainerName() {
            return containerName;
        }

        /** 构建容器基础URL */
        public String getBaseUrl() {
            return "http://" + ipAddress + ":" + port;
        }

        @Override
        public String toString() {
            return "ReviewContainerConnection{" + "ipAddress='" + ipAddress + '\'' + ", port=" + port
                    + ", containerId='" + containerId + '\'' + ", containerName='" + containerName + '\'' + '}';
        }
    }

    /** 审核容器健康状态 */
    public static class ReviewContainerHealthStatus {
        private final boolean healthy;
        private final String message;
        private final ContainerDTO container;

        public ReviewContainerHealthStatus(boolean healthy, String message, ContainerDTO container) {
            this.healthy = healthy;
            this.message = message;
            this.container = container;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getMessage() {
            return message;
        }

        public ContainerDTO getContainer() {
            return container;
        }
    }
}