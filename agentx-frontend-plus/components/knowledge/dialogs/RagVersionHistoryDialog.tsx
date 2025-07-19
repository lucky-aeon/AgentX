"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { Clock, FileText, Tag, CheckCircle, XCircle, AlertCircle, MinusCircle } from "lucide-react"

import type { RagDataset } from "@/types/rag-dataset"
import type { RagVersionDTO } from "@/types/rag-publish"
import { getRagVersionHistory } from "@/lib/rag-publish-service"
import { RagPublishStatus, RagPublishStatusText, getPublishStatusColor, formatDateTime } from "@/types/rag-publish"

interface RagVersionHistoryDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  dataset: RagDataset | null
}

export function RagVersionHistoryDialog({ 
  open, 
  onOpenChange, 
  dataset 
}: RagVersionHistoryDialogProps) {
  const [versions, setVersions] = useState<RagVersionDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 获取版本历史
  const loadVersionHistory = async () => {
    if (!dataset) return

    setLoading(true)
    setError(null)

    try {
      const response = await getRagVersionHistory(dataset.id)
      
      if (response.code === 200) {
        setVersions(response.data)
      } else {
        setError(response.message)
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : "获取版本历史失败")
    } finally {
      setLoading(false)
    }
  }

  // 当对话框打开时加载数据
  useEffect(() => {
    if (open && dataset) {
      loadVersionHistory()
    }
  }, [open, dataset])

  // 获取状态图标
  const getStatusIcon = (status: RagPublishStatus) => {
    switch (status) {
      case RagPublishStatus.PUBLISHED:
        return <CheckCircle className="h-4 w-4 text-green-600" />
      case RagPublishStatus.REJECTED:
        return <XCircle className="h-4 w-4 text-red-600" />
      case RagPublishStatus.REVIEWING:
        return <AlertCircle className="h-4 w-4 text-yellow-600" />
      case RagPublishStatus.REMOVED:
        return <MinusCircle className="h-4 w-4 text-gray-600" />
      default:
        return <AlertCircle className="h-4 w-4 text-gray-600" />
    }
  }

  if (!dataset) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>版本历史</DialogTitle>
          <DialogDescription>
            查看知识库"{dataset.name}"的发布历史
          </DialogDescription>
        </DialogHeader>
        
        <ScrollArea className="max-h-[60vh] pr-4">
          {loading ? (
            // 加载状态
            <div className="space-y-4">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="border rounded-lg p-4">
                  <div className="flex items-center justify-between mb-2">
                    <Skeleton className="h-5 w-20" />
                    <Skeleton className="h-5 w-16" />
                  </div>
                  <Skeleton className="h-4 w-full mb-2" />
                  <Skeleton className="h-4 w-3/4" />
                </div>
              ))}
            </div>
          ) : error ? (
            // 错误状态
            <div className="text-center py-8">
              <div className="text-red-500 mb-4">{error}</div>
              <Button variant="outline" onClick={loadVersionHistory}>
                重试
              </Button>
            </div>
          ) : versions.length === 0 ? (
            // 空状态
            <div className="text-center py-8">
              <div className="text-muted-foreground mb-4">
                暂无版本历史
              </div>
              <div className="text-sm text-muted-foreground">
                发布知识库后，版本历史会显示在这里
              </div>
            </div>
          ) : (
            // 版本列表
            <div className="space-y-4">
              {versions.map((version) => (
                <div key={version.id} className="border rounded-lg p-4 hover:bg-muted/50 transition-colors">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">v{version.version}</Badge>
                      {version.version === "0.0.1" && (
                        <Badge variant="secondary" className="text-xs">
                          私有版本
                        </Badge>
                      )}
                      <Badge 
                        variant="outline" 
                        className={getPublishStatusColor(version.publishStatus)}
                      >
                        {getStatusIcon(version.publishStatus)}
                        <span className="ml-1">{version.publishStatusDesc}</span>
                      </Badge>
                    </div>
                    <div className="flex items-center gap-1 text-xs text-muted-foreground">
                      <Clock className="h-3 w-3" />
                      {formatDateTime(version.createdAt)}
                    </div>
                  </div>

                  {version.changeLog && (
                    <div className="mb-3">
                      <div className="text-sm font-medium mb-1">更新日志:</div>
                      <div className="text-sm text-muted-foreground bg-muted/50 p-2 rounded">
                        {version.changeLog}
                      </div>
                    </div>
                  )}

                  {version.labels && version.labels.length > 0 && (
                    <div className="mb-3">
                      <div className="flex items-center gap-1 text-sm text-muted-foreground mb-1">
                        <Tag className="h-3 w-3" />
                        标签:
                      </div>
                      <div className="flex flex-wrap gap-1">
                        {version.labels.map((label, index) => (
                          <Badge key={index} variant="secondary" className="text-xs">
                            {label}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="flex items-center gap-4 text-xs text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <FileText className="h-3 w-3" />
                      {version.fileCount} 文件
                    </div>
                    <div className="flex items-center gap-1">
                      <span>{version.documentCount} 文档</span>
                    </div>
                    {version.installCount > 0 && (
                      <div className="flex items-center gap-1">
                        <span>{version.installCount} 安装</span>
                      </div>
                    )}
                  </div>

                  {version.publishStatus === RagPublishStatus.REJECTED && version.rejectReason && (
                    <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded text-sm">
                      <div className="font-medium text-red-800 mb-1">拒绝原因:</div>
                      <div className="text-red-700">{version.rejectReason}</div>
                    </div>
                  )}

                  {version.publishStatus === RagPublishStatus.PUBLISHED && version.publishedAt && (
                    <div className="mt-2 text-xs text-muted-foreground">
                      发布于: {formatDateTime(version.publishedAt)}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}