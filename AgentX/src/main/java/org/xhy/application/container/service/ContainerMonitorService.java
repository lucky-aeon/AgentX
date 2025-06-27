package org.xhy.application.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.docker.DockerService;

import java.util.List;

/** 容器监控服务 */
@Service
public class ContainerMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerMonitorService.class);

    private final ContainerDomainService containerDomainService;
    private final DockerService dockerService;

    public ContainerMonitorService(ContainerDomainService containerDomainService, DockerService dockerService) {
        this.containerDomainService = containerDomainService;
        this.dockerService = dockerService;
    }

    /** 定期检查容器状态
     * 每5分钟执行一次 */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkContainerStatus() {
        try {
            List<ContainerEntity> containers = containerDomainService.getMonitoringContainers();
            logger.info("开始检查 {} 个容器的状态", containers.size());

            for (ContainerEntity container : containers) {
                checkSingleContainer(container);
            }

            logger.info("容器状态检查完成");
        } catch (Exception e) {
            logger.error("容器状态检查失败", e);
        }
    }

    /** 更新容器资源使用率
     * 每2分钟执行一次 */
    @Scheduled(fixedRate = 120000) // 2分钟
    public void updateContainerStats() {
        try {
            List<ContainerEntity> containers = containerDomainService.getMonitoringContainers();
            logger.debug("开始更新 {} 个容器的资源使用率", containers.size());

            for (ContainerEntity container : containers) {
                updateContainerResourceUsage(container);
            }

            logger.debug("容器资源使用率更新完成");
        } catch (Exception e) {
            logger.error("容器资源使用率更新失败", e);
        }
    }

    /** 检查单个容器状态 */
    private void checkSingleContainer(ContainerEntity container) {
        try {
            if (container.getDockerContainerId() == null) {
                return;
            }

            // 检查Docker容器是否存在
            if (!dockerService.containerExists(container.getDockerContainerId())) {
                logger.warn("Docker容器不存在: {}", container.getDockerContainerId());
                containerDomainService.markContainerError(
                        container.getId(), "Docker容器不存在", null);
                return;
            }

            // 获取Docker容器状态
            String dockerStatus = dockerService.getContainerStatus(container.getDockerContainerId());
            ContainerStatus expectedStatus = mapDockerStatusToContainerStatus(dockerStatus);

            // 如果状态不一致，更新数据库中的状态
            if (!expectedStatus.equals(container.getStatus())) {
                logger.info("容器状态不一致，更新: {} {} -> {}", 
                           container.getName(), container.getStatus(), expectedStatus);
                containerDomainService.updateContainerStatus(
                        container.getId(), expectedStatus, null, null);
            }

        } catch (Exception e) {
            logger.error("检查容器状态失败: {}", container.getName(), e);
        }
    }

    /** 更新容器资源使用率 */
    private void updateContainerResourceUsage(ContainerEntity container) {
        try {
            if (container.getDockerContainerId() == null || !container.isRunning()) {
                return;
            }

            DockerService.ContainerStats stats = dockerService.getContainerStats(container.getDockerContainerId());
            if (stats != null) {
                containerDomainService.updateResourceUsage(
                        container.getId(), stats.getCpuUsage(), stats.getMemoryUsage());
            }

        } catch (Exception e) {
            logger.debug("更新容器资源使用率失败: {}", container.getName(), e);
        }
    }

    /** 将Docker状态映射到容器状态 */
    private ContainerStatus mapDockerStatusToContainerStatus(String dockerStatus) {
        switch (dockerStatus.toLowerCase()) {
            case "running":
                return ContainerStatus.RUNNING;
            case "exited":
            case "stopped":
                return ContainerStatus.STOPPED;
            case "created":
                return ContainerStatus.CREATING;
            case "dead":
            case "removing":
                return ContainerStatus.ERROR;
            default:
                return ContainerStatus.ERROR;
        }
    }
}