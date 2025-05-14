import { toast } from "@/hooks/use-toast";
import { MarketTool, UserTool } from "./types";
import { API_ENDPOINTS } from "@/lib/api-config";
import { httpClient } from "@/lib/http-client";
import { withToast } from "@/lib/toast-utils";

// 定义API响应类型，和withToast期望的类型保持一致
interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 在开发环境中使用模拟数据
const isDev = process.env.NODE_ENV === 'development';

// 获取市场工具列表
async function getMarketTools(params: { name?: string } = {}): Promise<ApiResponse<MarketTool[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOLS, { params });
  } catch (error) {
    console.error("获取工具市场数据失败", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now()
    };
  }
}

// 获取市场工具列表（带Toast提示）
export const getMarketToolsWithToast = withToast(getMarketTools);

// 获取用户工具列表
async function getUserTools(): Promise<ApiResponse<UserTool[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.USER_TOOLS);
  } catch (error) {
    console.error("获取用户工具数据失败", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now()
    };
  }
}

// 获取用户工具列表（带Toast提示）
export const getUserToolsWithToast = withToast(getUserTools);

// 安装工具
async function installTool(toolId: string, version: string = "0.0.1"): Promise<ApiResponse<any>> {
  try {
    return await httpClient.post(API_ENDPOINTS.INSTALL_TOOL(toolId, version));
  } catch (error) {
    console.error("安装工具失败", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now()
    };
  }
}

// 安装工具（带Toast提示）
export const installToolWithToast = withToast(
  installTool,
  { 
    successTitle: "安装成功",
    showSuccessToast: true
  }
);

// 删除用户安装的工具（卸载）
async function deleteUserTool(toolId: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.delete(API_ENDPOINTS.DELETE_USER_TOOL(toolId));
  } catch (error) {
    console.error("卸载工具失败", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now()
    };
  }
}

// 删除用户安装的工具（带Toast提示）
export const deleteUserToolWithToast = withToast(
  deleteUserTool,
  {
    successTitle: "卸载成功",
    showSuccessToast: true
  }
);

// 删除用户创建的工具
async function deleteTool(toolId: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.delete(API_ENDPOINTS.DELETE_TOOL(toolId));
  } catch (error) {
    console.error("删除工具失败", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now()
    };
  }
}

// 删除用户创建的工具（带Toast提示）
export const deleteToolWithToast = withToast(
  deleteTool,
  {
    successTitle: "删除成功",
    showSuccessToast: true
  }
); 