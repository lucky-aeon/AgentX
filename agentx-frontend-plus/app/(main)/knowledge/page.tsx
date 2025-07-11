"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Book, Edit, MoreHorizontal, Plus, Trash, Search, RefreshCw, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
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
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"

import { 
  getDatasetsWithToast,
  deleteDatasetWithToast,
} from "@/lib/rag-dataset-service"
import type { RagDataset, PageResponse } from "@/types/rag-dataset"
import { CreateDatasetDialog } from "@/components/knowledge/CreateDatasetDialog"
import { EditDatasetDialog } from "@/components/knowledge/EditDatasetDialog"
import { DatasetCard } from "@/components/knowledge/DatasetCard"

export default function KnowledgePage() {
  const [datasets, setDatasets] = useState<RagDataset[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [datasetToDelete, setDatasetToDelete] = useState<RagDataset | null>(null)
  const [datasetToEdit, setDatasetToEdit] = useState<RagDataset | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  
  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<RagDataset>>({
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

  // 获取数据集列表
  useEffect(() => {
    loadDatasets(1, debouncedQuery)
  }, [debouncedQuery])

  // 加载数据集
  const loadDatasets = async (page: number = 1, keyword?: string) => {
    try {
      setLoading(true)
      setError(null)

      const response = await getDatasetsWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      })

      if (response.code === 200) {
        setPageData(response.data)
        setDatasets(response.data.records || [])
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

  // 处理删除数据集
  const handleDeleteDataset = async () => {
    if (!datasetToDelete) return

    try {
      setIsDeleting(true)
      const response = await deleteDatasetWithToast(datasetToDelete.id)

      if (response.code === 200) {
        // 重新加载当前页
        loadDatasets(pageData.current, debouncedQuery)
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsDeleting(false)
      setDatasetToDelete(null)
    }
  }

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return
    loadDatasets(page, debouncedQuery)
  }

  // 生成分页数字
  const generatePageNumbers = () => {
    const pages: (number | string)[] = []
    const current = pageData.current
    const total = pageData.pages

    if (total <= 7) {
      // 7页以内显示全部
      for (let i = 1; i <= total; i++) {
        pages.push(i)
      }
    } else {
      // 超过7页的情况
      if (current <= 4) {
        // 当前页在前4页
        for (let i = 1; i <= 5; i++) {
          pages.push(i)
        }
        pages.push('...')
        pages.push(total)
      } else if (current >= total - 3) {
        // 当前页在后4页
        pages.push(1)
        pages.push('...')
        for (let i = total - 4; i <= total; i++) {
          pages.push(i)
        }
      } else {
        // 当前页在中间
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

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">知识库</h1>
          <p className="text-muted-foreground">管理您的RAG数据集</p>
        </div>
        <CreateDatasetDialog onSuccess={() => loadDatasets(1, debouncedQuery)} />
      </div>

      {/* 搜索栏 */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索数据集..."
            className="pl-10 pr-10"
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
              <span className="sr-only">清除搜索</span>
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        // 加载状态
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 4 }).map((_, index) => (
            <Card key={index}>
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-8 w-8 rounded-full" />
                    <Skeleton className="h-5 w-32" />
                  </div>
                  <Skeleton className="h-8 w-8 rounded-md" />
                </div>
                <Skeleton className="h-4 w-24 mt-2" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-4 w-full mt-2" />
                <Skeleton className="h-4 w-3/4 mt-2" />
                <Skeleton className="h-6 w-16 mt-4" />
              </CardContent>
              <CardFooter className="flex justify-between">
                <Skeleton className="h-9 w-20" />
                <Skeleton className="h-9 w-20" />
              </CardFooter>
            </Card>
          ))}
        </div>
      ) : error ? (
        // 错误状态
        <div className="text-center py-10">
          <div className="text-red-500 mb-4">{error}</div>
          <Button variant="outline" onClick={() => loadDatasets(1, debouncedQuery)}>
            <RefreshCw className="mr-2 h-4 w-4" />
            重试
          </Button>
        </div>
      ) : datasets.length === 0 ? (
        // 空状态
        <div className="text-center py-16 border rounded-lg bg-gray-50">
          <Book className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">
            {searchQuery ? "未找到匹配的数据集" : "还没有创建任何数据集"}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchQuery ? "尝试使用不同的搜索词" : "创建您的第一个RAG数据集，开始知识管理"}
          </p>
          {!searchQuery && (
            <CreateDatasetDialog onSuccess={() => loadDatasets(1, debouncedQuery)} />
          )}
        </div>
      ) : (
        // 数据集列表
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {datasets.map((dataset) => (
              <DatasetCard 
                key={dataset.id}
                dataset={dataset}
                onEdit={setDatasetToEdit}
                onDelete={setDatasetToDelete}
              />
            ))}
          </div>

          {/* 分页 */}
          {pageData.pages > 1 && (
            <div className="flex justify-center mt-8">
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

      {/* 编辑数据集对话框 */}
      <EditDatasetDialog
        dataset={datasetToEdit}
        open={!!datasetToEdit}
        onOpenChange={(open) => !open && setDatasetToEdit(null)}
        onSuccess={() => {
          loadDatasets(pageData.current, debouncedQuery)
          setDatasetToEdit(null)
        }}
      />

      {/* 删除确认对话框 */}
      <Dialog open={!!datasetToDelete} onOpenChange={(open) => !open && setDatasetToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              您确定要删除数据集 "{datasetToDelete?.name}" 吗？此操作无法撤销，将同时删除数据集中的所有文件。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDatasetToDelete(null)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteDataset} disabled={isDeleting}>
              {isDeleting ? "删除中..." : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

