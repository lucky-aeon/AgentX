import { CheckCircle, Clock, AlertCircle } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import type { FileDetail } from "@/types/rag-dataset"
import { FileInitializeStatus, FileEmbeddingStatus } from "@/types/rag-dataset"

interface FileStatusBadgeProps {
  file: FileDetail
  type: "initialize" | "embedding"
}

export function FileStatusBadge({ file, type }: FileStatusBadgeProps) {
  const getStatusConfig = () => {
    if (type === "initialize") {
      switch (file.isInitialize as FileInitializeStatus) {
        case FileInitializeStatus.NOT_INITIALIZED:
          return {
            text: "待初始化",
            variant: "outline" as const,
            icon: <Clock className="h-3 w-3" />,
            className: "text-yellow-600 border-yellow-300"
          }
        case FileInitializeStatus.INITIALIZED:
          return {
            text: "已初始化",
            variant: "default" as const,
            icon: <CheckCircle className="h-3 w-3" />,
            className: "text-green-600 bg-green-50 border-green-300"
          }
        default:
          return {
            text: "未知状态",
            variant: "destructive" as const,
            icon: <AlertCircle className="h-3 w-3" />,
            className: "text-red-600"
          }
      }
    } else {
      switch (file.isEmbedding as FileEmbeddingStatus) {
        case FileEmbeddingStatus.NOT_EMBEDDED:
          return {
            text: "待向量化",
            variant: "outline" as const,
            icon: <Clock className="h-3 w-3" />,
            className: "text-yellow-600 border-yellow-300"
          }
        case FileEmbeddingStatus.EMBEDDED:
          return {
            text: "已向量化",
            variant: "default" as const,
            icon: <CheckCircle className="h-3 w-3" />,
            className: "text-green-600 bg-green-50 border-green-300"
          }
        default:
          return {
            text: "未知状态",
            variant: "destructive" as const,
            icon: <AlertCircle className="h-3 w-3" />,
            className: "text-red-600"
          }
      }
    }
  }

  const config = getStatusConfig()

  return (
    <div className="flex items-center gap-1">
      {config.icon}
      <Badge 
        variant={config.variant}
        className={`text-xs ${config.className}`}
      >
        {config.text}
      </Badge>
    </div>
  )
}