package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.agent.model.AgentTool;

import java.util.List;

/**
 * 模型配置转换器
 */
@MappedTypes(List.class)
public class ListConverter extends JsonToStringConverter<List> {

    public ListConverter() {
        super(List.class);
    }
}