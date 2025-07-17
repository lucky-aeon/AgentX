"use client"

import { useState, useEffect } from "react"
import { Plus, Search, RefreshCw, X, Book } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

import { getDatasetsWithToast, deleteDatasetWithToast } from "@/lib/rag-dataset-service"
import type { RagDataset, PageResponse } from "@/types/rag-dataset"
import { CreatedRagCard } from "../cards/CreatedRagCard"
import { CreateDatasetDialog } from "../CreateDatasetDialog"
import { EditDatasetDialog } from "../EditDatasetDialog"
import { PublishRagDialog } from "../dialogs/PublishRagDialog"
import { RagVersionHistoryDialog } from "../dialogs/RagVersionHistoryDialog"

export function CreatedRagsSection() {
  const [datasets, setDatasets] = useState<RagDataset[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [datasetToDelete, setDatasetToDelete] = useState<RagDataset | null>(null)
  const [datasetToEdit, setDatasetToEdit] = useState<RagDataset | null>(null)
  const [datasetToPublish, setDatasetToPublish] = useState<RagDataset | null>(null)
  const [datasetToViewHistory, setDatasetToViewHistory] = useState<RagDataset | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [showAll, setShowAll] = useState(false)
  
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

  // 显示的数据集数量
  const displayedDatasets = showAll ? datasets : datasets.slice(0, 6)
  const hasMore = datasets.length > 6

  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      {/* 操作栏 */}
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-primary/10 p-1.5 rounded-md text-primary mr-2">
              <Book className="h-5 w-5" />
            </span>
            我创建的知识库
          </h2>
          <p className="text-sm text-muted-foreground mt-1">
            管理您创建的RAG数据集，支持发布到市场
          </p>
        </div>
      </div>

      {/* 搜索栏 */}
      <div className="relative mb-4">
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

      {/* 内容区域 */}
      {loading ? (
        // 加载状态
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="border rounded-lg p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <Skeleton className="h-8 w-8 rounded-full" />
                  <Skeleton className="h-5 w-32" />
                </div>
                <Skeleton className="h-8 w-8 rounded-md" />
              </div>
              <Skeleton className="h-4 w-24 mb-2" />
              <Skeleton className="h-4 w-full mb-2" />
              <Skeleton className="h-4 w-3/4 mb-4" />
              <div className="flex justify-between">
                <Skeleton className="h-9 w-20" />
                <Skeleton className="h-9 w-20" />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        // 错误状态
        <div className="text-center py-10 bg-red-50 rounded-lg border border-red-100">
          <div className="text-red-500 mb-4">{error}</div>
          <Button variant="outline" onClick={() => loadDatasets(1, debouncedQuery)}>
            <RefreshCw className="mr-2 h-4 w-4" />
            重试
          </Button>
        </div>
      ) : datasets.length === 0 ? (
        // 空状态
        <div className="text-center py-12 bg-gray-50 rounded-lg border border-gray-100">
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
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
            {displayedDatasets.map((dataset) => (
              <CreatedRagCard 
                key={dataset.id}
                dataset={dataset}
                onEdit={setDatasetToEdit}
                onDelete={setDatasetToDelete}
                onPublish={setDatasetToPublish}
                onViewHistory={setDatasetToViewHistory}
              />
            ))}
          </div>

          {/* 查看更多/收起按钮 */}
          {hasMore && (
            <div className="flex justify-center mt-4">
              <Button
                variant="outline"
                onClick={() => setShowAll(!showAll)}
              >
                {showAll ? "收起" : `查看更多 (${datasets.length - 6})`}
              </Button>
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

      {/* 发布RAG对话框 */}
      <PublishRagDialog
        open={!!datasetToPublish}
        onOpenChange={(open) => !open && setDatasetToPublish(null)}
        dataset={datasetToPublish}
        onSuccess={() => {
          loadDatasets(pageData.current, debouncedQuery)
          setDatasetToPublish(null)
        }}
      />

      {/* 版本历史对话框 */}
      <RagVersionHistoryDialog
        open={!!datasetToViewHistory}
        onOpenChange={(open) => !open && setDatasetToViewHistory(null)}
        dataset={datasetToViewHistory}
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