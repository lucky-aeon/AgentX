package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.llm.model.config.LLMModelConfig;

/**
 * 模型配置转换器
 */
@MappedTypes(LLMModelConfig.class)
public class ModelConfigConverter extends JsonToStringConverter<LLMModelConfig> {
    
    public ModelConfigConverter() {
        super(LLMModelConfig.class);
    }
} 