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

// ========== 新增接口类型定义 ==========

// 文件预处理请求（对应 ProcessFileRequest）
export interface ProcessFileRequest {
  fileId: string;
  datasetId: string;
  processType?: number; // 处理类型：1-初始化，2-向量化
}

// 文件处理进度响应（对应 FileProcessProgressDTO）
export interface FileProcessProgressDTO {
  fileId: string;
  filename: string;
  isInitialize: number; // 初始化状态
  isEmbedding: number; // 向量化状态
  currentPageNumber?: number; // 当前处理页数
  filePageSize?: number; // 总页数
  processProgress?: number; // 处理进度百分比
  statusDescription?: string; // 状态描述
}

// RAG搜索请求（对应 RagSearchRequest）
export interface RagSearchRequest {
  datasetIds: string[]; // 数据集ID列表
  question: string; // 搜索问题
  maxResults?: number; // 最大返回结果数量，默认15
}

// 文档单元响应（对应 DocumentUnitDTO）
export interface DocumentUnitDTO {
  id: string;
  fileId: string;
  page: number;
  content: string;
  isOcr: boolean; // 是否OCR处理
  isVector: boolean; // 是否向量化
  createdAt: string;
  updatedAt: string;
}

// 文件处理类型枚举
export enum ProcessType {
  INITIALIZE = 1, // 初始化
  EMBEDDING = 2, // 向量化
}

// 处理进度状态
export interface ProcessProgressStatus {
  fileId: string;
  filename: string;
  isInitialize: FileInitializeStatus;
  isEmbedding: FileEmbeddingStatus;
  processProgress: number;
  statusDescription: string;
}