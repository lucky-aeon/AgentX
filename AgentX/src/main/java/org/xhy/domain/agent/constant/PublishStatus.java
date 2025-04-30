package org.xhy.domain.agent.constant;

import org.xhy.infrastructure.exception.BusinessException;

/** 版本发布状态枚举 */
public enum PublishStatus {

    /** 审核中状态 */
    REVIEWING(1, "审核中"),

    /** 已发布状态 */
    PUBLISHED(2, "已发布"),

    /** 发布拒绝状态 */
    REJECTED(3, "拒绝"),

    /** 已下架状态 */
    REMOVED(4, "已下架");

    private final Integer code;
    private final String description;

    PublishStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据状态码获取枚举值 */
    public static PublishStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (PublishStatus status : PublishStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        throw new BusinessException("INVALID_STATUS_CODE", "无效的发布状态码: " + code);
    }
}