package org.xhy.domain.plugins.model;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * 授权协议值对象
 */
public class License {
    private final String value;

    private License(String value) {
        this.value = value;
    }

    public static License from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("授权协议不能为空");
        }
        return new License(value.trim());
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        License license = (License) o;
        return value.equals(license.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
