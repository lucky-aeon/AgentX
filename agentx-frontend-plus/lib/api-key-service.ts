import type { ApiResponse } from "@/types/api"
import type {
  ApiKeyResponse,
  CreateApiKeyRequest,
  UpdateApiKeyStatusRequest,
  GetUserApiKeysParams,
  GetAgentApiKeysParams
} from "@/types/api-key"
import { withToast } from "./toast-utils"
import { httpClient } from "@/lib/http-client"
import { API_ENDPOINTS } from "@/lib/api-config"

// 获取用户的API密钥列表
export async function getUserApiKeys(params?: GetUserApiKeysParams): Promise<ApiResponse<ApiKeyResponse[]>> {
  try {
    console.log("Fetching user API keys with params:", params)
    
    const response = await httpClient.get<ApiResponse<ApiKeyResponse[]>>(
      API_ENDPOINTS.API_KEYS,
      { params }
    )
    
    return response
  } catch (error) {
    console.error("获取API密钥列表错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 获取Agent的API密钥列表
export async function getAgentApiKeys(params: GetAgentApiKeysParams): Promise<ApiResponse<ApiKeyResponse[]>> {
  try {
    console.log(`Fetching API keys for agent: ${params.agentId}`)
    
    const response = await httpClient.get<ApiResponse<ApiKeyResponse[]>>(
      API_ENDPOINTS.AGENT_API_KEYS(params.agentId)
    )
    
    return response
  } catch (error) {
    console.error("获取Agent API密钥列表错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 获取API密钥详情
export async function getApiKey(apiKeyId: string): Promise<ApiResponse<ApiKeyResponse>> {
  try {
    console.log(`Fetching API key detail: ${apiKeyId}`)
    
    const response = await httpClient.get<ApiResponse<ApiKeyResponse>>(
      API_ENDPOINTS.API_KEY_DETAIL(apiKeyId)
    )
    
    return response
  } catch (error) {
    console.error("获取API密钥详情错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as ApiKeyResponse,
      timestamp: Date.now(),
    }
  }
}

// 创建API密钥
export async function createApiKey(request: CreateApiKeyRequest): Promise<ApiResponse<ApiKeyResponse>> {
  try {
    console.log("Creating API key", request)
    
    const response = await httpClient.post<ApiResponse<ApiKeyResponse>>(
      API_ENDPOINTS.CREATE_API_KEY,
      request
    )
    
    return response
  } catch (error) {
    console.error("创建API密钥错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as ApiKeyResponse,
      timestamp: Date.now(),
    }
  }
}

// 更新API密钥状态
export async function updateApiKeyStatus(
  apiKeyId: string, 
  request: UpdateApiKeyStatusRequest
): Promise<ApiResponse<void>> {
  try {
    console.log(`Updating API key status: ${apiKeyId}`, request)
    
    const response = await httpClient.put<ApiResponse<void>>(
      API_ENDPOINTS.UPDATE_API_KEY_STATUS(apiKeyId),
      request
    )
    
    return response
  } catch (error) {
    console.error("更新API密钥状态错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as void,
      timestamp: Date.now(),
    }
  }
}

// 删除API密钥
export async function deleteApiKey(apiKeyId: string): Promise<ApiResponse<void>> {
  try {
    console.log(`Deleting API key: ${apiKeyId}`)
    
    const response = await httpClient.delete<ApiResponse<void>>(
      API_ENDPOINTS.DELETE_API_KEY(apiKeyId)
    )
    
    return response
  } catch (error) {
    console.error("删除API密钥错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as void,
      timestamp: Date.now(),
    }
  }
}

// 重置API密钥
export async function resetApiKey(apiKeyId: string): Promise<ApiResponse<ApiKeyResponse>> {
  try {
    console.log(`Resetting API key: ${apiKeyId}`)
    
    const response = await httpClient.post<ApiResponse<ApiKeyResponse>>(
      API_ENDPOINTS.RESET_API_KEY(apiKeyId),
      {}
    )
    
    return response
  } catch (error) {
    console.error("重置API密钥错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as ApiKeyResponse,
      timestamp: Date.now(),
    }
  }
}

// 带Toast提示的API服务函数
export const getUserApiKeysWithToast = withToast(getUserApiKeys, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取API密钥列表失败"
})

export const getAgentApiKeysWithToast = withToast(getAgentApiKeys, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取Agent API密钥列表失败"
})

export const getApiKeyWithToast = withToast(getApiKey, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取API密钥详情失败"
})

export const createApiKeyWithToast = withToast(createApiKey, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "创建API密钥成功",
  errorTitle: "创建API密钥失败"
})

export const updateApiKeyStatusWithToast = withToast(updateApiKeyStatus, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "更新API密钥状态成功",
  errorTitle: "更新API密钥状态失败"
})

export const deleteApiKeyWithToast = withToast(deleteApiKey, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "删除API密钥成功",
  errorTitle: "删除API密钥失败"
})

export const resetApiKeyWithToast = withToast(resetApiKey, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "重置API密钥成功",
  errorTitle: "重置API密钥失败"
})