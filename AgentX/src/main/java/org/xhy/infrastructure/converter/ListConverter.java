package org.xhy.infrastructure.converter;

import java.util.ArrayList;
import org.apache.ibatis.type.MappedTypes;

/** List JSON转换器 */
@MappedTypes(ArrayList.class)
public class ListConverter extends JsonToStringConverter<ArrayList> {

    public ListConverter() {
        super(ArrayList.class);
    }
}
