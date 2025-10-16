import { API_ENDPOINTS } from "@/lib/api-config";
import { httpClient } from "@/lib/http-client";
import { withToast } from "@/lib/toast-utils";
import type { ApiResponse, CreateMemoryRequest, MemoryItem, PageResponse, QueryMemoryRequest } from "@/types/memory";

// 获取记忆分页列表
export async function getMemories(params?: QueryMemoryRequest): Promise<ApiResponse<PageResponse<MemoryItem>>> {
  try {
    const response = await httpClient.get<ApiResponse<PageResponse<MemoryItem>>>(
      API_ENDPOINTS.MEMORY_ITEMS,
      { params }
    );
    return response;
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now(),
    };
  }
}

// 新增记忆
export async function createMemory(request: CreateMemoryRequest): Promise<ApiResponse<null>> {
  try {
    const response = await httpClient.post<ApiResponse<null>>(
      API_ENDPOINTS.CREATE_MEMORY,
      request
    );
    return response;
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    };
  }
}

// 删除（归档）记忆
export async function deleteMemory(itemId: string): Promise<ApiResponse<null>> {
  try {
    const response = await httpClient.delete<ApiResponse<null>>(
      API_ENDPOINTS.DELETE_MEMORY(itemId)
    );
    return response;
  } catch (error) {
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    };
  }
}

// 带Toast的封装
export const getMemoriesWithToast = withToast(getMemories, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取记忆失败",
});

export const createMemoryWithToast = withToast(createMemory, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "创建记忆成功",
  errorTitle: "创建记忆失败",
});

export const deleteMemoryWithToast = withToast(deleteMemory, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "删除记忆成功",
  errorTitle: "删除记忆失败",
});

