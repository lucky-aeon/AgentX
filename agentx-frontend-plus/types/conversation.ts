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

// 消息类型枚举
export enum MessageType {
  /**
   * 普通文本消息
   */
  TEXT = "TEXT",
  
  /**
   * 工具调用消息
   */
  TOOL_CALL = "TOOL_CALL",

  /**
   * 任务执行消息
   */
  TASK_EXEC = "TASK_EXEC",

  /**
   * 任务状态更新消息
   */
  TASK_STATUS = "TASK_STATUS",
  
  /**
   * 任务ID列表消息
   */
  TASK_IDS = "TASK_IDS",
  
  /**
   * 任务拆分完成消息
   */
  TASK_SPLIT_FINISH = "TASK_SPLIT_FINISH",

  /**
   * 任务进行中消息
   */
  TASK_IN_PROGRESS = "TASK_IN_PROGRESS",

  /**
   * 任务完成消息
   */
  TASK_COMPLETED = "TASK_COMPLETED",
  
  /**
   * 任务状态变为已完成消息
   */
  TASK_STATUS_TO_FINISH = "TASK_STATUS_TO_FINISH",
  
  /**
   * 任务状态变为加载中消息
   */
  TASK_STATUS_TO_LOADING = "TASK_STATUS_TO_LOADING"
}

// 消息接口
export interface Message {
  id: string
  sessionId?: string
  role: "USER" | "SYSTEM" | "assistant"
  content: string
  type?: MessageType
  createdAt?: string
  updatedAt?: string
  tasks?: any[] // 任务列表
  taskId?: string // 任务ID
  fileUrls?: string[] // 附件文件URL列表
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

