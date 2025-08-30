import { API_CONFIG, API_ENDPOINTS } from "./api-config"
import type { ApiResponse } from "@/types/agent"
import { MessageType } from "@/types/conversation"
import { withToast } from "./toast-utils"
import { httpClient } from "@/lib/http-client"

// 消息类型定义
export interface MessageDTO {
  id: string
  sessionId: string
  role: string
  content: string
  messageType?: MessageType | string
  createdAt?: string
  updatedAt?: string
  fileUrls?: string[] // 文件URL列表
}

// 获取会话消息列表
export async function getSessionMessages(sessionId: string): Promise<ApiResponse<MessageDTO[]>> {
  try {
    const data = await httpClient.get<ApiResponse<MessageDTO[]>>(
      API_ENDPOINTS.SESSION_MESSAGES(sessionId)
    )
    return data
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取会话消息失败",
      data: [] as MessageDTO[],
      timestamp: Date.now(),
    }
  }
}

export async function createSession(title: string, userId: string, description?: string): Promise<ApiResponse<any>> {
  try {
    const params: Record<string, any> = { title, userId }
    if (description) params.description = description
    const data = await httpClient.post<ApiResponse<any>>(
      API_ENDPOINTS.SESSION,
      {},
      { method: "POST", params }
    )
    return data
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "创建会话失败",
      data: null,
      timestamp: Date.now(),
    }
  }
}

export async function updateSession(id: string, title: string, description?: string): Promise<ApiResponse<any>> {
  try {
    const params: Record<string, any> = { title }
    if (description) params.description = description
    const data = await httpClient.put<ApiResponse<any>>(
      API_ENDPOINTS.SESSION_DETAIL(id),
      {},
      { method: "PUT", params }
    )
    return data
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "更新会话失败",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 删除会话
export async function deleteSession(sessionId: string): Promise<ApiResponse<null>> {
  try {
    const data = await httpClient.delete<ApiResponse<null>>(
      API_ENDPOINTS.DELETE_SESSION(sessionId)
    )
    return data
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "删除会话失败",
      data: null,
      timestamp: Date.now(),
    }
  }
}

export async function getSessions(userId: string, archived?: boolean): Promise<ApiResponse<any>> {
  try {
    const params: Record<string, any> = { userId }
    if (archived !== undefined) params.archived = archived
    const data = await httpClient.get<ApiResponse<any>>(
      API_ENDPOINTS.SESSION,
      { params }
    )
    return data
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取会话列表失败",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 使用toast包装的API函数
export const getSessionMessagesWithToast = withToast(getSessionMessages, {
  showSuccessToast: false,
  errorTitle: "获取会话消息失败"
})

export const createSessionWithToast = withToast(createSession, {
  successTitle: "创建会话成功",
  errorTitle: "创建会话失败"
})

export const updateSessionWithToast = withToast(updateSession, {
  successTitle: "更新会话成功",
  errorTitle: "更新会话失败"
})

export const deleteSessionWithToast = withToast(deleteSession, {
  successTitle: "删除会话成功",
  errorTitle: "删除会话失败"
})

export const getSessionsWithToast = withToast(getSessions, {
  showSuccessToast: false,
  errorTitle: "获取会话列表失败"
})

