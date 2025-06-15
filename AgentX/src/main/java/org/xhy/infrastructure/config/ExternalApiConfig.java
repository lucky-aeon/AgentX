package org.xhy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xhy.infrastructure.auth.ExternalApiKeyInterceptor;

/**
 * 外部API配置
 * 配置外部API相关的拦截器和其他设置
 */
@Configuration
public class ExternalApiConfig implements WebMvcConfigurer {

    private final ExternalApiKeyInterceptor externalApiKeyInterceptor;

    public ExternalApiConfig(ExternalApiKeyInterceptor externalApiKeyInterceptor) {
        this.externalApiKeyInterceptor = externalApiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 为 /v1/* 路径添加API Key验证拦截器
        registry.addInterceptor(externalApiKeyInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/health"); // 健康检查接口不需要验证
    }
} 