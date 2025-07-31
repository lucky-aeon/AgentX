import { API_ENDPOINTS } from "@/lib/api-config"
import { httpClient } from "@/lib/http-client"
import { withToast } from "@/lib/toast-utils"
import type {
  RagDataset,
  FileDetail,
  CreateDatasetRequest,
  UpdateDatasetRequest,
  QueryDatasetRequest,
  QueryDatasetFileRequest,
  UploadFileRequest,
  PageResponse,
  ApiResponse,
  ProcessFileRequest,
  FileProcessProgressDTO,
  RagSearchRequest,
  DocumentUnitDTO,
} from "@/types/rag-dataset"

// 数据集管理相关API

// 分页查询数据集
export async function getDatasets(params?: QueryDatasetRequest): Promise<ApiResponse<PageResponse<RagDataset>>> {
  try {
    console.log("Fetching datasets with params:", params)
    
    const response = await httpClient.get<ApiResponse<PageResponse<RagDataset>>>(
      API_ENDPOINTS.RAG_DATASETS,
      { params }
    )
    
    return response
  } catch (error) {
    console.error("获取数据集列表错误:", error)
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

// 获取所有数据集
export async function getAllDatasets(): Promise<ApiResponse<RagDataset[]>> {
  try {
    console.log("Fetching all datasets")
    
    const response = await httpClient.get<ApiResponse<RagDataset[]>>(
      API_ENDPOINTS.RAG_ALL_DATASETS
    )
    
    return response
  } catch (error) {
    console.error("获取所有数据集错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 获取数据集详情
export async function getDatasetDetail(datasetId: string): Promise<ApiResponse<RagDataset>> {
  try {
    console.log(`Fetching dataset detail: ${datasetId}`)
    
    const response = await httpClient.get<ApiResponse<RagDataset>>(
      API_ENDPOINTS.RAG_DATASET_DETAIL(datasetId)
    )
    
    return response
  } catch (error) {
    console.error("获取数据集详情错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as RagDataset,
      timestamp: Date.now(),
    }
  }
}

// 创建数据集
export async function createDataset(request: CreateDatasetRequest): Promise<ApiResponse<RagDataset>> {
  try {
    console.log("Creating dataset:", request)
    
    const response = await httpClient.post<ApiResponse<RagDataset>>(
      API_ENDPOINTS.RAG_DATASETS,
      request
    )
    
    return response
  } catch (error) {
    console.error("创建数据集错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as RagDataset,
      timestamp: Date.now(),
    }
  }
}

// 更新数据集
export async function updateDataset(
  datasetId: string,
  request: UpdateDatasetRequest
): Promise<ApiResponse<RagDataset>> {
  try {
    console.log(`Updating dataset: ${datasetId}`, request)
    
    const response = await httpClient.put<ApiResponse<RagDataset>>(
      API_ENDPOINTS.RAG_DATASET_DETAIL(datasetId),
      request
    )
    
    return response
  } catch (error) {
    console.error("更新数据集错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as RagDataset,
      timestamp: Date.now(),
    }
  }
}

// 删除数据集
export async function deleteDataset(datasetId: string): Promise<ApiResponse<null>> {
  try {
    console.log(`Deleting dataset: ${datasetId}`)
    
    const response = await httpClient.delete<ApiResponse<null>>(
      API_ENDPOINTS.RAG_DATASET_DETAIL(datasetId)
    )
    
    return response
  } catch (error) {
    console.error("删除数据集错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// 文件管理相关API

// 上传文件到数据集
export async function uploadFile(datasetId: string, file: File): Promise<ApiResponse<FileDetail>> {
  try {
    console.log(`Uploading file to dataset: ${datasetId}`, file.name)
    
    const formData = new FormData()
    formData.append('datasetId', datasetId)
    formData.append('file', file)
    
    const response = await httpClient.post<ApiResponse<FileDetail>>(
      API_ENDPOINTS.RAG_UPLOAD_FILE,
      formData
    )
    
    return response
  } catch (error) {
    console.error("上传文件错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as FileDetail,
      timestamp: Date.now(),
    }
  }
}

// 分页查询数据集文件
export async function getDatasetFiles(
  datasetId: string,
  params?: QueryDatasetFileRequest
): Promise<ApiResponse<PageResponse<FileDetail>>> {
  try {
    console.log(`Fetching files for dataset: ${datasetId}`, params)
    
    const response = await httpClient.get<ApiResponse<PageResponse<FileDetail>>>(
      API_ENDPOINTS.RAG_DATASET_FILES(datasetId),
      { params }
    )
    
    return response
  } catch (error) {
    console.error("获取数据集文件错误:", error)
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

// 获取数据集所有文件
export async function getAllDatasetFiles(datasetId: string): Promise<ApiResponse<FileDetail[]>> {
  try {
    console.log(`Fetching all files for dataset: ${datasetId}`)
    
    const response = await httpClient.get<ApiResponse<FileDetail[]>>(
      API_ENDPOINTS.RAG_ALL_DATASET_FILES(datasetId)
    )
    
    return response
  } catch (error) {
    console.error("获取数据集所有文件错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 删除数据集文件
export async function deleteFile(datasetId: string, fileId: string): Promise<ApiResponse<null>> {
  try {
    console.log(`Deleting file: ${fileId} from dataset: ${datasetId}`)
    
    const response = await httpClient.delete<ApiResponse<null>>(
      API_ENDPOINTS.RAG_DATASET_FILE_DELETE(datasetId, fileId)
    )
    
    return response
  } catch (error) {
    console.error("删除文件错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

// ========== 新增API方法 ==========

// 启动文件预处理
export async function processFile(request: ProcessFileRequest): Promise<ApiResponse<void>> {
  try {
    console.log("Processing file:", request)
    
    const response = await httpClient.post<ApiResponse<void>>(
      API_ENDPOINTS.RAG_PROCESS_FILE,
      request
    )
    
    return response
  } catch (error) {
    console.error("启动文件预处理错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: undefined as unknown as void,
      timestamp: Date.now(),
    }
  }
}

// 获取文件处理进度
export async function getFileProgress(fileId: string): Promise<ApiResponse<FileProcessProgressDTO>> {
  try {
    console.log(`Getting file progress: ${fileId}`)
    
    const response = await httpClient.get<ApiResponse<FileProcessProgressDTO>>(
      API_ENDPOINTS.RAG_FILE_PROGRESS(fileId)
    )
    
    return response
  } catch (error) {
    console.error("获取文件处理进度错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as FileProcessProgressDTO,
      timestamp: Date.now(),
    }
  }
}

// 获取数据集文件处理进度列表
export async function getDatasetFilesProgress(datasetId: string): Promise<ApiResponse<FileProcessProgressDTO[]>> {
  try {
    console.log(`Getting dataset files progress: ${datasetId}`)
    
    const response = await httpClient.get<ApiResponse<FileProcessProgressDTO[]>>(
      API_ENDPOINTS.RAG_DATASET_FILES_PROGRESS(datasetId)
    )
    
    return response
  } catch (error) {
    console.error("获取数据集文件处理进度错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// RAG搜索文档
export async function ragSearch(request: RagSearchRequest): Promise<ApiResponse<DocumentUnitDTO[]>> {
  try {
    console.log("RAG searching documents:", request)
    
    const response = await httpClient.post<ApiResponse<DocumentUnitDTO[]>>(
      API_ENDPOINTS.RAG_SEARCH,
      request
    )
    
    return response
  } catch (error) {
    console.error("RAG搜索文档错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 基于已安装知识库的RAG搜索
export async function ragSearchByUserRag(userRagId: string, request: RagSearchRequest): Promise<ApiResponse<DocumentUnitDTO[]>> {
  try {
    console.log("RAG searching by user rag:", userRagId, request)
    
    const response = await httpClient.post<ApiResponse<DocumentUnitDTO[]>>(
      API_ENDPOINTS.RAG_SEARCH_BY_USER_RAG(userRagId),
      request
    )
    
    return response
  } catch (error) {
    console.error("基于已安装知识库的RAG搜索错误:", error)
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [],
      timestamp: Date.now(),
    }
  }
}

// 使用 withToast 包装器的API函数
export const getDatasetsWithToast = withToast(getDatasets, {
  showSuccessToast: false,
  errorTitle: "获取数据集列表失败"
})

export const getAllDatasetsWithToast = withToast(getAllDatasets, {
  showSuccessToast: false,
  errorTitle: "获取所有数据集失败"
})

export const getDatasetDetailWithToast = withToast(getDatasetDetail, {
  showSuccessToast: false,
  errorTitle: "获取数据集详情失败"
})

export const createDatasetWithToast = withToast(createDataset, {
  successTitle: "创建数据集成功",
  errorTitle: "创建数据集失败"
})

export const updateDatasetWithToast = withToast(updateDataset, {
  successTitle: "更新数据集成功",
  errorTitle: "更新数据集失败"
})

export const deleteDatasetWithToast = withToast(deleteDataset, {
  successTitle: "删除数据集成功",
  errorTitle: "删除数据集失败"
})

export const uploadFileWithToast = withToast(uploadFile, {
  successTitle: "上传文件成功",
  errorTitle: "上传文件失败"
})

export const getDatasetFilesWithToast = withToast(getDatasetFiles, {
  showSuccessToast: false,
  errorTitle: "获取文件列表失败"
})

export const getAllDatasetFilesWithToast = withToast(getAllDatasetFiles, {
  showSuccessToast: false,
  errorTitle: "获取所有文件失败"
})

export const deleteFileWithToast = withToast(deleteFile, {
  successTitle: "删除文件成功",
  errorTitle: "删除文件失败"
})

// ========== 新增API方法的withToast包装器 ==========

export const processFileWithToast = withToast(processFile, {
  successTitle: "启动文件预处理成功",
  errorTitle: "启动文件预处理失败"
})

export const getFileProgressWithToast = withToast(getFileProgress, {
  showSuccessToast: false,
  errorTitle: "获取文件处理进度失败"
})

export const getDatasetFilesProgressWithToast = withToast(getDatasetFilesProgress, {
  showSuccessToast: false,
  errorTitle: "获取数据集文件处理进度失败"
})

export const ragSearchWithToast = withToast(ragSearch, {
  showSuccessToast: false,
  errorTitle: "RAG搜索失败"
})

export const ragSearchByUserRagWithToast = withToast(ragSearchByUserRag, {
  showSuccessToast: false,
  errorTitle: "基于已安装知识库的RAG搜索失败"
})