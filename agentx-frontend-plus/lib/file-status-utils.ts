import { FileProcessingStatusEnum, FileDetail, FileProcessProgressDTO } from "@/types/rag-dataset"

// 文件状态显示配置
export interface FileStatusDisplayConfig {
  text: string;
  variant: "default" | "secondary" | "destructive" | "outline";
  color: string;
  iconType: "check" | "clock" | "alert" | "loading";
}

// 根据新的处理状态枚举获取状态显示配置
export function getFileStatusConfig(
  file: FileDetail,
  progressInfo?: FileProcessProgressDTO
): {
  status: FileStatusDisplayConfig;
  canStartOcr: boolean;
  canStartEmbedding: boolean;
  progress: number;
} {
  // 优先使用进度信息中的状态
  const statusEnum = progressInfo?.processingStatusEnum;
  const statusCode = progressInfo?.processingStatus;
  
  let statusConfig: FileStatusDisplayConfig;
  let canStartOcr = false;
  let canStartEmbedding = false;
  let progress = progressInfo?.processProgress || 0;

  if (statusEnum) {
    // 使用新的统一状态枚举
    switch (statusEnum) {
      case FileProcessingStatusEnum.UPLOADED:
        statusConfig = {
          text: "已上传",
          variant: "outline",
          color: "text-yellow-600 border-yellow-300",
          iconType: "clock"
        };
        canStartOcr = true;
        break;

      case FileProcessingStatusEnum.OCR_PROCESSING:
        statusConfig = {
          text: "OCR处理中",
          variant: "outline",
          color: "text-blue-600 border-blue-300",
          iconType: "loading"
        };
        progress = progressInfo?.ocrProcessProgress || 0;
        break;

      case FileProcessingStatusEnum.OCR_COMPLETED:
        statusConfig = {
          text: "OCR处理完成",
          variant: "secondary",
          color: "text-green-600 bg-green-50 border-green-300",
          iconType: "check"
        };
        // 只有 OCR 完成时才能开始向量化（状态码为 2）
        canStartEmbedding = statusCode === 2;
        break;

      case FileProcessingStatusEnum.EMBEDDING_PROCESSING:
        statusConfig = {
          text: "向量化处理中",
          variant: "outline",
          color: "text-blue-600 border-blue-300",
          iconType: "loading"
        };
        progress = progressInfo?.embeddingProcessProgress || 0;
        break;

      case FileProcessingStatusEnum.COMPLETED:
        statusConfig = {
          text: "处理完成",
          variant: "default",
          color: "text-green-600 bg-green-50 border-green-300",
          iconType: "check"
        };
        progress = 100;
        break;

      case FileProcessingStatusEnum.OCR_FAILED:
        statusConfig = {
          text: "OCR处理失败",
          variant: "destructive",
          color: "text-red-600 border-red-300",
          iconType: "alert"
        };
        canStartOcr = true; // 失败后可以重试
        break;

      case FileProcessingStatusEnum.EMBEDDING_FAILED:
        statusConfig = {
          text: "向量化处理失败",
          variant: "destructive",
          color: "text-red-600 border-red-300",
          iconType: "alert"
        };
        // 只有 OCR 完成时才能重试向量化
        canStartEmbedding = statusCode === 2;
        break;

      default:
        statusConfig = {
          text: "未知状态",
          variant: "destructive",
          color: "text-red-600",
          iconType: "alert"
        };
    }
  } else {
    // 兼容旧的状态字段
    if (file.isInitialize === 0) {
      statusConfig = {
        text: "待初始化",
        variant: "outline",
        color: "text-yellow-600 border-yellow-300",
        iconType: "clock"
      };
      canStartOcr = true;
    } else if (file.isInitialize === 1) {
      statusConfig = {
        text: "已初始化",
        variant: "secondary",
        color: "text-green-600 bg-green-50 border-green-300",
        iconType: "check"
      };
      canStartEmbedding = file.isEmbedding === 0;
    } else {
      statusConfig = {
        text: "处理中",
        variant: "outline",
        color: "text-blue-600 border-blue-300",
        iconType: "loading"
      };
    }
  }

  return {
    status: statusConfig,
    canStartOcr,
    canStartEmbedding,
    progress
  };
}

// 获取状态描述文本
export function getStatusDescription(
  statusEnum?: FileProcessingStatusEnum,
  statusDescription?: string
): string {
  if (statusDescription) {
    return statusDescription;
  }

  if (!statusEnum) {
    return "未知状态";
  }

  const statusMap = {
    [FileProcessingStatusEnum.UPLOADED]: "已上传，待开始处理",
    [FileProcessingStatusEnum.OCR_PROCESSING]: "OCR处理中",
    [FileProcessingStatusEnum.OCR_COMPLETED]: "OCR处理完成，待向量化",
    [FileProcessingStatusEnum.EMBEDDING_PROCESSING]: "向量化处理中",
    [FileProcessingStatusEnum.COMPLETED]: "全部处理完成",
    [FileProcessingStatusEnum.OCR_FAILED]: "OCR处理失败",
    [FileProcessingStatusEnum.EMBEDDING_FAILED]: "向量化处理失败"
  };

  return statusMap[statusEnum] || "未知状态";
} 