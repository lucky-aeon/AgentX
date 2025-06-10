/**
 * API相关类型定义
 */

// API响应基础结构
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 流式响应类型
export interface StreamResponse {
  content: string;
  done: boolean;
  sessionId: string;
  provider?: string;
  model?: string;
  timestamp: number;
  messageType?: string;
  taskId?: string;
  tasks?: any[];
}

// 模型配置类型
export interface ModelConfig {
  modelId: string;
  temperature: number;
  topP: number;
  maxTokens: number;
  strategyType: string;
  reserveRatio: number;
  summaryThreshold: number;
}

// Provider类型
export interface Provider {
  id: string;
  name: string;
  type: string;
  protocol: string;
  baseUrl: string;
  apiKey: string;
  apiSecret?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: any;
}

// Model类型
export interface Model {
  id: string;
  name: string;
  providerId: string;
  providerName?: string;
  type: string;
  modelEndpoint?: string;
  contextSize: number;
  status: number;
  createdAt?: string;
  updatedAt?: string;
  [key: string]: any;
}

// Task类型
export interface Task {
  id: string;
  taskName: string;
  status: string;
  progress: number;
  parentTaskId: string;
  taskResult?: string;
  startTime?: string;
  endTime?: string;
}

// HTTP客户端请求配置
export interface RequestConfig extends RequestInit {
  params?: Record<string, any>;
  timeout?: number;
}

// HTTP客户端请求选项
export interface RequestOptions {
  raw?: boolean;
}

// HTTP客户端拦截器
export interface Interceptor {
  request?: (config: RequestConfig) => RequestConfig;
  response?: (response: Response, options?: RequestOptions) => Promise<any>;
  error?: (error: any) => Promise<any>;
} 