"use client"

import { useState, useEffect } from "react"
import ReactMarkdown from 'react-markdown'
import { 
  FileText, 
  Edit, 
  Trash, 
  Save, 
  X, 
  Loader2,
  Search,
  FileSearch,
  CheckCircle,
  AlertCircle
} from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { toast } from "@/hooks/use-toast"
import {
  getFileInfoWithToast,
  getDocumentUnitsWithToast,
  updateDocumentUnitWithToast,
  deleteDocumentUnitWithToast
} from "@/lib/rag-file-service"
import type { 
  FileDetail, 
  FileDetailInfoDTO,
  DocumentUnitDTO,
  PageResponse 
} from "@/types/rag-dataset"

interface DocumentUnitsDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  file: FileDetail
}

export function DocumentUnitsDialog({ open, onOpenChange, file }: DocumentUnitsDialogProps) {
  const [fileInfo, setFileInfo] = useState<FileDetailInfoDTO | null>(null)
  const [documentUnits, setDocumentUnits] = useState<DocumentUnitDTO[]>([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [editingUnit, setEditingUnit] = useState<string | null>(null)
  const [editContent, setEditContent] = useState("")
  const [savingUnit, setSavingUnit] = useState<string | null>(null)
  const [deletingUnit, setDeletingUnit] = useState<DocumentUnitDTO | null>(null)
  
  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<DocumentUnitDTO>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  })

  // 防抖处理搜索
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)
    return () => clearTimeout(timer)
  }, [searchQuery])

  // 加载文件信息和文档单元
  useEffect(() => {
    if (open && file) {
      loadFileInfo()
      loadDocumentUnits(1, debouncedQuery)
    }
  }, [open, file, debouncedQuery])

  // 加载文件信息
  const loadFileInfo = async () => {
    try {
      const response = await getFileInfoWithToast(file.id)
      if (response.code === 200) {
        setFileInfo(response.data)
      }
    } catch (error) {
      console.error("加载文件信息失败:", error)
    }
  }

  // 加载文档单元列表
  const loadDocumentUnits = async (page: number = 1, keyword?: string) => {
    try {
      setLoading(true)
      const response = await getDocumentUnitsWithToast({
        fileId: file.id,
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      })
      
      if (response.code === 200) {
        setPageData(response.data)
        setDocumentUnits(response.data.records || [])
      }
    } catch (error) {
      console.error("加载文档单元失败:", error)
    } finally {
      setLoading(false)
    }
  }

  // 开始编辑
  const startEdit = (unit: DocumentUnitDTO) => {
    setEditingUnit(unit.id)
    setEditContent(unit.content)
  }

  // 取消编辑
  const cancelEdit = () => {
    setEditingUnit(null)
    setEditContent("")
  }

  // 保存编辑
  const saveEdit = async (unitId: string) => {
    if (!editContent.trim()) {
      toast({
        title: "内容不能为空",
        variant: "destructive"
      })
      return
    }

    try {
      setSavingUnit(unitId)
      const response = await updateDocumentUnitWithToast({
        id: unitId,
        content: editContent.trim()
      })
      
      if (response.code === 200) {
        // 更新本地数据
        setDocumentUnits(prev => 
          prev.map(unit => 
            unit.id === unitId 
              ? { ...unit, content: editContent.trim() }
              : unit
          )
        )
        cancelEdit()
      }
    } catch (error) {
      console.error("保存失败:", error)
    } finally {
      setSavingUnit(null)
    }
  }

  // 删除文档单元
  const handleDelete = async () => {
    if (!deletingUnit) return

    try {
      const response = await deleteDocumentUnitWithToast(deletingUnit.id)
      if (response.code === 200) {
        // 重新加载列表
        loadDocumentUnits(pageData.current, debouncedQuery)
      }
    } catch (error) {
      console.error("删除失败:", error)
    } finally {
      setDeletingUnit(null)
    }
  }

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return
    loadDocumentUnits(page, debouncedQuery)
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

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-5xl max-h-[85vh] p-0 flex flex-col">
          <DialogHeader className="px-6 py-4 border-b shrink-0">
            <div className="flex items-center gap-3">
              <FileText className="h-5 w-5" />
              <div>
                <DialogTitle>{file.originalFilename}</DialogTitle>
                <DialogDescription>
                  查看和编辑文档中的语料单元 {fileInfo?.filePath && `• ${fileInfo.filePath}`}
                </DialogDescription>
              </div>
            </div>
          </DialogHeader>

          <div className="px-6 py-3 border-b shrink-0">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Badge variant="outline" className="text-xs">
                  共 {pageData.total} 个语料
                </Badge>
                <Badge variant="outline" className="text-xs">
                  {file.filePageSize || 0} 页
                </Badge>
              </div>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="搜索语料内容..."
                  className="pl-10 pr-10 w-64"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                {searchQuery && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
                    onClick={() => setSearchQuery("")}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>
          </div>

          <ScrollArea className="flex-1 px-6 py-4 overflow-y-auto" style={{ maxHeight: 'calc(85vh - 160px)' }}>
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-8 w-8 animate-spin" />
                <span className="ml-2">加载中...</span>
              </div>
            ) : documentUnits.length === 0 ? (
              <div className="text-center py-8">
                <FileSearch className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium mb-2">
                  {searchQuery ? "未找到匹配的语料" : "暂无语料数据"}
                </h3>
                <p className="text-muted-foreground">
                  {searchQuery ? "尝试使用不同的搜索词" : "请先对文件进行初始化处理"}
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {documentUnits.map((unit) => (
                  <Card key={unit.id} className="p-4">
                    <div className="space-y-3">
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-2">
                          <Badge variant="outline" className="text-xs">
                            第 {unit.page + 1} 页
                          </Badge>
                          {unit.isVector && (
                            <Badge variant="default" className="text-xs">
                              <CheckCircle className="h-3 w-3 mr-1" />
                              已向量化
                            </Badge>
                          )}
                          {unit.isOcr && (
                            <Badge variant="secondary" className="text-xs">
                              OCR处理
                            </Badge>
                          )}
                        </div>
                        <div className="flex items-center gap-1">
                          {editingUnit === unit.id ? (
                            <>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => saveEdit(unit.id)}
                                disabled={savingUnit === unit.id}
                                title="保存修改"
                              >
                                {savingUnit === unit.id ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Save className="h-4 w-4" />
                                )}
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={cancelEdit}
                                disabled={savingUnit === unit.id}
                                title="取消编辑"
                              >
                                <X className="h-4 w-4" />
                              </Button>
                            </>
                          ) : (
                            <>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => {
                                  console.log('编辑语料单元:', unit);
                                  startEdit(unit);
                                }}
                                title="编辑语料"
                              >
                                <Edit className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="text-red-600 hover:text-red-700"
                                onClick={() => {
                                  console.log('删除语料单元:', unit);
                                  setDeletingUnit(unit);
                                }}
                                title="删除语料"
                              >
                                <Trash className="h-4 w-4" />
                              </Button>
                            </>
                          )}
                        </div>
                      </div>

                      <div className="text-sm">
                        {editingUnit === unit.id ? (
                          <Textarea
                            value={editContent}
                            onChange={(e) => setEditContent(e.target.value)}
                            className="min-h-[100px] resize-y"
                            disabled={savingUnit === unit.id}
                          />
                        ) : (
                          <div className="leading-tight whitespace-pre-wrap">
                            <ReactMarkdown 
                              components={{
                                h1: ({ children }) => <h1 className="text-xl font-bold mb-1 leading-tight">{children}</h1>,
                                h2: ({ children }) => <h2 className="text-lg font-semibold mb-1 leading-tight">{children}</h2>,
                                h3: ({ children }) => <h3 className="text-md font-medium mb-0.5 leading-tight">{children}</h3>,
                                p: ({ children }) => <p className="mb-0.5 leading-tight">{children}</p>,
                                ul: ({ children }) => <ul className="list-disc list-inside mb-0.5 space-y-0">{children}</ul>,
                                ol: ({ children }) => <ol className="list-decimal list-inside mb-0.5 space-y-0">{children}</ol>,
                                li: ({ children }) => <li className="leading-tight">{children}</li>,
                                strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                                em: ({ children }) => <em className="italic">{children}</em>,
                                code: ({ children }) => <code className="bg-gray-100 px-1 py-0.5 rounded text-sm font-mono">{children}</code>,
                                pre: ({ children }) => <pre className="bg-gray-100 p-2 rounded text-sm font-mono overflow-x-auto mb-1">{children}</pre>,
                                blockquote: ({ children }) => <blockquote className="border-l-4 border-gray-300 pl-4 italic mb-0.5 leading-tight">{children}</blockquote>,
                                a: ({ children, href }) => <a href={href} className="text-blue-600 hover:underline" target="_blank" rel="noopener noreferrer">{children}</a>,
                                hr: () => <hr className="my-4 border-gray-200" />,
                                table: ({ children }) => <table className="border-collapse border border-gray-300 w-full mb-1">{children}</table>,
                                th: ({ children }) => <th className="border border-gray-300 px-2 py-1 bg-gray-50 font-semibold">{children}</th>,
                                td: ({ children }) => <td className="border border-gray-300 px-2 py-1">{children}</td>,
                              }}
                            >
                              {unit.content}
                            </ReactMarkdown>
                          </div>
                        )}
                      </div>

                      <div className="flex items-center justify-between pt-2 border-t">
                        <span className="text-xs text-muted-foreground">
                          ID: {unit.id}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          更新时间: {new Date(unit.updatedAt).toLocaleString('zh-CN')}
                        </span>
                      </div>
                    </div>
                  </Card>
                ))}
              </div>
            )}
          </ScrollArea>

          {pageData.pages > 1 && (
            <div className="px-6 py-4 border-t shrink-0">
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
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!deletingUnit} onOpenChange={(open) => !open && setDeletingUnit(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              您确定要删除这个语料单元吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive text-destructive-foreground">
              确认删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}