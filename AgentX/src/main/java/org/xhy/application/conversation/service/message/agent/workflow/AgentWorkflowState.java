package org.xhy.application.conversation.service.message.agent.workflow;

/**
 * Agent工作流状态
 * 定义任务执行过程中的各个状态
 */
public enum AgentWorkflowState {
    ANALYSER_MESSAGE, // 分析用户消息
    INITIALIZED,      // 初始化
    TASK_SPLITTING,   // 任务拆分中
    TASK_SPLIT_COMPLETED, // 任务拆分完成
    TASK_EXECUTING,   // 任务执行中
    TASK_EXECUTED,    // 任务执行完成
    SUMMARIZING,      // 结果汇总中
    COMPLETED,        // 完成
    FAILED,            // 失败
    WAITING_INPUT_FOR_TASK_SPLIT,
    WAITING_INPUT_FOR_TASK_EXECUTION
} 