package org.xhy.domain.container.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.domain.container.model.ContainerEntity;
import org.xhy.domain.container.repository.ContainerRepository;
import org.xhy.infrastructure.entity.Operator;
import org.xhy.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/** 容器领域服务 */
@Service
public class ContainerDomainService {

    private final ContainerRepository containerRepository;
    private final Random random = new Random();

    public ContainerDomainService(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    /** 创建用户容器
     * 
     * @param userId 用户ID
     * @param containerName 容器名称
     * @param image 镜像名称
     * @param internalPort 内部端口
     * @param volumePath 数据卷路径
     * @return 容器实体 */
    public ContainerEntity createUserContainer(String userId, String containerName, String image, Integer internalPort,
            String volumePath) {
        // 检查用户是否已有容器
        ContainerEntity existingContainer = containerRepository.findByUserIdAndType(userId, ContainerType.USER);
        if (existingContainer != null && existingContainer.isOperatable()) {
            throw new BusinessException("用户已存在容器，无法重复创建");
        }

        // 分配外部端口
        Integer externalPort = allocateExternalPort();

        // 创建容器实体
        ContainerEntity container = new ContainerEntity();
        container.setName(containerName);
        container.setUserId(userId);
        container.setType(ContainerType.USER);
        container.setStatus(ContainerStatus.CREATING);
        container.setImage(image);
        container.setInternalPort(internalPort);
        container.setExternalPort(externalPort);
        container.setVolumePath(volumePath);
        container.setLastAccessedAt(LocalDateTime.now()); // 设置初始访问时间

        containerRepository.insert(container);
        return container;
    }

    /** 创建审核容器
     * 
     * @param containerName 容器名称
     * @param image 镜像名称
     * @param internalPort 内部端口
     * @param volumePath 数据卷路径
     * @return 容器实体 */
    public ContainerEntity createReviewContainer(String containerName, String image, Integer internalPort,
            String volumePath) {
        // 检查是否已有审核容器
        ContainerEntity existingContainer = findReviewContainer();
        if (existingContainer != null && existingContainer.isOperatable()) {
            throw new BusinessException("系统已存在审核容器，无法重复创建");
        }

        // 分配外部端口
        Integer externalPort = allocateExternalPort();

        // 创建审核容器实体
        ContainerEntity container = new ContainerEntity();
        container.setName(containerName);
        container.setUserId("SYSTEM"); // 审核容器使用系统用户ID
        container.setType(ContainerType.REVIEW);
        container.setStatus(ContainerStatus.CREATING);
        container.setImage(image);
        container.setInternalPort(internalPort);
        container.setExternalPort(externalPort);
        container.setVolumePath(volumePath);
        container.setLastAccessedAt(LocalDateTime.now()); // 设置初始访问时间

        containerRepository.insert(container);
        return container;
    }

    /** 获取用户容器
     * 
     * @param userId 用户ID
     * @return 用户容器，可能为null */
    public ContainerEntity findUserContainer(String userId) {
        return containerRepository.findByUserIdAndType(userId, ContainerType.USER);
    }

    /** 获取审核容器
     * 
     * @return 审核容器，可能为null */
    public ContainerEntity findReviewContainer() {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .eq(ContainerEntity::getType, ContainerType.REVIEW)
                .ne(ContainerEntity::getStatus, ContainerStatus.DELETED).orderByDesc(ContainerEntity::getCreatedAt);

        List<ContainerEntity> containers = containerRepository.selectList(wrapper);
        return containers.isEmpty() ? null : containers.get(0);
    }

    /** 根据ID获取容器
     * 
     * @param containerId 容器ID
     * @return 容器实体 */
    public ContainerEntity getContainerById(String containerId) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }
        return container;
    }

    /** 检查用户是否有正在运行的容器
     * 
     * @param userId 用户ID
     * @return 是否有运行中的容器 */
    public boolean hasRunningContainer(String userId) {
        ContainerEntity container = findUserContainer(userId);
        return container != null && container.isRunning();
    }

    /** 更新容器状态
     * 
     * @param containerId 容器ID
     * @param status 新状态
     * @param operator 操作者
     * @param dockerContainerId Docker容器ID（可选） */
    public void updateContainerStatus(String containerId, ContainerStatus status, Operator operator,
            String dockerContainerId) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }

        container.updateStatus(status);
        if (dockerContainerId != null) {
            container.setDockerContainerId(dockerContainerId);
        }

        containerRepository.updateById(container);
    }

    /** 更新容器IP地址
     * 
     * @param containerId 容器ID
     * @param ipAddress IP地址
     * @param operator 操作者 */
    public void updateContainerIpAddress(String containerId, String ipAddress, Operator operator) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }

        container.setIpAddress(ipAddress);
        containerRepository.updateById(container);
    }

    /** 更新容器最后访问时间
     * 
     * @param containerId 容器ID
     * @param lastAccessedAt 最后访问时间 */
    public void updateLastAccessTime(String containerId, LocalDateTime lastAccessedAt) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }

        container.setLastAccessedAt(lastAccessedAt);
        containerRepository.updateById(container);
    }

    /** 更新容器资源使用率
     * 
     * @param containerId 容器ID
     * @param cpuUsage CPU使用率
     * @param memoryUsage 内存使用率 */
    public void updateResourceUsage(String containerId, Double cpuUsage, Double memoryUsage) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            return; // 容器不存在时忽略，避免异常
        }

        container.updateResourceUsage(cpuUsage, memoryUsage);
        containerRepository.updateById(container);
    }

    /** 标记容器为错误状态
     * 
     * @param containerId 容器ID
     * @param errorMessage 错误信息
     * @param operator 操作者 */
    public void markContainerError(String containerId, String errorMessage, Operator operator) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }

        container.markError(errorMessage);
        containerRepository.updateById(container);
    }

    /** 删除容器
     * 
     * @param containerId 容器ID
     * @param operator 操作者 */
    public void deleteContainer(String containerId, Operator operator) {
        ContainerEntity container = containerRepository.selectById(containerId);
        if (container == null) {
            throw new BusinessException("容器不存在");
        }

        // 标记为删除中状态
        container.setStatus(ContainerStatus.DELETING);
        containerRepository.updateById(container);
    }

    /** 物理删除容器记录
     * 
     * @param containerId 容器ID */
    public void physicalDeleteContainer(String containerId) {
        containerRepository.deleteById(containerId);
    }

    /** 更新容器最后访问时间
     * 
     * @param containerId 容器ID */
    public void updateContainerLastAccessed(String containerId) {
        LambdaUpdateWrapper<ContainerEntity> updateWrapper = Wrappers.<ContainerEntity>lambdaUpdate()
                .eq(ContainerEntity::getId, containerId).set(ContainerEntity::getLastAccessedAt, LocalDateTime.now());

        containerRepository.update(null, updateWrapper);
    }

    /** 分页查询容器
     * 
     * @param page 分页参数
     * @param keyword 搜索关键词
     * @param status 容器状态
     * @param type 容器类型
     * @return 分页结果 */
    public Page<ContainerEntity> getContainersPage(Page<ContainerEntity> page, String keyword, ContainerStatus status,
            ContainerType type) {
        return containerRepository.selectPageWithConditions(page, keyword, status, type);
    }

    /** 获取所有需要监控的容器
     * 
     * @return 运行中的容器列表 */
    public List<ContainerEntity> getMonitoringContainers() {
        return containerRepository.findByStatus(ContainerStatus.RUNNING);
    }

    /** 根据状态获取容器列表
     * 
     * @param status 容器状态
     * @return 容器列表 */
    public List<ContainerEntity> getContainersByStatus(ContainerStatus status) {
        return containerRepository.findByStatus(status);
    }

    /** 获取所有活跃容器（非已删除状态）
     * 
     * @return 活跃容器列表 */
    public List<ContainerEntity> getAllActiveContainers() {
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .ne(ContainerEntity::getStatus, ContainerStatus.DELETED).orderByAsc(ContainerEntity::getLastAccessedAt);
        return containerRepository.selectList(wrapper);
    }

    /** 获取需要暂停的容器（运行中且1天未访问）
     * 
     * @return 需要暂停的容器列表 */
    public List<ContainerEntity> getContainersNeedingSuspension() {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .lt(ContainerEntity::getLastAccessedAt, oneDayAgo).orderByAsc(ContainerEntity::getLastAccessedAt);
        return containerRepository.selectList(wrapper);
    }

    /** 获取需要删除的容器（5天未访问）
     * 
     * @return 需要删除的容器列表 */
    public List<ContainerEntity> getContainersNeedingDeletion() {
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        LambdaQueryWrapper<ContainerEntity> wrapper = Wrappers.<ContainerEntity>lambdaQuery()
                .lt(ContainerEntity::getLastAccessedAt, fiveDaysAgo).orderByAsc(ContainerEntity::getLastAccessedAt);
        return containerRepository.selectList(wrapper);
    }

    /** 分配外部端口
     * 
     * @return 可用的外部端口 */
    private Integer allocateExternalPort() {
        int maxAttempts = 100;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 生成30000-40000范围内的随机端口
            Integer port = 30000 + random.nextInt(10000);
            if (!containerRepository.isPortOccupied(port)) {
                return port;
            }
        }
        throw new BusinessException("无法分配可用端口，请稍后重试");
    }

    /** 获取容器统计信息
     * 
     * @return 统计信息 */
    public ContainerStatistics getStatistics() {
        long totalContainers = containerRepository.selectCount(null);
        long runningContainers = containerRepository.countRunningContainers();

        return new ContainerStatistics(totalContainers, runningContainers);
    }

    /** 容器统计信息内部类 */
    public static class ContainerStatistics {
        private final long totalContainers;
        private final long runningContainers;

        public ContainerStatistics(long totalContainers, long runningContainers) {
            this.totalContainers = totalContainers;
            this.runningContainers = runningContainers;
        }

        public long getTotalContainers() {
            return totalContainers;
        }

        public long getRunningContainers() {
            return runningContainers;
        }
    }
}