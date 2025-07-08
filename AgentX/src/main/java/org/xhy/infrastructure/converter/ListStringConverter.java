package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;

import java.util.List;

/** 字符串列表JSON转换器 */
@MappedTypes(List.class)
public class ListStringConverter extends JsonToStringConverter<List<String>> {

    public ListStringConverter() {
        super((Class<List<String>>) (Class<?>) List.class);
    }
}