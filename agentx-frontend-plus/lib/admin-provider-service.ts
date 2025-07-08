import { httpClient } from "@/lib/http-client";
import { ApiResponse } from "@/types/api";

// 服务商协议枚举
export enum ProviderProtocol {
  OPENAI = "OPENAI",
  ANTHROPIC = "ANTHROPIC"
}

// 模型类型枚举
export enum ModelType {
  CHAT = "CHAT",
  EMBEDDING = "EMBEDDING", 
  IMAGE = "IMAGE"
}

// 服务商配置接口
export interface ProviderConfig {
  apiKey: string;
  baseUrl?: string;
}

// 服务商接口
export interface Provider {
  id: string;
  protocol: ProviderProtocol;
  name: string;
  description?: string;
  config: ProviderConfig;
  isOfficial: boolean;
  status: boolean;
  createdAt: string;
  updatedAt: string;
  models: Model[];
}

// 模型接口
export interface Model {
  id: string;
  userId: string;
  providerId: string;
  providerName?: string;
  modelId: string;
  name: string;
  description: string;
  type: ModelType;
  modelEndpoint?: string;
  config: any;
  isOfficial?: boolean;
  status: boolean;
  createdAt: string;
  updatedAt: string;
}

// 创建服务商请求接口
export interface CreateProviderRequest {
  protocol: ProviderProtocol;
  name: string;
  description?: string;
  config: ProviderConfig;
}

// 更新服务商请求接口
export interface UpdateProviderRequest {
  id: string;
  protocol: ProviderProtocol;
  name: string;
  description?: string;
  config: ProviderConfig;
}

// 创建模型请求接口
export interface CreateModelRequest {
  providerId: string;
  modelId: string;
  name: string;
  description: string;
  type: ModelType;
  modelEndpoint?: string;
  config?: any;
}

// 更新模型请求接口
export interface UpdateModelRequest {
  id: string;
  modelId: string;
  name: string;
  description: string;
  modelEndpoint?: string;
}

/**
 * 管理员服务商管理服务
 */
export class AdminProviderService {
  
