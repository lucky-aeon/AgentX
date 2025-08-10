// 管理员规则管理API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { 
  Rule, 
  CreateRuleRequest, 
  UpdateRuleRequest, 
  QueryRuleRequest,
  RuleOption
} from '@/types/rule';
import { PageResponse } from '@/types/billing';

// API端点
const API_ENDPOINTS = {
  RULES: '/admin/rules',
  RULE_BY_ID: (id: string) => `/admin/rules/${id}`,
  RULE_BY_HANDLER: (handlerKey: string) => `/admin/rules/handler/${handlerKey}`,
  RULE_EXISTS: (id: string) => `/admin/rules/${id}/exists`,
  HANDLER_EXISTS: (handlerKey: string) => `/admin/rules/handler/${handlerKey}/exists`
} as const;

export class AdminRuleService {
  // 分页查询计费规则
  static async getRules(params?: QueryRuleRequest): Promise<ApiResponse<PageResponse<Rule>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.RULES, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取规则列表失败',
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  // 获取所有计费规则
  static async getAllRules(): Promise<ApiResponse<Rule[]>> {
    try {
      return await httpClient.get(`${API_ENDPOINTS.RULES}/all`);
    } catch (error) {
      return {
        code: 500,
        message: '获取所有规则失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }

  // 根据ID获取计费规则
  static async getRuleById(id: string): Promise<ApiResponse<Rule>> {
    try {
      return await httpClient.get(API_ENDPOINTS.RULE_BY_ID(id));
    } catch (error) {
      return {
        code: 500,
        message: '获取规则详情失败',
        data: {} as Rule,
        timestamp: Date.now()
      };
    }
  }

  // 根据处理器标识获取规则
  static async getRuleByHandlerKey(handlerKey: string): Promise<ApiResponse<Rule>> {
    try {
      return await httpClient.get(API_ENDPOINTS.RULE_BY_HANDLER(handlerKey));
    } catch (error) {
      return {
        code: 500,
        message: '获取规则失败',
        data: {} as Rule,
        timestamp: Date.now()
      };
    }
  }

  // 创建计费规则
  static async createRule(data: CreateRuleRequest): Promise<ApiResponse<Rule>> {
    try {
      return await httpClient.post(API_ENDPOINTS.RULES, data);
    } catch (error) {
      return {
        code: 500,
        message: '创建规则失败',
        data: {} as Rule,
        timestamp: Date.now()
      };
    }
  }

  // 更新计费规则
  static async updateRule(id: string, data: UpdateRuleRequest): Promise<ApiResponse<Rule>> {
    try {
      return await httpClient.put(API_ENDPOINTS.RULE_BY_ID(id), data);
    } catch (error) {
      return {
        code: 500,
        message: '更新规则失败',
        data: {} as Rule,
        timestamp: Date.now()
      };
    }
  }

  // 删除计费规则
  static async deleteRule(id: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete(API_ENDPOINTS.RULE_BY_ID(id));
    } catch (error) {
      return {
        code: 500,
        message: '删除规则失败',
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  // 检查规则是否存在
  static async existsRule(id: string): Promise<ApiResponse<boolean>> {
    try {
      return await httpClient.get(API_ENDPOINTS.RULE_EXISTS(id));
    } catch (error) {
      return {
        code: 500,
        message: '检查规则存在性失败',
        data: false,
        timestamp: Date.now()
      };
    }
  }

  // 检查处理器标识是否存在
  static async existsByHandlerKey(handlerKey: string): Promise<ApiResponse<boolean>> {
    try {
      return await httpClient.get(API_ENDPOINTS.HANDLER_EXISTS(handlerKey));
    } catch (error) {
      return {
        code: 500,
        message: '检查处理器标识存在性失败',
        data: false,
        timestamp: Date.now()
      };
    }
  }

  // 获取规则选项列表（用于下拉选择）
  static async getRuleOptions(): Promise<RuleOption[]> {
    try {
      const response = await this.getAllRules();
      if (response.code === 200) {
        return response.data.map(rule => ({
          value: rule.id,
          label: rule.name,
          description: rule.description
        }));
      }
      return [];
    } catch (error) {
      return [];
    }
  }
}

// 带Toast提示的API服务方法
export const AdminRuleServiceWithToast = {
  async getRules(params?: QueryRuleRequest) {
    return AdminRuleService.getRules(params);
  },

  async createRule(data: CreateRuleRequest) {
    return httpClient.post(API_ENDPOINTS.RULES, data, {}, { showToast: true });
  },

  async updateRule(id: string, data: UpdateRuleRequest) {
    return httpClient.put(API_ENDPOINTS.RULE_BY_ID(id), data, {}, { showToast: true });
  },

  async deleteRule(id: string) {
    return httpClient.delete(API_ENDPOINTS.RULE_BY_ID(id), {}, { showToast: true });
  }
};

export default AdminRuleService;