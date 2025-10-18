package org.xhy.domain.conversation.constant;

/** 消息类型枚举 */
public enum MessageType {
    /** 普通文本消息 */
    TEXT,

    /** 工具调用消息 */
    TOOL_CALL,

    /** 任务执行消息 */
    TASK_EXEC,
    /** 任务状态进行中 */
    TASK_STATUS_TO_LOADING,

    /** 任务状态完成 */
    TASK_STATUS_TO_FINISH,

    /** 任务拆分结束消息 */
    TASK_SPLIT_FINISH,

    /** RAG检索开始 */
    RAG_RETRIEVAL_START,

    /** RAG检索进行中 */
    RAG_RETRIEVAL_PROGRESS,

    /** RAG检索结束 */
    RAG_RETRIEVAL_END,

    /** RAG思考开始 */
    RAG_THINKING_START,

    /** RAG思考进行中 */
    RAG_THINKING_PROGRESS,

    /** RAG思考结束 */
    RAG_THINKING_END,

    /** RAG回答开始 */
    RAG_ANSWER_START,

    /** RAG回答进行中 */
    RAG_ANSWER_PROGRESS,

    /** RAG回答结束 */
    RAG_ANSWER_END,

    /** 子Agent调用开始（多智能体） */
    SUB_AGENT_CALL_START,

    /** 子Agent流式片段（多智能体） */
    SUB_AGENT_PARTIAL,

    /** 子Agent调用完成（多智能体） */
    SUB_AGENT_COMPLETE,

    /** 子Agent调用异常（多智能体） */
    SUB_AGENT_ERROR
}
