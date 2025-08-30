// 支付API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { RechargeRequest } from '@/types/account';
import { 
  PaymentResponse, 
  OrderStatusResponse, 
  PaymentMethodDTO 
} from '@/types/payment';

// API端点
const API_ENDPOINTS = {
  CREATE_RECHARGE_PAYMENT: '/payments/recharge',
  QUERY_ORDER_STATUS: '/payments/orders',
  GET_PAYMENT_METHODS: '/payments/methods'
} as const;

export class PaymentService {
  
  /** 创建充值支付 */
  static async createRechargePayment(data: RechargeRequest): Promise<ApiResponse<PaymentResponse>> {
    try {
      return await httpClient.post(API_ENDPOINTS.CREATE_RECHARGE_PAYMENT, data);
    } catch (error) {
      return {
        code: 500,
        message: '创建支付失败',
        data: {} as PaymentResponse,
        timestamp: Date.now()
      };
    }
  }

  /** 查询订单状态 */
  static async queryOrderStatus(orderNo: string): Promise<ApiResponse<OrderStatusResponse>> {
    try {
      return await httpClient.get(`${API_ENDPOINTS.QUERY_ORDER_STATUS}/${orderNo}/status`);
    } catch (error) {
      return {
        code: 500,
        message: '查询订单状态失败',
        data: {} as OrderStatusResponse,
        timestamp: Date.now()
      };
    }
  }

  /** 获取可用的支付方法列表 */
  static async getAvailablePaymentMethods(): Promise<ApiResponse<PaymentMethodDTO[]>> {
    try {
      return await httpClient.get(API_ENDPOINTS.GET_PAYMENT_METHODS);
    } catch (error) {
      return {
        code: 500,
        message: '获取支付方法失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }

  /** 轮询订单状态 */
  static async pollOrderStatus(
    orderNo: string,
    callbacks: {
      onStatusChange?: (status: OrderStatusResponse) => void;
      onSuccess?: (orderNo: string) => void;
      onFailed?: (reason: string) => void;
      onExpired?: () => void;
      onError?: (error: string) => void;
    },
    config: {
      maxDuration?: number; // 最大轮询时间（毫秒）
      interval?: number; // 轮询间隔（毫秒）
    } = {}
  ): Promise<() => void> {
    
    // 默认轮询配置：每3秒查询一次，最多查询5分钟
    const defaultConfig = {
      maxDuration: 300000, // 5分钟
      interval: 3000 // 每3秒查询一次
    };
    
    const finalConfig = { ...defaultConfig, ...config };
    
    let intervalHandle: NodeJS.Timeout | null = null;
    let isPolling = true;
    let elapsedTime = 0;
    
    const stopPolling = () => {
 
      isPolling = false;
      if (intervalHandle) {
        clearInterval(intervalHandle);
        intervalHandle = null;
 
      }
    };
    
    const startPolling = () => {
      if (!isPolling) {
 
        return;
      }
      
 
      
      intervalHandle = setInterval(async () => {
        if (!isPolling) {
 
          stopPolling();
          return;
        }
        
 
        
        try {
          const response = await PaymentService.queryOrderStatus(orderNo);
          
          if (response.code === 200) {
            const orderStatus = response.data;
 
            
            // 通知状态变化
            callbacks.onStatusChange?.(orderStatus);
            
            // 检查订单状态
            switch (orderStatus.status) {
              case 'PAID':
 
                callbacks.onSuccess?.(orderNo);
                stopPolling();
                return;
              case 'CANCELLED':
 
                callbacks.onFailed?.('订单已取消');
                stopPolling();
                return;
              case 'EXPIRED':
 
                callbacks.onExpired?.();
                stopPolling();
                return;
              case 'PENDING':
 
                break;
              default:
 
                break;
            }
          } else {
 
          }
        } catch (error) {
 
          callbacks.onError?.('网络错误，请检查网络连接');
        }
        
        // 更新经过时间
        elapsedTime += finalConfig.interval;
 
        
        // 检查是否超过总时间限制
        if (elapsedTime >= finalConfig.maxDuration) {
 
          callbacks.onExpired?.();
          stopPolling();
          return;
        }
      }, finalConfig.interval);
      
 
    };
    
    // 立即执行一次查询
 
    try {
      const response = await PaymentService.queryOrderStatus(orderNo);
      if (response.code === 200) {
 
        callbacks.onStatusChange?.(response.data);
        
        // 如果已经完成，就不需要轮询了
        if (['PAID', 'CANCELLED', 'EXPIRED'].includes(response.data.status)) {
 
          switch (response.data.status) {
            case 'PAID':
              callbacks.onSuccess?.(orderNo);
              break;
            case 'CANCELLED':
              callbacks.onFailed?.('订单已取消');
              break;
            case 'EXPIRED':
              callbacks.onExpired?.();
              break;
          }
          return stopPolling;
        }
      } else {
 
      }
    } catch (error) {
 
    }
    
    // 开始轮询
 
    startPolling();
    
    // 返回停止函数
 
    return stopPolling;
  }
}

// 带Toast提示的API服务方法
export const PaymentServiceWithToast = {
  async createRechargePayment(data: RechargeRequest) {
    return httpClient.post(API_ENDPOINTS.CREATE_RECHARGE_PAYMENT, data, {}, { showToast: true });
  }
};

export default PaymentService;