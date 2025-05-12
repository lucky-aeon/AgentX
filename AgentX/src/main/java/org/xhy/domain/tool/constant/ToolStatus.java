package org.xhy.domain.tool.constant;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * 工具审核状态枚举
 */
public enum ToolStatus {
    WAITING_REVIEW( "等待审核"),
    DEPLOYING("部署中"),
    FETCHING_TOOLS( "获取工具列表"),
    MANUAL_REVIEW("人工审核"),
    APPROVED( "通过"),
    FAILED( "失败");

    private final String name;

    ToolStatus( String desc) {
        this.name = desc;
    }

    public String getName() {
        return name;
    }

    public static ToolStatus fromCode(String name) {
        for (ToolStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        throw new BusinessException("未知的工具状态码: " + name);
    }
}