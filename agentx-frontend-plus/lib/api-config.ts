export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api",
  CURRENT_USER_ID: "1", // 当前用户ID
}

// API 端点
export const API_ENDPOINTS = {
  // 会话相关
  SESSION: "/agent/session",
  SESSION_DETAIL: (id: string) => `/agent/session/${id}`,
  SESSION_MESSAGES: (id: string) => `/agent/session/${id}/messages`,
  DELETE_SESSION: (id: string) => `/agent/session/${id}`,
  CHAT: "/agent/session/chat",
  SEND_MESSAGE: (sessionId: string) => `/agent/session/${sessionId}/message`,
  
  // 任务相关
  SESSION_TASKS: (sessionId: string) => `/tasks/session/${sessionId}/latest`,
  SESSION_TASK_DETAIL: (taskId: string) => `/tasks/${taskId}`,

  // 助理相关
  USER_AGENTS: (userId: string) => `/agent/user/${userId}`,
  AGENT_DETAIL: (id: string) => `/agent/${id}`,
  CREATE_AGENT: "/agent",
  UPDATE_AGENT: (id: string) => `/agent/${id}`,
  DELETE_AGENT: (id: string) => `/agent/${id}`,
  TOGGLE_AGENT_STATUS: (id: string) => `/agent/${id}/toggle-status`,
  AGENT_VERSIONS: (id: string) => `/agent/${id}/versions`,
  AGENT_VERSION_DETAIL: (id: string, version: string) => `/agent/${id}/versions/${version}`,
  AGENT_LATEST_VERSION: (id: string) => `/agent/${id}/versions/latest`,
  PUBLISH_AGENT_VERSION: (id: string) => `/agent/${id}/publish`,
  PUBLISHED_AGENTS: "/agent/published",
  
  // Agent工作区相关
  AGENT_WORKSPACE: "/agent/workspace",
  ADD_AGENT_TO_WORKSPACE: (agentId: string) => `/agent/workspace/${agentId}`,
  AGENT_MODEL_CONFIG: (agentId: string) => `/agent/workspace/${agentId}/model-config`,
  SET_AGENT_MODEL_CONFIG: (agentId: string) => `/agent/workspace/${agentId}/model/config`,
  
  // LLM相关
  PROVIDERS: "/llm/providers",
  PROVIDER_DETAIL: (id: string) => `/llm/providers/${id}`,
  CREATE_PROVIDER: "/llm/providers",
  UPDATE_PROVIDER: "/llm/providers",
  DELETE_PROVIDER: (id: string) => `/llm/providers/${id}`,
  PROVIDER_PROTOCOLS: "/llm/providers/protocols",
  TOGGLE_PROVIDER_STATUS: (id: string) => `/llm/providers/${id}/status`,
  
  // 模型相关
  MODEL_DETAIL: (id: string) => `/llm/models/${id}`,
  CREATE_MODEL: "/llm/models",
  UPDATE_MODEL: "/llm/models",
  DELETE_MODEL: (id: string) => `/llm/models/${id}`,
  TOGGLE_MODEL_STATUS: (id: string) => `/llm/models/${id}/status`,
  MODEL_TYPES: "/llm/models/types",
}

// 构建完整的API URL
export function buildApiUrl(endpoint: string, queryParams?: Record<string, any>): string {
  let url = `${API_CONFIG.BASE_URL}${endpoint}`

  if (queryParams && Object.keys(queryParams).length > 0) {
    const query = Object.entries(queryParams)
      .filter(([_, value]) => value !== undefined && value !== null)
      .map(([key, value]) => {
        if (typeof value === "boolean") {
          return value ? key : null
        }
        return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`
      })
      .filter(Boolean)
      .join("&")

    if (query) {
      url += `?${query}`
    }
  }

  return url
}

