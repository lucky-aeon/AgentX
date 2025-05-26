package org.xhy.domain.scheduledtask.constant;

import org.xhy.infrastructure.exception.BusinessException;

/** 定时任务重复类型枚举 */
public enum RepeatType {

    /** 不重复 */
    NONE,

    /** 每天 */
    DAILY,

    /** 每周 */
    WEEKLY,

    /** 每月 */
    MONTHLY,

    /** 工作日 */
    WORKDAYS,

    /** 自定义 */
    CUSTOM;

    public static RepeatType fromCode(String code) {
        for (RepeatType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        throw new BusinessException("未知的重复类型码: " + code);
    }
}