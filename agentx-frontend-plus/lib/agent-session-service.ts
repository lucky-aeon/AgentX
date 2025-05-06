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
}

// 获取助理会话列表
export async function getAgentSessions(agentId: string): Promise<ApiResponse<SessionDTO[]>> {
  try {
    const data = await httpClient.get<ApiResponse<SessionDTO[]>>(
      `/agent/session/${agentId}`
    )
    return data
  } catch (error) {
    console.error("获取助理会话列表错误:", error)
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
      `/agent/session/${agentId}`
    )
    toast({
      description: data.message,
      variant: "default",
    });
    return data
  } catch (error) {
    console.error("创建助理会话错误:", error)
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
      `/agent/session/${sessionId}`,
      {},
      { params }
    )
    return data
  } catch (error) {
    console.error("更新助理会话错误:", error)
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
      `/agent/session/${sessionId}`
    )
    return data
  } catch (error) {
    console.error("删除助理会话错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
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

