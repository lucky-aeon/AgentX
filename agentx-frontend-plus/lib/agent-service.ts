import { API_CONFIG, API_ENDPOINTS, buildApiUrl } from "@/lib/api-config"
import type {
  Agent,
  ApiResponse,
  GetAgentsParams,
  CreateAgentRequest,
  UpdateAgentRequest,
  PublishAgentVersionRequest,
  AgentVersion,
  SearchAgentsRequest,
} from "@/types/agent"

// 获取用户的助理列表
export async function getUserAgents(params?: Partial<GetAgentsParams>): Promise<ApiResponse<Agent[]>> {
  try {
    const userId = params?.userId || API_CONFIG.CURRENT_USER_ID
    const queryParams: Record<string, any> = {}

    // 添加名称搜索参数
    if (params?.name) {
      queryParams.name = params.name
    }

    const url = buildApiUrl(API_ENDPOINTS.USER_AGENTS(userId), queryParams)

    console.log(`Fetching user agents from: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`获取助理列表失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("获取助理列表错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as Agent[],
      timestamp: Date.now(),
    }
  }
}

// 获取已发布的助理列表
export async function getPublishedAgents(name?: string): Promise<ApiResponse<AgentVersion[]>> {
  try {
    const queryParams: Record<string, any> = {}

    // 添加名称搜索参数
    if (name) {
      queryParams.name = name
    }

    const url = buildApiUrl(API_ENDPOINTS.PUBLISHED_AGENTS, queryParams)

    console.log(`Fetching published agents from: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`获取已发布助理列表失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("获取已发布助理列表错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as AgentVersion[],
      timestamp: Date.now(),
    }
  }
}

// 获取助理详情
export async function getAgentDetail(agentId: string): Promise<ApiResponse<Agent>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.AGENT_DETAIL(agentId))

    console.log(`Fetching agent detail from: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`获取助理详情失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("获取助理详情错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Agent,
      timestamp: Date.now(),
    }
  }
}

// 创建助理
export async function createAgent(params: CreateAgentRequest): Promise<ApiResponse<Agent>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.CREATE_AGENT)

    console.log(`Creating agent at: ${url}`)

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify(params),
    })

    if (!response.ok) {
      throw new Error(`创建助理失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("创建助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Agent,
      timestamp: Date.now(),
    }
  }
}

// 更新助理
export async function updateAgent(agentId: string, params: UpdateAgentRequest): Promise<ApiResponse<Agent>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.UPDATE_AGENT(agentId))

    console.log(`Updating agent at: ${url}`)

    const response = await fetch(url, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify(params),
    })

    if (!response.ok) {
      throw new Error(`更新助理失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("更新助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Agent,
      timestamp: Date.now(),
    }
  }
}

// 切换助理启用/禁用状态
export async function toggleAgentStatus(agentId: string): Promise<ApiResponse<Agent>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.TOGGLE_AGENT_STATUS(agentId))

    console.log(`Toggling agent status at: ${url}`)

    const response = await fetch(url, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`切换助理状态失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("切换助理状态错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Agent,
      timestamp: Date.now(),
    }
  }
}

// 删除助理
export async function deleteAgent(agentId: string): Promise<ApiResponse<null>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.DELETE_AGENT(agentId))

    console.log(`Deleting agent at: ${url}`)

    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`删除助理失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("删除助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 发布助理版本
export async function publishAgentVersion(
  agentId: string,
  params: PublishAgentVersionRequest,
): Promise<ApiResponse<AgentVersion>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.PUBLISH_AGENT_VERSION(agentId))

    console.log(`Publishing agent version at: ${url}`)

    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify(params),
    })

    if (!response.ok) {
      throw new Error(`发布助理版本失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("发布助理版本错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as AgentVersion,
      timestamp: Date.now(),
    }
  }
}

// 获取助理版本列表
export async function getAgentVersions(agentId: string): Promise<ApiResponse<AgentVersion[]>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.AGENT_VERSIONS(agentId))

    console.log(`Fetching agent versions from: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`获取助理版本列表失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("获取助理版本列表错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as AgentVersion[],
      timestamp: Date.now(),
    }
  }
}

// 获取助理特定版本
export async function getAgentVersion(agentId: string, versionNumber: string): Promise<ApiResponse<AgentVersion>> {
  try {
    const url = buildApiUrl(API_ENDPOINTS.AGENT_VERSION_DETAIL(agentId, versionNumber))

    console.log(`Fetching agent version from: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`获取助理版本详情失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("获取助理版本详情错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as AgentVersion,
      timestamp: Date.now(),
    }
  }
}

// 搜索助理
export async function searchAgents(params: SearchAgentsRequest): Promise<ApiResponse<Agent[]>> {
  try {
    const userId = API_CONFIG.CURRENT_USER_ID
    const url = buildApiUrl(API_ENDPOINTS.USER_AGENTS(userId), params)

    console.log(`Searching agents at: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    if (!response.ok) {
      throw new Error(`搜索助理失败: ${response.status}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error("搜索助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as Agent[],
      timestamp: Date.now(),
    }
  }
}

