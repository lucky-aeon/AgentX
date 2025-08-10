import { httpClient } from "@/lib/http-client";
import { API_ENDPOINTS } from "@/lib/api-config";
import { ApiResponse } from "@/types/api";
import { Order, GetAllOrdersParams, PageResponse } from "@/types/order";

/**
 * 管理员订单服务
 */
export class AdminOrderService {
  
  /**
   * 分页获取所有订单列表
   * @param params 查询参数
   * @returns 订单分页数据
   */
  static async getAllOrders(params?: GetAllOrdersParams): Promise<ApiResponse<PageResponse<Order>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ADMIN_ORDERS, { params });
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
      return await httpClient.get(API_ENDPOINTS.ADMIN_ORDER_DETAIL(orderId));
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
 * 带Toast提示的管理员订单服务
 */

// 获取所有订单列表（带Toast提示）
export async function getAllOrdersWithToast(params?: GetAllOrdersParams): Promise<ApiResponse<PageResponse<Order>>> {
  try {
    const response = await AdminOrderService.getAllOrders(params);
    return response;
  } catch (error) {
    console.error('获取订单列表失败:', error);
    return {
      code: 500,
      message: "获取订单列表失败",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now()
    };
  }
}

// 获取订单详情（带Toast提示）
export async function getAdminOrderDetailWithToast(orderId: string): Promise<ApiResponse<Order>> {
  try {
    const response = await AdminOrderService.getOrderDetail(orderId);
    return response;
  } catch (error) {
    console.error('获取订单详情失败:', error);
    return {
      code: 500,
      message: "获取订单详情失败",
      data: {} as Order,
      timestamp: Date.now()
    };
  }
}