import { httpClient } from "@/lib/http-client";
import { ApiResponse } from "@/types/api";

// RAG统计数据
export interface RagStatisticsDTO {
  totalRags: number;
  pendingReview: number;
  approved: number;
  rejected: number;
  removed: number;
  totalInstalls: number;
}

// RAG版本DTO
export interface RagVersionDTO {
  id: string;
  name: string;
  icon?: string;
  description?: string;
  userId: string;
  userNickname?: string;
  version: string;
  changeLog?: string;
  labels: string[];
  originalRagId: string;
  originalRagName?: string;
  fileCount: number;
  totalSize: number;
  documentCount: number;
  publishStatus: number;
  publishStatusDesc: string;
  rejectReason?: string;
  reviewTime?: string;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  installCount: number;
  isInstalled?: boolean;
}

// RAG内容预览
export interface RagContentPreviewDTO {
  id: string;
  name: string;
  description?: string;
  version: string;
  files: RagVersionFileDTO[];
  sampleDocuments: RagVersionDocumentDTO[];
  totalDocuments: number;
  totalSize: number;
}

// RAG版本文件DTO
export interface RagVersionFileDTO {
  id: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  processStatus: number;
  embeddingStatus: number;
}

// RAG版本文档DTO
export interface RagVersionDocumentDTO {
  id: string;
  content: string;
  page: number;
  fileName: string;
}

// 分页响应
export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// 查询参数
export interface GetRagVersionsParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  status?: number;
}

// 批量审核请求
export interface BatchReviewRequest {
  versionIds: string[];
  status: number;
  rejectReason?: string;
}

// 审核请求
export interface ReviewRagVersionRequest {
  status: number;
  rejectReason?: string;
}

// 发布状态枚举
export enum RagPublishStatus {
  REVIEWING = 1,
  PUBLISHED = 2,
  REJECTED = 3,
  REMOVED = 4
}

// 获取状态文本
export function getPublishStatusText(status: number): string {
  switch (status) {
    case RagPublishStatus.REVIEWING:
      return "审核中";
    case RagPublishStatus.PUBLISHED:
      return "已发布";
    case RagPublishStatus.REJECTED:
      return "已拒绝";
    case RagPublishStatus.REMOVED:
      return "已下架";
    default:
      return "未知";
  }
}

// 获取状态颜色
export function getPublishStatusColor(status: number): string {
  switch (status) {
    case RagPublishStatus.REVIEWING:
      return "bg-orange-100 text-orange-800";
    case RagPublishStatus.PUBLISHED:
      return "bg-green-100 text-green-800";
    case RagPublishStatus.REJECTED:
      return "bg-red-100 text-red-800";
    case RagPublishStatus.REMOVED:
      return "bg-gray-100 text-gray-800";
    default:
      return "bg-gray-100 text-gray-800";
  }
}

// 格式化文件大小
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
}

// 管理员RAG服务类
export class AdminRagService {
  
  // 获取RAG统计数据
  static async getRagStatistics(): Promise<ApiResponse<RagStatisticsDTO>> {
    try {
      return await httpClient.get<ApiResponse<RagStatisticsDTO>>("/admin/rags/statistics");
    } catch (error) {
      return {
        code: 500,
        message: "获取统计数据失败",
        data: {
          totalRags: 0,
          pendingReview: 0,
          approved: 0,
          rejected: 0,
          removed: 0,
          totalInstalls: 0
        },
        timestamp: Date.now()
      };
    }
  }
  
