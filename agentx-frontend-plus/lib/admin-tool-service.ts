import { httpClient } from "@/lib/http-client";
import { API_ENDPOINTS } from "@/lib/api-config";
import { ApiResponse } from "@/types/api";

// 工具状态枚举
export enum ToolStatus {
  WAITING_REVIEW = "WAITING_REVIEW",
  GITHUB_URL_VALIDATE = "GITHUB_URL_VALIDATE", 
  DEPLOYING = "DEPLOYING",
  FETCHING_TOOLS = "FETCHING_TOOLS",
  MANUAL_REVIEW = "MANUAL_REVIEW",
  APPROVED = "APPROVED",
  FAILED = "FAILED"
}

// 工具类型枚举
export enum ToolType {
  MCP = "MCP"
}

// 上传类型枚举
export enum UploadType {
  GITHUB = "GITHUB",
  ZIP = "ZIP"
}

// 工具信息接口（包含用户信息）
export interface Tool {
  id: string;
  name: string;
  icon?: string;
  subtitle?: string;
  description: string;
  userId: string;
  userNickname?: string;
  userEmail?: string;
  userAvatarUrl?: string;
  labels: string[];
  toolType: ToolType;
  uploadType: UploadType;
  uploadUrl: string;
  installCommand?: any;
  toolList?: any[];
  status: ToolStatus;
  isOffice: boolean;
  installCount?: number;
  currentVersion?: string;
  createdAt: string;
  updatedAt: string;
  rejectReason?: string;
  failedStepStatus?: ToolStatus;
  mcpServerName?: string;
  isGlobal?: boolean; // 是否为全局工具
}

// 工具查询参数接口
export interface GetToolsParams {
  keyword?: string;    // 搜索关键词（名称、描述）
  status?: ToolStatus; // 工具状态筛选
  isOffice?: boolean;  // 是否为官方工具
  page?: number;       // 页码
  pageSize?: number;   // 每页大小
}

// 工具统计信息接口
export interface ToolStatistics {
  totalTools: number;        // 总工具数量
  pendingReviewTools: number; // 待审核工具数量
  manualReviewTools: number;  // 人工审核工具数量
  approvedTools: number;      // 已通过工具数量
  failedTools: number;        // 审核失败工具数量
  officialTools: number;      // 官方工具数量
}

// 分页响应接口
export interface PageResponse<T> {
  records: T[];      // 数据记录
  total: number;     // 总记录数
  size: number;      // 每页大小
  current: number;   // 当前页码
  pages: number;     // 总页数
}

// 创建工具请求接口
export interface CreateToolRequest {
  name: string;                  // 工具名称
  icon?: string;                // 工具图标
  subtitle: string;             // 副标题
  description: string;          // 工具描述
  labels: string[];             // 标签
  uploadUrl: string;            // 上传地址
  installCommand: Record<string, any>; // 安装命令
}

/**
 * 管理员工具服务
 */
export class AdminToolService {
  
  /**
   * 分页获取工具列表
   * @param params 查询参数
   * @returns 工具分页数据
   */
  static async getTools(params?: GetToolsParams): Promise<ApiResponse<PageResponse<Tool>>> {
    try {
      return await httpClient.get("/admin/tools", { params });
    } catch (error) {
      return {
        code: 500,
        message: "获取工具列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  /**
   * 获取工具统计信息
   * @returns 工具统计数据
   */
  static async getToolStatistics(): Promise<ApiResponse<ToolStatistics>> {
    try {
      return await httpClient.get("/admin/tools/statistics");
    } catch (error) {
      return {
        code: 500,
        message: "获取工具统计失败",
        data: { 
          totalTools: 0, 
          pendingReviewTools: 0, 
          manualReviewTools: 0, 
          approvedTools: 0, 
          failedTools: 0, 
          officialTools: 0 
        },
        timestamp: Date.now()
      };
    }
  }

  /**
   * 创建官方工具
   * @param toolData 工具数据
   * @returns 创建结果
   */
  static async createOfficialTool(toolData: CreateToolRequest): Promise<ApiResponse<string>> {
    try {
      return await httpClient.post("/admin/tools/official", toolData);
    } catch (error) {
      return {
        code: 500,
        message: "创建官方工具失败",
        data: "",
        timestamp: Date.now()
      };
    }
  }

  /**
   * 上传官方工具（兼容前台上传工具的接口）
   * @param data 工具数据（与前台uploadTool相同的参数结构）
   * @returns 创建结果
   */
  static async uploadOfficialTool(data: any): Promise<ApiResponse<any>> {
    try {
      const toolData: CreateToolRequest = {
        name: data.name,
        subtitle: data.subtitle,
        description: data.description,
        uploadUrl: data.uploadUrl,
        labels: data.labels,
        installCommand: data.installCommand,
        icon: data.icon
      };
      return await httpClient.post("/admin/tools/official", toolData);
    } catch (error) {
      return {
        code: 500,
        message: "创建官方工具失败",
        data: null,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 更新工具状态
   * @param toolId 工具ID
   * @param status 目标状态
   * @param reason 拒绝原因（可选）
   * @returns 操作结果
   */
  static async updateToolStatus(toolId: string, status: ToolStatus, reason?: string): Promise<ApiResponse<void>> {
    try {
      const url = `/admin/tools/${toolId}/status`;
      const params: any = { status };
      if (reason) {
        params.reason = reason;
      }
      return await httpClient.post(url, null, { params });
    } catch (error) {
      return {
        code: 500,
        message: "更新工具状态失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  /**
   * 更新工具全局状态
   * @param toolId 工具ID
   * @param isGlobal 是否为全局工具
   * @returns 操作结果
   */
  static async updateToolGlobalStatus(toolId: string, isGlobal: boolean): Promise<ApiResponse<void>> {
    try {
      const url = `/admin/tools/${toolId}/global-status`;
      return await httpClient.put(url, { isGlobal });
    } catch (error) {
      return {
        code: 500,
        message: "更新工具全局状态失败",
        data: undefined,
        timestamp: Date.now()
      };
    }
  }
}

/**
 * 获取工具状态的中文描述
 */
export function getToolStatusText(status: ToolStatus): string {
  switch (status) {
    case ToolStatus.WAITING_REVIEW:
      return "等待审核";
    case ToolStatus.GITHUB_URL_VALIDATE:
      return "URL验证中";
    case ToolStatus.DEPLOYING:
      return "部署中";
    case ToolStatus.FETCHING_TOOLS:
      return "获取工具中";
    case ToolStatus.MANUAL_REVIEW:
      return "人工审核";
    case ToolStatus.APPROVED:
      return "已通过";
    case ToolStatus.FAILED:
      return "审核失败";
    default:
      return "未知状态";
  }
}

/**
 * 获取工具状态的颜色主题
 */
export function getToolStatusColor(status: ToolStatus): string {
  switch (status) {
    case ToolStatus.WAITING_REVIEW:
    case ToolStatus.GITHUB_URL_VALIDATE:
    case ToolStatus.DEPLOYING:
    case ToolStatus.FETCHING_TOOLS:
      return "bg-blue-100 text-blue-800";
    case ToolStatus.MANUAL_REVIEW:
      return "bg-yellow-100 text-yellow-800";
    case ToolStatus.APPROVED:
      return "bg-green-100 text-green-800";
    case ToolStatus.FAILED:
      return "bg-red-100 text-red-800";
    default:
      return "bg-gray-100 text-gray-800";
  }
}