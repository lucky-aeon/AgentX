import { httpClient } from './http-client';
import type { 
  AuthConfig, 
  AuthSetting, 
  UpdateAuthSettingRequest, 
  ApiResponse 
} from './types/auth-config';

// API端点定义
const API_ENDPOINTS = {
  AUTH_CONFIG: '/auth/config',
  ADMIN_AUTH_SETTINGS: '/admin/auth-settings',
};

/**
 * 认证配置API服务
 */
export class AuthConfigService {
  
  /** 获取用户端认证配置
   * 
   * @returns 认证配置 */
  static async getAuthConfig(): Promise<ApiResponse<AuthConfig>> {
    try {
      return await httpClient.get<ApiResponse<AuthConfig>>(API_ENDPOINTS.AUTH_CONFIG);
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取认证配置失败",
        data: {
          loginMethods: {},
          registerEnabled: false
        },
        timestamp: Date.now()
      };
    }
  }

  /** 获取所有认证设置（管理员）
   * 
   * @returns 认证设置列表 */
  static async getAllAuthSettings(): Promise<ApiResponse<AuthSetting[]>> {
    try {
      return await httpClient.get<ApiResponse<AuthSetting[]>>(API_ENDPOINTS.ADMIN_AUTH_SETTINGS);
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取认证设置失败",
        data: [],
        timestamp: Date.now()
      };
    }
  }

  /** 根据ID获取认证设置（管理员）
   * 
   * @param id 设置ID
   * @returns 认证设置 */
  static async getAuthSettingById(id: string): Promise<ApiResponse<AuthSetting>> {
    try {
      return await httpClient.get<ApiResponse<AuthSetting>>(`${API_ENDPOINTS.ADMIN_AUTH_SETTINGS}/${id}`);
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : "获取认证设置失败");
    }
  }

  /** 切换认证设置状态（管理员）
   * 
   * @param id 设置ID
   * @returns 更新后的设置 */
  static async toggleAuthSetting(id: string): Promise<ApiResponse<AuthSetting>> {
    try {
      return await httpClient.put<ApiResponse<AuthSetting>>(`${API_ENDPOINTS.ADMIN_AUTH_SETTINGS}/${id}/toggle`);
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : "切换设置状态失败");
    }
  }

  /** 更新认证设置（管理员）
   * 
   * @param id 设置ID
   * @param data 更新数据
   * @returns 更新后的设置 */
  static async updateAuthSetting(id: string, data: UpdateAuthSettingRequest): Promise<ApiResponse<AuthSetting>> {
    try {
      return await httpClient.put<ApiResponse<AuthSetting>>(`${API_ENDPOINTS.ADMIN_AUTH_SETTINGS}/${id}`, data);
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : "更新认证设置失败");
    }
  }

  /** 删除认证设置（管理员）
   * 
   * @param id 设置ID
   * @returns 操作结果 */
  static async deleteAuthSetting(id: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete<ApiResponse<void>>(`${API_ENDPOINTS.ADMIN_AUTH_SETTINGS}/${id}`);
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : "删除认证设置失败");
    }
  }
}

// 带Toast的API方法
export async function getAuthConfigWithToast(): Promise<ApiResponse<AuthConfig>> {
  try {
    return await AuthConfigService.getAuthConfig();
  } catch (error) {
    console.error('获取认证配置失败:', error);
    return {
      code: 500,
      message: "获取认证配置失败",
      data: {
        loginMethods: {},
        registerEnabled: false
      },
      timestamp: Date.now()
    };
  }
}

export async function getAllAuthSettingsWithToast(): Promise<ApiResponse<AuthSetting[]>> {
  try {
    return await AuthConfigService.getAllAuthSettings();
  } catch (error) {
    console.error('获取认证设置失败:', error);
    return {
      code: 500,
      message: "获取认证设置失败",
      data: [],
      timestamp: Date.now()
    };
  }
}

export async function toggleAuthSettingWithToast(id: string): Promise<ApiResponse<AuthSetting>> {
  try {
    return await AuthConfigService.toggleAuthSetting(id);
  } catch (error) {
    console.error('切换设置状态失败:', error);
    throw error;
  }
}

export async function updateAuthSettingWithToast(id: string, data: UpdateAuthSettingRequest): Promise<ApiResponse<AuthSetting>> {
  try {
    return await AuthConfigService.updateAuthSetting(id, data);
  } catch (error) {
    console.error('更新认证设置失败:', error);
    throw error;
  }
}

export async function deleteAuthSettingWithToast(id: string): Promise<ApiResponse<void>> {
  try {
    return await AuthConfigService.deleteAuthSetting(id);
  } catch (error) {
    console.error('删除认证设置失败:', error);
    throw error;
  }
}