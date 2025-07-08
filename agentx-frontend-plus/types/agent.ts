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
  toolIds?: string[] // 工具ID列表，与后端返回格式兼容
  toolPresetParams?: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数
  knowledgeBaseIds: string[]
  publishedVersion: string | null
  enabled: boolean // 更新为布尔值，表示启用/禁用状态
  userId: string
  createdAt: string
  updatedAt: string
  statusText?: string
  modelId?: string // 关联的模型ID
  modelName?: string // 关联的模型名称
  multiModal?: boolean // 多模态功能开关
}

// API响应基本结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
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

  systemPrompt?: string
  welcomeMessage?: string
  modelConfig: ModelConfig
  tools?: AgentTool[]
  toolIds?: string[] // 工具ID列表，用于传递给后端
  toolPresetParams?: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数
  knowledgeBaseIds?: string[]
  userId: string
  multiModal?: boolean // 多模态功能开关
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
  toolIds?: string[] // 工具ID列表，用于传递给后端
  toolPresetParams?: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数
  knowledgeBaseIds?: string[]

  enabled?: boolean
  multiModal?: boolean // 多模态功能开关
}

// 发布助理版本请求参数
export interface PublishAgentVersionRequest {
  versionNumber: string
  changeLog: string
  systemPrompt?: string
  welcomeMessage?: string
  modelConfig?: ModelConfig
  tools?: AgentTool[]
  toolIds?: string[] // 工具ID列表，用于传递给后端
  toolPresetParams?: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数
  knowledgeBaseIds?: string[]
  multiModal?: boolean // 多模态功能开关
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
  toolIds?: string[] // 工具ID列表
  toolPresetParams?: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数
  knowledgeBaseIds: string[]
  changeLog: string
  publishStatus: number // 1-审核中, 2-已发布, 3-拒绝, 4-已下架
  rejectReason: string | null
  reviewTime: string
  publishedAt: string
  userId: string
  createdAt: string
  updatedAt: string
  deletedAt: string | null
  publishStatusText?: string
  published?: boolean
  rejected?: boolean
  reviewing?: boolean
  removed?: boolean
  addWorkspace?: boolean // 是否已添加到工作区
  multiModal?: boolean // 多模态功能开关
}

// 发布状态枚举
export enum PublishStatus {
  REVIEWING = 1,
  PUBLISHED = 2,
  REJECTED = 3,
  REMOVED = 4,
}

