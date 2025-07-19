"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { 
  ArrowLeft, 
  Upload, 
  File, 
  FileText, 
  Image, 
  Video, 
  Trash, 
  Search, 
  RefreshCw, 
  X,
  CheckCircle,
  Clock,
  AlertCircle,
  Download,
  Play,
  Pause,
  Loader2,
  Settings,
  FileSearch,
  BookOpen,
  MessageSquare
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Textarea } from "@/components/ui/textarea"
import { toast } from "@/hooks/use-toast"

import {
  getDatasetDetailWithToast,
  getDatasetFilesWithToast,
  uploadFileWithToast,
  deleteFileWithToast,
  processFileWithToast,
  getDatasetFilesProgressWithToast,
  ragSearchWithToast,
} from "@/lib/rag-dataset-service"
import type { 
  RagDataset, 
  FileDetail, 
  PageResponse,
  FileProcessProgressDTO,
  ProcessType,
  DocumentUnitDTO 
} from "@/types/rag-dataset"
import { FileInitializeStatus, FileEmbeddingStatus } from "@/types/rag-dataset"
import { getFileStatusConfig as getFileStatusInfo } from "@/lib/file-status-utils"
import { RagChatDialog } from "@/components/knowledge/RagChatDialog"
import { DocumentUnitsDialog } from "@/components/knowledge/DocumentUnitsDialog"

