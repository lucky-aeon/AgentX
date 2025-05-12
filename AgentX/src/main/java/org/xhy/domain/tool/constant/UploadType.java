package org.xhy.domain.tool.constant;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * 工具上传方式枚举
 */
public enum UploadType {

    GITHUB, ZIP;

    public static UploadType fromCode(String code) {
        for (UploadType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new BusinessException("未知的上传类型码: " + code);
    }
} 