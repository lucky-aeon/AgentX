import { httpClient } from "@/lib/http-client";
import { API_ENDPOINTS } from "@/lib/api-config";
import { ApiResponse } from "@/types/api";

// 用户信息接口
export interface User {
  id: string;
  nickname: string;
  email: string;
  phone: string;
  githubId?: string;
  githubLogin?: string;
  avatarUrl?: string;
  loginPlatform?: string;
  createdAt: string;
  updatedAt: string;
}

// 用户查询参数接口
export interface GetUsersParams {
  keyword?: string;  // 搜索关键词（昵称、邮箱、手机号）
  page?: number;     // 页码
  pageSize?: number; // 每页大小
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
 * 管理员用户服务
 */
export class AdminUserService {
  
  /**
   * 分页获取用户列表
   * @param params 查询参数
   * @returns 用户分页数据
   */
  static async getUsers(params?: GetUsersParams): Promise<ApiResponse<PageResponse<User>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ADMIN_USERS, { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取用户列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }
}

/**
 * 带Toast提示的用户管理服务
 */

// 获取用户列表（带Toast提示）
export async function getUsersWithToast(params?: GetUsersParams): Promise<ApiResponse<PageResponse<User>>> {
  try {
    const response = await AdminUserService.getUsers(params);
    return response;
  } catch (error) {
    console.error('获取用户列表失败:', error);
    return {
      code: 500,
      message: "获取用户列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    };
  }
}