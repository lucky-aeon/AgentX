import { httpClient } from "@/lib/http-client"
import { API_ENDPOINTS, API_CONFIG } from "@/lib/api-config"
import { toast } from "@/hooks/use-toast"
import { Tool, ToolVersion, ApiResponse, GetMarketToolsParams } from "@/types/tool"
import { withToast } from "./toast-utils"

// 获取工具市场列表
export async function getMarketTools(params?: GetMarketToolsParams): Promise<ApiResponse<Tool[]>> {
  try {
    return await httpClient.get(API_ENDPOINTS.MARKET_TOOLS, { params })
  } catch (error) {
    console.error("获取工具市场列表失败", error)
    return {
      code: 500,
      message: "获取工具市场列表失败",
      data: [],
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
export const getMarketToolVersionsWithToast = withToast(getMarketToolVersions)

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
export async function installTool(toolVersionId: string): Promise<ApiResponse<any>> {
  try {
    return await httpClient.post(API_ENDPOINTS.INSTALL_TOOL, { toolVersionId })
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
export async function getUserToolsWithToast(params?: any) {
  return withToast(getUserTools, {
    successTitle: "获取成功",
    errorTitle: "获取失败",
    showSuccessToast: true
  })(params);
}

/**
 * 获取用户已安装的工具和推荐工具列表
 */
async function getUserTools(params?: any): Promise<ApiResponse<any>> {
  try {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境模拟数据
      return mockUserToolsResponse();
    }
    
    const response = await fetch(`/api/tools/user`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error(`获取工具数据失败: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("获取工具失败", error);
    return {
      code: 500,
      message: "获取工具数据失败",
      data: [],
      timestamp: Date.now()
    }
  }
}

/**
 * 模拟用户工具数据响应
 */
function mockUserToolsResponse(): ApiResponse<any> {
  return {
    code: 200,
    message: "获取成功",
    data: {
      userTools: [
        // 示例数据，实际开发时应该替换
        {
          id: "tool-1",
          name: "示例工具1",
          subtitle: "这是一个示例工具",
          // 其他必要属性...
        }
      ],
      recommendedTools: [
        // 示例数据，实际开发时应该替换
        {
          id: "tool-rec-1",
          name: "推荐工具1",
          subtitle: "这是一个推荐工具",
          // 其他必要属性...
        }
      ]
    },
    timestamp: Date.now()
  };
}

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