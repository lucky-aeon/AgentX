"use client"

import { DropdownMenuItem } from "@/components/ui/dropdown-menu"
import { Metadata } from "next"
import { redirect } from "next/navigation"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Bot, Edit, MoreHorizontal, Plus, Trash, Search, RefreshCw, Power, PowerOff, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { toast } from "@/hooks/use-toast"
import { 
  getUserAgents, 
  deleteAgent, 
  toggleAgentStatus,
  deleteAgentWithToast,
  toggleAgentStatusWithToast,
  getUserAgentsWithToast
} from "@/lib/agent-service"
import type { Agent } from "@/types/agent"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

export default function StudioPage() {
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [agentToDelete, setAgentToDelete] = useState<Agent | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState<string | null>(null)

  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // 获取助理列表
  useEffect(() => {
    async function fetchAgents() {
      try {
        setLoading(true)
        setError(null)

        const response = await getUserAgentsWithToast({ name: debouncedQuery })

        if (response.code === 200) {
          setAgents(response.data)
        } else {
          setError(response.message)
          // toast已由withToast处理
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        setError(errorMessage)
        // toast已由withToast处理
      } finally {
        setLoading(false)
      }
    }

    fetchAgents()
  }, [debouncedQuery])

  // 处理删除助理
  const handleDeleteAgent = async () => {
    if (!agentToDelete) return

    try {
      setIsDeleting(true)
      const response = await deleteAgentWithToast(agentToDelete.id)

      if (response.code === 200) {
        // toast已由withToast处理
        // 更新列表，移除已删除的助理
        setAgents(agents.filter((agent) => agent.id !== agentToDelete.id))
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsDeleting(false)
      setAgentToDelete(null)
    }
  }

  // 处理切换助理状态
  const handleToggleStatus = async (agent: Agent) => {
    try {
      setIsTogglingStatus(agent.id)
      const response = await toggleAgentStatusWithToast(agent.id)

      if (response.code === 200) {
        // toast已由withToast处理
        // 更新列表中的助理状态
        setAgents(agents.map((a) => (a.id === agent.id ? response.data : a)))
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsTogglingStatus(null)
    }
  }

  // 获取助理类型文本
  const getAgentTypeText = (type: number) => {
    return type === 1 ? "聊天助理" : "功能性助理"
  }

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">工作室</h1>
          <p className="text-muted-foreground">创建和管理您的 AI 助理</p>
        </div>
        <Button asChild>
          <Link href="/studio/new">
            <Plus className="mr-2 h-4 w-4" />
            创建新助理
          </Link>
        </Button>
      </div>

      {/* 搜索栏 */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索助理..."
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
          <Button variant="outline" onClick={() => window.location.reload()}>
            <RefreshCw className="mr-2 h-4 w-4" />
            重试
          </Button>
        </div>
      ) : agents.length === 0 ? (
        // 空状态
        <div className="text-center py-16 border rounded-lg bg-gray-50">
          <Bot className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">{searchQuery ? "未找到匹配的助理" : "还没有创建任何助理"}</h3>
          <p className="text-muted-foreground mb-6">
            {searchQuery ? "尝试使用不同的搜索词" : "创建您的第一个AI助理，开始智能对话"}
          </p>
          {!searchQuery && (
            <Button asChild>
              <Link href="/studio/new">
                <Plus className="mr-2 h-4 w-4" />
                创建新助理
              </Link>
            </Button>
          )}
        </div>
      ) : (
        // 助理列表
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {agents.map((agent) => (
            <Card key={agent.id}>
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-primary-foreground overflow-hidden">
                      {agent.avatar ? (
                        <img
                          src={agent.avatar || "/placeholder.svg"}
                          alt={agent.name}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        <Bot className="h-4 w-4" />
                      )}
                    </div>
                    <CardTitle className="text-base">{agent.name}</CardTitle>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                        <span className="sr-only">打开菜单</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuLabel>操作</DropdownMenuLabel>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem asChild>
                        <Link href={`/studio/edit/${agent.id}`}>
                          <Edit className="mr-2 h-4 w-4" />
                          编辑
                        </Link>
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handleToggleStatus(agent)}>
                        {isTogglingStatus === agent.id ? (
                          <>
                            <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                            处理中...
                          </>
                        ) : agent.enabled ? (
                          <>
                            <PowerOff className="mr-2 h-4 w-4" />
                            禁用
                          </>
                        ) : (
                          <>
                            <Power className="mr-2 h-4 w-4" />
                            启用
                          </>
                        )}
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-red-600" onClick={() => setAgentToDelete(agent)}>
                        <Trash className="mr-2 h-4 w-4" />
                        删除
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
                <CardDescription className="text-xs flex items-center gap-2 mt-1">
                  <span>{getAgentTypeText(agent.agentType)}</span>
                  <span className="w-1 h-1 rounded-full bg-gray-300"></span>
                  <span>{agent.enabled ? "已启用" : "已禁用"}</span>
                  <span className="w-1 h-1 rounded-full bg-gray-300"></span>
                  <span>更新于 {new Date(agent.updatedAt).toLocaleDateString()}</span>
                </CardDescription>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">{agent.description || "无描述"}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* 删除确认对话框 */}
      <Dialog open={!!agentToDelete} onOpenChange={(open) => !open && setAgentToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>您确定要删除助理 "{agentToDelete?.name}" 吗？此操作无法撤销。</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAgentToDelete(null)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteAgent} disabled={isDeleting}>
              {isDeleting ? "删除中..." : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

