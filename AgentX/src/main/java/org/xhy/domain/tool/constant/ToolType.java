package org.xhy.domain.tool.constant;

import org.xhy.infrastructure.exception.BusinessException;

/** 工具类型枚举 */
public enum ToolType {

    MCP;

    public static ToolType fromCode(String code) {
        for (ToolType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new BusinessException("未知的工具类型码: " + code);
    }
}