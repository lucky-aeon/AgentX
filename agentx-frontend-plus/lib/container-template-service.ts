import { httpClient } from './http-client';

export interface ContainerTemplate {
  id: string;
  name: string;
  description: string;
  type: string;
  image: string;
  imageTag: string;
  fullImageName: string;
  internalPort: number;
  cpuLimit: number;
  memoryLimit: number;
  environment: Record<string, string>;
  volumeMountPath: string;
  command: string[];
  networkMode: string;
  restartPolicy: string;
  healthCheck: Record<string, any>;
  resourceConfig: Record<string, any>;
  enabled: boolean;
  isDefault: boolean;
  createdBy: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateContainerTemplateRequest {
  name: string;
  description?: string;
  type: string;
  image: string;
  imageTag?: string;
  internalPort: number;
  cpuLimit: number;
  memoryLimit: number;
  environment?: Record<string, string>;
  volumeMountPath?: string;
  command?: string[];
  networkMode?: string;
  restartPolicy?: string;
  healthCheck?: Record<string, any>;
  resourceConfig?: Record<string, any>;
  enabled?: boolean;
  isDefault?: boolean;
  sortOrder?: number;
}

export interface UpdateContainerTemplateRequest {
  name?: string;
  description?: string;
  type?: string;
  image?: string;
  imageTag?: string;
  internalPort?: number;
  cpuLimit?: number;
  memoryLimit?: number;
  environment?: Record<string, string>;
  volumeMountPath?: string;
  command?: string[];
  networkMode?: string;
  restartPolicy?: string;
  healthCheck?: Record<string, any>;
  resourceConfig?: Record<string, any>;
  enabled?: boolean;
  isDefault?: boolean;
  sortOrder?: number;
}

export interface QueryContainerTemplateParams {
  keyword?: string;
  type?: string;
  enabled?: boolean;
  page?: number;
  pageSize?: number;
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface TemplateStatistics {
  totalTemplates: number;
  enabledTemplates: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export class ContainerTemplateService {
  private static readonly BASE_PATH = '/admin/container-templates';
  private static readonly USER_PATH = '/container-templates';

  // ============== 管理员API ==============

  /** 分页获取容器模板列表 */
  static async getTemplates(params?: QueryContainerTemplateParams): Promise<ApiResponse<PageResponse<ContainerTemplate>>> {
    try {
      return await httpClient.get<ApiResponse<PageResponse<ContainerTemplate>>>(
        this.BASE_PATH,
        { params }
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取模板列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now(),
      };
    }
  }

  /** 获取容器模板详情 */
  static async getTemplate(templateId: string): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate>>(
        `${this.BASE_PATH}/${templateId}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取模板详情失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 根据类型获取模板列表 */
  static async getTemplatesByType(type: string): Promise<ApiResponse<ContainerTemplate[]>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate[]>>(
        `${this.BASE_PATH}/by-type/${type}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取模板列表失败",
        data: [],
        timestamp: Date.now(),
      };
    }
  }

