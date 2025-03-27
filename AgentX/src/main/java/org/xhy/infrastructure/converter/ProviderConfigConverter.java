package org.xhy.infrastructure.converter;

import org.xhy.domain.llm.model.config.ProviderConfig;

/**
 * Provider配置转换器
 */
public class ProviderConfigConverter extends JsonToStringConverter<ProviderConfig> {
    
    public ProviderConfigConverter() {
        super(ProviderConfig.class);
    }
} 