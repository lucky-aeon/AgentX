// 账户相关类型定义

// 账户接口（匹配后端AccountDTO）
export interface Account {
  id: string;
  userId: string;
  balance: number;
  credit: number;
  totalConsumed: number;
  lastTransactionAt?: string;
  createdAt: string;
  updatedAt: string;
  availableBalance: number; // 可用余额（余额+信用额度，后端计算字段）
}

// 充值请求（匹配后端RechargeRequest）
export interface RechargeRequest {
  amount: number;
  paymentPlatform: string; // 支付平台代码（如：alipay, stripe, wechat）
  paymentType: string; // 支付类型代码（如：WEB, QR_CODE, MOBILE）
  remark?: string; // 备注信息
}

// 添加信用额度请求
export interface AddCreditRequest {
  amount: number;
  reason?: string;
}

// 账户统计接口
export interface AccountStats {
  totalUsers: number;
  totalBalance: number;
  totalCredit: number;
  totalConsumed: number;
  averageBalance: number;
}

// 账户余额变动记录
export interface BalanceTransaction {
  id: string;
  userId: string;
  type: 'recharge' | 'consume' | 'credit' | 'refund';
  amount: number;
  balance: number;
  description: string;
  createdAt: string;
}