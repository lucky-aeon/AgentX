import { httpClient } from "@/lib/http-client";
import { API_ENDPOINTS } from "@/lib/api-config";
import { ApiResponse } from "@/types/api";

// Agent版本信息接口
export interface AgentVersion {
  id: string;
  agentId: string;
  versionNumber: string;
  publishStatus: number; // 1-审核中, 2-已发布, 3-已拒绝, 4-已下架
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  rejectReason?: string;
}

// Agent信息接口（包含用户信息和版本信息）
export interface Agent {
  id: string;
  name: string;
  description: string;
  avatar?: string;
  systemPrompt?: string;
  welcomeMessage?: string;
  toolIds?: string[];
  knowledgeBaseIds?: string[];
  enabled: boolean;
  userId: string;
  userNickname?: string;
  userEmail?: string;
  userAvatarUrl?: string;
  publishedVersion?: string;
  versions: AgentVersion[];
  createdAt: string;
  updatedAt: string;
}

// Agent查询参数接口
export interface GetAgentsParams {
  keyword?: string;  // 搜索关键词（名称、描述）
  enabled?: boolean; // 启用状态筛选
  page?: number;     // 页码
  pageSize?: number; // 每页大小
}

// Agent统计信息接口
export interface AgentStatistics {
  totalAgents: number;     // 总Agent数量
  enabledAgents: number;   // 启用的Agent数量
  disabledAgents: number;  // 禁用的Agent数量
  pendingVersions: number; // 待审核版本数量
}

// 分页响应接口
export interface PageResponse<T> {
  records: T[];      // 数据记录
  total: number;     // 总记录数
  size: number;      // 每页大小
  current: number;   // 当前页码
  pages: number;     // 总页数
}

/**
 * 管理员Agent服务
 */
export class AdminAgentService {
  
  /**
   * 分页获取Agent列表
   * @param params 查询参数
   * @returns Agent分页数据
   */
  static async getAgents(params?: GetAgentsParams): Promise<ApiResponse<PageResponse<Agent>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ADMIN_AGENTS, { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取Agent列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取Agent统计信息
   * @returns Agent统计数据
   */
  static async getAgentStatistics(): Promise<ApiResponse<AgentStatistics>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ADMIN_AGENT_STATISTICS);
    } catch (error) {
      return {
        code: 500,
        message: "获取Agent统计失败",
        data: { totalAgents: 0, enabledAgents: 0, disabledAgents: 0, pendingVersions: 0 },
        timestamp: Date.now()
      };
    }
  }
}

/**
 * 带Toast提示的Agent管理服务
 */

// 获取Agent列表（带Toast提示）
export async function getAgentsWithToast(params?: GetAgentsParams): Promise<ApiResponse<PageResponse<Agent>>> {
  try {
    const response = await AdminAgentService.getAgents(params);
    return response;
  } catch (error) {
    console.error('获取Agent列表失败:', error);
    return {
      code: 500,
      message: "获取Agent列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    };
  }
}