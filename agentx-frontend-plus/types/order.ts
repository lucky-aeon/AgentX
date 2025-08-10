// 订单相关类型定义

// 订单接口
export interface Order {
  id: string;
  userId: string;
  userNickname?: string;
  orderNo: string;
  orderType: string;
  title: string;
  description?: string;
  amount: number;
  currency: string;
  status: number;
  statusName: string;
  expiredAt?: string;
  paidAt?: string;
  cancelledAt?: string;
  refundedAt?: string;
  refundAmount?: number;
  paymentPlatform?: string;
  paymentPlatformName?: string;
  paymentType?: string;
  paymentTypeName?: string;
  providerOrderId?: string;
  metadata?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

// 用户订单查询参数
export interface GetUserOrdersParams {
  orderType?: string;  // 订单类型
  status?: number;     // 订单状态
  page?: number;       // 页码
  pageSize?: number;   // 每页大小
}

// 管理员订单查询参数  
export interface GetAllOrdersParams {
  userId?: string;     // 用户ID
  orderType?: string;  // 订单类型
  status?: number;     // 订单状态
  keyword?: string;    // 关键词搜索
  page?: number;       // 页码
  pageSize?: number;   // 每页大小
}

// 分页响应接口
export interface PageResponse<T> {
  records: T[];        // 数据记录
  total: number;       // 总记录数
  size: number;        // 每页大小
  current: number;     // 当前页码
  pages: number;       // 总页数
}

// 订单状态枚举（匹配后端状态码）
export enum OrderStatus {
  PENDING = 1,    // 待支付
  PAID = 2,       // 已支付
  CANCELLED = 3,  // 已取消
  REFUNDED = 4,   // 已退款
  EXPIRED = 5     // 已过期
}

// 订单状态显示名称映射
export const ORDER_STATUS_NAMES = {
  [OrderStatus.PENDING]: '待支付',
  [OrderStatus.PAID]: '已支付',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDED]: '已退款',
  [OrderStatus.EXPIRED]: '已过期'
};

// 订单状态颜色映射（用于Badge组件）
export const ORDER_STATUS_VARIANTS = {
  [OrderStatus.PENDING]: 'secondary' as const,
  [OrderStatus.PAID]: 'default' as const,
  [OrderStatus.CANCELLED]: 'destructive' as const,
  [OrderStatus.REFUNDED]: 'outline' as const,
  [OrderStatus.EXPIRED]: 'destructive' as const
};

// 订单类型枚举
export enum OrderType {
  RECHARGE = 'RECHARGE',    // 充值订单
  PURCHASE = 'PURCHASE',    // 购买订单
  SUBSCRIBE = 'SUBSCRIBE',  // 订阅订单
  RENEWAL = 'RENEWAL'       // 续费订单
}

// 订单类型显示名称映射
export const ORDER_TYPE_NAMES = {
  [OrderType.RECHARGE]: '充值订单',
  [OrderType.PURCHASE]: '购买订单',
  [OrderType.SUBSCRIBE]: '订阅订单',
  [OrderType.RENEWAL]: '续费订单'
};

// 支付平台枚举
export enum PaymentPlatform {
  ALIPAY = 'ALIPAY',
  WECHAT = 'WECHAT',
  STRIPE = 'STRIPE'
}

// 支付平台显示名称映射
export const PAYMENT_PLATFORM_NAMES = {
  [PaymentPlatform.ALIPAY]: '支付宝',
  [PaymentPlatform.WECHAT]: '微信支付',
  [PaymentPlatform.STRIPE]: 'Stripe'
};