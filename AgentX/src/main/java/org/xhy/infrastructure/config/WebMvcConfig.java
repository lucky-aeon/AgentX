package org.xhy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xhy.infrastructure.auth.UserAuthInterceptor;

/** Web MVC 配置类 用于配置拦截器、跨域等 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userAuthInterceptor;

    public WebMvcConfig(UserAuthInterceptor userAuthInterceptor) {
        this.userAuthInterceptor = userAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userAuthInterceptor).addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns( // 不拦截以下路径
                        "/login", // 登录接口
                        "/health", // 健康检查接口
                        "/register", // 注册接口
                        "/send-email-code", "/verify-email-code", "/get-captcha", "/reset-password",
                        "/send-reset-password-code", "/oauth/github/authorize", "/oauth/github/callback", "/v1/**"); // 外部API接口，使用专门的API
                                                                                                                     // Key拦截器
    }
}