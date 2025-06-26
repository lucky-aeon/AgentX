// API Key 相关类型定义

// API Key 状态枚举
export enum ApiKeyStatus {
  ACTIVE = true,
  INACTIVE = false
}

// API Key 响应类型
export interface ApiKeyResponse {
  id: string
  apiKey: string
  agentId: string
  agentName?: string
  userId: string
  name: string
  status: boolean
  usageCount: number
  lastUsedAt?: string
  expiresAt?: string
  createdAt: string
  updatedAt: string
  expired?: boolean
  available?: boolean
}

// 创建 API Key 请求类型
export interface CreateApiKeyRequest {
  agentId: string
  name: string
}

// 更新 API Key 状态请求类型
export interface UpdateApiKeyStatusRequest {
  status: boolean
}

// 获取用户 API Keys 请求参数
export interface GetUserApiKeysParams {
  name?: string
  status?: boolean
  agentId?: string
}

// 获取 Agent API Keys 请求参数
export interface GetAgentApiKeysParams {
  agentId: string
}

// API Key 统计信息
export interface ApiKeyStats {
  total: number
  active: number
  inactive: number
  totalUsage: number
}