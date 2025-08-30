import { httpClient } from './http-client'
import { withToast } from './toast-utils'

// 知识库基本信息接口
export interface KnowledgeBase {
  id: string
  userRagId?: string  // 用户RAG安装记录ID（用于调用已安装RAG相关接口）
  name: string
  description: string
  icon?: string
  fileCount: number
  createdAt: string
  updatedAt: string
}

// 文件详情接口
export interface FileDetail {
  id: string
  filename: string
  originalFilename: string
  ext: string
  size: number
  contentType: string
  dataSetId: string
  filePageSize: number
  isInitialize: boolean
  isEmbedding: boolean
  processProgress: number
  createdAt: string
  updatedAt: string
}

// 查询文件列表的参数接口
export interface QueryFilesParams {
  page?: number
  pageSize?: number
  keyword?: string
}

// 分页响应接口
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// API响应基本结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// API端点
const API_ENDPOINTS = {
  AVAILABLE_KNOWLEDGE_BASES: '/agents/knowledge-bases/available',
  KNOWLEDGE_BASE_DETAIL: (id: string) => `/agents/knowledge-bases/${id}`,
  KNOWLEDGE_BASES_BATCH: '/agents/knowledge-bases/batch',
  DATASET_FILES: (datasetId: string) => `/rag/datasets/${datasetId}/files`,
  DATASET_ALL_FILES: (datasetId: string) => `/rag/datasets/${datasetId}/files/all`,
}

/**
 * 获取用户可用的知识库列表（用于Agent配置）
 */
export async function getAvailableKnowledgeBases(): Promise<ApiResponse<KnowledgeBase[]>> {
  try {
 
    
    const response = await httpClient.get<ApiResponse<KnowledgeBase[]>>(
      API_ENDPOINTS.AVAILABLE_KNOWLEDGE_BASES
    )
    
 
    return response
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

/**
 * 获取知识库详情
 */
export async function getKnowledgeBaseDetail(knowledgeBaseId: string): Promise<ApiResponse<KnowledgeBase>> {
  try {
 
    
    const response = await httpClient.get<ApiResponse<KnowledgeBase>>(
      API_ENDPOINTS.KNOWLEDGE_BASE_DETAIL(knowledgeBaseId)
    )
    
 
    return response
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: {} as KnowledgeBase,
      timestamp: Date.now(),
    }
  }
}

/**
 * 批量获取知识库详情
 */
export async function getKnowledgeBasesBatch(knowledgeBaseIds: string[]): Promise<ApiResponse<KnowledgeBase[]>> {
  try {
    if (!knowledgeBaseIds || knowledgeBaseIds.length === 0) {
      return {
        code: 200,
        message: "成功",
        data: [],
        timestamp: Date.now(),
      }
    }
    
 
    
    const response = await httpClient.get<ApiResponse<KnowledgeBase[]>>(
      API_ENDPOINTS.KNOWLEDGE_BASES_BATCH,
      {
        params: {
          knowledgeBaseIds: knowledgeBaseIds.join(',')
        }
      }
    )
    
 
    return response
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

/**
 * 分页获取知识库文件列表
 */
export async function getKnowledgeBaseFiles(
  knowledgeBaseId: string, 
  params?: QueryFilesParams
): Promise<ApiResponse<PageResponse<FileDetail>>> {
  try {
 
    
    const response = await httpClient.get<ApiResponse<PageResponse<FileDetail>>>(
      API_ENDPOINTS.DATASET_FILES(knowledgeBaseId),
      { params }
    )
    
 
    return response
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
      timestamp: Date.now(),
    }
  }
}

/**
 * 获取知识库所有文件列表
 */
export async function getAllKnowledgeBaseFiles(
  knowledgeBaseId: string
): Promise<ApiResponse<FileDetail[]>> {
  try {
 
    
    const response = await httpClient.get<ApiResponse<FileDetail[]>>(
      API_ENDPOINTS.DATASET_ALL_FILES(knowledgeBaseId)
    )
    
 
    return response
  } catch (error) {
 
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 使用 withToast 包装器的API函数
export const getAvailableKnowledgeBasesWithToast = withToast(getAvailableKnowledgeBases, {
  showSuccessToast: false,
  errorTitle: "获取知识库列表失败"
})

export const getKnowledgeBaseDetailWithToast = withToast(getKnowledgeBaseDetail, {
  showSuccessToast: false,
  errorTitle: "获取知识库详情失败"
})

export const getKnowledgeBasesBatchWithToast = withToast(getKnowledgeBasesBatch, {
  showSuccessToast: false,
  errorTitle: "获取知识库详情失败"
})

export const getKnowledgeBaseFilesWithToast = withToast(getKnowledgeBaseFiles, {
  showSuccessToast: false,
  errorTitle: "获取知识库文件列表失败"
})

export const getAllKnowledgeBaseFilesWithToast = withToast(getAllKnowledgeBaseFiles, {
  showSuccessToast: false,
  errorTitle: "获取知识库文件失败"
})
