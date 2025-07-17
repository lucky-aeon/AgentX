package org.xhy.domain.rag.constant;

import org.xhy.infrastructure.exception.BusinessException;

/** RAG版本发布状态枚举 */
public enum RagPublishStatus {

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

    RagPublishStatus(Integer code, String description) {
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
    public static RagPublishStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (RagPublishStatus status : RagPublishStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        throw new BusinessException("INVALID_RAG_STATUS_CODE", "无效的RAG发布状态码: " + code);
    }
}