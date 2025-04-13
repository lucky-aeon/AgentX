package org.xhy.infrastructure.enums;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * @author shilong.zang
 * @date 09:43 <br/>
 */
public interface IBaseEnum {

    Object getValue();
    String getLabel();

    static <E extends Enum<E> & IBaseEnum> E getEnumByValue(Object value, Class<E> clazz) {
        Objects.requireNonNull(value);
        EnumSet<E> allEnums = EnumSet.allOf(clazz);
        return allEnums.stream().filter(e -> value.equals(e.getValue())).findFirst().orElse(null);
    }

    static <E extends Enum<E> & IBaseEnum> String getLabelByValue(Object value, Class<E> clazz) {
        E matchEnum = getEnumByValue(value, clazz);
        String label = null;
        if (Objects.nonNull(matchEnum)) {
            label = matchEnum.getLabel();
        }
        return label;
    }

    static <E extends Enum<E> & IBaseEnum> E getEnumByLabel(String label, Class<E> clazz) {
        Objects.requireNonNull(label);
        EnumSet<E> allEnums = EnumSet.allOf(clazz);
        Optional<E> match = allEnums.stream().filter(e -> label.equals(e.getLabel())).findFirst();
        return match.orElse(null);
    }
}
