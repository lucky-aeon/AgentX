package org.xhy.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.xhy.infrastructure.auth.UserAuthInterceptor;

/**
 * Web MVC 配置类
 * 用于配置拦截器、跨域等
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final UserAuthInterceptor userAuthInterceptor;

    public WebMvcConfig(UserAuthInterceptor userAuthInterceptor) {
        this.userAuthInterceptor = userAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户鉴权拦截器，并指定拦截路径
        registry.addInterceptor(userAuthInterceptor)
                // 添加拦截路径 - 拦截所有API请求
                .addPathPatterns("/**")
                // 排除不需要鉴权的路径，例如登录、注册等
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }
}