package org.xhy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置
 * 启用Spring的异步处理功能，用于异步事件处理
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot会自动配置默认的异步执行器
    // 如果需要自定义线程池，可以在这里配置
} 