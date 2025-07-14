import { API_ENDPOINTS } from "@/lib/api-config"
import { httpClient } from "@/lib/http-client"
import { withToast } from "@/lib/toast-utils"
import type {
  ApiResponse,
  FileDetailInfoDTO,
  QueryDocumentUnitsRequest,
  PageResponse,
  DocumentUnitDTO,
  UpdateDocumentUnitRequest,
} from "@/types/rag-dataset"

// 文件操作相关API

// 获取文件详细信息（包含文件路径）
export async function getFileInfo(fileId: string): Promise<ApiResponse<FileDetailInfoDTO>> {
  try {
    console.log(`Fetching file info: ${fileId}`)
    
    const response = await httpClient.get<ApiResponse<FileDetailInfoDTO>>(
      API_ENDPOINTS.RAG_FILE_INFO(fileId)
    )
    
    return response
  } catch (error) {
    console.error("获取文件信息错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as FileDetailInfoDTO,
      timestamp: Date.now(),
    }
  }
}

// 分页查询文件的语料
export async function getDocumentUnits(
  request: QueryDocumentUnitsRequest
): Promise<ApiResponse<PageResponse<DocumentUnitDTO>>> {
  try {
    console.log("Fetching document units:", request)
    
    const response = await httpClient.post<ApiResponse<PageResponse<DocumentUnitDTO>>>(
      API_ENDPOINTS.RAG_DOCUMENT_UNITS,
      request
    )
    
    return response
  } catch (error) {
    console.error("获取文档单元列表错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: {
        records: [],
        total: 0,
        size: 15,
        current: 1,
        pages: 0
      },
      timestamp: Date.now(),
    }
  }
}

// 更新语料内容
export async function updateDocumentUnit(
  request: UpdateDocumentUnitRequest
): Promise<ApiResponse<DocumentUnitDTO>> {
  try {
    console.log("Updating document unit:", request)
    
    const response = await httpClient.put<ApiResponse<DocumentUnitDTO>>(
      API_ENDPOINTS.RAG_UPDATE_DOCUMENT_UNIT,
      request
    )
    
    return response
  } catch (error) {
    console.error("更新文档单元错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as DocumentUnitDTO,
      timestamp: Date.now(),
    }
  }
}

// 删除语料
export async function deleteDocumentUnit(documentUnitId: string): Promise<ApiResponse<void>> {
  try {
    console.log(`Deleting document unit: ${documentUnitId}`)
    
    const response = await httpClient.delete<ApiResponse<void>>(
      API_ENDPOINTS.RAG_DELETE_DOCUMENT_UNIT(documentUnitId)
    )
    
    return response
  } catch (error) {
    console.error("删除文档单元错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: undefined as unknown as void,
      timestamp: Date.now(),
    }
  }
}

// 使用 withToast 包装器的API函数
export const getFileInfoWithToast = withToast(getFileInfo, {
  showSuccessToast: false,
  errorTitle: "获取文件信息失败"
})

export const getDocumentUnitsWithToast = withToast(getDocumentUnits, {
  showSuccessToast: false,
  errorTitle: "获取文档单元列表失败"
})

export const updateDocumentUnitWithToast = withToast(updateDocumentUnit, {
  successTitle: "更新语料成功",
  errorTitle: "更新语料失败"
})

export const deleteDocumentUnitWithToast = withToast(deleteDocumentUnit, {
  successTitle: "删除语料成功",
  errorTitle: "删除语料失败"
})