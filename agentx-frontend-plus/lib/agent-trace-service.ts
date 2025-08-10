import { httpClient } from "@/lib/http-client"
import { withToast } from "./toast-utils"

// Agent 追踪统计信息类型定义
export interface AgentTraceStatistics {
  agentId: string
  agentName: string
  totalExecutions: number
  successfulExecutions: number
  failedExecutions: number
  successRate: number
  totalTokens: number
  totalInputTokens: number
  totalOutputTokens: number
  totalToolCalls: number
  totalCost: number
  totalSessions: number
  lastExecutionTime: string
  lastExecutionSuccess: boolean
}

// 会话追踪统计信息类型定义
export interface SessionTraceStatistics {
  sessionId: string
  sessionTitle: string
  agentId: string
  agentName: string
  totalExecutions: number
  successfulExecutions: number
  failedExecutions: number
  successRate: number
  totalTokens: number
  totalInputTokens: number
  totalOutputTokens: number
  totalToolCalls: number
  totalExecutionTime: number
  totalCost: number
  sessionCreatedTime?: string
  lastExecutionTime: string
  lastExecutionSuccess: boolean
  isArchived: boolean
}

// API 响应类型定义
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 查询参数类型定义
export interface GetAgentTraceStatisticsParams {
  keyword?: string
  startTime?: string
  endTime?: string
  hasSuccessfulExecution?: boolean
}

export interface GetSessionTraceStatisticsParams {
  keyword?: string
  startTime?: string
  endTime?: string
  hasSuccessfulExecution?: boolean
  includeArchived?: boolean
}

// 会话执行摘要信息类型定义
export interface SessionExecutionSummary {
  traceId: string
  agentId: string
  sessionId: string
  executionStartTime: string
  executionEndTime: string
  totalExecutionTime: number
  totalTokens: number
  totalInputTokens: number
  totalOutputTokens: number
  toolCallCount: number
  totalCost: number
  executionSuccess: boolean
  errorPhase?: string
  errorMessage?: string
}

// 会话执行详情类型定义
export interface SessionExecutionDetail {
  id: string
  traceId: string
  sequenceNo: number
  stepType: string
  messageType: string
  content: string
  modelId?: string
  providerName?: string
  tokenCount?: number
  executionTime?: number
  cost?: number
  success: boolean
  errorMessage?: string
  toolName?: string
  createdAt: string
  // 会话相关信息
  sessionId: string
  agentId: string
  executionStartTime: string
  executionEndTime: string
}

