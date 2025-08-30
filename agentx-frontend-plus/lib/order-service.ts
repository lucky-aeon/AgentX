import { httpClient } from "@/lib/http-client";
import { API_ENDPOINTS } from "@/lib/api-config";
import { ApiResponse } from "@/types/api";
import { Order, GetUserOrdersParams, PageResponse } from "@/types/order";

/**
 * 用户订单服务
 */
export class OrderService {
  
  /**
   * 获取用户订单列表（已支付订单）
   * @param params 查询参数
   * @returns 订单分页数据
   */
  static async getUserOrders(params?: GetUserOrdersParams): Promise<ApiResponse<PageResponse<Order>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ORDERS, { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取订单列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取订单详情
   * @param orderId 订单ID
   * @returns 订单详情
   */
  static async getOrderDetail(orderId: string): Promise<ApiResponse<Order>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ORDER_DETAIL(orderId));
    } catch (error) {
      return {
        code: 500,
        message: "获取订单详情失败",
        data: {} as Order,
        timestamp: Date.now()
      };
    }
  }
}

/**
 * 带Toast提示的用户订单服务
 */

// 获取用户订单列表（带Toast提示）
export async function getUserOrdersWithToast(params?: GetUserOrdersParams): Promise<ApiResponse<PageResponse<Order>>> {
  try {
    const response = await OrderService.getUserOrders(params);
    return response;
  } catch (error) {
 
    return {
      code: 500,
      message: "获取订单列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    };
  }
}

// 获取订单详情（带Toast提示）
export async function getOrderDetailWithToast(orderId: string): Promise<ApiResponse<Order>> {
  try {
    const response = await OrderService.getOrderDetail(orderId);
    return response;
  } catch (error) {
 
    return {
      code: 500,
      message: "获取订单详情失败",
      data: {} as Order,
      timestamp: Date.now()
    };
  }
}