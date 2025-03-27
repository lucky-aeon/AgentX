package org.xhy.infrastructure.converter;

import org.xhy.domain.llm.model.config.ModelConfig;

/**
 * Model配置转换器
 */
public class ModelConfigConverter extends JsonToStringConverter<ModelConfig> {
    
    public ModelConfigConverter() {
        super(ModelConfig.class);
    }
} 