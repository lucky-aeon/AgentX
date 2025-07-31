"use client"

import { useState, useEffect } from "react"
import { Search, RefreshCw, X, Book, Download } from "lucide-react"

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

import { 
  getUserInstalledRagsWithToast, 
  uninstallRagVersionWithToast
} from "@/lib/rag-publish-service"
import { getCurrentUserId, getCurrentUserIdAsync } from "@/lib/user-service"
import type { UserRagDTO, PageResponse } from "@/types/rag-publish"
import { InstalledRagCard } from "../cards/InstalledRagCard"
import { InstalledRagDetailDialog } from "../dialogs/InstalledRagDetailDialog"

export function InstalledRagsSection() {
  const [installedRags, setInstalledRags] = useState<UserRagDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [ragToUninstall, setRagToUninstall] = useState<UserRagDTO | null>(null)
  const [ragToViewDetails, setRagToViewDetails] = useState<UserRagDTO | null>(null)
  const [isUninstalling, setIsUninstalling] = useState(false)
  
  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<UserRagDTO>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  })

  // 获取当前用户ID
  useEffect(() => {
    const initCurrentUserId = async () => {
      // 先尝试从token解析
      let userId = getCurrentUserId()
      if (!userId) {
        // 如果token解析失败，从API获取
        userId = await getCurrentUserIdAsync()
      }
      setCurrentUserId(userId)
    }
    
    initCurrentUserId()
  }, [])

  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // 获取安装的RAG列表
  useEffect(() => {
    loadInstalledRags(1, debouncedQuery)
  }, [debouncedQuery])

  // 加载安装的RAG
  const loadInstalledRags = async (page: number = 1, keyword?: string) => {
    try {
      setLoading(true)
      setError(null)

      const response = await getUserInstalledRagsWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      })

      if (response.code === 200) {
        setPageData(response.data)
        setInstalledRags(response.data.records || [])
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

  // 处理版本切换
  const handleVersionSwitch = (updatedUserRag: UserRagDTO) => {
    // 更新本地状态中对应的RAG
    setInstalledRags(prev => 
      prev.map(rag => 
        rag.id === updatedUserRag.id ? updatedUserRag : rag
      )
    )
    
    // 如果当前查看详情的RAG被更新，也要更新详情状态
    if (ragToViewDetails?.id === updatedUserRag.id) {
      setRagToViewDetails(updatedUserRag)
    }
  }

  // 处理卸载RAG
  const handleUninstallRag = async () => {
    if (!ragToUninstall) return

    try {
      setIsUninstalling(true)
      const response = await uninstallRagVersionWithToast(ragToUninstall.ragVersionId)

      if (response.code === 200) {
        // 重新加载当前页
        loadInstalledRags(pageData.current, debouncedQuery)
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsUninstalling(false)
      setRagToUninstall(null)
    }
  }


  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return
    loadInstalledRags(page, debouncedQuery)
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
  const displayedRags = installedRags.slice(0, 4)
  const hasMore = installedRags.length > 4

  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      {/* 操作栏 */}
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-blue-50 p-1.5 rounded-md text-blue-500 mr-2">
              <Download className="h-5 w-5" />
            </span>
            我安装的知识库
          </h2>
          <p className="text-sm text-muted-foreground mt-1">
            管理您安装的RAG版本快照，可以在对话中使用
          </p>
        </div>
      </div>

      {/* 搜索栏 */}
      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          type="search"
          placeholder="搜索已安装的知识库..."
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
          <Button variant="outline" onClick={() => loadInstalledRags(1, debouncedQuery)}>
            <RefreshCw className="mr-2 h-4 w-4" />
            重试
          </Button>
        </div>
      ) : installedRags.length === 0 ? (
        // 空状态
        <div className="text-center py-12 bg-blue-50 rounded-lg border border-blue-100">
          <Download className="h-12 w-12 mx-auto text-blue-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">
            {searchQuery ? "未找到匹配的知识库" : "还没有安装任何知识库"}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchQuery ? "尝试使用不同的搜索词" : "浏览下方推荐知识库，安装感兴趣的知识库"}
          </p>
        </div>
      ) : (
        // 已安装RAG列表
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
            {displayedRags.map((userRag) => (
              <InstalledRagCard 
                key={userRag.id}
                userRag={userRag}
                onUninstall={setRagToUninstall}
                onCardClick={setRagToViewDetails}
                onVersionSwitch={handleVersionSwitch}
                currentUserId={currentUserId}
              />
            ))}
          </div>

          {/* 查看更多按钮 */}
          {hasMore && (
            <div className="flex justify-center mt-4">
              <Button
                variant="outline"
                onClick={() => loadInstalledRags(1, debouncedQuery)}
              >
                查看全部 ({installedRags.length})
              </Button>
            </div>
          )}
        </>
      )}

      {/* 详情对话框 */}
      <InstalledRagDetailDialog
        open={!!ragToViewDetails}
        onOpenChange={(open) => !open && setRagToViewDetails(null)}
        userRag={ragToViewDetails}
        onUninstall={setRagToUninstall}
        onVersionSwitch={handleVersionSwitch}
        currentUserId={currentUserId}
      />

      {/* 卸载确认对话框 */}
      <Dialog open={!!ragToUninstall} onOpenChange={(open) => !open && setRagToUninstall(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认卸载</DialogTitle>
            <DialogDescription>
              您确定要卸载知识库 "{ragToUninstall?.name}" 吗？卸载后将无法在对话中使用该知识库。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRagToUninstall(null)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleUninstallRag} disabled={isUninstalling}>
              {isUninstalling ? "卸载中..." : "确认卸载"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}