export default function DatasetDetailPage() {
  const params = useParams()
  const router = useRouter()
  const datasetId = params.id as string

  const [dataset, setDataset] = useState<RagDataset | null>(null)
  const [files, setFiles] = useState<FileDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [filesLoading, setFilesLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [fileToDelete, setFileToDelete] = useState<FileDetail | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  
  // 新增状态：文件处理进度
  const [filesProgress, setFilesProgress] = useState<FileProcessProgressDTO[]>([])
  const [isProcessing, setIsProcessing] = useState<{ [fileId: string]: boolean }>({})
  
  // 新增状态：RAG搜索
  const [searchDocuments, setSearchDocuments] = useState<DocumentUnitDTO[]>([])
  const [ragSearchQuery, setRagSearchQuery] = useState("")
  const [isRagSearching, setIsRagSearching] = useState(false)
  const [showRagResults, setShowRagResults] = useState(false)
  
  // RAG聊天对话框状态
  const [showRagChat, setShowRagChat] = useState(false)
  
  // 文档单元对话框状态
  const [selectedFileForUnits, setSelectedFileForUnits] = useState<FileDetail | null>(null)

  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<FileDetail>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  })

  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // 获取数据集详情
  useEffect(() => {
    if (datasetId) {
      loadDatasetDetail()
    }
  }, [datasetId])

  // 获取文件列表
  useEffect(() => {
    if (datasetId) {
      loadFiles(1, debouncedQuery)
      loadFilesProgress() // 同时加载文件处理进度
    }
  }, [datasetId, debouncedQuery])

  // 定期刷新文件处理进度（智能刷新）
  useEffect(() => {
    if (!datasetId) return

    const interval = setInterval(() => {
      // 只有当有文件正在处理时才刷新进度
      const hasProcessingFiles = filesProgress.some(p => 
        p.processProgress !== undefined && p.processProgress < 100
      )
      
      if (hasProcessingFiles || Object.keys(isProcessing).some(key => isProcessing[key])) {
        loadFilesProgress()
      }
    }, 3000) // 缩短为3秒刷新一次

    return () => clearInterval(interval)
  }, [datasetId, filesProgress, isProcessing])

  // 监控进度变化，智能刷新文件列表
  useEffect(() => {
    // 检查是否有刚完成的文件（进度从<100变为100）
    const completedFiles = filesProgress.filter(p => p.processProgress === 100)
    if (completedFiles.length > 0) {
      // 延迟刷新文件列表，避免频繁刷新
      const timeoutId = setTimeout(() => {
        loadFiles(pageData.current, debouncedQuery)
      }, 1000)
      
      return () => clearTimeout(timeoutId)
    }
  }, [filesProgress, pageData.current, debouncedQuery])

  // 加载数据集详情
  const loadDatasetDetail = async () => {
    try {
      setLoading(true)
      setError(null)

      const response = await getDatasetDetailWithToast(datasetId)

      if (response.code === 200) {
        setDataset(response.data)
      } else {
        setError(response.message)
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "未知错误"
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  // 加载文件列表
  const loadFiles = async (page: number = 1, keyword?: string) => {
    try {
      setFilesLoading(true)

      const response = await getDatasetFilesWithToast(datasetId, {
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      })

      if (response.code === 200) {
        setPageData(response.data)
        setFiles(response.data.records || [])
      }
    } catch (error) {
      console.error("加载文件列表失败:", error)
    } finally {
      setFilesLoading(false)
    }
  }

  // 处理文件上传
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = event.target.files
    if (!selectedFiles || selectedFiles.length === 0) return

    try {
      setIsUploading(true)

      // 上传所有选中的文件
      for (const file of Array.from(selectedFiles)) {
        const response = await uploadFileWithToast(datasetId, file)
        
        if (response.code === 200) {
          // 重新加载文件列表和数据集信息
          loadFiles(pageData.current, debouncedQuery)
          loadDatasetDetail()
        }
      }
    } catch (error) {
      console.error("文件上传失败:", error)
    } finally {
      setIsUploading(false)
      // 清空文件输入
      event.target.value = ""
    }
  }

  // 处理删除文件
  const handleDeleteFile = async () => {
    if (!fileToDelete) return

    try {
      setIsDeleting(true)
      const response = await deleteFileWithToast(datasetId, fileToDelete.id)

      if (response.code === 200) {
        // 重新加载文件列表和数据集信息
        loadFiles(pageData.current, debouncedQuery)
        loadDatasetDetail()
      }
    } catch (error) {
      console.error("删除文件失败:", error)
    } finally {
      setIsDeleting(false)
      setFileToDelete(null)
    }
  }

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return
    loadFiles(page, debouncedQuery)
  }

  // 生成分页数字
  const generatePageNumbers = () => {
    const pages: (number | string)[] = []
    const current = pageData.current
    const total = pageData.pages

    if (total <= 7) {
      for (let i = 1; i <= total; i++) {
        pages.push(i)
      }
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) {
          pages.push(i)
        }
        pages.push('...')
        pages.push(total)
      } else if (current >= total - 3) {
        pages.push(1)
        pages.push('...')
        for (let i = total - 4; i <= total; i++) {
          pages.push(i)
        }
      } else {
        pages.push(1)
        pages.push('...')
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i)
        }
        pages.push('...')
        pages.push(total)
      }
    }

    return pages
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
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  // 获取文件状态配置（使用新的统一状态逻辑）
  const getFileStatusDisplay = (file: FileDetail) => {
    const progressInfo = getFileProgressInfo(file.id)
    return getFileStatusInfo(file, progressInfo)
  }

  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  // ========== 新增方法：文件处理进度相关 ==========

  // 加载文件处理进度
  const loadFilesProgress = async () => {
    try {
      const response = await getDatasetFilesProgressWithToast(datasetId)
      if (response.code === 200) {
        // 避免不必要的状态更新
        const newProgress = response.data
        const hasChanged = JSON.stringify(newProgress) !== JSON.stringify(filesProgress)
        
        if (hasChanged) {
          setFilesProgress(newProgress)
          console.log('文件进度已更新:', newProgress)
        }
      }
    } catch (error) {
      console.error("加载文件处理进度失败:", error)
    }
  }

  // 启动文件预处理
  const handleProcessFile = async (fileId: string, processType: ProcessType) => {
    try {
      setIsProcessing(prev => ({ ...prev, [fileId]: true }))
      
      const response = await processFileWithToast({
        fileId,
        datasetId,
        processType
      })
      
      if (response.code === 200) {
        // 立即刷新进度
        setTimeout(() => {
          loadFilesProgress()
        }, 1000)
        
        // 重新加载文件列表以更新状态
        setTimeout(() => {
          loadFiles(pageData.current, debouncedQuery)
        }, 2000)
      }
    } catch (error) {
      console.error("启动文件预处理失败:", error)
    } finally {
      setIsProcessing(prev => ({ ...prev, [fileId]: false }))
    }
  }
  
  // 这些函数已经被新的统一状态逻辑替代，不再需要

  // 获取文件处理进度信息
  const getFileProgressInfo = (fileId: string) => {
    return filesProgress.find(progress => progress.fileId === fileId)
  }

  // 获取状态图标
  const getStatusIcon = (iconType: string) => {
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

  // ========== 新增方法：RAG搜索相关 ==========

  // 执行RAG搜索
  const handleRagSearch = async () => {
    if (!ragSearchQuery.trim()) {
      toast({
        title: "请输入搜索内容",
        variant: "destructive",
      })
      return
    }

    try {
      setIsRagSearching(true)
      setShowRagResults(true)
      
      const response = await ragSearchWithToast({
        datasetIds: [datasetId],
        question: ragSearchQuery.trim(),
        maxResults: 15
      })
      
      if (response.code === 200) {
        setSearchDocuments(response.data)
      } else {
        setSearchDocuments([])
      }
    } catch (error) {
      console.error("RAG搜索失败:", error)
      setSearchDocuments([])
    } finally {
      setIsRagSearching(false)
    }
  }

  // 清除RAG搜索
  const clearRagSearch = () => {
    setRagSearchQuery("")
    setSearchDocuments([])
    setShowRagResults(false)
  }

  if (loading) {
    return (
      <div className="container py-6">
        <div className="flex items-center gap-4 mb-6">
          <Skeleton className="h-10 w-10 rounded-md" />
          <div>
            <Skeleton className="h-8 w-48 mb-2" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <Skeleton className="h-6 w-24" />
              </CardHeader>
              <CardContent className="space-y-4">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
              </CardContent>
            </Card>
          </div>
          <div className="lg:col-span-3">
            <Card>
              <CardHeader>
                <Skeleton className="h-6 w-32" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-64 w-full" />
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    )
  }

  if (error || !dataset) {
    return (
      <div className="container py-6">
        <div className="text-center py-10">
          <AlertCircle className="h-12 w-12 mx-auto text-red-500 mb-4" />
          <div className="text-red-500 mb-4">{error || "数据集不存在"}</div>
          <div className="flex gap-2 justify-center">
            <Button variant="outline" onClick={() => router.back()}>
              <ArrowLeft className="mr-2 h-4 w-4" />
              返回
            </Button>
            <Button variant="outline" onClick={loadDatasetDetail}>
              <RefreshCw className="mr-2 h-4 w-4" />
              重试
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="container py-6">
      {/* 面包屑导航 */}
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{dataset.name}</h1>
          <p className="text-muted-foreground">
            <Link href="/knowledge" className="hover:underline">知识库</Link>
            <span className="mx-2">/</span>
            <span>{dataset.name}</span>
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* 数据集信息 */}
        <div className="lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">数据集信息</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">名称</label>
                <p className="text-sm">{dataset.name}</p>
              </div>
              
              {dataset.description && (
                <div>
                  <label className="text-sm font-medium text-muted-foreground">描述</label>
                  <p className="text-sm">{dataset.description}</p>
                </div>
              )}
              
              <div>
                <label className="text-sm font-medium text-muted-foreground">文件数量</label>
                <p className="text-sm">{dataset.fileCount} 个文件</p>
              </div>
              
              <div>
                <label className="text-sm font-medium text-muted-foreground">创建时间</label>
                <p className="text-sm">{formatDate(dataset.createdAt)}</p>
              </div>
              
              <div>
                <label className="text-sm font-medium text-muted-foreground">更新时间</label>
                <p className="text-sm">{formatDate(dataset.updatedAt)}</p>
              </div>

              {/* RAG功能区 */}
              <div className="pt-4 border-t space-y-4">
                {/* RAG智能问答 */}
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-2 flex items-center gap-2">
                    <MessageSquare className="h-4 w-4" />
                    智能问答
                  </label>
                  <Button 
                    onClick={() => setShowRagChat(true)}
                    variant="outline"
                    className="w-full"
                  >
                    <MessageSquare className="mr-2 h-4 w-4" />
                    开始对话
                  </Button>
                </div>

                {/* RAG搜索 */}
                <div>
                  <label className="text-sm font-medium text-muted-foreground mb-2 flex items-center gap-2">
                    <FileSearch className="h-4 w-4" />
                    文档搜索
                  </label>
                  <div className="space-y-2">
                    <Textarea
                      placeholder="输入问题进行文档搜索..."
                      value={ragSearchQuery}
                      onChange={(e) => setRagSearchQuery(e.target.value)}
                      className="min-h-[80px] resize-none"
                    />
                    <div className="flex gap-2">
                      <Button 
                        onClick={handleRagSearch}
                        disabled={isRagSearching || !ragSearchQuery.trim()}
                        className="flex-1"
                        size="sm"
                      >
                        {isRagSearching ? (
                          <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            搜索中...
                          </>
                        ) : (
                          <>
                            <Search className="mr-2 h-4 w-4" />
                            搜索文档
                          </>
                        )}
                      </Button>
                      {ragSearchQuery && (
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={clearRagSearch}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* 文件上传 */}
              <div className="pt-4 border-t">
                <label htmlFor="file-upload" className="block">
                  <Button 
                    variant="outline" 
                    className="w-full" 
                    disabled={isUploading}
                    asChild
                  >
                    <span>
                      <Upload className="mr-2 h-4 w-4" />
                      {isUploading ? "上传中..." : "上传文件"}
                    </span>
                  </Button>
                </label>
                <input
                  id="file-upload"
                  type="file"
                  multiple
                  className="hidden"
                  onChange={handleFileUpload}
                  accept=".pdf,.doc,.docx,.txt,.md,.html,.json,.csv,.xlsx,.xls"
                />
                <p className="text-xs text-muted-foreground mt-2">
                  支持 PDF、Word、文本等格式
                </p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 文件列表 */}
        <div className="lg:col-span-3">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">文件列表</CardTitle>
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      type="search"
                      placeholder="搜索文件..."
                      className="pl-10 pr-10 w-64"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                    />
                    {searchQuery && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
                        onClick={clearSearch}
                      >
                        <X className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {filesLoading ? (
                <div className="space-y-3">
                  {Array.from({ length: 5 }).map((_, index) => (
                    <div key={index} className="flex items-center gap-4 p-3 border rounded">
                      <Skeleton className="h-8 w-8" />
                      <div className="flex-1">
                        <Skeleton className="h-4 w-48 mb-2" />
                        <Skeleton className="h-3 w-32" />
                      </div>
                      <Skeleton className="h-6 w-16" />
                      <Skeleton className="h-6 w-16" />
                      <Skeleton className="h-8 w-8" />
                    </div>
                  ))}
                </div>
              ) : files.length === 0 ? (
                <div className="text-center py-8">
                  <File className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                  <h3 className="text-lg font-medium mb-2">
                    {searchQuery ? "未找到匹配的文件" : "还没有上传任何文件"}
                  </h3>
                  <p className="text-muted-foreground mb-4">
                    {searchQuery ? "尝试使用不同的搜索词" : "上传文件开始构建您的知识库"}
                  </p>
                </div>
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-12"></TableHead>
                        <TableHead>文件名</TableHead>
                        <TableHead>大小</TableHead>
                        <TableHead>处理状态</TableHead>
                        <TableHead>处理进度</TableHead>
                        <TableHead>上传时间</TableHead>
                        <TableHead className="w-20">操作</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {files.map((file) => {
                        const fileStatusDisplay = getFileStatusDisplay(file)
                        const progressInfo = getFileProgressInfo(file.id)
                        const processing = isProcessing[file.id]
                        
                        return (
                          <TableRow key={file.id}>
                            <TableCell>
                              {getFileIcon(file.contentType, file.ext)}
                            </TableCell>
                            <TableCell>
                              <div>
                                <p className="font-medium">{file.originalFilename}</p>
                                <p className="text-xs text-muted-foreground">{file.ext.toUpperCase()}</p>
                              </div>
                            </TableCell>
                            <TableCell className="text-sm">
                              {formatFileSize(file.size)}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-1">
                                {getStatusIcon(fileStatusDisplay.status.iconType)}
                                <Badge 
                                  variant={fileStatusDisplay.status.variant}
                                  className={`text-xs ${fileStatusDisplay.status.color}`}
                                >
                                  {fileStatusDisplay.status.text}
                                </Badge>
                              </div>
                            </TableCell>
                            <TableCell>
                              {progressInfo && progressInfo.processProgress !== undefined ? (
                                <div className="space-y-1">
                                  <div className="flex items-center justify-between">
                                    <span className="text-xs text-muted-foreground">
                                      {Math.round(progressInfo.processProgress)}%
                                    </span>
                                    {progressInfo.currentPageNumber && progressInfo.filePageSize && (
                                      <span className="text-xs text-muted-foreground">
                                        {progressInfo.currentPageNumber}/{progressInfo.filePageSize}
                                      </span>
                                    )}
                                  </div>
                                  <Progress value={progressInfo.processProgress} className="h-2" />
                                  {progressInfo.statusDescription && (
                                    <p className="text-xs text-muted-foreground">
                                      {progressInfo.statusDescription}
                                    </p>
                                  )}
                                </div>
                              ) : (
                                <span className="text-xs text-muted-foreground">-</span>
                              )}
                            </TableCell>
                            <TableCell className="text-sm">
                              {formatDate(file.createdAt)}
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-1">
                                
                                {(fileStatusDisplay.status.text === "处理完成" || fileStatusDisplay.status.text === "OCR处理完成") && (
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => setSelectedFileForUnits(file)}
                                    title="查看语料"
                                  >
                                    <FileText className="h-4 w-4" />
                                  </Button>
                                )}
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8"
                                  onClick={() => window.open(file.url, '_blank')}
                                  title="下载文件"
                                >
                                  <Download className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 text-red-600 hover:text-red-700"
                                  onClick={() => setFileToDelete(file)}
                                  title="删除文件"
                                >
                                  <Trash className="h-4 w-4" />
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>

                  {/* 分页 */}
                  {pageData.pages > 1 && (
                    <div className="flex justify-center mt-6">
                      <Pagination>
                        <PaginationContent>
                          <PaginationItem>
                            <PaginationPrevious 
                              onClick={() => handlePageChange(pageData.current - 1)}
                              className={pageData.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                            />
                          </PaginationItem>
                          
                          {generatePageNumbers().map((page, index) => (
                            <PaginationItem key={index}>
                              {page === '...' ? (
                                <PaginationEllipsis />
                              ) : (
                                <PaginationLink
                                  onClick={() => handlePageChange(page as number)}
                                  isActive={page === pageData.current}
                                  className="cursor-pointer"
                                >
                                  {page}
                                </PaginationLink>
                              )}
                            </PaginationItem>
                          ))}
                          
                          <PaginationItem>
                            <PaginationNext 
                              onClick={() => handlePageChange(pageData.current + 1)}
                              className={pageData.current >= pageData.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                            />
                          </PaginationItem>
                        </PaginationContent>
                      </Pagination>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* RAG搜索结果对话框 */}
      <Dialog open={showRagResults} onOpenChange={(open) => !open && clearRagSearch()}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <BookOpen className="h-5 w-5" />
              搜索结果
            </DialogTitle>
            <DialogDescription>
              针对问题 "{ragSearchQuery}" 的文档搜索结果
            </DialogDescription>
          </DialogHeader>
          
          <div className="max-h-[60vh] overflow-y-auto">
            {isRagSearching ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-8 w-8 animate-spin" />
                <span className="ml-2">搜索中...</span>
              </div>
            ) : searchDocuments.length === 0 ? (
              <div className="text-center py-8">
                <FileSearch className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium mb-2">未找到相关文档</h3>
                <p className="text-muted-foreground">
                  尝试使用不同的关键词或检查文档是否已完成向量化处理
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {searchDocuments.map((doc, index) => (
                  <Card key={doc.id} className="p-4">
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-xs">
                          第 {doc.page} 页
                        </Badge>
                        <Badge variant={doc.isVector ? "default" : "secondary"} className="text-xs">
                          {doc.isVector ? "已向量化" : "未向量化"}
                        </Badge>
                        {doc.isOcr && (
                          <Badge variant="outline" className="text-xs">
                            OCR处理
                          </Badge>
                        )}
                      </div>
                      <span className="text-xs text-muted-foreground">
                        #{index + 1}
                      </span>
                    </div>
                    <div className="text-sm leading-relaxed">
                      {doc.content.length > 500 
                        ? `${doc.content.substring(0, 500)}...` 
                        : doc.content}
                    </div>
                    <div className="flex items-center justify-between mt-3 pt-3 border-t">
                      <span className="text-xs text-muted-foreground">
                        文档ID: {doc.fileId}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {formatDate(doc.updatedAt)}
                      </span>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={clearRagSearch}>
              关闭
            </Button>
            {searchDocuments.length > 0 && (
              <Button onClick={() => {
                // 可以添加导出功能
                toast({
                  title: "搜索完成",
                  description: `找到 ${searchDocuments.length} 个相关文档片段`,
                })
              }}>
                <Download className="mr-2 h-4 w-4" />
                导出结果
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 删除文件确认对话框 */}
      <Dialog open={!!fileToDelete} onOpenChange={(open) => !open && setFileToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              您确定要删除文件 "{fileToDelete?.originalFilename}" 吗？此操作无法撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFileToDelete(null)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteFile} disabled={isDeleting}>
              {isDeleting ? "删除中..." : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* RAG聊天对话框 */}
      <RagChatDialog 
        open={showRagChat}
        onOpenChange={setShowRagChat}
        dataset={dataset}
      />

      {/* 文档单元对话框 */}
      {selectedFileForUnits && (
        <DocumentUnitsDialog
          open={!!selectedFileForUnits}
          onOpenChange={(open) => !open && setSelectedFileForUnits(null)}
          file={selectedFileForUnits}
        />
      )}
    </div>
  )
}