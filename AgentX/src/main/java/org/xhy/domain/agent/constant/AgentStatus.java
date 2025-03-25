package org.xhy.domain.agent.constant;

import org.xhy.infrastructure.exception.BusinessException;

/**
 * Agent状态枚举
 */
public enum AgentStatus {

    /**
     * 草稿状态
     */
    DRAFT(0, "草稿"),

    /**
     * 待审核状态
     */
    PENDING_REVIEW(1, "待审核"),

    /**
     * 已上架状态
     */
    PUBLISHED(2, "已上架"),

    /**
     * 已下架状态
     */
    UNPUBLISHED(3, "已下架"),

    /**
     * 审核拒绝状态
     */
    REJECTED(4, "审核拒绝");

    private final Integer code;
    private final String description;

    AgentStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据状态码获取枚举值
     */
    public static AgentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (AgentStatus status : AgentStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        throw new BusinessException("INVALID_AGENT_STATUS", "无效的Agent状态码: " + code);
    }
}