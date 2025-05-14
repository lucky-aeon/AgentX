import { httpClient } from "@/lib/http-client"
import { API_ENDPOINTS, API_CONFIG } from "@/lib/api-config"
import { toast } from "@/hooks/use-toast"
import { Tool, ToolVersion, ApiResponse, GetMarketToolsParams } from "@/types/tool"
import { withToast } from "./toast-utils"

// 获取工具市场列表
export async function getMarketTools(params?: GetMarketToolsParams): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOLS, { params })
  } catch (error) {
    console.error("获取工具市场列表失败", error)
    return {
      code: 500,
      message: "获取工具市场列表失败",
      data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      timestamp: Date.now()
    }
  }
}

// 获取工具市场列表（带Toast提示）
export const getMarketToolsWithToast = withToast(getMarketTools)

// 获取工具详情
export async function getMarketToolDetail(id: string): Promise<ApiResponse<Tool>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOL_DETAIL(id))
  } catch (error) {
    console.error("获取工具详情失败", error)
    return {
      code: 500,
      message: "获取工具详情失败",
      data: null as any,
      timestamp: Date.now()
    }
  }
}

// 获取工具详情（带Toast提示）
export const getMarketToolDetailWithToast = withToast(getMarketToolDetail)

// 获取工具版本详情
export async function getMarketToolVersionDetail(id: string, version: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOL_VERSION_DETAIL(id, version))
  } catch (error) {
    console.error("获取工具版本详情失败", error)
    return {
      code: 500,
      message: "获取工具版本详情失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

// 获取工具版本详情（带Toast提示）
export const getMarketToolVersionDetailWithToast = withToast(getMarketToolVersionDetail)

// 获取工具版本列表
export async function getMarketToolVersions(id: string): Promise<ApiResponse<ToolVersion[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOL_VERSIONS(id))
  } catch (error) {
    console.error("获取工具版本列表失败", error)
    return {
      code: 500,
      message: "获取工具版本列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

// 获取工具版本列表（带Toast提示）
export const getMarketToolVersionsWithToast = withToast(getMarketToolVersions, {
  showSuccessToast: false
});

// 获取工具标签列表
export async function getMarketToolLabels(): Promise<ApiResponse<string[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOL_LABELS)
  } catch (error) {
    console.error("获取工具标签列表失败", error)
    return {
      code: 500,
      message: "获取工具标签列表失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

// 安装工具
export async function installTool(toolId: string, version: string): Promise<ApiResponse<any>> {
  try {
    console.log(`安装工具：toolId=${toolId}, version=${version}`);
    return await httpClient.post(API_ENDPOINTS.INSTALL_TOOL(toolId, version))
  } catch (error) {
    console.error("安装工具失败", error)
    return {
      code: 500,
      message: "安装工具失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

// 安装工具（带Toast提示）
export const installToolWithToast = withToast(
  installTool,
  { 
    successTitle: "安装成功",
    showSuccessToast: true
  }
)

/**
 * 获取用户已安装的工具和推荐工具列表
 */
export async function getUserTools(params?: any): Promise<ApiResponse<Tool[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.USER_TOOLS, { params });
  } catch (error) {
    console.error("获取用户工具失败", error);
    return {
      code: 500,
      message: "获取用户工具数据失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/**
 * 获取用户已安装的工具（带Toast提示）
 */
export const getUserToolsWithToast = withToast(
  getUserTools, 
  {
    successTitle: "获取成功",
    errorTitle: "获取失败",
    showSuccessToast: false
  }
);

// 删除用户安装的工具
export async function deleteUserTool(id: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.delete(API_ENDPOINTS.DELETE_USER_TOOL(id))
  } catch (error) {
    console.error("删除工具失败", error)
    return {
      code: 500,
      message: "删除工具失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

// 删除用户安装的工具（带Toast提示）
export const deleteUserToolWithToast = withToast(
  deleteUserTool,
  {
    successTitle: "删除成功",
    showSuccessToast: true
  }
)

// 上传工具
export async function uploadTool(data: any): Promise<ApiResponse<Tool>> {
  try {
    return await httpClient.post(API_ENDPOINTS.UPLOAD_TOOL, data)
  } catch (error) {
    console.error("上传工具失败", error)
    return {
      code: 500,
      message: "上传工具失败",
      data: null as any,
      timestamp: Date.now()
    }
  }
}

// 上传工具（带Toast提示）
export const uploadToolWithToast = withToast(
  uploadTool,
  {
    successTitle: "上传成功",
    showSuccessToast: true
  }
)

// 删除工具
export async function deleteTool(id: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.delete(API_ENDPOINTS.DELETE_TOOL(id))
  } catch (error) {
    console.error("删除工具失败", error)
    return {
      code: 500,
      message: "删除工具失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

// 删除工具（带Toast提示）
export const deleteToolWithToast = withToast(
  deleteTool,
  {
    successTitle: "删除成功",
    showSuccessToast: true
  }
)

// 获取用户工具详情
export async function getToolDetail(id: string): Promise<ApiResponse<Tool>> {
  try {
    return await httpClient.get(API_ENDPOINTS.TOOL_DETAIL(id))
  } catch (error) {
    console.error("获取工具详情失败", error)
    return {
      code: 500,
      message: "获取工具详情失败",
      data: null as any,
      timestamp: Date.now()
    }
  }
}

// 获取用户工具详情（带Toast提示）
export const getToolDetailWithToast = withToast(getToolDetail)

// 更新工具
export async function updateTool(id: string, data: any): Promise<ApiResponse<Tool>> {
  try {
    return await httpClient.put(API_ENDPOINTS.UPDATE_TOOL(id), data)
  } catch (error) {
    console.error("更新工具失败", error)
    return {
      code: 500,
      message: "更新工具失败",
      data: null as any,
      timestamp: Date.now()
    }
  }
}

// 更新工具（带Toast提示）
export const updateToolWithToast = withToast(
  updateTool,
  {
    successTitle: "更新成功",
    showSuccessToast: true
  }
)

/**
 * 获取用户已安装的工具列表
 */
export async function getInstalledTools(params?: {
  page?: number;
  pageSize?: number;
  toolName?: string;
}): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get(API_ENDPOINTS.INSTALLED_TOOLS, { params });
  } catch (error) {
    console.error("获取已安装工具失败", error);
    return {
      code: 500,
      message: "获取已安装工具失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/**
 * 获取用户已安装的工具列表（带Toast提示）
 */
export const getInstalledToolsWithToast = withToast(
  getInstalledTools, 
  {
    successTitle: "获取成功",
    errorTitle: "获取失败",
    showSuccessToast: false
  }
);

/**
 * 卸载工具
 */
export async function uninstallTool(toolId: string): Promise<ApiResponse<any>> {
  try {
    console.log(`卸载工具：toolId=${toolId}`);
    return await httpClient.post(API_ENDPOINTS.UNINSTALL_TOOL(toolId))
  } catch (error) {
    console.error("卸载工具失败", error);
    return {
      code: 500,
      message: "卸载工具失败",
      data: null,
      timestamp: Date.now()
    }
  }
}

/**
 * 卸载工具（带Toast提示）
 */
export const uninstallToolWithToast = withToast(
  uninstallTool,
  {
    successTitle: "卸载成功",
    errorTitle: "卸载失败",
    showSuccessToast: true
  }
); 