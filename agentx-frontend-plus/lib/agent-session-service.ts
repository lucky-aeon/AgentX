import { API_CONFIG } from "@/lib/api-config"
import type { ApiResponse } from "@/types/agent"
import { withToast } from "./toast-utils"
import { toast } from "@/hooks/use-toast"
import { httpClient } from "@/lib/http-client"

// 会话类型定义
export interface SessionDTO {
  id: string
  title: string
  description: string
  createdAt: string
  updatedAt: string
  isArchived: boolean
  agentId: string
  multiModal?: boolean
}

// 获取助理会话列表
export async function getAgentSessions(agentId: string): Promise<ApiResponse<SessionDTO[]>> {
  try {
    const data = await httpClient.get<ApiResponse<SessionDTO[]>>(
      `/agents/sessions/${agentId}`
    )
    return data
  } catch (error) {
 
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as SessionDTO[],
      timestamp: Date.now(),
    }
  }
}

// 创建助理会话
export async function createAgentSession(agentId: string): Promise<ApiResponse<SessionDTO>> {
  try {
    const data = await httpClient.post<ApiResponse<SessionDTO>>(
      `/agents/sessions/${agentId}`
    )
    toast({
      description: data.message,
      variant: "default",
    });
    return data
  } catch (error) {
 
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as SessionDTO,
      timestamp: Date.now(),
    }
  }
}

// 更新助理会话
export async function updateAgentSession(sessionId: string, title: string): Promise<ApiResponse<null>> {
  try {
    const params = { title }
    const data = await httpClient.put<ApiResponse<null>>(
      `/agents/sessions/${sessionId}`,
      {},
      { params }
    )
    return data
  } catch (error) {
 
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 删除助理会话
export async function deleteAgentSession(sessionId: string): Promise<ApiResponse<null>> {
  try {
    const data = await httpClient.delete<ApiResponse<null>>(
      `/agents/sessions/${sessionId}`
    )
    return data
  } catch (error) {
 
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 中断助理会话
export async function interruptSession(sessionId: string): Promise<ApiResponse<string>> {
  try {
    const data = await httpClient.post<ApiResponse<string>>(
      `/agents/sessions/${sessionId}/interrupt`
    )
    return data
  } catch (error) {
 
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "中断会话失败",
      data: "",
      timestamp: Date.now(),
    }
  }
}

// 使用toast包装的API函数
export const getAgentSessionsWithToast = withToast(getAgentSessions, {
  showSuccessToast: false,
  errorTitle: "获取助理会话列表失败"
})

export const createAgentSessionWithToast = withToast(createAgentSession, {
  successTitle: "创建助理会话成功",
  errorTitle: "创建助理会话失败"
})

export const updateAgentSessionWithToast = withToast(updateAgentSession, {
  successTitle: "更新助理会话成功",
  errorTitle: "更新助理会话失败"
})

export const deleteAgentSessionWithToast = withToast(deleteAgentSession, {
  successTitle: "删除助理会话成功",
  errorTitle: "删除助理会话失败"
})

export const interruptSessionWithToast = withToast(interruptSession, {
  successTitle: "对话中断成功",
  errorTitle: "对话中断失败",
  showSuccessToast: false // 由useInterruptableChat Hook自己处理toast
})

// 统一的AgentSessionService对象，方便其他模块调用
export const AgentSessionService = {
  getAgentSessions,
  createAgentSession,
  updateAgentSession,
  deleteAgentSession,
  interruptSession,
  getAgentSessionsWithToast,
  createAgentSessionWithToast,
  updateAgentSessionWithToast,
  deleteAgentSessionWithToast,
  interruptSessionWithToast
}

