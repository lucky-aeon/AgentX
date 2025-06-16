package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.tool.model.config.ToolDefinition;
import org.xhy.infrastructure.utils.JsonUtils;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/** 工具定义列表JSON转换器 */
@MappedTypes(List.class)
public class ToolDefinitionListConverter extends JsonToStringConverter<List<ToolDefinition>> {

    public ToolDefinitionListConverter() {
        super((Class<List<ToolDefinition>>) (Class<?>) List.class);
    }

    /**
     * 重写parseJson方法，使用JsonUtils.parseArray来正确处理List<ToolDefinition>的反序列化
     */
    @Override
    protected List<ToolDefinition> parseJson(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 使用JsonUtils.parseArray来正确处理List<ToolDefinition>的反序列化
        List<ToolDefinition> result = JsonUtils.parseArray(json, ToolDefinition.class);
        return result != null ? result : Collections.emptyList();
    }
}