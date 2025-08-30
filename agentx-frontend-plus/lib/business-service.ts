// 业务数据服务 - 用于获取模型、Agent等业务实体数据

import { httpClient, ApiResponse } from '@/lib/http-client';

// 模型接口
export interface Model {
  id: string;
  name: string;
  modelId: string;
  providerId: string;
  isOfficial?: boolean;
}



// 业务实体映射接口
export interface BusinessEntity {
  id: string;
  name: string;
  description?: string;
}

export class BusinessService {
  // 获取所有官方模型
  static async getOfficialModels(): Promise<ApiResponse<Model[]>> {
    try {
      return await httpClient.get('/llms/models', {
        params: { official: true }
      });
    } catch (error) {
      return {
        code: 500,
        message: '获取模型列表失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }



  // 根据商品类型获取业务选项
  static async getBusinessOptions(productType: string): Promise<BusinessEntity[]> {
    switch (productType) {
      case 'MODEL_USAGE':
        const modelResponse = await this.getOfficialModels();
        if (modelResponse.code === 200) {
          return modelResponse.data.map(model => ({
            id: model.id,
            name: model.name,
            description: `模型ID: ${model.modelId}`
          }));
        }
        return [];

      case 'AGENT_CREATION':
      case 'STORAGE_USAGE':
        // 这些类型不需要选择具体的业务实体
        return [];

      default:
        return [];
    }
  }

  // 批量获取业务实体名称映射
  static async getBusinessNameMappings(): Promise<Map<string, string>> {
    const nameMap = new Map<string, string>();

    try {
      // 获取所有模型
      const modelResponse = await this.getOfficialModels();
      if (modelResponse.code === 200) {
        modelResponse.data.forEach(model => {
          nameMap.set(model.id, model.name);
        });
      }

      // 添加固定类型的映射
      nameMap.set('agent_creation', 'Agent创建');
      nameMap.set('storage', '存储使用');

    } catch (error) {
 
    }

    return nameMap;
  }

  // 根据商品类型和服务ID获取显示名称
  static getBusinessDisplayName(
    productType: string, 
    serviceId: string, 
    nameMap: Map<string, string>
  ): string {
    // 首先尝试从映射中获取名称
    const mappedName = nameMap.get(serviceId);
    if (mappedName) {
      return mappedName;
    }

    // 根据商品类型提供默认显示名称
    switch (productType) {
      case 'MODEL_USAGE':
        return `模型 ${serviceId}`;
      case 'AGENT_CREATION':
        return 'Agent创建';
      case 'STORAGE_USAGE':
        return '存储使用';
      default:
        return serviceId;
    }
  }

  // 获取商品类型对应的固定业务ID
  static getFixedServiceId(productType: string): string | null {
    switch (productType) {
      case 'AGENT_CREATION':
        return 'agent_creation';
      case 'STORAGE_USAGE':
        return 'storage';
      default:
        return null; // 需要用户选择
    }
  }

  // 检查商品类型是否需要业务ID选择器
  static needsServiceIdSelector(productType: string): boolean {
    const fixedServiceIdTypes = ['AGENT_CREATION', 'STORAGE_USAGE'];
    return !fixedServiceIdTypes.includes(productType);
  }
}