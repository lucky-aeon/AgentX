import { httpClient } from "@/lib/http-client"
import { withToast } from "@/lib/toast-utils"

// API响应类型
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 用户设置配置类型 - 对应后端UserSettingsConfig
export interface UserSettingsConfig {
  defaultModel: string | null
}

// 用户设置类型 - 对应后端UserSettingsDTO
export interface UserSettings {
  id?: string
  userId?: string
  settingConfig: UserSettingsConfig
}

// 更新用户设置请求类型 - 对应后端UserSettingsUpdateRequest
export interface UserSettingsUpdateRequest {
  settingConfig: UserSettingsConfig
}

// 模型类型 - 对应后端ModelDTO
export interface Model {
  id: string
  userId?: string
  providerId: string
  providerName?: string
  modelId: string
  name: string
  description?: string
  type: string
  isOfficial: boolean
  status: boolean
  createdAt?: string
  updatedAt?: string
}

// 获取用户设置
export async function getUserSettings(): Promise<ApiResponse<UserSettings>> {
  try {
    const response = await httpClient.get<ApiResponse<UserSettings>>('/users/settings')
    return response
  } catch (error) {
    console.error("获取用户设置错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as UserSettings,
      timestamp: Date.now(),
    }
  }
}

// 更新用户设置
export async function updateUserSettings(data: UserSettingsUpdateRequest): Promise<ApiResponse<UserSettings>> {
  try {
    console.log('Updating user settings:', data)
    
    const response = await httpClient.put<ApiResponse<UserSettings>>('/users/settings', data)
    
    return response
  } catch (error) {
    console.error("更新用户设置错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as UserSettings,
      timestamp: Date.now(),
    }
  }
}

// 获取用户默认模型ID
export async function getUserDefaultModelId(): Promise<ApiResponse<string>> {
  try {
    const response = await httpClient.get<ApiResponse<string>>('/users/settings/default-model')
    return response
  } catch (error) {
    console.error("获取用户默认模型错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as string,
      timestamp: Date.now(),
    }
  }
}

// 获取所有激活的模型（使用正确的API）
export async function getAllModels(): Promise<ApiResponse<Model[]>> {
  try {
    console.log('Fetching all models for user settings')
    
    const response = await httpClient.get<ApiResponse<Model[]>>('/llm/models', {
      params: { type: 'all' }
    })
    
    return response
  } catch (error) {
    console.error("获取模型列表错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 带Toast提示的函数
export const getUserSettingsWithToast = withToast(getUserSettings, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取用户设置失败"
})

export const updateUserSettingsWithToast = withToast(updateUserSettings, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "用户设置已更新",
  errorTitle: "更新用户设置失败"
})

export const getUserDefaultModelIdWithToast = withToast(getUserDefaultModelId, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取默认模型失败"
})

export const getAllModelsWithToast = withToast(getAllModels, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取模型列表失败"
}) 