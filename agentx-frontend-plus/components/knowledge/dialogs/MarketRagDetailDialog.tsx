"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { 
  Download, 
  User, 
  Calendar, 
  FileText, 
  Database, 
  Eye,
  Star,
  Clock,
  Package,
  Tag,
  File,
  Image,
  Video,
  Loader2
} from "lucide-react"

import type { RagMarketDTO } from "@/types/rag-publish"
import type { FileDetail } from "@/types/rag-dataset"
import { installRagVersionWithToast, getMarketRagFilesWithToast } from "@/lib/rag-publish-service"
import { formatFileSize, formatDateTime, getLabelColor } from "@/types/rag-publish"

interface MarketRagDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  ragMarket: RagMarketDTO | null
  onInstall?: (ragMarket: RagMarketDTO) => void
  onInstallSuccess?: () => void
}

export function MarketRagDetailDialog({ 
  open, 
  onOpenChange, 
  ragMarket,
  onInstall,
  onInstallSuccess
}: MarketRagDetailDialogProps) {
  const [isInstalling, setIsInstalling] = useState(false)
  const [files, setFiles] = useState<FileDetail[]>([])
  const [filesLoading, setFilesLoading] = useState(false)
  const [activeTab, setActiveTab] = useState("info")

  // 加载文件列表
  useEffect(() => {
    if (ragMarket && activeTab === "files") {
      loadFiles()
    }
  }, [ragMarket, activeTab])

  const loadFiles = async () => {
    if (!ragMarket) return

    try {
      setFilesLoading(true)
      const response = await getMarketRagFilesWithToast(ragMarket.id)

      if (response.code === 200) {
        setFiles(response.data || [])
      } else {
        setFiles([])
      }
    } catch (error) {
      console.error("加载文件列表失败:", error)
      setFiles([])
    } finally {
      setFilesLoading(false)
    }
  }

  // 处理安装
  const handleInstall = async () => {
    if (!ragMarket) return

    // 如果有外部安装处理器，使用它
    if (onInstall) {
      onInstall(ragMarket)
      return
    }

    // 否则直接安装
    setIsInstalling(true)

    try {
      const response = await installRagVersionWithToast({
        ragVersionId: ragMarket.id
      })
      
      if (response.code === 200) {
        onInstallSuccess?.()
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsInstalling(false)
    }
  }

  // 获取文件图标
  const getFileIcon = (contentType: string, ext: string) => {
    if (contentType.startsWith('image/')) {
      return <Image className="h-4 w-4" />
    } else if (contentType.startsWith('video/')) {
      return <Video className="h-4 w-4" />
    } else if (ext === 'pdf' || contentType === 'application/pdf') {
      return <FileText className="h-4 w-4" />
    } else {
      return <File className="h-4 w-4" />
    }
  }

  // 格式化文件大小
  const formatFileSizeLocal = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  if (!ragMarket) return null

  return (
    <>
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[700px] max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Eye className="h-5 w-5" />
            知识库详情
          </DialogTitle>
          <DialogDescription>
            查看知识库的详细信息、统计数据和文件列表
          </DialogDescription>
        </DialogHeader>
        
        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 overflow-hidden flex flex-col">
          <TabsList className="grid grid-cols-2 w-full">
            <TabsTrigger value="info">基本信息</TabsTrigger>
            <TabsTrigger value="files">文件列表 ({ragMarket.fileCount})</TabsTrigger>
          </TabsList>
          
          <TabsContent value="info" className="flex-1 overflow-y-auto">
            <div className="space-y-6 p-1">
          {/* 基本信息 */}
          <div className="flex items-start gap-4">
            <div className="flex h-16 w-16 items-center justify-center rounded-lg bg-primary text-primary-foreground overflow-hidden">
              {ragMarket.icon ? (
                <img
                  src={ragMarket.icon}
                  alt={ragMarket.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Database className="h-8 w-8" />
              )}
            </div>
            <div className="flex-1">
              <h3 className="text-xl font-semibold">{ragMarket.name}</h3>
              <Badge variant="secondary" className="mt-1 mb-2">
                v{ragMarket.version}
              </Badge>
              <p className="text-sm text-muted-foreground">
                {ragMarket.description || "暂无描述"}
              </p>
            </div>
          </div>

          <Separator />

          {/* 作者信息 */}
          <div className="space-y-3">
            <h4 className="text-sm font-medium flex items-center gap-2">
              <User className="h-4 w-4" />
              作者信息
            </h4>
            <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
              <Avatar className="h-8 w-8">
                <AvatarImage src={ragMarket.userAvatar} />
                <AvatarFallback className="text-sm">
                  {ragMarket.userNickname?.[0] || "U"}
                </AvatarFallback>
              </Avatar>
              <div>
                <div className="font-medium text-sm">{ragMarket.userNickname}</div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Calendar className="h-3 w-3" />
                  发布于 {formatDateTime(ragMarket.publishedAt)}
                </div>
              </div>
            </div>
          </div>

          {/* 标签 */}
          {ragMarket.labels && ragMarket.labels.length > 0 && (
            <div className="space-y-3">
              <h4 className="text-sm font-medium flex items-center gap-2">
                <Tag className="h-4 w-4" />
                标签
              </h4>
              <div className="flex flex-wrap gap-2">
                {ragMarket.labels.map((label, index) => (
                  <Badge 
                    key={label} 
                    variant="outline" 
                    className={`text-xs ${getLabelColor(index)}`}
                  >
                    {label}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          <Separator />

          {/* 统计信息 */}
          <div className="space-y-3">
            <h4 className="text-sm font-medium flex items-center gap-2">
              <Package className="h-4 w-4" />
              统计信息
            </h4>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-3">
                <div className="flex items-center gap-3 p-3 bg-blue-50 rounded-lg">
                  <div className="h-8 w-8 bg-blue-100 rounded-full flex items-center justify-center">
                    <Download className="h-4 w-4 text-blue-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-lg">{ragMarket.installCount}</div>
                    <div className="text-xs text-muted-foreground">安装次数</div>
                  </div>
                </div>
                
                <div className="flex items-center gap-3 p-3 bg-green-50 rounded-lg">
                  <div className="h-8 w-8 bg-green-100 rounded-full flex items-center justify-center">
                    <FileText className="h-4 w-4 text-green-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-lg">{ragMarket.fileCount}</div>
                    <div className="text-xs text-muted-foreground">文件数量</div>
                  </div>
                </div>
              </div>
              
              <div className="space-y-3">
                <div className="flex items-center gap-3 p-3 bg-purple-50 rounded-lg">
                  <div className="h-8 w-8 bg-purple-100 rounded-full flex items-center justify-center">
                    <Database className="h-4 w-4 text-purple-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-lg">{ragMarket.documentCount}</div>
                    <div className="text-xs text-muted-foreground">文档数量</div>
                  </div>
                </div>
                
                <div className="flex items-center gap-3 p-3 bg-orange-50 rounded-lg">
                  <div className="h-8 w-8 bg-orange-100 rounded-full flex items-center justify-center">
                    <Package className="h-4 w-4 text-orange-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-lg">
                      {ragMarket.totalSizeDisplay || formatFileSize(ragMarket.totalSize)}
                    </div>
                    <div className="text-xs text-muted-foreground">总大小</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* 评分信息（如果有） */}
          {ragMarket.rating && (
            <>
              <Separator />
              <div className="space-y-3">
                <h4 className="text-sm font-medium flex items-center gap-2">
                  <Star className="h-4 w-4" />
                  用户评价
                </h4>
                <div className="flex items-center gap-3 p-3 bg-yellow-50 rounded-lg">
                  <div className="h-8 w-8 bg-yellow-100 rounded-full flex items-center justify-center">
                    <Star className="h-4 w-4 text-yellow-600" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-lg">{ragMarket.rating.toFixed(1)}</span>
                      <span className="text-xs text-muted-foreground">/ 5.0</span>
                    </div>
                    {ragMarket.reviewCount && (
                      <div className="text-xs text-muted-foreground">
                        基于 {ragMarket.reviewCount} 个评价
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </>
          )}

              {/* 最后更新时间 */}
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Clock className="h-3 w-3" />
                最后更新: {formatDateTime(ragMarket.updatedAt)}
              </div>
            </div>
          </TabsContent>

          <TabsContent value="files" className="flex-1 overflow-hidden">
            <ScrollArea className="h-full">
              {filesLoading ? (
                <div className="space-y-3 p-1">
                  {Array.from({ length: 5 }).map((_, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 border rounded-lg">
                      <Skeleton className="h-8 w-8" />
                      <div className="flex-1">
                        <Skeleton className="h-4 w-48 mb-2" />
                        <Skeleton className="h-3 w-32" />
                      </div>
                      <Skeleton className="h-6 w-16" />
                    </div>
                  ))}
                </div>
              ) : files.length === 0 ? (
                <div className="text-center py-12">
                  <File className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium mb-2">暂无文件</h3>
                  <p className="text-muted-foreground">此知识库中暂时没有文件</p>
                </div>
              ) : (
                <div className="space-y-2 p-1">
                  {files.map((file) => (
                    <div
                      key={file.id}
                      className="flex items-center gap-3 p-3 border rounded-lg"
                    >
                      <div className="flex-shrink-0">
                        {getFileIcon(file.contentType, file.ext)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium truncate" title={file.originalFilename}>
                          {file.originalFilename}
                        </div>
                        <div className="text-sm text-muted-foreground flex items-center gap-2">
                          <span>{file.ext.toUpperCase()}</span>
                          <span>•</span>
                          <span>{formatFileSizeLocal(file.size)}</span>
                          {file.filePageSize && (
                            <>
                              <span>•</span>
                              <span>{file.filePageSize} 页</span>
                            </>
                          )}
                        </div>
                      </div>
                      <div className="flex-shrink-0">
                        <Badge variant="outline" className="text-xs">
                          {formatDateTime(file.createdAt).split(' ')[0]}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </ScrollArea>
          </TabsContent>
        </Tabs>

        <DialogFooter className="flex-col sm:flex-row gap-2">
          <Button 
            type="button" 
            variant="outline" 
            onClick={() => onOpenChange(false)}
            className="w-full sm:w-auto"
          >
            关闭
          </Button>
          <Button 
            onClick={handleInstall}
            disabled={isInstalling || ragMarket.isInstalled}
            className="w-full sm:w-auto"
          >
            <Download className="mr-2 h-4 w-4" />
            {isInstalling ? "安装中..." : ragMarket.isInstalled ? "已安装" : "安装知识库"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    {/* 文件预览对话框 */}
  </>
  )
}