  /**
   * 获取官方服务商列表
   * @param page 页码（可选）
   * @param pageSize 每页大小（可选）
   * @returns 官方服务商列表
   */
  static async getProviders(page?: number, pageSize?: number): Promise<ApiResponse<Provider[]>> {
    try {
      const params: any = {};
      if (page) params.page = page;
      if (pageSize) params.pageSize = pageSize;
      
      return await httpClient.get("/admin/llms/providers", { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取服务商列表失败",
        data: [],
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取服务商详情
   * @param providerId 服务商ID
   * @returns 服务商详情
   */
  static async getProviderDetail(providerId: string): Promise<ApiResponse<Provider>> {
    try {
      return await httpClient.get(`/admin/llms/providers/${providerId}`);
    } catch (error) {
      return {
        code: 500,
        message: "获取服务商详情失败",
        data: null as any,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 创建官方服务商
   * @param data 服务商数据
   * @returns 创建结果
   */
  static async createProvider(data: CreateProviderRequest): Promise<ApiResponse<Provider>> {
    try {
      return await httpClient.post("/admin/llms/providers", data);
    } catch (error) {
      return {
        code: 500,
        message: "创建服务商失败",
        data: null as any,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 更新服务商
   * @param data 服务商数据
   * @returns 更新结果
   */
  static async updateProvider(data: UpdateProviderRequest): Promise<ApiResponse<Provider>> {
    try {
      return await httpClient.put(`/admin/llms/providers/${data.id}`, data);
    } catch (error) {
      return {
        code: 500,
        message: "更新服务商失败",
        data: null as any,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 切换服务商状态
   * @param providerId 服务商ID
   * @returns 操作结果
   */
  static async toggleProviderStatus(providerId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.post(`/admin/llms/providers/${providerId}/status`);
    } catch (error) {
      return {
        code: 500,
        message: "切换服务商状态失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 删除服务商
   * @param providerId 服务商ID
   * @returns 操作结果
   */
  static async deleteProvider(providerId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete(`/admin/llms/providers/${providerId}`);
    } catch (error) {
      return {
        code: 500,
        message: "删除服务商失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取支持的协议列表
   * @returns 协议列表
   */
  static async getProviderProtocols(): Promise<ApiResponse<ProviderProtocol[]>> {
    try {
      return await httpClient.get("/admin/llms/providers/protocols");
    } catch (error) {
      return {
        code: 500,
        message: "获取协议列表失败",
        data: [],
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取模型列表
   * @param providerId 服务商ID（可选）
   * @param modelType 模型类型（可选）
   * @param page 页码（可选）
   * @param pageSize 每页大小（可选）
   * @returns 模型列表
   */
  static async getModels(
    providerId?: string, 
    modelType?: string, 
    page?: number, 
    pageSize?: number
  ): Promise<ApiResponse<Model[]>> {
    try {
      const params: any = {};
      if (providerId) params.providerId = providerId;
      if (modelType) params.modelType = modelType;
      if (page) params.page = page;
      if (pageSize) params.pageSize = pageSize;
      
      return await httpClient.get("/admin/llms/models", { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取模型列表失败",
        data: [],
        timestamp: Date.now()
      };
    }
  }

  /**
   * 创建模型
   * @param data 模型数据
   * @returns 创建结果
   */
  static async createModel(data: CreateModelRequest): Promise<ApiResponse<Model>> {
    try {
      return await httpClient.post("/admin/llms/models", data);
    } catch (error) {
      return {
        code: 500,
        message: "创建模型失败",
        data: null as any,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 更新模型
   * @param data 模型数据
   * @returns 更新结果
   */
  static async updateModel(data: UpdateModelRequest): Promise<ApiResponse<Model>> {
    try {
      return await httpClient.put(`/admin/llms/models/${data.id}`, data);
    } catch (error) {
      return {
        code: 500,
        message: "更新模型失败",
        data: null as any,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 切换模型状态
   * @param modelId 模型ID
   * @returns 操作结果
   */
  static async toggleModelStatus(modelId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.post(`/admin/llms/models/${modelId}/status`);
    } catch (error) {
      return {
        code: 500,
        message: "切换模型状态失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 删除模型
   * @param modelId 模型ID
   * @returns 操作结果
   */
  static async deleteModel(modelId: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete(`/admin/llms/models/${modelId}`);
    } catch (error) {
      return {
        code: 500,
        message: "删除模型失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取模型类型列表
   * @returns 模型类型列表
   */
  static async getModelTypes(): Promise<ApiResponse<ModelType[]>> {
    try {
      return await httpClient.get("/admin/llms/models/types");
    } catch (error) {
      return {
        code: 500,
        message: "获取模型类型失败",
        data: [],
        timestamp: Date.now()
      };
    }
  }
}

/**
 * 获取协议的中文描述
 */
export function getProtocolText(protocol: ProviderProtocol): string {
  switch (protocol) {
    case ProviderProtocol.OPENAI:
      return "OpenAI";
    case ProviderProtocol.ANTHROPIC:
      return "Anthropic";
    default:
      return protocol;
  }
}

/**
 * 获取模型类型的中文描述
 */
export function getModelTypeText(type: ModelType): string {
  switch (type) {
    case ModelType.CHAT:
      return "对话模型";
    case ModelType.EMBEDDING:
      return "嵌入模型";
    case ModelType.IMAGE:
      return "图像模型";
    default:
      return type;
  }
}

/**
 * 获取协议的默认配置字段
 */
export function getProtocolConfig(protocol: ProviderProtocol) {
  switch (protocol) {
    case ProviderProtocol.OPENAI:
      return [
        { label: "API Key", placeholder: "输入API Key", required: true, type: "text" },
        { label: "基础URL", placeholder: "可选，例如：https://api.openai.com/v1", required: false, type: "url" }
      ];
    case ProviderProtocol.ANTHROPIC:
      return [
        { label: "API Key", placeholder: "输入Anthropic API Key", required: true, type: "text" },
        { label: "基础URL", placeholder: "可选，例如：https://api.anthropic.com", required: false, type: "url" }
      ];
    default:
      return [
        { label: "API Key", placeholder: "输入API Key", required: true, type: "text" },
        { label: "基础URL", placeholder: "输入基础URL", required: false, type: "url" }
      ];
  }
}