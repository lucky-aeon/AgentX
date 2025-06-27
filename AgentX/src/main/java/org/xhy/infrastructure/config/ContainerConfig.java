package org.xhy.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 容器管理配置 */
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "agentx.container")
public class ContainerConfig {

    /** Docker连接配置 */
    private String dockerHost = "unix:///var/run/docker.sock";
    
    /** 用户数据卷基础路径 */
    private String userVolumeBasePath = "/docker/users";
    
    /** 默认MCP网关镜像 */
    private String defaultMcpGatewayImage = "ghcr.io/lucky-aeon/mcp-gateway:latest";
    
    /** 容器监控间隔（毫秒） */
    private long monitorInterval = 300000; // 5分钟
    
    /** 资源使用率更新间隔（毫秒） */
    private long statsUpdateInterval = 120000; // 2分钟

    public String getDockerHost() {
        return dockerHost;
    }

    public void setDockerHost(String dockerHost) {
        this.dockerHost = dockerHost;
    }

    public String getUserVolumeBasePath() {
        return userVolumeBasePath;
    }

    public void setUserVolumeBasePath(String userVolumeBasePath) {
        this.userVolumeBasePath = userVolumeBasePath;
    }

    public String getDefaultMcpGatewayImage() {
        return defaultMcpGatewayImage;
    }

    public void setDefaultMcpGatewayImage(String defaultMcpGatewayImage) {
        this.defaultMcpGatewayImage = defaultMcpGatewayImage;
    }

    public long getMonitorInterval() {
        return monitorInterval;
    }

    public void setMonitorInterval(long monitorInterval) {
        this.monitorInterval = monitorInterval;
    }

    public long getStatsUpdateInterval() {
        return statsUpdateInterval;
    }

    public void setStatsUpdateInterval(long statsUpdateInterval) {
        this.statsUpdateInterval = statsUpdateInterval;
    }
}