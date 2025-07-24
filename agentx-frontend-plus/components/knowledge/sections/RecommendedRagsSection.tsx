"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Search, RefreshCw, X, Store, TrendingUp } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

import { 
  getMarketRagVersionsWithToast
} from "@/lib/rag-publish-service"
import type { RagMarketDTO, PageResponse } from "@/types/rag-publish"
import { MarketRagCard } from "../cards/MarketRagCard"
import { InstallRagDialog } from "../dialogs/InstallRagDialog"
import { MarketRagDetailDialog } from "../dialogs/MarketRagDetailDialog"

export function RecommendedRagsSection() {
  const [marketRags, setMarketRags] = useState<RagMarketDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [sortBy, setSortBy] = useState<string>("publishedAt")
  const [ragToInstall, setRagToInstall] = useState<RagMarketDTO | null>(null)
  const [ragToViewDetails, setRagToViewDetails] = useState<RagMarketDTO | null>(null)
  
  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<RagMarketDTO>>({
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

  // 获取市场RAG列表
  useEffect(() => {
    loadMarketRags(1, debouncedQuery)
  }, [debouncedQuery, sortBy])

  // 加载市场RAG
  const loadMarketRags = async (page: number = 1, keyword?: string) => {
    try {
      setLoading(true)
      setError(null)

      const response = await getMarketRagVersionsWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      })

      if (response.code === 200) {
        setPageData(response.data)
        
        // 根据排序方式处理数据
        let sortedRags = [...response.data.records]
        switch (sortBy) {
          case "installCount":
            sortedRags.sort((a, b) => b.installCount - a.installCount)
            break
          case "publishedAt":
            sortedRags.sort((a, b) => new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime())
            break
          case "fileCount":
            sortedRags.sort((a, b) => b.fileCount - a.fileCount)
            break
          case "rating":
            sortedRags.sort((a, b) => (b.rating || 0) - (a.rating || 0))
            break
          default:
            break
        }
        
        setMarketRags(sortedRags)
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

  // 处理安装成功
  const handleInstallSuccess = () => {
    // 更新当前页面数据，标记为已安装
    if (ragToInstall) {
      setMarketRags(rags => 
        rags.map(rag => 
          rag.id === ragToInstall.id 
            ? { ...rag, isInstalled: true, installCount: rag.installCount + 1 } 
            : rag
        )
      )
    }
    setRagToInstall(null)
  }

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return
    loadMarketRags(page, debouncedQuery)
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

  // 获取排序选项标签
  const getSortLabel = (value: string) => {
    switch (value) {
      case "publishedAt":
        return "最新发布"
      case "installCount":
        return "安装数量"
      case "fileCount":
        return "文件数量"
      case "rating":
        return "评分"
      default:
        return "默认排序"
    }
  }

  // 显示的数据集数量
  const displayedRags = marketRags.slice(0, 8)
  const hasMore = marketRags.length > 8

  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      {/* 操作栏 */}
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-green-50 p-1.5 rounded-md text-green-500 mr-2">
              <Store className="h-5 w-5" />
            </span>
            推荐知识库
          </h2>
          <p className="text-sm text-muted-foreground mt-1">
            发现并安装其他用户分享的优质知识库
          </p>
        </div>
      </div>

      {/* 知识库市场横幅 */}
      <div className="mb-6 rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 p-5 border border-green-100">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-4">
            <div className="rounded-full bg-white/80 p-3 hidden sm:flex shadow-sm">
              <Store className="h-5 w-5 text-green-500" />
            </div>
            <div>
              <h3 className="font-medium text-lg">探索知识库市场</h3>
              <p className="text-sm text-muted-foreground">发现更多优质知识库，扩展您的AI知识能力</p>
            </div>
          </div>
          <Button asChild className="shadow-sm">
            <Link href="/knowledge?tab=recommended">
              <Search className="mr-2 h-4 w-4" />
              浏览全部
            </Link>
          </Button>
        </div>
      </div>

      {/* 搜索和筛选栏 */}
      <div className="flex gap-4 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索知识库..."
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
        <Select value={sortBy} onValueChange={setSortBy}>
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="排序方式" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="publishedAt">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-4 w-4" />
                最新发布
              </div>
            </SelectItem>
            <SelectItem value="installCount">安装数量</SelectItem>
            <SelectItem value="fileCount">文件数量</SelectItem>
            <SelectItem value="rating">评分</SelectItem>
          </SelectContent>
        </Select>
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
                <Skeleton className="h-5 w-16" />
              </div>
              <Skeleton className="h-4 w-full mb-2" />
              <Skeleton className="h-4 w-3/4 mb-2" />
              <div className="flex gap-1 mb-2">
                <Skeleton className="h-5 w-12" />
                <Skeleton className="h-5 w-16" />
                <Skeleton className="h-5 w-14" />
              </div>
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
          <Button variant="outline" onClick={() => loadMarketRags(1, debouncedQuery)}>
            <RefreshCw className="mr-2 h-4 w-4" />
            重试
          </Button>
        </div>
      ) : marketRags.length === 0 ? (
        // 空状态
        <div className="text-center py-12 bg-green-50 rounded-lg border border-green-100">
          <Store className="h-12 w-12 mx-auto text-green-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">
            {searchQuery ? "未找到匹配的知识库" : "暂无推荐知识库"}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchQuery ? "尝试使用不同的搜索词" : "当前市场中还没有发布的知识库"}
          </p>
        </div>
      ) : (
        // 市场RAG列表
        <>
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
            {/* 只显示前8个作为推荐 */}
            {displayedRags.map((ragMarket) => (
              <MarketRagCard 
                key={ragMarket.id}
                ragMarket={ragMarket}
                onInstall={setRagToInstall}
                onViewDetails={setRagToViewDetails}
              />
            ))}
          </div>

          {/* 查看更多按钮 */}
          {hasMore && (
            <div className="flex justify-center mt-4">
              <Button
                variant="outline"
                onClick={() => loadMarketRags(1, debouncedQuery)}
              >
                查看更多推荐 ({marketRags.length - 8})
              </Button>
            </div>
          )}
        </>
      )}

      {/* 安装RAG对话框 */}
      <InstallRagDialog
        open={!!ragToInstall}
        onOpenChange={(open) => !open && setRagToInstall(null)}
        ragMarket={ragToInstall}
        onSuccess={handleInstallSuccess}
      />

      {/* 查看详情对话框 */}
      <MarketRagDetailDialog
        open={!!ragToViewDetails}
        onOpenChange={(open) => !open && setRagToViewDetails(null)}
        ragMarket={ragToViewDetails}
        onInstall={setRagToInstall}
        onInstallSuccess={handleInstallSuccess}
      />
    </div>
  )
}