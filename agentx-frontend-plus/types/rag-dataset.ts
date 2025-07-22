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

// 文件处理状态枚举（对应后端 FileProcessingStatusEnum）
export enum FileProcessingStatusEnum {
  UPLOADED = "UPLOADED",                    // 已上传，待开始处理
  OCR_PROCESSING = "OCR_PROCESSING",        // OCR处理中
  OCR_COMPLETED = "OCR_COMPLETED",          // OCR处理完成，待向量化
  EMBEDDING_PROCESSING = "EMBEDDING_PROCESSING", // 向量化处理中
  COMPLETED = "COMPLETED",                  // 全部处理完成
  OCR_FAILED = "OCR_FAILED",               // OCR处理失败
  EMBEDDING_FAILED = "EMBEDDING_FAILED"    // 向量化处理失败
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
  // 保留旧字段以兼容
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
  PROCESSING = 2, // 添加处理中状态
}

export enum FileEmbeddingStatus {
  NOT_EMBEDDED = 0,
  EMBEDDED = 1,
  PROCESSING = 2, // 添加处理中状态
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
  // 新的统一状态字段
  processingStatusEnum: FileProcessingStatusEnum; // 处理状态枚举
  processingStatus: number; // 处理状态码
  processingStatusDescription: string; // 处理状态描述
  // 分项进度字段
  currentOcrPageNumber?: number; // 当前OCR处理页数
  currentEmbeddingPageNumber?: number; // 当前向量化处理页数
  filePageSize?: number; // 总页数
  ocrProcessProgress?: number; // OCR处理进度百分比
  embeddingProcessProgress?: number; // 向量化处理进度百分比
  statusDescription?: string; // 状态描述
  // 保留旧字段以兼容
  isInitialize: number; // 初始化状态
  isEmbedding: number; // 向量化状态
  initializeStatus?: string; // 初始化状态描述
  embeddingStatus?: string; // 向量化状态描述
  currentPageNumber?: number; // 当前处理页数
  processProgress?: number; // 处理进度百分比
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

// ========== 文件操作相关类型定义 ==========

// 文件详细信息（包含文件路径）
export interface FileDetailInfoDTO {
  id: string;
  url: string;
  size: number;
  filename: string;
  originalFilename: string;
  ext: string;
  contentType: string;
  dataSetId: string;
  filePageSize?: number;
  isInitialize: number;
  isEmbedding: number;
  userId: string;
  createdAt: string;
  updatedAt: string;
  filePath?: string; // 文件路径
}

// 查询文档单元请求
export interface QueryDocumentUnitsRequest {
  fileId: string;
  page?: number;
  pageSize?: number;
  keyword?: string;
}

// 更新文档单元请求
export interface UpdateDocumentUnitRequest {
  id: string;
  content: string;
}

// ========== RAG流式聊天相关类型定义 ==========

// RAG流式聊天请求
export interface RagStreamChatRequest {
  datasetIds: string[]; // 数据集ID列表
  question: string; // 用户问题
  stream?: boolean; // 是否流式返回，默认true
}

// SSE消息类型
export interface SSEMessage {
  type: 'thinking' | 'content' | 'error' | 'done';
  data?: any;
  content?: string;
  error?: string;
}

// RAG思考过程数据
export interface RagThinkingData {
  type: 'retrieval' | 'thinking' | 'answer';
  status: 'start' | 'progress' | 'end';
  message?: string;
  retrievedCount?: number;
  documents?: Array<{
    fileId: string;
    fileName: string;
    documentId: string;
    score: number;
  }>;
  content?: string; // 思考过程的内容
}

// ========== 文件详情查看相关类型定义 ==========

// 检索到的文件信息（从RAG_RETRIEVAL_END消息中解析）
export interface RetrievedFileInfo {
  fileId: string;
  fileName: string;
  documentId?: string;
  score?: number;
  filePath?: string;
  isInstalledRag?: boolean;
  userRagId?: string;
}

// 文件详情请求
export interface GetFileDetailRequest {
  fileId: string;
  documentId?: string;
}

// 文件详情响应
export interface FileDetailResponse {
  fileId: string;
  fileName: string;
  content: string;
  pageCount: number;
  fileSize: number;
  fileType: string;
  createdAt: string;
  updatedAt: string;
}

// 文件内容数据
export interface FileContentData {
  fileId: string;
  documentId?: string;
  fileName: string;
  content: string;
  pageCount: number;
  fileSize: number;
  fileType: string;
  createdAt: string;
  updatedAt: string;
}

// 聊天布局类型
export type ChatLayout = 'single' | 'split';

// 聊天界面状态
export interface ChatUIState {
  layout: ChatLayout;
  selectedFile: RetrievedFileInfo | null;
  showFileDetail: boolean;
  fileDetailData: FileContentData | null;
  fileDetailLoading: boolean;
  fileDetailError: string | null;
}