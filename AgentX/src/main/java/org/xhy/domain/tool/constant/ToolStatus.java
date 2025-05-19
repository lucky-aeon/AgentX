package org.xhy.domain.tool.constant;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * 工具审核状态枚举
 */
public enum ToolStatus {
    WAITING_REVIEW,      // 等待审核
    GITHUB_URL_VALIDATE, // GitHub URL 验证中
    DEPLOYING,           // （原）部署中 - 根据新流程，此状态可能调整或移除，暂时保留
    FETCHING_TOOLS,      // （原）获取工具中 - 根据新流程，此状态可能调整或移除，暂时保留
    MANUAL_REVIEW,       // 人工审核
    PUBLISHING,          // 发布中 (正在下载源并推送到目标仓库)
    APPROVED,            // 已通过
    PUBLISH_FAILED,      // 发布失败
    FAILED;              // 通用失败状态


    /**
     * 根据名称获取工具状态枚举。
     *
     * @param name 状态名称
     * @return 对应的工具状态枚举
     * @throws BusinessException 如果找不到对应的状态
     */
    public static ToolStatus fromCode(String name) {
        for (ToolStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }
}