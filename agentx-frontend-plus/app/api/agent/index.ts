import axios from 'axios';

// API基础URL
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://127.0.0.1:8080';

// Agent类型定义
export interface AgentDTO {
  id: string;
  name: string;
  description: string;
  userId: string;
  avatarUrl?: string;
  status: number;
  createdAt: string;
  updatedAt: string;
  configuration?: {
    type?: "chat" | "agent";
    systemPrompt?: string;
    welcomeMessage?: string;
    modelConfig?: {
      model: string;
      temperature: number;
      maxTokens: number;
    };
    tools?: string[];
    knowledgeBaseIds?: string[];
  };
  // 其他属性根据实际情况添加
}

export interface AgentVersionDTO {
  id: string;
  agentId: string;
  versionNumber: string;
  configuration: any;
  createdAt: string;
  // 其他属性根据实际情况添加
}

// 请求类型定义
export interface CreateAgentRequest {
  name: string;
  description: string;
  userId: string;
  avatarUrl?: string;
  configuration?: any;
}

export interface UpdateAgentRequest {
  name?: string;
  description?: string;
  avatarUrl?: string;
  configuration?: any;
}

export interface SearchAgentsRequest {
  userId?: string;
  keyword?: string;
}

export interface PublishAgentVersionRequest {
  versionNumber: string;
  changeLog: string;
}

// Agent API服务
const AgentAPI = {
  // 创建新Agent
  createAgent: async (request: CreateAgentRequest): Promise<AgentDTO> => {
    const response = await axios.post(`${API_BASE_URL}/agent`, request);
    return response.data.data;
  },

  // 获取Agent详情
  getAgent: async (agentId: string): Promise<AgentDTO> => {
    const response = await axios.get(`${API_BASE_URL}/agent/${agentId}`);
    return response.data.data;
  },

  // 获取用户的Agent列表
  getUserAgents: async (userId: string, statusCode?: number): Promise<AgentDTO[]> => {
    const url = statusCode !== undefined 
      ? `${API_BASE_URL}/agent/user/${userId}?statusCode=${statusCode}`
      : `${API_BASE_URL}/agent/user/${userId}`;
    const response = await axios.get(url);
    return response.data.data;
  },

  // 获取已上架的Agent列表
  getPublishedAgents: async (): Promise<AgentDTO[]> => {
    const response = await axios.get(`${API_BASE_URL}/agent/published`);
    return response.data.data;
  },

  // 获取待审核的Agent列表
  getPendingReviewAgents: async (): Promise<AgentDTO[]> => {
    const response = await axios.get(`${API_BASE_URL}/agent/pending`);
    return response.data.data;
  },

  // 更新Agent信息
  updateAgent: async (agentId: string, request: UpdateAgentRequest): Promise<AgentDTO> => {
    const response = await axios.put(`${API_BASE_URL}/agent/${agentId}`, request);
    return response.data.data;
  },

  // 删除Agent
  deleteAgent: async (agentId: string): Promise<void> => {
    await axios.delete(`${API_BASE_URL}/agent/${agentId}`);
  },

  // 搜索Agent
  searchAgents: async (request: SearchAgentsRequest): Promise<AgentDTO[]> => {
    const response = await axios.post(`${API_BASE_URL}/agent/search`, request);
    return response.data.data;
  },

  // 发布Agent版本
  publishAgentVersion: async (agentId: string, request: PublishAgentVersionRequest): Promise<AgentVersionDTO> => {
    const response = await axios.post(`${API_BASE_URL}/agent/${agentId}/publish`, request);
    return response.data.data;
  },

  // 获取Agent的所有版本
  getAgentVersions: async (agentId: string): Promise<AgentVersionDTO[]> => {
    const response = await axios.get(`${API_BASE_URL}/agent/${agentId}/versions`);
    return response.data.data;
  },

  // 获取Agent的特定版本
  getAgentVersion: async (agentId: string, versionNumber: string): Promise<AgentVersionDTO> => {
    const response = await axios.get(`${API_BASE_URL}/agent/${agentId}/versions/${versionNumber}`);
    return response.data.data;
  },

  // 获取Agent的最新版本
  getLatestAgentVersion: async (agentId: string): Promise<AgentVersionDTO> => {
    const response = await axios.get(`${API_BASE_URL}/agent/${agentId}/versions/latest`);
    return response.data.data;
  }
};

export default AgentAPI; 