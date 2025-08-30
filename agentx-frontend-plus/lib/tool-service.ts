import { httpClient } from "@/lib/http-client"
import { API_ENDPOINTS, API_CONFIG } from "@/lib/api-config"
import { toast } from "@/hooks/use-toast"
import { Tool, ToolVersion, ApiResponse, GetMarketToolsParams, PublishToolToMarketParams } from "@/types/tool"
import { withToast } from "./toast-utils"

// 获取工具市场列表
export async function getMarketTools(params?: GetMarketToolsParams): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOLS, { params })
  } catch (error) {
 
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
 
    return await httpClient.post(API_ENDPOINTS.INSTALL_TOOL(toolId, version))
  } catch (error) {
 
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
 
    return await httpClient.post(API_ENDPOINTS.UNINSTALL_TOOL(toolId))
  } catch (error) {
 
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

/**
 * 获取推荐工具列表
 */
export async function getRecommendTools(): Promise<ApiResponse<any>> {
  try {
    return await httpClient.get(API_ENDPOINTS.RECOMMEND_TOOLS);
  } catch (error) {
 
    return {
      code: 500,
      message: "获取推荐工具失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/**
 * 获取推荐工具列表（带Toast提示）
 */
export const getRecommendToolsWithToast = withToast(
  getRecommendTools,
  {
    successTitle: "获取成功",
    errorTitle: "获取失败",
    showSuccessToast: false
  }
);

/**
 * 修改工具版本发布状态
 * @param toolId 工具ID
 * @param version 版本号
 * @param publishStatus 发布状态
 */
export async function updateToolVersionStatus(
  toolId: string, 
  version: string, 
  publishStatus: boolean
): Promise<ApiResponse<any>> {
  try {
    return await httpClient.post(
      API_ENDPOINTS.UPDATE_TOOL_VERSION_STATUS(toolId, version), 
      null, 
      { 
        params: { 
          publishStatus: publishStatus.toString() 
        } 
      }
    );
  } catch (error) {
 
    return {
      code: 500,
      message: "修改工具版本状态失败",
      data: null,
      timestamp: Date.now()
    };
  }
}

/**
 * 修改工具版本发布状态（带Toast提示）
 */
export const updateToolVersionStatusWithToast = withToast(
  updateToolVersionStatus,
  {
    successTitle: "状态修改成功",
    errorTitle: "状态修改失败",
    showSuccessToast: true
  }
);

// 获取工具最新版本
export async function getToolLatestVersion(toolId: string): Promise<ApiResponse<{ version: string }>> {
  try {
    return await httpClient.get(API_ENDPOINTS.GET_TOOL_LATEST_VERSION(toolId));
  } catch (error) {
 
    return {
      code: 500,
      message: "获取工具最新版本失败",
      data: { version: "0.0.0" }, // 返回一个默认值或错误标识
      timestamp: Date.now(),
    };
  }
}

// 上架工具到市场
export async function publishToolToMarket(
  params: PublishToolToMarketParams
): Promise<ApiResponse<any>> {
  try {
    // 与其他接口保持一致，直接返回 httpClient 的响应
    return await httpClient.post(API_ENDPOINTS.PUBLISH_TOOL_TO_MARKET, params);
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "上架工具失败",
      data: null,
      timestamp: Date.now()
    };
  }
}

export const publishToolToMarketWithToast = withToast(publishToolToMarket, {
  showSuccessToast: true,
  showErrorToast: true,
}); 