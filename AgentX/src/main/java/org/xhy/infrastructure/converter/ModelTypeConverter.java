package org.xhy.infrastructure.converter;

import org.xhy.domain.llm.model.enums.ModelType;

/**
 * ModelType转换器
 */
public class ModelTypeConverter extends JsonToStringConverter<ModelType> {
    
    public ModelTypeConverter() {
        super(ModelType.class);
    }
} 