  // 获取所有RAG版本列表
  static async getAllRagVersions(params?: GetRagVersionsParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> {
    try {
      return await httpClient.get<ApiResponse<PageResponse<RagVersionDTO>>>(
        "/admin/rags/versions",
        { params }
      );
    } catch (error) {
      return {
        code: 500,
        message: "获取RAG版本列表失败",
        data: {
          records: [],
          total: 0,
          size: 15,
          current: 1,
          pages: 0
        },
        timestamp: Date.now()
      };
    }
  }
  
  // 获取待审核RAG版本列表
  static async getPendingReviewVersions(params?: GetRagVersionsParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> {
    try {
      return await httpClient.get<ApiResponse<PageResponse<RagVersionDTO>>>(
        "/admin/rags/pending",
        { params }
      );
    } catch (error) {
      return {
        code: 500,
        message: "获取待审核RAG版本列表失败",
        data: {
          records: [],
          total: 0,
          size: 15,
          current: 1,
          pages: 0
        },
        timestamp: Date.now()
      };
    }
  }
  
  // 获取RAG版本详情
  static async getRagVersionDetail(versionId: string): Promise<ApiResponse<RagVersionDTO>> {
    try {
      return await httpClient.get<ApiResponse<RagVersionDTO>>(`/admin/rags/${versionId}`);
    } catch (error) {
      throw new Error("获取RAG版本详情失败");
    }
  }
  
  // 获取RAG内容预览
  static async getRagContentPreview(versionId: string): Promise<ApiResponse<RagContentPreviewDTO>> {
    try {
      return await httpClient.get<ApiResponse<RagContentPreviewDTO>>(`/admin/rags/${versionId}/preview`);
    } catch (error) {
      throw new Error("获取RAG内容预览失败");
    }
  }
  
  // 审核RAG版本
  static async reviewRagVersion(versionId: string, request: ReviewRagVersionRequest): Promise<ApiResponse<RagVersionDTO>> {
    try {
      return await httpClient.post<ApiResponse<RagVersionDTO>>(`/admin/rags/${versionId}`, request);
    } catch (error) {
      throw new Error("审核RAG版本失败");
    }
  }
  
  // 批量审核RAG版本
  static async batchReviewRagVersions(request: BatchReviewRequest): Promise<ApiResponse<string>> {
    try {
      return await httpClient.post<ApiResponse<string>>("/admin/rags/batch-review", request);
    } catch (error) {
      throw new Error("批量审核RAG版本失败");
    }
  }
  
  // 下架RAG版本
  static async removeRagVersion(versionId: string): Promise<ApiResponse<RagVersionDTO>> {
    try {
      return await httpClient.post<ApiResponse<RagVersionDTO>>(`/admin/rags/${versionId}/remove`);
    } catch (error) {
      throw new Error("下架RAG版本失败");
    }
  }
}

// 带Toast提示的服务方法
import { toast } from "@/hooks/use-toast";

export const getRagStatisticsWithToast = async (): Promise<ApiResponse<RagStatisticsDTO>> => {
  try {
    const response = await AdminRagService.getRagStatistics();
    return response;
  } catch (error) {
    toast({
      title: "获取统计数据失败",
      description: error instanceof Error ? error.message : "未知错误",
      variant: "destructive",
    });
    return {
      code: 500,
      message: "获取统计数据失败",
      data: {
        totalRags: 0,
        pendingReview: 0,
        approved: 0,
        rejected: 0,
        removed: 0,
        totalInstalls: 0
      },
      timestamp: Date.now()
    };
  }
};

export const getAllRagVersionsWithToast = async (params?: GetRagVersionsParams): Promise<ApiResponse<PageResponse<RagVersionDTO>>> => {
  try {
    const response = await AdminRagService.getAllRagVersions(params);
    return response;
  } catch (error) {
    toast({
      title: "获取RAG版本列表失败",
      description: error instanceof Error ? error.message : "未知错误",
      variant: "destructive",
    });
    return {
      code: 500,
      message: "获取RAG版本列表失败",
      data: {
        records: [],
        total: 0,
        size: 15,
        current: 1,
        pages: 0
      },
      timestamp: Date.now()
    };
  }
};

export const reviewRagVersionWithToast = async (versionId: string, request: ReviewRagVersionRequest): Promise<ApiResponse<RagVersionDTO>> => {
  try {
    const response = await AdminRagService.reviewRagVersion(versionId, request);
    if (response.code === 200) {
      toast({
        title: "审核成功",
        description: request.status === RagPublishStatus.PUBLISHED ? "RAG版本已通过审核" : "RAG版本已拒绝",
      });
    }
    return response;
  } catch (error) {
    toast({
      title: "审核失败",
      description: error instanceof Error ? error.message : "未知错误",
      variant: "destructive",
    });
    throw error;
  }
};

export const batchReviewRagVersionsWithToast = async (request: BatchReviewRequest): Promise<ApiResponse<string>> => {
  try {
    const response = await AdminRagService.batchReviewRagVersions(request);
    if (response.code === 200) {
      toast({
        title: "批量审核成功",
        description: response.data,
      });
    }
    return response;
  } catch (error) {
    toast({
      title: "批量审核失败",
      description: error instanceof Error ? error.message : "未知错误",
      variant: "destructive",
    });
    throw error;
  }
};

export const removeRagVersionWithToast = async (versionId: string): Promise<ApiResponse<RagVersionDTO>> => {
  try {
    const response = await AdminRagService.removeRagVersion(versionId);
    if (response.code === 200) {
      toast({
        title: "下架成功",
        description: "RAG版本已下架",
      });
    }
    return response;
  } catch (error) {
    toast({
      title: "下架失败",
      description: error instanceof Error ? error.message : "未知错误",
      variant: "destructive",
    });
    throw error;
  }
};