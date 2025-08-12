// API地址配置 - 智能IP获取，自动适配所有环境
function getDefaultApiUrl(): string {
  // 客户端环境：直接获取当前访问的IP/域名
  if (typeof window !== 'undefined') {
    const { protocol, hostname } = window.location;
    
    // 自动使用当前访问的地址 + 8088端口
    // 支持: localhost, 127.0.0.1, 192.168.x.x, 服务器IP, 域名等
    const apiProtocol = protocol === 'https:' ? 'https:' : 'http:';
    return `${apiProtocol}//${hostname}:8088/api`;
  }
  
  // 服务端渲染时的回退值
  return '/api';
}

export const API_CONFIG = {
  BASE_URL: getDefaultApiUrl(),
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
  
  // 管理员相关
  ADMIN_USERS: "/admin/users", // 管理员获取用户列表
  ADMIN_AGENTS: "/admin/agents", // 管理员获取Agent列表
  ADMIN_AGENT_STATISTICS: "/admin/agents/statistics", // 管理员获取Agent统计
  ADMIN_ORDERS: "/admin/orders", // 管理员获取订单列表
  ADMIN_ORDER_DETAIL: (id: string) => `/admin/orders/${id}`, // 管理员获取订单详情
  
  // 订单相关
  ORDERS: "/orders", // 用户获取订单列表
  ORDER_DETAIL: (id: string) => `/orders/${id}`, // 用户获取订单详情
  
  // API Key 相关
  API_KEYS: "/api-keys", // 获取用户API密钥列表
  CREATE_API_KEY: "/api-keys", // 创建API密钥
  API_KEY_DETAIL: (id: string) => `/api-keys/${id}`, // 获取API密钥详情
  UPDATE_API_KEY_STATUS: (id: string) => `/api-keys/${id}/status`, // 更新API密钥状态
  DELETE_API_KEY: (id: string) => `/api-keys/${id}`, // 删除API密钥
  RESET_API_KEY: (id: string) => `/api-keys/${id}/reset`, // 重置API密钥
  AGENT_API_KEYS: (agentId: string) => `/api-keys/agent/${agentId}`, // 获取Agent的API密钥列表
  
  // RAG 数据集相关
  RAG_DATASETS: "/rag/datasets", // 分页查询数据集
  RAG_ALL_DATASETS: "/rag/datasets/all", // 获取所有数据集
  RAG_DATASET_DETAIL: (id: string) => `/rag/datasets/${id}`, // 获取数据集详情
  RAG_UPLOAD_FILE: "/rag/datasets/files", // 上传文件到数据集
  RAG_DATASET_FILES: (id: string) => `/rag/datasets/${id}/files`, // 分页查询数据集文件
  RAG_ALL_DATASET_FILES: (id: string) => `/rag/datasets/${id}/files/all`, // 获取数据集所有文件
  RAG_DATASET_FILE_DELETE: (datasetId: string, fileId: string) => `/rag/datasets/${datasetId}/files/${fileId}`, // 删除数据集文件
  // 新增RAG接口
  RAG_PROCESS_FILE: "/rag/datasets/files/process", // 启动文件预处理
  RAG_FILE_PROGRESS: (fileId: string) => `/rag/datasets/files/${fileId}/progress`, // 获取文件处理进度
  RAG_DATASET_FILES_PROGRESS: (datasetId: string) => `/rag/datasets/${datasetId}/files/progress`, // 获取数据集文件处理进度列表
  RAG_SEARCH: "/rag/search", // RAG搜索文档
  RAG_SEARCH_BY_USER_RAG: (userRagId: string) => `/rag/search/user-rag/${userRagId}`, // 基于已安装知识库的RAG搜索
  
  // RAG 文件操作相关
  RAG_FILE_INFO: (fileId: string) => `/rag/files/${fileId}/info`, // 获取文件详细信息
  RAG_DOCUMENT_UNITS: "/rag/files/document-units/list", // 分页查询文件的语料
  RAG_UPDATE_DOCUMENT_UNIT: "/rag/files/document-units", // 更新语料内容
  RAG_DELETE_DOCUMENT_UNIT: (documentUnitId: string) => `/rag/files/document-units/${documentUnitId}`, // 删除语料
  
  // RAG 流式聊天相关
  RAG_STREAM_CHAT: "/rag/search/stream-chat", // RAG流式问答
  RAG_STREAM_CHAT_BY_USER_RAG: (userRagId: string) => `/rag/search/user-rag/${userRagId}/stream-chat`, // 基于已安装知识库的RAG流式问答
  
  // RAG 文件详情相关
  RAG_FILE_DETAIL: "/rag/files/detail", // 获取文件详情
  RAG_FILE_CONTENT: "/rag/files/content", // 获取文件内容
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

