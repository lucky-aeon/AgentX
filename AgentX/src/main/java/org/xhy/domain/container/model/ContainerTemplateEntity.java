package org.xhy.domain.container.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.xhy.domain.container.constant.ContainerType;
import org.xhy.infrastructure.entity.BaseEntity;
import org.xhy.infrastructure.converter.ContainerTypeConverter;
import org.xhy.infrastructure.converter.JsonConverter;

import java.util.Map;

/** 容器模板实体 */
@TableName(value = "container_templates")
public class ContainerTemplateEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 模板名称 */
    private String name;
    /** 模板描述 */
    private String description;
    /** 模板类型 */
    @TableField(typeHandler = ContainerTypeConverter.class)
    private ContainerType type;
    /** 镜像名称 */
    private String image;
    /** 镜像版本标签 */
    private String imageTag;
    /** 内部端口 */
    private Integer internalPort;
    /** CPU限制(核数) */
    private Double cpuLimit;
    /** 内存限制(MB) */
    private Integer memoryLimit;
    /** 环境变量配置 */
    @TableField(typeHandler = JsonConverter.class)
    private Map<String, String> environment;
    /** 数据卷挂载路径 */
    private String volumeMountPath;
    /** 启动命令 */
    @TableField(typeHandler = JsonConverter.class)
    private String[] command;
    /** 网络模式 */
    private String networkMode;
    /** 重启策略 */
    private String restartPolicy;
    /** 健康检查配置 */
    @TableField(typeHandler = JsonConverter.class)
    private Map<String, Object> healthCheck;
    /** 资源配置 */
    @TableField(typeHandler = JsonConverter.class)
    private Map<String, Object> resourceConfig;
    /** 是否启用 */
    private Boolean enabled;
    /** 是否为默认模板 */
    private Boolean isDefault;
    /** 创建者用户ID */
    private String createdBy;
    /** 排序权重 */
    private Integer sortOrder;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public Integer getInternalPort() {
        return internalPort;
    }

    public void setInternalPort(Integer internalPort) {
        this.internalPort = internalPort;
    }

    public Double getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(Double cpuLimit) {
        this.cpuLimit = cpuLimit;
    }

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getVolumeMountPath() {
        return volumeMountPath;
    }

    public void setVolumeMountPath(String volumeMountPath) {
        this.volumeMountPath = volumeMountPath;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] command) {
        this.command = command;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public String getRestartPolicy() {
        return restartPolicy;
    }

    public void setRestartPolicy(String restartPolicy) {
        this.restartPolicy = restartPolicy;
    }

    public Map<String, Object> getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(Map<String, Object> healthCheck) {
        this.healthCheck = healthCheck;
    }

    public Map<String, Object> getResourceConfig() {
        return resourceConfig;
    }

    public void setResourceConfig(Map<String, Object> resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 获取完整的镜像名称 */
    public String getFullImageName() {
        if (imageTag == null || imageTag.trim().isEmpty()) {
            return image;
        }
        return image + ":" + imageTag;
    }

    /** 转换为容器模板配置 */
    public ContainerTemplate toContainerTemplate() {
        ContainerTemplate template = new ContainerTemplate();
        template.setImage(getFullImageName());
        template.setInternalPort(this.internalPort);
        template.setCpuLimit(this.cpuLimit);
        template.setMemoryLimit(this.memoryLimit);
        template.setEnvironment(this.environment);
        template.setVolumeMountPath(this.volumeMountPath);
        template.setCommand(this.command);
        template.setNetworkMode(this.networkMode);
        template.setRestartPolicy(this.restartPolicy);
        return template;
    }

    /** 验证模板配置是否有效 */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && image != null && !image.trim().isEmpty()
                && internalPort != null && internalPort > 0 && internalPort <= 65535 && cpuLimit != null && cpuLimit > 0
                && memoryLimit != null && memoryLimit > 0;
    }
}