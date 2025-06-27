package org.xhy.application.container.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.service.ContainerDomainService;
import org.xhy.infrastructure.docker.DockerService;
import org.xhy.infrastructure.entity.Operator;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 容器自动清理服务
 * 
 * 清理策略：
 * - 1天不使用 -> 暂停容器
 * - 5天不使用 -> 销毁容器
 */
@Service
public class ContainerCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerCleanupService.class);

    private final ContainerDomainService containerDomainService;
    private final DockerService dockerService;

    public ContainerCleanupService(ContainerDomainService containerDomainService, 
                                 DockerService dockerService) {
        this.containerDomainService = containerDomainService;
        this.dockerService = dockerService;
    }

    /**
     * 定时清理空闲容器
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    @Transactional
    public void cleanupIdleContainers() {
        try {
            logger.info("开始执行容器自动清理任务");
            
            // 1. 暂停1天未使用的运行中容器
            suspendIdleContainers();
            
            // 2. 销毁5天未使用的容器
            deleteAbandonedContainers();
            
            logger.info("容器自动清理任务执行完成");
            
        } catch (Exception e) {
            logger.error("容器自动清理任务执行失败", e);
        }
    }

    /**
     * 暂停1天未使用的运行中容器
     */
    private void suspendIdleContainers() {
        // 直接查询需要暂停的容器（运行中且1天未访问）
        List<ContainerEntity> containersToSuspend = containerDomainService.getContainersNeedingSuspension();
        
        int suspendedCount = 0;
        for (ContainerEntity container : containersToSuspend) {
            try {
                suspendContainer(container);
                suspendedCount++;
                logger.info("暂停空闲容器: {} (用户: {}, 最后访问: {})", 
                          container.getName(), container.getUserId(), container.getLastAccessedAt());
            } catch (Exception e) {
                logger.error("暂停容器失败: {}", container.getName(), e);
            }
        }
        
        if (suspendedCount > 0) {
            logger.info("本次暂停了 {} 个空闲容器", suspendedCount);
        } else {
            logger.debug("没有发现需要暂停的空闲容器");
        }
    }

    /**
     * 销毁5天未使用的容器
     */
    private void deleteAbandonedContainers() {
        // 直接查询需要删除的容器（5天未访问）
        List<ContainerEntity> containersToDelete = containerDomainService.getContainersNeedingDeletion();
        
        int deletedCount = 0;
        for (ContainerEntity container : containersToDelete) {
            try {
                deleteContainer(container);
                deletedCount++;
                logger.info("销毁废弃容器: {} (用户: {}, 最后访问: {})", 
                          container.getName(), container.getUserId(), container.getLastAccessedAt());
            } catch (Exception e) {
                logger.error("销毁容器失败: {}", container.getName(), e);
            }
        }
        
        if (deletedCount > 0) {
            logger.info("本次销毁了 {} 个废弃容器", deletedCount);
        } else {
            logger.debug("没有发现需要销毁的废弃容器");
        }
    }

    /**
     * 暂停单个容器
     */
    private void suspendContainer(ContainerEntity container) {
        // 1. 停止Docker容器
        if (container.getDockerContainerId() != null) {
            dockerService.stopContainer(container.getDockerContainerId());
            logger.debug("Docker容器已停止: {}", container.getDockerContainerId());
        }
        
        // 2. 更新容器状态为SUSPENDED
        containerDomainService.updateContainerStatus(
            container.getId(), 
            ContainerStatus.SUSPENDED, 
            Operator.ADMIN, 
            null
        );
    }

    /**
     * 销毁单个容器
     */
    private void deleteContainer(ContainerEntity container) {
        // 1. 删除Docker容器
        if (container.getDockerContainerId() != null) {
            try {
                dockerService.removeContainer(container.getDockerContainerId(), true);
                logger.debug("Docker容器已删除: {}", container.getDockerContainerId());
            } catch (Exception e) {
                logger.warn("删除Docker容器失败，继续删除数据库记录: {}", container.getDockerContainerId(), e);
            }
        }
        
        // 2. 物理删除容器记录
        containerDomainService.physicalDeleteContainer(container.getId());
    }

    /**
     * 手动触发清理（用于测试）
     */
    public void manualCleanup() {
        logger.info("手动触发容器清理任务");
        cleanupIdleContainers();
    }
}