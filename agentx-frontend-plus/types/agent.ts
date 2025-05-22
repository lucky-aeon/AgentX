export interface ModelConfig {
  modelName: string
  temperature?: number
  topP?: number
  maxTokens?: number
  loadMemory?: boolean
  systemMessage?: string
}

export interface AgentTool {
  id: string
  name: string
  description?: string
  type?: string
  permissions?: string
  config?: Record<string, any>
  presetParameters?: Record<string, Record<string, string>> // 预设参数：{功能名: {参数名: 参数值}}
}

export interface Agent {
  id: string
  name: string
  avatar: string | null
  description: string
  systemPrompt: string
  welcomeMessage: string
  modelConfig: ModelConfig
  tools: AgentTool[]
  knowledgeBaseIds: string[]
  publishedVersion: string | null
  enabled: boolean // 更新为布尔值，表示启用/禁用状态
  agentType: number
  userId: string
  createdAt: string
  updatedAt: string
  statusText?: string
  agentTypeText?: string
  modelId?: string // 关联的模型ID
  modelName?: string // 关联的模型名称
}

// API响应基本结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 助理类型枚举
export enum AgentType {
  CHAT = 1,
  FUNCTIONAL = 2,
}

// 获取助理列表请求参数
export interface GetAgentsParams {
  userId: string
  name?: string // 添加名称搜索参数
}

// 创建助理请求参数
export interface CreateAgentRequest {
  name: string
  avatar?: string | null
  description?: string
  agentType: "CHAT_ASSISTANT" | "FUNCTIONAL_AGENT"
  systemPrompt?: string
  welcomeMessage?: string
  modelConfig: ModelConfig
  tools?: AgentTool[]
  knowledgeBaseIds?: string[]
  userId: string
}

// 更新助理请求参数
export interface UpdateAgentRequest {
  name?: string
  avatar?: string | null
  description?: string
  systemPrompt?: string
  welcomeMessage?: string
  modelConfig?: ModelConfig
  tools?: AgentTool[]
  knowledgeBaseIds?: string[]
  agentType?: number
  enabled?: boolean
}

// 发布助理版本请求参数
export interface PublishAgentVersionRequest {
  versionNumber: string
  changeLog: string
  systemPrompt?: string
  welcomeMessage?: string
  modelConfig?: ModelConfig
  tools?: AgentTool[]
  knowledgeBaseIds?: string[]
}

// 搜索助理请求参数
export interface SearchAgentsRequest {
  name?: string
}

// 助理版本信息
export interface AgentVersion {
  id: string
  agentId: string
  name: string
  avatar: string
  description: string
  versionNumber: string
  systemPrompt: string
  welcomeMessage: string
  modelConfig: ModelConfig
  tools: AgentTool[]
  knowledgeBaseIds: string[]
  changeLog: string
  agentType: number
  publishStatus: number // 1-审核中, 2-已发布, 3-拒绝, 4-已下架
  rejectReason: string | null
  reviewTime: string
  publishedAt: string
  userId: string
  createdAt: string
  updatedAt: string
  deletedAt: string | null
  agentTypeText?: string
  publishStatusText?: string
  published?: boolean
  rejected?: boolean
  reviewing?: boolean
  removed?: boolean
  addWorkspace?: boolean // 是否已添加到工作区
}

// 发布状态枚举
export enum PublishStatus {
  REVIEWING = 1,
  PUBLISHED = 2,
  REJECTED = 3,
  REMOVED = 4,
}

