package org.xhy.domain.conversation.constant;

/**
 * 消息类型枚举
 */
public enum MessageType {
    /**
     * 普通文本消息
     */
    TEXT,
    
    /**
     * 工具调用消息
     */
    TOOL_CALL,

    /**
     * 任务执行消息
     */
    TASK_EXEC,
    /**
     * 任务状态更新消息
     */
    TASK_STATUS,
    
    /**
     * 任务ID列表消息
     */
    TASK_IDS
} 