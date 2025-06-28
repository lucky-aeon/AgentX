package org.xhy.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.docker.DockerService;
import org.xhy.infrastructure.entity.Operator;

import java.util.List;

/** 容器关闭监听器 在应用关闭时自动停止所有运行中的容器以节省资源 */
@Component
public class ContainerShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ContainerShutdownListener.class);

    private final ContainerDomainService containerDomainService;
    private final DockerService dockerService;

    public ContainerShutdownListener(ContainerDomainService containerDomainService, DockerService dockerService) {
        this.containerDomainService = containerDomainService;
        this.dockerService = dockerService;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("应用正在关闭，开始停止所有运行中的容器以节省资源...");

        try {
            // 获取所有运行中的容器
            List<ContainerEntity> runningContainers = containerDomainService.getMonitoringContainers();

            if (runningContainers.isEmpty()) {
                logger.info("没有找到运行中的容器");
                return;
            }

            logger.info("找到 {} 个运行中的容器，开始停止...", runningContainers.size());

            int stoppedCount = 0;
            int failedCount = 0;

            for (ContainerEntity container : runningContainers) {
                try {
                    stopContainer(container);
                    stoppedCount++;
                    logger.info("已停止容器: {} (Docker ID: {})", container.getName(), container.getDockerContainerId());
                } catch (Exception e) {
                    failedCount++;
                    logger.error("停止容器失败: {} (Docker ID: {})", container.getName(), container.getDockerContainerId(),
                            e);
                }
            }

            logger.info("容器停止完成 - 成功: {}, 失败: {}", stoppedCount, failedCount);

        } catch (Exception e) {
            logger.error("停止容器过程中发生错误", e);
        }
    }

    /** 停止单个容器
     * 
     * @param container 容器实体 */
    private void stopContainer(ContainerEntity container) {
        String dockerContainerId = container.getDockerContainerId();

        if (dockerContainerId == null || dockerContainerId.trim().isEmpty()) {
            logger.warn("容器 {} 没有Docker容器ID，跳过停止操作", container.getName());
            return;
        }

        try {
            // 停止Docker容器
            dockerService.stopContainer(dockerContainerId);

            // 更新数据库状态
            containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.STOPPED, Operator.ADMIN,
                    dockerContainerId);

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响其他容器的停止
            logger.error("停止容器失败: {}", container.getName(), e);

            // 尝试将容器标记为错误状态
            try {
                containerDomainService.markContainerError(container.getId(), "应用关闭时停止容器失败: " + e.getMessage(),
                        Operator.ADMIN);
            } catch (Exception updateException) {
                logger.error("更新容器错误状态失败: {}", container.getName(), updateException);
            }

            throw e;
        }
    }
}