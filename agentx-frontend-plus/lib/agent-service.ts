import { API_CONFIG, API_ENDPOINTS } from "@/lib/api-config"
import { httpClient } from "@/lib/http-client"
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
import { withToast } from "./toast-utils"

// 获取用户的助理列表
export async function getUserAgents(params?: Partial<GetAgentsParams>): Promise<ApiResponse<Agent[]>> {
  try {
    console.log(`Fetching user agents`)
    
    const response = await httpClient.get<ApiResponse<Agent[]>>("/agent/user", {
      params: params
    });
    
    return response;
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

// 获取工作区下的助理列表
export async function getWorkspaceAgents(): Promise<ApiResponse<Agent[]>> {
  try {
    console.log(`Fetching workspace agents`)
    
    const response = await httpClient.get<ApiResponse<Agent[]>>("/agent/workspace/agents");
    
    return response;
  } catch (error) {
    console.error("获取工作区助理列表错误:", error)
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
    console.log(`Fetching published agents`)
    
    const response = await httpClient.get<ApiResponse<AgentVersion[]>>(API_ENDPOINTS.PUBLISHED_AGENTS, {
      params: name ? { name } : undefined
    });
    
    return response;
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
    console.log(`Fetching agent detail for: ${agentId}`)
    
    const response = await httpClient.get<ApiResponse<Agent>>(API_ENDPOINTS.AGENT_DETAIL(agentId));
    
    return response;
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
    console.log(`Creating agent`)
    
    const response = await httpClient.post<ApiResponse<Agent>>(API_ENDPOINTS.CREATE_AGENT, params);
    
    return response;
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
export async function updateAgent(agentId: string, agentData: Partial<UpdateAgentRequest>): Promise<ApiResponse<Agent>> {
  try {
    console.log(`Updating agent with ID: ${agentId}`);
    
    // 确保请求数据中包含必要字段
    const requestData = {
      ...agentData,
      // 确保agentType字段存在，这样后端才能正确计算statusText
      agentType: agentData.agentType || 1
    };
    
    const response = await httpClient.put<ApiResponse<Agent>>(API_ENDPOINTS.UPDATE_AGENT(agentId), requestData);
    
    return response;
  } catch (error) {
    console.error("更新助理错误:", error);
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
    console.log(`Toggling agent status: ${agentId}`)
    
    const response = await httpClient.put<ApiResponse<Agent>>(API_ENDPOINTS.TOGGLE_AGENT_STATUS(agentId));
    
    return response;
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
    console.log(`Deleting agent: ${agentId}`)
    
    const response = await httpClient.delete<ApiResponse<null>>(API_ENDPOINTS.DELETE_AGENT(agentId));
    
    return response;
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
    console.log(`Publishing agent version: ${agentId}`)
    
    const response = await httpClient.post<ApiResponse<AgentVersion>>(
      API_ENDPOINTS.PUBLISH_AGENT_VERSION(agentId), 
      params
    );
    
    return response;
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
    console.log(`Fetching agent versions: ${agentId}`)
    
    const response = await httpClient.get<ApiResponse<AgentVersion[]>>(API_ENDPOINTS.AGENT_VERSIONS(agentId));
    
    return response;
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
    console.log(`Fetching agent version: ${agentId}, ${versionNumber}`)
    
    const response = await httpClient.get<ApiResponse<AgentVersion>>(
      API_ENDPOINTS.AGENT_VERSION_DETAIL(agentId, versionNumber)
    );
    
    return response;
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
    console.log(`Searching agents for user: ${userId}`)
    
    const response = await httpClient.get<ApiResponse<Agent[]>>(API_ENDPOINTS.USER_AGENTS(userId), {
      params
    });
    
    return response;
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

// Add this function to get agent details by session ID
export async function getAgentBySessionId(sessionId: string): Promise<ApiResponse<Agent>> {
  try {
    console.log(`Fetching agent by session ID: ${sessionId}`)
    
    const response = await httpClient.get<ApiResponse<Agent>>(`/agent/session-agent/${sessionId}`);
    
    return response;
  } catch (error) {
    console.error("获取会话关联的助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Agent,
      timestamp: Date.now(),
    }
  }
}

// Add this function to delete an agent from the workspace
export async function deleteWorkspaceAgent(agentId: string): Promise<ApiResponse<null>> {
  try {
    console.log(`Deleting workspace agent: ${agentId}`)
    
    const response = await httpClient.delete<ApiResponse<null>>(`/agent/workspace/agents/${agentId}`);
    
    return response;
  } catch (error) {
    console.error("删除工作区助理错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 使用toast包装的API函数
export const getUserAgentsWithToast = withToast(getUserAgents, {
  showSuccessToast: false,
  errorTitle: "获取助理列表失败"
})

export const getWorkspaceAgentsWithToast = withToast(getWorkspaceAgents, {
  showSuccessToast: false,
  errorTitle: "获取工作区助理列表失败"
})

export const getPublishedAgentsWithToast = withToast(getPublishedAgents, {
  showSuccessToast: false,
  errorTitle: "获取已发布助理列表失败"
})

export const getAgentDetailWithToast = withToast(getAgentDetail, {
  showSuccessToast: false,
  errorTitle: "获取助理详情失败"
})

export const createAgentWithToast = withToast(createAgent, {
  successTitle: "创建助理成功",
  errorTitle: "创建助理失败"
})

export const updateAgentWithToast = withToast(updateAgent, {
  successTitle: "更新助理成功",
  errorTitle: "更新助理失败"
})

export const toggleAgentStatusWithToast = withToast(toggleAgentStatus, {
  successTitle: "更改助理状态成功",
  errorTitle: "更改助理状态失败"
})

export const deleteAgentWithToast = withToast(deleteAgent, {
  successTitle: "删除助理成功",
  errorTitle: "删除助理失败"
})

export const publishAgentVersionWithToast = withToast(publishAgentVersion, {
  successTitle: "发布助理版本成功",
  errorTitle: "发布助理版本失败"
})

export const searchAgentsWithToast = withToast(searchAgents, {
  showSuccessToast: false,
  errorTitle: "搜索助理失败"
})

export const getAgentBySessionIdWithToast = withToast(getAgentBySessionId, {
  showSuccessToast: false,
  errorTitle: "获取会话助理失败"
})

export const deleteWorkspaceAgentWithToast = withToast(deleteWorkspaceAgent, {
  successTitle: "移除工作区助理成功",
  errorTitle: "移除工作区助理失败"
})

