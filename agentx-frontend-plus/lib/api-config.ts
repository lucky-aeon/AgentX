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
  CHAT: (sessionId: string) => `/conversation/chat/${sessionId}`,
  SEND_MESSAGE: (sessionId: string) => `/agent/session/${sessionId}/message`,

  // 助理相关
  USER_AGENTS: (userId: string) => `/agent/user/${userId}`,
  AGENT_DETAIL: (id: string) => `/agent/${id}`,
  CREATE_AGENT: "/agent",
  UPDATE_AGENT: (id: string) => `/agent/${id}`,
  DELETE_AGENT: (id: string) => `/agent/${id}`,
  TOGGLE_AGENT_STATUS: (id: string) => `/agent/${id}/toggle-status`,
  AGENT_VERSIONS: (id: string) => `/agent/${id}/versions`,
  AGENT_VERSION_DETAIL: (id: string, version: string) => `/agent/${id}/versions/${version}`,
  PUBLISH_AGENT_VERSION: (id: string) => `/agent/${id}/publish`,
  PUBLISHED_AGENTS: "/agent/published",
  
  // LLM相关
  PROVIDERS: "/llm/providers",
  PROVIDER_DETAIL: (id: string) => `/llm/providers/${id}`,
  CREATE_PROVIDER: "/llm/providers",
  UPDATE_PROVIDER: "/llm/providers",
  DELETE_PROVIDER: (id: string) => `/llm/providers/${id}`,
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

