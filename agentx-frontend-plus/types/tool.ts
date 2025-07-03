export interface Tool {
  id: string
  toolId?: string // 工具ID
  name: string
  icon: string | null
  subtitle: string
  description: string
  user_id: string
  author: string // 作者名称（前端显示用）
  labels: string[]
  tool_type: string
  upload_type: string
  upload_url: string
  install_command: PluginInstallConfig
  tool_list: ToolItem[]
  status: ToolStatus
  is_office: boolean
  installCount: number // 前端展示用
  current_version?: string // 当前版本号
  mcpServerName?: string // MCP服务器名称，用于预设参数
  isGlobal?: boolean // 是否为全局工具
  createdAt: string
  updatedAt: string
}

export interface ToolVersion {
  id: string
  name: string
  icon: string | null
  subtitle: string
  description: string
  user_id: string
  version: string
  tool_id: string
  upload_type: string
  upload_url: string
  tool_list: ToolItem[]
  labels: string[]
  is_office: boolean
  public_status: boolean
  createdAt: string
  updatedAt: string
  author: string // 作者名称（前端展示用）
}

export interface ToolItem {
  name: string
  description: string
  inputSchema?: {
    type: string
    properties: Record<string, any>
    required: string[]
  }
  parameters?: {
    properties: Record<string, { description: string | null } | any>
    required: string[]
  }
}

export type PluginInstallConfig = SsePluginConfig | StdioPluginConfig;

export interface SsePluginConfig {
  type: 'sse'
  url: string
}

export interface StdioPluginConfig {
  type: 'stdio'
  command: string
  args: string[]
  env: Record<string, string>
}

export enum ToolStatus {
  PENDING = "等待审核",
  DEPLOYING = "部署中",
  FETCHING_TOOLS = "获取工具列表",
  MANUAL_REVIEW = "人工审核",
  APPROVED = "通过",
  FAILED = "失败"
}

// API响应基本结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应结构
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 获取工具市场工具列表请求参数
export interface GetMarketToolsParams {
  toolName?: string
  labels?: string[]
  toolType?: string
  isOffice?: boolean
  page?: number
  pageSize?: number
}

// 工具版本详情响应
export interface ToolVersionDTO {
  id: string
  name: string
  icon: string
  subtitle: string
  description: string
  userId: string
  version: string
  toolId: string
  uploadType: string
  uploadUrl: string
  toolList: ToolItem[]
  labels: string[]
  isOffice: boolean
  publicStatus: boolean
  changeLog: string
  createdAt: string
  updatedAt: string
  userName: string
  versions: ToolVersionDTO[]
  office: boolean
}

// 用户安装工具请求参数
export interface InstallToolParams {
  toolId: string
  version: string
}

export interface PublishToolToMarketParams {
  toolId: string;
  version: string;
  changeLog: string;
} 