package org.xhy.domain.container.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.container.constant.ContainerStatus;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.infrastructure.converter.ContainerStatusConverter;
import org.xhy.infrastructure.converter.ContainerTypeConverter;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.entity.Operator;

import java.time.LocalDateTime;

/** 容器实体 */
@TableName("user_containers")
public class ContainerEntity extends BaseEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    /** 容器名称 */
    private String name;
    /** 用户ID */
    private String userId;
    /** 容器类型 */
    @TableField(typeHandler = ContainerTypeConverter.class)
    private ContainerType type;
    /** 容器状态 */
    @TableField(typeHandler = ContainerStatusConverter.class)
    private ContainerStatus status;
    /** Docker容器ID */
    private String dockerContainerId;
    /** 容器镜像 */
    private String image;
    /** 内部端口 */
    private Integer internalPort;
    /** 外部映射端口 */
    private Integer externalPort;
    /** 容器IP地址 */
    private String ipAddress;
    /** CPU使用率(%) */
    private Double cpuUsage;
    /** 内存使用率(%) */
    private Double memoryUsage;
    /** 数据卷路径 */
    private String volumePath;
    /** 环境变量配置(JSON) */
    private String envConfig;
    /** 容器配置(JSON) */
    private String containerConfig;
    /** 错误信息 */
    private String errorMessage;
    /** 最后访问时间 */
    private LocalDateTime lastAccessedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public ContainerStatus getStatus() {
        return status;
    }

    public void setStatus(ContainerStatus status) {
        this.status = status;
    }

    public String getDockerContainerId() {
        return dockerContainerId;
    }

    public void setDockerContainerId(String dockerContainerId) {
        this.dockerContainerId = dockerContainerId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(Integer internalPort) {
        this.internalPort = internalPort;
    }

    public Integer getExternalPort() {
        return externalPort;
    }

    public void setExternalPort(Integer externalPort) {
        this.externalPort = externalPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public void setVolumePath(String volumePath) {
        this.volumePath = volumePath;
    }

    public String getEnvConfig() {
        return envConfig;
    }

    public void setEnvConfig(String envConfig) {
        this.envConfig = envConfig;
    }

    public String getContainerConfig() {
        return containerConfig;
    }

    public void setContainerConfig(String containerConfig) {
        this.containerConfig = containerConfig;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    /** 检查容器是否正在运行 */
    public boolean isRunning() {
        return ContainerStatus.RUNNING.equals(this.status);
    }

    /** 检查容器是否可以操作 */
    public boolean isOperatable() {
        return this.status != null && !ContainerStatus.DELETING.equals(this.status)
                && !ContainerStatus.DELETED.equals(this.status);
    }

    /** 检查容器是否已暂停 */
    public boolean isSuspended() {
        return ContainerStatus.SUSPENDED.equals(this.status);
    }

    /** 更新最后访问时间 */
    public void updateLastAccessedAt() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /** 标记容器为错误状态 */
    public void markError(String errorMessage) {
        this.status = ContainerStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    /** 更新容器状态 */
    public void updateStatus(ContainerStatus newStatus) {
        this.status = newStatus;
        if (ContainerStatus.RUNNING.equals(newStatus)) {
            this.errorMessage = null;
        }
    }

    /** 更新资源使用率 */
    public void updateResourceUsage(Double cpuUsage, Double memoryUsage) {
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
    }

    @Override
    public boolean needCheckUserId() {
        return true;
    }
}