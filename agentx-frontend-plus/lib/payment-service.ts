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
      console.log(`[轮询] 停止轮询 - orderNo: ${orderNo}, intervalHandle: ${intervalHandle}`);
      isPolling = false;
      if (intervalHandle) {
        clearInterval(intervalHandle);
        intervalHandle = null;
        console.log(`[轮询] 定时器已清除 - orderNo: ${orderNo}`);
      }
    };
    
    const startPolling = () => {
      if (!isPolling) {
        console.log(`[轮询] 轮询已停止，不启动新的轮询 - orderNo: ${orderNo}`);
        return;
      }
      
      console.log(`[轮询] 开始轮询订单状态 - orderNo: ${orderNo}, 间隔: ${finalConfig.interval}ms, 最大时长: ${finalConfig.maxDuration}ms`);
      
      intervalHandle = setInterval(async () => {
        if (!isPolling) {
          console.log(`[轮询] 轮询状态为停止，结束轮询 - orderNo: ${orderNo}`);
          stopPolling();
          return;
        }
        
        console.log(`[轮询] 第${Math.floor(elapsedTime / finalConfig.interval) + 1}次查询订单状态 - orderNo: ${orderNo}, 已经过时间: ${elapsedTime}ms`);
        
        try {
          const response = await PaymentService.queryOrderStatus(orderNo);
          
          if (response.code === 200) {
            const orderStatus = response.data;
            console.log(`[轮询] 查询成功，订单状态: ${orderStatus.status} - orderNo: ${orderNo}`);
            
            // 通知状态变化
            callbacks.onStatusChange?.(orderStatus);
            
            // 检查订单状态
            switch (orderStatus.status) {
              case 'PAID':
                console.log(`[轮询] 订单已支付，停止轮询 - orderNo: ${orderNo}`);
                callbacks.onSuccess?.(orderNo);
                stopPolling();
                return;
              case 'CANCELLED':
                console.log(`[轮询] 订单已取消，停止轮询 - orderNo: ${orderNo}`);
                callbacks.onFailed?.('订单已取消');
                stopPolling();
                return;
              case 'EXPIRED':
                console.log(`[轮询] 订单已过期，停止轮询 - orderNo: ${orderNo}`);
                callbacks.onExpired?.();
                stopPolling();
                return;
              case 'PENDING':
                console.log(`[轮询] 订单待支付，继续轮询 - orderNo: ${orderNo}`);
                break;
              default:
                console.log(`[轮询] 未知订单状态: ${orderStatus.status}，继续轮询 - orderNo: ${orderNo}`);
                break;
            }
          } else {
            console.warn(`[轮询] API调用失败: ${response.message} - orderNo: ${orderNo}`);
          }
        } catch (error) {
          console.error(`[轮询] 轮询异常 - orderNo: ${orderNo}`, error);
          callbacks.onError?.('网络错误，请检查网络连接');
        }
        
        // 更新经过时间
        elapsedTime += finalConfig.interval;
        console.log(`[轮询] 更新经过时间: ${elapsedTime}ms / ${finalConfig.maxDuration}ms - orderNo: ${orderNo}`);
        
        // 检查是否超过总时间限制
        if (elapsedTime >= finalConfig.maxDuration) {
          console.log(`[轮询] 达到最大时长，停止轮询 - orderNo: ${orderNo}`);
          callbacks.onExpired?.();
          stopPolling();
          return;
        }
      }, finalConfig.interval);
      
      console.log(`[轮询] 轮询定时器已启动，intervalHandle: ${intervalHandle} - orderNo: ${orderNo}`);
    };
    
    // 立即执行一次查询
    console.log(`[轮询] 立即执行初始查询 - orderNo: ${orderNo}`);
    try {
      const response = await PaymentService.queryOrderStatus(orderNo);
      if (response.code === 200) {
        console.log(`[轮询] 初始查询成功，订单状态: ${response.data.status} - orderNo: ${orderNo}`);
        callbacks.onStatusChange?.(response.data);
        
        // 如果已经完成，就不需要轮询了
        if (['PAID', 'CANCELLED', 'EXPIRED'].includes(response.data.status)) {
          console.log(`[轮询] 订单已完成，无需轮询 - orderNo: ${orderNo}, status: ${response.data.status}`);
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
        console.warn(`[轮询] 初始查询失败: ${response.message} - orderNo: ${orderNo}`);
      }
    } catch (error) {
      console.error(`[轮询] 初始查询订单状态异常 - orderNo: ${orderNo}`, error);
    }
    
    // 开始轮询
    console.log(`[轮询] 开始启动定时轮询 - orderNo: ${orderNo}`);
    startPolling();
    
    // 返回停止函数
    console.log(`[轮询] 返回停止函数 - orderNo: ${orderNo}`);
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