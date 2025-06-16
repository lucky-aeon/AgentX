export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api",
  CURRENT_USER_ID: "1", // 当前用户ID
}

// API 端点
export const API_ENDPOINTS = {
  // 会话相关
  SESSION: "/agents/sessions",
  SESSION_DETAIL: (id: string) => `/agents/sessions/${id}`,
  SESSION_MESSAGES: (id: string) => `/agents/sessions/${id}/messages`,
  DELETE_SESSION: (id: string) => `/agents/sessions/${id}`,
  CHAT: "/agents/sessions/chat",
  SEND_MESSAGE: (sessionId: string) => `/agents/sessions/${sessionId}/message`,
  
  // 任务相关
  SESSION_TASKS: (sessionId: string) => `/tasks/session/${sessionId}/latest`,
  SESSION_TASK_DETAIL: (taskId: string) => `/tasks/${taskId}`,

  // 助理相关
  USER_AGENTS: (userId: string) => `/agents/user/${userId}`,
  AGENT_DETAIL: (id: string) => `/agents/${id}`,
  CREATE_AGENT: "/agents",
  UPDATE_AGENT: (id: string) => `/agents/${id}`,
  DELETE_AGENT: (id: string) => `/agents/${id}`,
  TOGGLE_AGENT_STATUS: (id: string) => `/agents/${id}/toggle-status`,
  AGENT_VERSIONS: (id: string) => `/agents/${id}/versions`,
  AGENT_VERSION_DETAIL: (id: string, version: string) => `/agents/${id}/versions/${version}`,
  AGENT_LATEST_VERSION: (id: string) => `/agents/${id}/versions/latest`,
  PUBLISH_AGENT_VERSION: (id: string) => `/agents/${id}/publish`,
  PUBLISHED_AGENTS: "/agents/published",
  GENERATE_SYSTEM_PROMPT: "/agents/generate-system-prompt",
  
  // Agent工作区相关
  AGENT_WORKSPACE: "/agents/workspaces",
  ADD_AGENT_TO_WORKSPACE: (agentId: string) => `/agents/workspaces/${agentId}`,
  AGENT_MODEL_CONFIG: (agentId: string) => `/agents/workspaces/${agentId}/model-config`,
  SET_AGENT_MODEL_CONFIG: (agentId: string) => `/agents/workspaces/${agentId}/model/config`,
  
  // LLM相关
  PROVIDERS: "/llms/providers",
  PROVIDER_DETAIL: (id: string) => `/llms/providers/${id}`,
  CREATE_PROVIDER: "/llms/providers",
  UPDATE_PROVIDER: "/llms/providers",
  DELETE_PROVIDER: (id: string) => `/llms/providers/${id}`,
  PROVIDER_PROTOCOLS: "/llms/providers/protocols",
  TOGGLE_PROVIDER_STATUS: (id: string) => `/llms/providers/${id}/status`,
  
  // 模型相关
  MODELS: "/llms/models", // 获取模型列表
  DEFAULT_MODEL: "/llms/models/default", // 获取默认模型
  MODEL_DETAIL: (id: string) => `/llms/models/${id}`,
  CREATE_MODEL: "/llms/models",
  UPDATE_MODEL: "/llms/models",
  DELETE_MODEL: (id: string) => `/llms/models/${id}`,
  TOGGLE_MODEL_STATUS: (id: string) => `/llms/models/${id}/status`,
  MODEL_TYPES: "/llms/models/types",
  
  // 工具市场相关
  MARKET_TOOLS: "/tools/market",
  MARKET_TOOL_DETAIL: (id: string) => `/tools/market/${id}`,
  MARKET_TOOL_VERSION_DETAIL: (id: string, version: string) => `/tools/market/${id}/${version}`,
  MARKET_TOOL_VERSIONS: (id: string) => `/tools/market/${id}/versions`,
  MARKET_TOOL_LABELS: "/tools/market/labels",
  RECOMMEND_TOOLS: "/tools/recommend", // 推荐工具列表
  INSTALL_TOOL: (toolId: string, version: string) => `/tools/install/${toolId}/${version}`,
  USER_TOOLS: "/tools/user",
  INSTALLED_TOOLS: "/tools/installed", // 已安装的工具列表
  UNINSTALL_TOOL: (toolId: string) => `/tools/uninstall/${toolId}`, // 卸载工具
  DELETE_USER_TOOL: (id: string) => `/tools/user/${id}`,
  UPLOAD_TOOL: "/tools",
  UPDATE_TOOL: (toolId: string) => `/tools/${toolId}`,
  TOOL_DETAIL: (toolId: string) => `/tools/${toolId}`,
  DELETE_TOOL: (toolId: string) => `/tools/${toolId}`,
  GET_TOOL_LATEST_VERSION: (toolId: string) => `/tools/${toolId}/latest`, // 获取工具最新版本
  UPDATE_TOOL_VERSION_STATUS: (toolId: string, version: string) => `/tools/user/${toolId}/${version}/status`, // 修改工具版本发布状态
  PUBLISH_TOOL_TO_MARKET: "/tools/market", // 上架工具到市场
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

