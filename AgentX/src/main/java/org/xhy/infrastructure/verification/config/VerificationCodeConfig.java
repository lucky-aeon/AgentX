package org.xhy.infrastructure.verification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xhy.infrastructure.verification.storage.CodeStorage;
import org.xhy.infrastructure.verification.storage.MemoryCodeStorage;

/**
 * 验证码存储配置类
 * 根据环境自动选择存储实现：
 * 1. 如果环境中有Redis，则使用Redis存储
 * 2. 否则使用内存存储
 */
@Configuration
public class VerificationCodeConfig {

    /**
     * 如果没有RedisTemplate，则使用内存存储
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisCodeStorage")
    public CodeStorage memoryCodeStorage() {
        return new MemoryCodeStorage();
    }
} 