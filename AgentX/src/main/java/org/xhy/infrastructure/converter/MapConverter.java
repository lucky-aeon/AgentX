package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.MappedTypes;
import java.util.Map;

/** Map对象JSON转换器 */
@MappedTypes(Map.class)
public class MapConverter extends JsonToStringConverter<Map> {

    public MapConverter() {
        super(Map.class);
    }
}