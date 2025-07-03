package org.xhy.domain.container.model;

import java.util.Map;

/** 容器模板配置 */
public class ContainerTemplate {
    /** 镜像名称 */
    private String image;
    /** 内部端口 */
    private Integer internalPort;
    /** CPU限制(核数) */
    private Double cpuLimit;
    /** 内存限制(MB) */
    private Integer memoryLimit;
    /** 环境变量 */
    private Map<String, String> environment;
    /** 数据卷挂载路径 */
    private String volumeMountPath;
    /** 命令参数 */
    private String[] command;
    /** 网络模式 */
    private String networkMode;
    /** 重启策略 */
    private String restartPolicy;

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

    /** 获取默认的MCP网关容器模板 */
    public static ContainerTemplate getDefaultMcpGatewayTemplate() {
        ContainerTemplate template = new ContainerTemplate();
        template.setImage("ghcr.io/lucky-aeon/mcp-gateway:latest");
        template.setInternalPort(8080);
        template.setCpuLimit(1.0);
        template.setMemoryLimit(512);
        template.setVolumeMountPath("/app/data");
        template.setNetworkMode("bridge");
        template.setRestartPolicy("unless-stopped");
        return template;
    }
}