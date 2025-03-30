package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.llm.model.config.ProviderConfig;

/**
 * 服务商配置转换器
 */
@MappedTypes(ProviderConfig.class)
public class ProviderConfigConverter extends JsonToStringConverter<ProviderConfig> {
    
    public ProviderConfigConverter() {
        super(ProviderConfig.class);
    }
} 