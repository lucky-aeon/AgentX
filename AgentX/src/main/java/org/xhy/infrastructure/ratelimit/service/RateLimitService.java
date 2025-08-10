package org.xhy.infrastructure.ratelimit.service;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.xhy.infrastructure.exception.RateLimitException;
import org.xhy.infrastructure.ratelimit.config.RateLimitConfig;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/** 限流服务 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitConfig rateLimitConfig;

    /** 用户充值限流器缓存 */
    private final ConcurrentHashMap<String, UserRateLimiter> rechargeRateLimiters = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    /** 检查用户充值是否超过限流
     * 
     * @param userId 用户ID
     * @throws RateLimitException 当触发限流时抛出异常 */
    public void checkRechargeRateLimit(String userId) {
        if (!rateLimitConfig.getRecharge().isEnabled()) {
            return;
        }

        UserRateLimiter userLimiter = getUserRechargeRateLimiter(userId);

        if (!userLimiter.getRateLimiter().tryAcquire()) {
            logger.warn("用户充值触发限流: userId={}, permitsPerSecond={}", userId,
                    rateLimitConfig.getRecharge().getPermitsPerSecond());
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        logger.debug("用户充值限流检查通过: userId={}", userId);
    }

    /** 获取用户充值限流器 */
    private UserRateLimiter getUserRechargeRateLimiter(String userId) {
        return rechargeRateLimiters.computeIfAbsent(userId, k -> {
            logger.debug("为用户创建充值限流器: userId={}, permitsPerSecond={}", userId,
                    rateLimitConfig.getRecharge().getPermitsPerSecond());

            RateLimiter rateLimiter = RateLimiter.create(rateLimitConfig.getRecharge().getPermitsPerSecond());
            return new UserRateLimiter(rateLimiter, LocalDateTime.now());
        });
    }

    /** 定时清理不活跃的限流器缓存 每30分钟执行一次 */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
    public void cleanupInactiveRateLimiters() {
        if (!rateLimitConfig.getRecharge().isEnabled()) {
            return;
        }

        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusMinutes(rateLimitConfig.getRecharge().getCleanupIntervalMinutes());

        int removedCount = 0;

        rechargeRateLimiters.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastAccessTime().isBefore(cutoffTime)) {
                logger.debug("清理不活跃的充值限流器: userId={}", entry.getKey());
                return true;
            }
            return false;
        });

        if (removedCount > 0) {
            logger.info("清理不活跃的充值限流器完成: 清理数量={}, 剩余数量={}", removedCount, rechargeRateLimiters.size());
        }

        // 检查缓存大小，防止内存泄漏
        int currentSize = rechargeRateLimiters.size();
        int maxSize = rateLimitConfig.getRecharge().getMaxCachedUsers();

        if (currentSize > maxSize) {
            logger.warn("充值限流器缓存大小超过限制: 当前大小={}, 最大限制={}", currentSize, maxSize);
        }
    }

    /** 获取限流统计信息 */
    public RateLimitStats getRechargeRateLimitStats() {
        return new RateLimitStats(rechargeRateLimiters.size(), rateLimitConfig.getRecharge().getPermitsPerSecond(),
                rateLimitConfig.getRecharge().isEnabled());
    }

    /** 用户限流器包装类 */
    private static class UserRateLimiter {
        private final RateLimiter rateLimiter;
        private volatile LocalDateTime lastAccessTime;

        public UserRateLimiter(RateLimiter rateLimiter, LocalDateTime lastAccessTime) {
            this.rateLimiter = rateLimiter;
            this.lastAccessTime = lastAccessTime;
        }

        public RateLimiter getRateLimiter() {
            this.lastAccessTime = LocalDateTime.now(); // 更新最后访问时间
            return rateLimiter;
        }

        public LocalDateTime getLastAccessTime() {
            return lastAccessTime;
        }
    }

    /** 限流统计信息 */
    public static class RateLimitStats {
        private final int cachedUsersCount;
        private final double permitsPerSecond;
        private final boolean enabled;

        public RateLimitStats(int cachedUsersCount, double permitsPerSecond, boolean enabled) {
            this.cachedUsersCount = cachedUsersCount;
            this.permitsPerSecond = permitsPerSecond;
            this.enabled = enabled;
        }

        public int getCachedUsersCount() {
            return cachedUsersCount;
        }

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }
}