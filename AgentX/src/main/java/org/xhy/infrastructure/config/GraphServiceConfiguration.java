package org.xhy.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** 知识图谱服务配置类 注册API密钥认证过滤器
 * 
 * @author zang */
@Configuration
public class GraphServiceConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GraphServiceConfiguration.class);

    /** 注册知识图谱API密钥认证过滤器 */
    @Bean
    public FilterRegistrationBean<GraphApiKeyAuthenticationFilter> graphApiKeyFilter(
            GraphApiKeyAuthenticationFilter filter) {

        logger.info("注册知识图谱API密钥认证过滤器");

        FilterRegistrationBean<GraphApiKeyAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/api/v1/graph/*"); // 只对知识图谱API生效
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // 高优先级
        registrationBean.setName("graphApiKeyAuthenticationFilter");

        return registrationBean;
    }
}