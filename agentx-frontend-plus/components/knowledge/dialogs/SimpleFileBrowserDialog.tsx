"use client"

import { useEffect, useState } from "react"
import { 
  FolderOpen, 
  Search, 
  File, 
  FileText, 
  Image, 
  Video,
  Eye,
  X
} from "lucide-react"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { toast } from "@/hooks/use-toast"

import { 
  getInstalledRagFilesWithToast 
} from "@/lib/rag-publish-service"
import type { 
  UserRagDTO,
  FileDetail
} from "@/types/rag-dataset"
import { FileDetailPanel } from "@/components/rag-chat/FileDetailPanel"

interface SimpleFileBrowserDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  userRag: UserRagDTO | null
}

export function SimpleFileBrowserDialog({
  open,
  onOpenChange,
  userRag
}: SimpleFileBrowserDialogProps) {
  const [files, setFiles] = useState<FileDetail[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [selectedFile, setSelectedFile] = useState<FileDetail | null>(null)
  const [showFileDetail, setShowFileDetail] = useState(false)

  // 搜索防抖
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timeoutId)
  }, [searchQuery])

  // 加载文件列表
  const loadFiles = async () => {
    if (!userRag?.id) return

    setLoading(true)
    try {
      const response = await getInstalledRagFilesWithToast(userRag.id)

      if (response.code === 200) {
        let files = response.data || []
        
        // 客户端过滤（如果有搜索查询）
        if (debouncedQuery?.trim()) {
          const query = debouncedQuery.trim().toLowerCase()
          files = files.filter(file => 
            file.originalFilename?.toLowerCase().includes(query)
          )
        }
        
        setFiles(files.slice(0, 100)) // 限制最多显示100个文件
      }
    } catch (error) {
      toast({
        title: "加载文件列表失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive"
      })
    } finally {
      setLoading(false)
    }
  }

  // 初始加载和搜索变化时重新加载
  useEffect(() => {
    if (open && userRag) {
      loadFiles()
    }
  }, [open, userRag, debouncedQuery])

  // 获取文件类型图标
  const getFileIcon = (fileName: string) => {
    if (!fileName) {
      return <File className="h-4 w-4" />
    }
    const extension = fileName.split('.').pop()?.toLowerCase()
    
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(extension || '')) {
      return <Image className="h-4 w-4" />
    }
    if (['mp4', 'avi', 'mov', 'mkv'].includes(extension || '')) {
      return <Video className="h-4 w-4" />
    }
    if (['txt', 'md', 'pdf', 'doc', 'docx'].includes(extension || '')) {
      return <FileText className="h-4 w-4" />
    }
    return <File className="h-4 w-4" />
  }

  // 处理文件查看
  const handleViewFile = (file: FileDetail) => {
    setSelectedFile(file)
    setShowFileDetail(true)
  }

  // 格式化文件大小
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  if (!userRag) return null

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-4xl max-h-[80vh] flex flex-col">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-3">
              <FolderOpen className="h-5 w-5" />
              文件浏览 - {userRag.name}
            </DialogTitle>
            <DialogDescription>
              浏览知识库中的所有文件
            </DialogDescription>
          </DialogHeader>

          {/* 搜索框 */}
          <div className="flex items-center gap-3 py-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="搜索文件名..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>

          {/* 文件列表 */}
          <div className="flex-1 overflow-auto">
            {loading ? (
              <div className="space-y-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            ) : files.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <FolderOpen className="h-12 w-12 text-muted-foreground mb-4" />
                <p className="text-muted-foreground">
                  {debouncedQuery ? "未找到匹配的文件" : "该知识库暂无文件"}
                </p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>文件名</TableHead>
                    <TableHead>大小</TableHead>
                    <TableHead>上传时间</TableHead>
                    <TableHead className="w-[100px]">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {files.map((file) => (
                    <TableRow key={file.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          {getFileIcon(file.originalFilename)}
                          <span className="truncate max-w-[300px]" title={file.originalFilename}>
                            {file.originalFilename}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatFileSize(file.size || 0)}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(file.createdAt).toLocaleString('zh-CN')}
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewFile(file)}
                          className="h-8 w-8 p-0"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          {/* 文件总数 */}
          <div className="border-t pt-3">
            <p className="text-sm text-muted-foreground">
              共 {files.length} 个文件
            </p>
          </div>
        </DialogContent>
      </Dialog>

      {/* 文件详情对话框 */}
      {selectedFile && (
        <Dialog open={showFileDetail} onOpenChange={setShowFileDetail}>
          <DialogContent className="sm:max-w-4xl max-h-[80vh] flex flex-col">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-3">
                <FileText className="h-5 w-5" />
                文档内容 - {selectedFile.originalFilename}
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowFileDetail(false)}
                  className="ml-auto h-8 w-8 p-0"
                >
                  <X className="h-4 w-4" />
                </Button>
              </DialogTitle>
            </DialogHeader>
            
            <div className="flex-1 overflow-auto">
              <FileDetailPanel
                selectedFile={{
                  fileId: selectedFile.id,
                  fileName: selectedFile.originalFilename,
                  filePath: selectedFile.url || "",
                  isInstalledRag: true,
                  userRagId: userRag.id
                }}
                onDataLoad={() => {}}
              />
            </div>
          </DialogContent>
        </Dialog>
      )}
    </>
  )
}