// 获取用户的 Agent 执行链路统计信息
export async function getUserAgentTraceStatistics(
  params?: GetAgentTraceStatisticsParams
): Promise<ApiResponse<AgentTraceStatistics[]>> {
  try {
    console.log('Fetching user agent trace statistics')
    
    const response = await httpClient.get<ApiResponse<AgentTraceStatistics[]>>(
      '/traces/agents',
      { params }
    );
    
    return response;
  } catch (error) {
    console.error('获取 Agent 追踪统计信息错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 获取指定 Agent 下的会话执行链路统计信息
export async function getAgentSessionTraceStatistics(
  agentId: string,
  params?: GetSessionTraceStatisticsParams
): Promise<ApiResponse<SessionTraceStatistics[]>> {
  try {
    console.log(`Fetching session trace statistics for agent: ${agentId}`)
    
    const response = await httpClient.get<ApiResponse<SessionTraceStatistics[]>>(
      `/traces/agents/${agentId}/sessions`,
      { params }
    );
    
    return response;
  } catch (error) {
    console.error('获取会话追踪统计信息错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 获取执行详情（复用现有接口）
export async function getTraceDetail(traceId: string): Promise<ApiResponse<any>> {
  try {
    console.log(`Fetching trace detail: ${traceId}`)
    
    const response = await httpClient.get<ApiResponse<any>>(`/traces/${traceId}`);
    
    return response;
  } catch (error) {
    console.error('获取追踪详情错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 获取执行详情列表（复用现有接口）
export async function getExecutionDetails(traceId: string): Promise<ApiResponse<any[]>> {
  try {
    console.log(`Fetching execution details: ${traceId}`)
    
    const response = await httpClient.get<ApiResponse<any[]>>(`/traces/${traceId}/details`);
    
    return response;
  } catch (error) {
    console.error('获取执行详情列表错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 根据会话ID直接获取执行详情列表
export async function getExecutionDetailsBySessionId(sessionId: string): Promise<ApiResponse<any[]>> {
  try {
    console.log(`Fetching execution details by session ID: ${sessionId}`)
    
    const response = await httpClient.get<ApiResponse<any[]>>(`/traces/${sessionId}/details`);
    
    return response;
  } catch (error) {
    console.error('获取会话执行详情列表错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 根据会话ID获取执行历史记录
export async function getSessionExecutionHistory(sessionId: string): Promise<ApiResponse<SessionExecutionSummary[]>> {
  try {
    console.log(`Fetching session execution history: ${sessionId}`)
    
    const response = await httpClient.get<ApiResponse<SessionExecutionSummary[]>>(`/traces/sessions/${sessionId}`);
    
    return response;
  } catch (error) {
    console.error('获取会话执行历史错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 根据会话ID获取所有执行详情
export async function getSessionExecutionDetails(sessionId: string): Promise<ApiResponse<SessionExecutionDetail[]>> {
  try {
    console.log(`Fetching session execution details: ${sessionId}`)
    
    // 直接通过会话ID调用详情接口
    const detailsResponse = await getExecutionDetailsBySessionId(sessionId);
    
    if (detailsResponse.code !== 200 || !detailsResponse.data) {
      return {
        code: detailsResponse.code,
        message: detailsResponse.message,
        data: [],
        timestamp: Date.now(),
      }
    }
    
    // 转换数据结构
    const sessionDetails = detailsResponse.data.map((detail: any, index: number) => ({
      id: sessionId + '_' + (detail.sequenceNo || index),
      traceId: detail.traceId || sessionId,
      sequenceNo: detail.sequenceNo || index,
      stepType: getStepTypeFromMessageType(detail.messageType),
      messageType: detail.messageType,
      content: detail.messageContent || '无内容',
      modelId: detail.modelId,
      providerName: detail.providerName,
      tokenCount: detail.messageTokens,
      executionTime: detail.modelCallTime || detail.toolExecutionTime,
      cost: detail.stepCost,
      success: detail.stepSuccess,
      errorMessage: detail.stepErrorMessage,
      toolName: detail.toolName,
      createdAt: detail.createdTime,
      // 会话相关信息
      sessionId: sessionId,
      agentId: detail.agentId,
      executionStartTime: detail.executionStartTime,
      executionEndTime: detail.executionEndTime,
    }));
    
    // 按序号排序
    sessionDetails.sort((a, b) => (a.sequenceNo || 0) - (b.sequenceNo || 0));
    
    return {
      code: 200,
      message: "获取成功",
      data: sessionDetails,
      timestamp: Date.now(),
    };
    
  } catch (error) {
    console.error('获取会话执行详情错误:', error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 辅助函数：根据消息类型推断步骤类型
function getStepTypeFromMessageType(messageType: string): string {
  switch (messageType?.toUpperCase()) {
    case 'USER':
    case 'HUMAN':
    case 'USER_MESSAGE':
      return 'USER_MESSAGE';
    case 'ASSISTANT':
    case 'AI':
    case 'AI_RESPONSE':
      return 'AI_RESPONSE';
    case 'TOOL':
    case 'TOOL_CALL':
      return 'TOOL_CALL';
    default:
      return messageType || 'UNKNOWN';
  }
}

// 使用 toast 包装的 API 函数
export const getUserAgentTraceStatisticsWithToast = withToast(getUserAgentTraceStatistics, {
  showSuccessToast: false,
  errorTitle: "获取 Agent 追踪统计失败"
})

export const getAgentSessionTraceStatisticsWithToast = withToast(getAgentSessionTraceStatistics, {
  showSuccessToast: false,
  errorTitle: "获取会话追踪统计失败"
})

export const getTraceDetailWithToast = withToast(getTraceDetail, {
  showSuccessToast: false,
  errorTitle: "获取追踪详情失败"
})

export const getExecutionDetailsWithToast = withToast(getExecutionDetails, {
  showSuccessToast: false,
  errorTitle: "获取执行详情失败"
})

export const getExecutionDetailsBySessionIdWithToast = withToast(getExecutionDetailsBySessionId, {
  showSuccessToast: false,
  errorTitle: "获取会话执行详情失败"
})

export const getSessionExecutionDetailsWithToast = withToast(getSessionExecutionDetails, {
  showSuccessToast: false,
  errorTitle: "获取会话执行详情失败"
})