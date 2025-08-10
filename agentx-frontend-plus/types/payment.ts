// 支付相关类型定义

// 支付响应（匹配后端PaymentResponseDTO）
export interface PaymentResponse {
  orderId: string;
  orderNo: string;
  paymentUrl: string; // 支付URL（网页跳转链接或二维码内容）
  paymentMethod: string; // 支付平台
  paymentType: string; // 支付类型
  amount: number;
  title: string;
  status: string;
}

// 订单状态响应（匹配后端OrderStatusResponseDTO）
export interface OrderStatusResponse {
  orderId: string;
  orderNo: string;
  status: string;
  paymentPlatform: string;
  paymentType: string;
  amount: number;
  title: string;
  paymentUrl?: string;
  createdAt: string;
  updatedAt: string;
  expiredAt?: string;
}

// 支付类型DTO（匹配后端PaymentMethodDTO.PaymentTypeDTO）
export interface PaymentTypeDTO {
  typeCode: string;
  typeName: string;
  requireRedirect: boolean;
  description?: string;
}

// 支付方法DTO（匹配后端PaymentMethodDTO）
export interface PaymentMethodDTO {
  platformCode: string;
  platformName: string;
  available: boolean;
  paymentTypes: PaymentTypeDTO[];
  description?: string;
  iconUrl?: string;
}

// 订单状态枚举
export enum OrderStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
  REFUNDED = 'REFUNDED'
}

// 支付状态
export type PaymentState = 'idle' | 'selecting' | 'generating' | 'waiting' | 'polling' | 'success' | 'failed' | 'expired';

// 轮询配置
export interface PollingConfig {
  maxDuration: number; // 最大轮询时间（毫秒）
  interval: number; // 轮询间隔（毫秒）
}

// 充值步骤
export enum RechargeStep {
  SELECT_AMOUNT = 'select_amount',
  SELECT_PAYMENT = 'select_payment',
  GENERATING_PAYMENT = 'generating_payment',
  WAITING_PAYMENT = 'waiting_payment',
  PAYMENT_SUCCESS = 'payment_success',
  PAYMENT_FAILED = 'payment_failed'
}

// 充值状态
export interface RechargeState {
  step: RechargeStep;
  amount: number;
  selectedPlatform?: string;
  selectedType?: string;
  paymentResponse?: PaymentResponse;
  orderStatus?: OrderStatusResponse;
  error?: string;
  isPolling?: boolean;
}

// QR码显示状态
export type QRCodeStatus = 'generating' | 'waiting' | 'expired' | 'scanned' | 'success' | 'failed';

// 支付组件属性
export interface PaymentComponentProps {
  onSuccess?: (orderNo: string) => void;
  onCancel?: () => void;
  onError?: (error: string) => void;
}

// 轮询回调
export interface PollingCallbacks {
  onStatusChange?: (status: OrderStatusResponse) => void;
  onSuccess?: (orderNo: string) => void;
  onFailed?: (reason: string) => void;
  onExpired?: () => void;
  onError?: (error: string) => void;
}