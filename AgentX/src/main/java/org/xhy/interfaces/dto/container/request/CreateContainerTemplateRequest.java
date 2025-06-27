package org.xhy.interfaces.dto.container.request;

import jakarta.validation.constraints.*;
import java.util.Map;

/** 创建容器模板请求 */
public class CreateContainerTemplateRequest {
    
    @NotBlank(message = "模板名称不可为空")
    @Size(max = 100, message = "模板名称长度不能超过100字符")
    private String name;
    
    @Size(max = 500, message = "模板描述长度不能超过500字符")
    private String description;
    
    @NotBlank(message = "模板类型不可为空")
    @Size(max = 50, message = "模板类型长度不能超过50字符")
    private String type;
    
    @NotBlank(message = "镜像名称不可为空")
    @Size(max = 200, message = "镜像名称长度不能超过200字符")
    private String image;
    
    @Size(max = 50, message = "镜像标签长度不能超过50字符")
    private String imageTag;
    
    @NotNull(message = "内部端口不可为空")
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号不能超过65535")
    private Integer internalPort;
    
    @NotNull(message = "CPU限制不可为空")
    @DecimalMin(value = "0.1", message = "CPU限制最少0.1核")
    @DecimalMax(value = "32.0", message = "CPU限制最多32核")
    private Double cpuLimit;
    
    @NotNull(message = "内存限制不可为空")
    @Min(value = 64, message = "内存限制最少64MB")
    @Max(value = 32768, message = "内存限制最多32768MB")
    private Integer memoryLimit;
    
    private Map<String, String> environment;
    
    @Size(max = 500, message = "数据卷路径长度不能超过500字符")
    private String volumeMountPath;
    
    private String[] command;
    
    @Size(max = 50, message = "网络模式长度不能超过50字符")
    private String networkMode;
    
    @Size(max = 50, message = "重启策略长度不能超过50字符")
    private String restartPolicy;
    
    private Map<String, Object> healthCheck;
    
    private Map<String, Object> resourceConfig;
    
    private Boolean enabled = true;
    
    private Boolean isDefault = false;
    
    @Min(value = 0, message = "排序权重不能为负数")
    private Integer sortOrder = 0;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}