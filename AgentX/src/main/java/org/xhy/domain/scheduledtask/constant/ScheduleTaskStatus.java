package org.xhy.domain.scheduledtask.constant;

import org.xhy.infrastructure.exception.BusinessException;

/** 定时任务状态枚举 */
public enum ScheduleTaskStatus {
    
    /** 活跃状态 */
    ACTIVE,
    
    /** 暂停状态 */
    PAUSED,
    
    /** 已完成状态 */
    COMPLETED;

    public static ScheduleTaskStatus fromCode(String code) {
        for (ScheduleTaskStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        throw new BusinessException("未知的任务状态码: " + code);
    }
} 