package org.xhy.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.xhy.domain.llm.service.LlmService;
import org.xhy.infrastructure.integration.llm.siliconflow.SiliconFlowLlmService;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM服务配置
 */
@Configuration
public class LlmConfig {
    
    @Value("${llm.provider.default:siliconflow}")
    private String defaultProvider;
    
    /**
     * 默认LLM服务
     */
    @Bean
    @Primary
    public LlmService defaultLlmService(SiliconFlowLlmService siliconFlowLlmService) {
        // 直接返回SiliconFlow服务作为默认服务
        return siliconFlowLlmService;
    }
    
    /**
     * LLM服务映射
     */
    @Bean
    public Map<String, LlmService> llmServiceMap(SiliconFlowLlmService siliconFlowLlmService) {
        Map<String, LlmService> serviceMap = new HashMap<>();
        // 确保键名与defaultProvider + "LlmService"匹配
        serviceMap.put("siliconflowLlmService", siliconFlowLlmService);
        return serviceMap;
    }
}
