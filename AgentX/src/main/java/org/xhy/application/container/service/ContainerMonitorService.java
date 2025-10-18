package org.xhy.application.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.docker.DockerService;
import org.xhy.infrastructure.entity.Operator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /** 定期检查容器状态 每5分钟执行一次 */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void checkContainerStatus() {
        try {
            List<ContainerEntity> containers = containerDomainService.getMonitoringContainers();

            for (ContainerEntity container : containers) {
                checkSingleContainer(container);
            }

        } catch (Exception e) {
            logger.error("容器状态检查失败", e);
        }
    }

    /** 更新容器资源使用率 每2分钟执行一次 */
    @Scheduled(fixedRate = 120000) // 2分钟
    public void updateContainerStats() {
        try {
            List<ContainerEntity> containers = containerDomainService.getMonitoringContainers();
            logger.debug("开始更新 {} 个容器的资源使用率", containers.size());

            for (ContainerEntity container : containers) {
                updateContainerResourceUsage(container);
            }

        } catch (Exception e) {
            logger.error("容器资源使用率更新失败", e);
        }
    }

    /** 检查单个容器状态（增强版） */
    private void checkSingleContainer(ContainerEntity container) {
        try {
            if (container.getDockerContainerId() == null) {
                logger.warn("容器缺少Docker容器ID，标记为错误状态: containerId={}", container.getId());
                containerDomainService.markContainerError(container.getId(), "缺少Docker容器ID，需要手动恢复", Operator.ADMIN);
                return;
            }

            // 使用增强的Docker状态检查
            DockerService.ContainerActualStatus actualStatus = dockerService
                    .getContainerActualStatus(container.getDockerContainerId());

            if (!actualStatus.exists()) {
                logger.warn("Docker容器不存在，更新数据库状态: containerId={}, dockerId={}", container.getId(),
                        container.getDockerContainerId());
                containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.STOPPED, Operator.ADMIN,
                        null);
                return;
            }

            if (!actualStatus.isRunning()) {
                logger.info("Docker容器未运行，尝试智能恢复: containerId={}, dockerStatus={}", container.getId(),
                        actualStatus.getStatus());

                // 尝试智能恢复：启动容器
                DockerService.ContainerRecoveryResult recoveryResult = dockerService
                        .forceStartContainerIfExists(container.getDockerContainerId());

                if (recoveryResult.isSuccess()) {
                    logger.info("容器自动启动成功: containerId={}", container.getId());
                    containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING,
                            Operator.ADMIN, null);
                } else {
                    logger.warn("容器自动启动失败，更新为停止状态: containerId={}, reason={}", container.getId(),
                            recoveryResult.getMessage());
                    containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.STOPPED,
                            Operator.ADMIN, null);
                }
                return;
            }

            // 容器运行正常，检查网络连通性
            if (container.getIpAddress() != null && container.getExternalPort() != null) {
                boolean networkOk = dockerService.isContainerNetworkAccessible(container.getIpAddress(),
                        container.getExternalPort());
                if (!networkOk) {
                    logger.warn("容器网络连通性异常但Docker运行正常: containerId={}, ip={}:{}", container.getId(),
                            container.getIpAddress(), container.getExternalPort());
                }
            }

            // 确保数据库状态为运行中
            if (!ContainerStatus.RUNNING.equals(container.getStatus())) {
                logger.info("容器Docker运行正常，更新数据库状态: containerId={} {} -> RUNNING", container.getId(),
                        container.getStatus());
                containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.RUNNING, Operator.ADMIN,
                        null);
            }

            logger.debug("容器状态检查完成: containerId={}, status=healthy", container.getId());

        } catch (Exception e) {
            logger.error("检查容器状态失败: containerId={}", container.getId(), e);
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
                containerDomainService.updateResourceUsage(container.getId(), stats.getCpuUsage(),
                        stats.getMemoryUsage());
            }

        } catch (Exception e) {
            logger.debug("更新容器资源使用率失败: {}", container.getName(), e);
        }
    }

    /** 同步单个容器状态（用于启动时同步） */
    private void syncSingleContainerStatus(ContainerEntity container) {
        try {
            if (container.getDockerContainerId() == null) {
                logger.warn("容器缺少Docker容器ID，标记为错误状态: containerId={}", container.getId());
                containerDomainService.markContainerError(container.getId(), "缺少Docker容器ID，需要手动恢复", Operator.ADMIN);
                return;
            }

            DockerService.ContainerActualStatus actualStatus = dockerService
                    .getContainerActualStatus(container.getDockerContainerId());

            if (!actualStatus.exists()) {
                logger.warn("Docker容器不存在，更新数据库状态: containerId={}, dockerId={}", container.getId(),
                        container.getDockerContainerId());
                containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.STOPPED, Operator.ADMIN,
                        null);
                return;
            }

            if (!actualStatus.isRunning()) {
                logger.info("Docker容器未运行，更新数据库状态: containerId={}, dockerStatus={}", container.getId(),
                        actualStatus.getStatus());
                containerDomainService.updateContainerStatus(container.getId(), ContainerStatus.STOPPED, Operator.ADMIN,
                        null);
                return;
            }

            // 容器运行正常，检查网络连通性
            if (container.getIpAddress() != null && container.getExternalPort() != null) {
                boolean networkOk = dockerService.isContainerNetworkAccessible(container.getIpAddress(),
                        container.getExternalPort());
                if (!networkOk) {
                    logger.warn("容器网络连通性异常但Docker运行正常: containerId={}, ip={}:{}", container.getId(),
                            container.getIpAddress(), container.getExternalPort());
                }
            }

            logger.debug("容器状态同步完成: containerId={}, status=healthy", container.getId());

        } catch (Exception e) {
            logger.error("同步容器状态失败: containerId={}", container.getId(), e);
        }
    }

    /** 将Docker状态映射到容器状态 */
    private ContainerStatus mapDockerStatusToContainerStatus(String dockerStatus) {
        switch (dockerStatus.toLowerCase()) {
            case "running" :
                return ContainerStatus.RUNNING;
            case "exited" :
            case "stopped" :
                return ContainerStatus.STOPPED;
            case "created" :
                return ContainerStatus.CREATING;
            case "dead" :
            case "removing" :
                return ContainerStatus.ERROR;
            default :
                return ContainerStatus.ERROR;
        }
    }
}