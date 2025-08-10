// 计费系统基础类型定义

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// MyBatis-Plus分页响应类型（匹配后端Page类型）
export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// 计费类型枚举
export enum BillingType {
  MODEL_USAGE = 'MODEL_USAGE',
  AGENT_CREATION = 'AGENT_CREATION',
  AGENT_USAGE = 'AGENT_USAGE',
  API_CALL = 'API_CALL',
  STORAGE_USAGE = 'STORAGE_USAGE'
}

// 规则处理器标识枚举
export enum RuleHandlerKey {
  MODEL_TOKEN_STRATEGY = 'MODEL_TOKEN_STRATEGY',
  PER_UNIT_STRATEGY = 'PER_UNIT_STRATEGY',
  TIERED_PRICING_STRATEGY = 'TIERED_PRICING_STRATEGY'
}

// 计费类型显示名称映射
export const BillingTypeNames = {
  [BillingType.MODEL_USAGE]: '模型调用计费',
  [BillingType.AGENT_CREATION]: 'Agent创建计费',
  [BillingType.AGENT_USAGE]: 'Agent使用计费',
  [BillingType.API_CALL]: 'API调用计费',
  [BillingType.STORAGE_USAGE]: '存储使用计费'
} as const;

// 规则处理器显示名称映射
export const RuleHandlerKeyNames = {
  [RuleHandlerKey.MODEL_TOKEN_STRATEGY]: '模型Token计费策略',
  [RuleHandlerKey.PER_UNIT_STRATEGY]: '按次计费策略',
  [RuleHandlerKey.TIERED_PRICING_STRATEGY]: '分层定价策略'
} as const;

// 商品状态枚举
export enum ProductStatus {
  ACTIVE = 1,
  INACTIVE = 0
}

// 商品状态显示名称映射
export const ProductStatusNames = {
  [ProductStatus.ACTIVE]: '激活',
  [ProductStatus.INACTIVE]: '禁用'
} as const;