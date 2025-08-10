// 用量记录API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { 
  UsageRecord, 
  QueryUsageRecordRequest, 
  UsageStats,
  UsageRecordDetail,
  UsageChartData
} from '@/types/usage-record';
import { PageResponse } from '@/types/billing';

// API端点（匹配后端PortalUsageRecordController路径）
const API_ENDPOINTS = {
  USAGE_RECORDS: '/usage-records',
  USAGE_RECORD_BY_ID: (id: string) => `/usage-records/${id}`,
  TOTAL_COST: '/usage-records/current/total-cost',
  // 注意：以下端点在当前后端中尚未实现
  USAGE_STATS: '/usage-records/stats',
  USAGE_CHART: '/usage-records/chart'
} as const;

export class UsageRecordService {
  // 按条件查询当前用户使用记录
  static async queryUsageRecords(params?: QueryUsageRecordRequest): Promise<ApiResponse<PageResponse<UsageRecord>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.USAGE_RECORDS, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取用量记录失败',
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  // 根据ID获取使用记录
  static async getUsageRecordById(id: string): Promise<ApiResponse<UsageRecord>> {
    try {
      return await httpClient.get(API_ENDPOINTS.USAGE_RECORD_BY_ID(id));
    } catch (error) {
      return {
        code: 500,
        message: '获取用量记录详情失败',
        data: {} as UsageRecord,
        timestamp: Date.now()
      };
    }
  }

  // 获取当前用户的总消费金额（后端返回BigDecimal，转为number）
  static async getCurrentUserTotalCost(): Promise<ApiResponse<number>> {
    try {
      const response = await httpClient.get(API_ENDPOINTS.TOTAL_COST);
      // 如果后端返回BigDecimal格式，转换为number
      if (response.code === 200 && response.data) {
        response.data = Number(response.data);
      }
      return response;
    } catch (error) {
      return {
        code: 500,
        message: '获取总消费金额失败',
        data: 0,
        timestamp: Date.now()
      };
    }
  }

  // 获取用量统计信息
  static async getUsageStats(params?: {
    startDate?: string;
    endDate?: string;
    productId?: string;
  }): Promise<ApiResponse<UsageStats>> {
    try {
      return await httpClient.get(API_ENDPOINTS.USAGE_STATS, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取用量统计失败',
        data: {} as UsageStats,
        timestamp: Date.now()
      };
    }
  }

  // 获取用量图表数据
  static async getUsageChartData(params?: {
    startDate?: string;
    endDate?: string;
    productId?: string;
    granularity?: 'day' | 'week' | 'month';
  }): Promise<ApiResponse<UsageChartData[]>> {
    try {
      return await httpClient.get(API_ENDPOINTS.USAGE_CHART, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取用量图表数据失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }

  // 导出用量记录
  static async exportUsageRecords(params?: QueryUsageRecordRequest): Promise<Blob> {
    try {
      const response = await httpClient.get(`${API_ENDPOINTS.USAGE_RECORDS}/export`, 
        { params }, 
        { raw: true }
      ) as Response;
      return await response.blob();
    } catch (error) {
      throw new Error('导出用量记录失败');
    }
  }
}

// 带Toast提示的API服务方法
export const UsageRecordServiceWithToast = {
  async queryUsageRecords(params?: QueryUsageRecordRequest) {
    return UsageRecordService.queryUsageRecords(params);
  },

  async exportUsageRecords(params?: QueryUsageRecordRequest) {
    try {
      const blob = await UsageRecordService.exportUsageRecords(params);
      // 创建下载链接
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = url;
      a.download = `usage-records-${new Date().toISOString().split('T')[0]}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      return {
        code: 200,
        message: '导出成功',
        data: null,
        timestamp: Date.now()
      };
    } catch (error) {
      return {
        code: 500,
        message: '导出失败',
        data: null,
        timestamp: Date.now()
      };
    }
  }
};

export default UsageRecordService;