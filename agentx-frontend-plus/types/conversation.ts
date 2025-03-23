// 会话类型定义
export interface Session {
  id: string
  title: string
  description: string | null
  createdAt: string
  updatedAt: string
  archived: boolean
}

// API响应基本结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 创建会话请求参数
export interface CreateSessionParams {
  title: string
  userId: string
  description?: string
}

// 获取会话列表请求参数
export interface GetSessionsParams {
  userId: string
  archived?: boolean
}

// 更新会话请求参数
export interface UpdateSessionParams {
  title?: string
  description?: string
  archived?: boolean
}

