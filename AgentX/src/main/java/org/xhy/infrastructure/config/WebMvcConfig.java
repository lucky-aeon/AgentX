package org.xhy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xhy.infrastructure.auth.UserAuthInterceptor;
import org.xhy.infrastructure.interceptor.AdminAuthInterceptor;
import org.xhy.infrastructure.logging.interceptor.LoggingInterceptor;

/** Web MVC 配置类 用于配置拦截器、跨域等 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final LoggingInterceptor loggingInterceptor;

    public WebMvcConfig(UserAuthInterceptor userAuthInterceptor, AdminAuthInterceptor adminAuthInterceptor,
            LoggingInterceptor loggingInterceptor) {
        this.userAuthInterceptor = userAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器 - 最先执行，拦截所有请求
        registry.addInterceptor(loggingInterceptor).addPathPatterns("/**").order(1);

        registry.addInterceptor(userAuthInterceptor).addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns( // 不拦截以下路径
                        "/login", // 登录接口
                        "/health", // 健康检查接口
                        "/register", // 注册接口
                        "/auth/config", // 认证配置接口
                        "/send-email-code", "/verify-email-code", "/get-captcha", "/reset-password",
                        "/send-reset-password-code", "/oauth/github/authorize", "/oauth/github/callback", "/sso/**", // SSO相关接口
                        "/widget/**", // Widget公开API接口，无需认证
                        "/v1/**", "/payments/callback/**") // 外部API接口，使用专门的API
                .order(2); // Key拦截器

        // 管理员权限拦截器，只拦截admin路径
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/admin/**").order(3);
    }
}