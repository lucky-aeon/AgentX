package org.xhy.infrastructure.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 限流配置 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    /** 充值限流配置 */
    private final Recharge recharge = new Recharge();

    public Recharge getRecharge() {
        return recharge;
    }

    /** 充值限流配置 */
    public static class Recharge {

        /** 每秒允许的请求数（每分钟60次） */
        private double permitsPerSecond = 1.0;

        /** 是否启用限流 */
        private boolean enabled = true;

        /** 限流器缓存的最大用户数 */
        private int maxCachedUsers = 10000;

        /** 缓存清理间隔（分钟） */
        private int cleanupIntervalMinutes = 30;

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public void setPermitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxCachedUsers() {
            return maxCachedUsers;
        }

        public void setMaxCachedUsers(int maxCachedUsers) {
            this.maxCachedUsers = maxCachedUsers;
        }

        public int getCleanupIntervalMinutes() {
            return cleanupIntervalMinutes;
        }

        public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
            this.cleanupIntervalMinutes = cleanupIntervalMinutes;
        }
    }
}