  /** 获取默认模板 */
  static async getDefaultTemplate(type: string): Promise<ApiResponse<ContainerTemplate | null>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate | null>>(
        `${this.BASE_PATH}/default/${type}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取默认模板失败",
        data: null,
        timestamp: Date.now(),
      };
    }
  }

  /** 获取所有启用的模板 */
  static async getEnabledTemplates(): Promise<ApiResponse<ContainerTemplate[]>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate[]>>(
        `${this.BASE_PATH}/enabled`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取启用模板失败",
        data: [],
        timestamp: Date.now(),
      };
    }
  }

  /** 创建容器模板 */
  static async createTemplate(request: CreateContainerTemplateRequest): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.post<ApiResponse<ContainerTemplate>>(
        this.BASE_PATH,
        request
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "创建模板失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 更新容器模板 */
  static async updateTemplate(templateId: string, request: UpdateContainerTemplateRequest): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.put<ApiResponse<ContainerTemplate>>(
        `${this.BASE_PATH}/${templateId}`,
        request
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "更新模板失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 删除容器模板 */
  static async deleteTemplate(templateId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete<ApiResponse<void>>(
        `${this.BASE_PATH}/${templateId}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "删除模板失败",
        data: undefined,
        timestamp: Date.now(),
      };
    }
  }

  /** 启用/禁用模板 */
  static async toggleTemplateStatus(templateId: string, enabled: boolean): Promise<ApiResponse<void>> {
    try {
      return await httpClient.put<ApiResponse<void>>(
        `${this.BASE_PATH}/${templateId}/status`,
        null,
        { params: { enabled } }
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "切换模板状态失败",
        data: undefined,
        timestamp: Date.now(),
      };
    }
  }

  /** 设置默认模板 */
  static async setDefaultTemplate(templateId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.put<ApiResponse<void>>(
        `${this.BASE_PATH}/${templateId}/default`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "设置默认模板失败",
        data: undefined,
        timestamp: Date.now(),
      };
    }
  }

  /** 获取模板统计信息 */
  static async getStatistics(): Promise<ApiResponse<TemplateStatistics>> {
    try {
      return await httpClient.get<ApiResponse<TemplateStatistics>>(
        `${this.BASE_PATH}/statistics`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取统计信息失败",
        data: { totalTemplates: 0, enabledTemplates: 0 },
        timestamp: Date.now(),
      };
    }
  }

  // ============== 用户API ==============

  /** 获取MCP网关默认模板 */
  static async getMcpGatewayTemplate(): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate>>(
        `${this.USER_PATH}/mcp-gateway`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取MCP网关模板失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 根据类型获取启用的模板列表 */
  static async getEnabledTemplatesByType(type: string): Promise<ApiResponse<ContainerTemplate[]>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate[]>>(
        `${this.USER_PATH}/enabled/by-type/${type}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取模板列表失败",
        data: [],
        timestamp: Date.now(),
      };
    }
  }

  /** 获取用户可用的所有启用模板 */
  static async getUserEnabledTemplates(): Promise<ApiResponse<ContainerTemplate[]>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate[]>>(
        `${this.USER_PATH}/enabled`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取可用模板失败",
        data: [],
        timestamp: Date.now(),
      };
    }
  }

  /** 获取用户可见的默认模板 */
  static async getUserDefaultTemplate(type: string): Promise<ApiResponse<ContainerTemplate | null>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate | null>>(
        `${this.USER_PATH}/default/${type}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取默认模板失败",
        data: null,
        timestamp: Date.now(),
      };
    }
  }

  /** 获取用户可见的模板详情 */
  static async getUserTemplate(templateId: string): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.get<ApiResponse<ContainerTemplate>>(
        `${this.USER_PATH}/${templateId}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "获取模板详情失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 用户创建自定义模板 */
  static async createUserTemplate(request: CreateContainerTemplateRequest): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.post<ApiResponse<ContainerTemplate>>(
        this.USER_PATH,
        request
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "创建模板失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 用户更新自定义模板 */
  static async updateUserTemplate(templateId: string, request: UpdateContainerTemplateRequest): Promise<ApiResponse<ContainerTemplate>> {
    try {
      return await httpClient.put<ApiResponse<ContainerTemplate>>(
        `${this.USER_PATH}/${templateId}`,
        request
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "更新模板失败",
        data: {} as ContainerTemplate,
        timestamp: Date.now(),
      };
    }
  }

  /** 用户删除自定义模板 */
  static async deleteUserTemplate(templateId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete<ApiResponse<void>>(
        `${this.USER_PATH}/${templateId}`
      );
    } catch (error) {
      return {
        code: 500,
        message: error instanceof Error ? error.message : "删除模板失败",
        data: undefined,
        timestamp: Date.now(),
      };
    }
  }
}