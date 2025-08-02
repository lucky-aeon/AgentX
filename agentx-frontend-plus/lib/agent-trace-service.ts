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