import { CheckCircle, Clock, AlertCircle, Loader2 } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import type { FileDetail, FileProcessProgressDTO } from "@/types/rag-dataset"
import { getFileStatusConfig, type FileStatusDisplayConfig } from "@/lib/file-status-utils"

interface FileStatusBadgeProps {
  file: FileDetail
  progressInfo?: FileProcessProgressDTO
}

export function FileStatusBadge({ file, progressInfo }: FileStatusBadgeProps) {
  const { status } = getFileStatusConfig(file, progressInfo)
  
  const getIcon = (iconType: FileStatusDisplayConfig['iconType']) => {
    switch (iconType) {
      case "check":
        return <CheckCircle className="h-3 w-3" />
      case "clock":
        return <Clock className="h-3 w-3" />
      case "alert":
        return <AlertCircle className="h-3 w-3" />
      case "loading":
        return <Loader2 className="h-3 w-3 animate-spin" />
      default:
        return <Clock className="h-3 w-3" />
    }
  }

  return (
    <div className="flex items-center gap-1">
      {getIcon(status.iconType)}
      <Badge 
        variant={status.variant}
        className={`text-xs ${status.color}`}
      >
        {status.text}
      </Badge>
    </div>
  )
}