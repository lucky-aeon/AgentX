package org.xhy.domain.conversation.constant;

/** 消息类型枚举 */
public enum MessageType {
    /**
     * 普通文本消息
     */
    TEXT,

    /**
     * 流式文本消息
     */
    TEXT_STREAM,

    /**
     * 工具调用消息
     */
    TOOL_CALL,

    /**
     * 工具调用结果消息
     */
    TOOL_RESULT,

    /**
     * 任务执行消息
     */
    TASK_EXEC,

    /**
     * 任务状态进行中
     */
    TASK_STATUS_TO_LOADING,

    /**
     * 任务状态完成
     */
    TASK_STATUS_TO_FINISH,

    /**
     * 任务拆分结束消息
     */
    TASK_SPLIT_FINISH,

    /**
     * 错误消息
     */
    ERROR,

    /**
     * 警告消息
     */
    WARNING
}