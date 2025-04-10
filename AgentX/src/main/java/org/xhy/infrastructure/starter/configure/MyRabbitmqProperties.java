package org.xhy.infrastructure.starter.configure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zang
 * @date 11:08 <br/>
 */
@Configuration
@ConfigurationProperties(prefix = "custom-rabbit")
public class MyRabbitmqProperties {

    /**
     * CachingConnectionFactory缓存 RabbitMQ 的 Channel 对象数量
     */
    private int cacheSize = 100;
    /**
     * RabbitMQ 与客户端之间的心跳间隔，以秒为单位
     */
    private int requestedHeartbeat = 60;
    /**
     * 建立连接的超时时间，以毫秒为单位
     */
    private int connectionTimeout = 60000;
    /**
     * 启用 Channel 的自动恢复功能
     */
    private boolean automaticRecovery = true;

    /**
     * 设置网络恢复间隔，以毫秒为单位10s
     */
    private int networkRecoveryInterval = 10000;

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getRequestedHeartbeat() {
        return requestedHeartbeat;
    }

    public void setRequestedHeartbeat(int requestedHeartbeat) {
        this.requestedHeartbeat = requestedHeartbeat;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public boolean isAutomaticRecovery() {
        return automaticRecovery;
    }

    public void setAutomaticRecovery(boolean automaticRecovery) {
        this.automaticRecovery = automaticRecovery;
    }

    public int getNetworkRecoveryInterval() {
        return networkRecoveryInterval;
    }

    public void setNetworkRecoveryInterval(int networkRecoveryInterval) {
        this.networkRecoveryInterval = networkRecoveryInterval;
    }
}
