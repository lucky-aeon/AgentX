// 账户管理API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { 
  Account, 
  RechargeRequest, 
  AddCreditRequest, 
  AccountStats,
  BalanceTransaction
} from '@/types/account';

// API端点（匹配后端Controller路径）
const API_ENDPOINTS = {
  CURRENT_ACCOUNT: '/accounts/current',
  // 注意：以下端点在当前后端中尚未实现
  RECHARGE: '/accounts/recharge',
  ADD_CREDIT: '/accounts/credit',
  ACCOUNT_STATS: '/accounts/stats',
  BALANCE_HISTORY: '/accounts/balance/history'
} as const;

export class AccountService {
  // 获取当前用户账户信息
  static async getCurrentUserAccount(): Promise<ApiResponse<Account>> {
    try {
      return await httpClient.get(API_ENDPOINTS.CURRENT_ACCOUNT);
    } catch (error) {
      return {
        code: 500,
        message: '获取账户信息失败',
        data: {} as Account,
        timestamp: Date.now()
      };
    }
  }

  // 账户充值
  static async recharge(data: RechargeRequest): Promise<ApiResponse<Account>> {
    try {
      return await httpClient.post(API_ENDPOINTS.RECHARGE, data);
    } catch (error) {
      return {
        code: 500,
        message: '充值失败',
        data: {} as Account,
        timestamp: Date.now()
      };
    }
  }

  // 添加信用额度
  static async addCredit(data: AddCreditRequest): Promise<ApiResponse<Account>> {
    try {
      return await httpClient.post(API_ENDPOINTS.ADD_CREDIT, data);
    } catch (error) {
      return {
        code: 500,
        message: '添加信用额度失败',
        data: {} as Account,
        timestamp: Date.now()
      };
    }
  }

  // 获取账户统计信息
  static async getAccountStats(): Promise<ApiResponse<AccountStats>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ACCOUNT_STATS);
    } catch (error) {
      return {
        code: 500,
        message: '获取账户统计失败',
        data: {} as AccountStats,
        timestamp: Date.now()
      };
    }
  }

  // 获取余额变动历史
  static async getBalanceHistory(params?: {
    startDate?: string;
    endDate?: string;
    type?: string;
    page?: number;
    pageSize?: number;
  }): Promise<ApiResponse<BalanceTransaction[]>> {
    try {
      return await httpClient.get(API_ENDPOINTS.BALANCE_HISTORY, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取余额历史失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }
}

// 带Toast提示的API服务方法
export const AccountServiceWithToast = {
  async getCurrentUserAccount() {
    return AccountService.getCurrentUserAccount();
  },

  async recharge(data: RechargeRequest) {
    return httpClient.post(API_ENDPOINTS.RECHARGE, data, {}, { showToast: true });
  },

  async addCredit(data: AddCreditRequest) {
    return httpClient.post(API_ENDPOINTS.ADD_CREDIT, data, {}, { showToast: true });
  }
};

export default AccountService;