"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Bot, Plus, Search, RefreshCw } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { getSessions, getSessionsWithToast } from "@/lib/api-services"
import { toast } from "@/components/ui/use-toast"
import type { Session } from "@/types/conversation"

// 将会话转换为代理
function sessionToAgent(session: Session) {
  return {
    id: session.id,
    name: session.title,
    avatar: session.title.charAt(0).toUpperCase(),
    lastMessage: session.description || "有什么可以帮您的？",
    updatedAt: session.updatedAt,
  }
}

export function AgentSidebar() {
  const pathname = usePathname()
  const [searchQuery, setSearchQuery] = useState("")
  const [agents, setAgents] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  // 获取会话列表并转换为代理
  const fetchAgents = async () => {
    try {
      setLoading(true)
      setError(null)
      // 使用固定的userId=1
      const response = await getSessionsWithToast({ userId: "1" })
      if (response.code === 200) {
        // 将会话转换为代理
        const agentList = response.data.filter((session) => !session.archived).map(sessionToAgent)
        setAgents(agentList)
      } else {
        setError(response.message || "获取代理列表失败")
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "未知错误"
      console.error("获取代理列表错误:", error)
      setError(`获取代理列表失败: ${errorMessage}`)
    } finally {
      setLoading(false)
    }
  }

  // 重试获取代理列表
  const retryFetchAgents = () => {
    setRetryCount((prev) => prev + 1)
  }

  // 过滤代理列表
  const filteredAgents = agents.filter((agent) => agent.name.toLowerCase().includes(searchQuery.toLowerCase()))

  // 初始加载时获取代理列表
  useEffect(() => {
    fetchAgents()
  }, [retryCount])

  return (
    <div className="w-[300px] border-r flex flex-col h-[calc(100vh-3.5rem)]">
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">我的代理</h2>
          <Button size="icon" variant="ghost">
            <Plus className="h-4 w-4" />
            <span className="sr-only">添加新代理</span>
          </Button>
        </div>
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索代理..."
            className="pl-8"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>
      <ScrollArea className="flex-1">
        <div className="p-2">
          {loading ? (
            // 加载状态显示骨架屏
            Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="flex items-center gap-3 rounded-lg px-3 py-2 mb-2">
                <Skeleton className="h-9 w-9 rounded-full" />
                <div className="space-y-1 flex-1">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/2" />
                </div>
              </div>
            ))
          ) : error ? (
            // 显示错误状态
            <div className="text-center py-8">
              <div className="text-red-500 mb-2">{error}</div>
              <Button variant="outline" size="sm" onClick={retryFetchAgents} className="flex items-center gap-1">
                <RefreshCw className="h-4 w-4" />
                重试
              </Button>
            </div>
          ) : filteredAgents.length > 0 ? (
            // 显示代理列表
            filteredAgents.map((agent) => (
              <Link
                key={agent.id}
                href={`/explore/chat/${agent.id}`}
                className={cn(
                  "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-accent",
                  pathname === `/explore/chat/${agent.id}` ? "bg-accent" : "transparent",
                )}
              >
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
                  {agent.avatar}
                </div>
                <div className="flex-1 overflow-hidden">
                  <div className="font-medium">{agent.name}</div>
                  <div className="text-xs text-muted-foreground truncate">{agent.lastMessage}</div>
                </div>
              </Link>
            ))
          ) : (
            // 没有代理时显示提示
            <div className="text-center py-8 text-muted-foreground">
              {searchQuery ? "没有找到匹配的代理" : "暂无代理"}
            </div>
          )}
        </div>
      </ScrollArea>
      <div className="p-4 border-t">
        <Button className="w-full" asChild>
          <Link href="/studio/new">
            <Bot className="mr-2 h-4 w-4" />
            创建新代理
          </Link>
        </Button>
      </div>
    </div>
  )
}

