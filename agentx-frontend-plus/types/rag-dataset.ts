// RAG 数据集相关类型定义
// 对应后端 RagQaDatasetController 的 DTO 类型

// 数据集接口（对应 RagQaDatasetDTO）
export interface RagDataset {
  id: string;
  name: string;
  icon?: string;
  description?: string;
  userId: string;
  fileCount: number;
  createdAt: string;
  updatedAt: string;
}

// 文件详情接口（对应 FileDetailDTO）
export interface FileDetail {
  id: string;
  url: string;
  size: number;
  filename: string;
  originalFilename: string;
  ext: string;
  contentType: string;
  dataSetId: string;
  filePageSize?: number;
  isInitialize: number; // 初始化状态: 0-未初始化, 1-已初始化
  isEmbedding: number;  // 向量化状态: 0-未向量化, 1-已向量化
  userId: string;
  createdAt: string;
  updatedAt: string;
}

// 创建数据集请求（对应 CreateDatasetRequest）
export interface CreateDatasetRequest {
  name: string;
  icon?: string;
  description?: string;
}

// 更新数据集请求（对应 UpdateDatasetRequest）
export interface UpdateDatasetRequest {
  name: string;
  icon?: string;
  description?: string;
}

// 查询数据集请求（对应 QueryDatasetRequest）
export interface QueryDatasetRequest {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 查询数据集文件请求（对应 QueryDatasetFileRequest）
export interface QueryDatasetFileRequest {
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 上传文件请求（对应 UploadFileRequest）
export interface UploadFileRequest {
  datasetId: string;
  file: File;
}

// 分页响应类型（对应 MyBatis-Plus Page）
export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// API 响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

// 文件状态枚举
export enum FileInitializeStatus {
  NOT_INITIALIZED = 0,
  INITIALIZED = 1,
}

export enum FileEmbeddingStatus {
  NOT_EMBEDDED = 0,
  EMBEDDED = 1,
}

// 文件状态显示配置
export interface FileStatusConfig {
  initializeStatus: {
    text: string;
    variant: "default" | "secondary" | "destructive" | "outline";
    color: string;
  };
  embeddingStatus: {
    text: string;
    variant: "default" | "secondary" | "destructive" | "outline";
    color: string;
  };
}