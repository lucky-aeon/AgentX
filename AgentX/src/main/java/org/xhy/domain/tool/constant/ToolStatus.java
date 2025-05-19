package org.xhy.domain.tool.constant;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * 工具审核状态枚举
 */
public enum ToolStatus {
    WAITING_REVIEW,
    GITHUB_URL_VALIDATE,
    DEPLOYING,
    FETCHING_TOOLS,
    MANUAL_REVIEW,
    APPROVED,
    FAILED;


    public static ToolStatus fromCode(String name) {
        for (ToolStatus status : values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        throw new BusinessException("未知的工具状态码: " + name);
    }
}