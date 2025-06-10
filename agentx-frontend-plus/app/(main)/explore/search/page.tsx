"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { Bot, Search, MessageCircle, Zap, Filter, X } from "lucide-react"
import { Metadata } from "next"
import { redirect } from "next/navigation"
import { toast } from "@/hooks/use-toast"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { getPublishedAgents } from "@/lib/agent-service"
import type { AgentVersion } from "@/types/agent"

export default function SearchPage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [agents, setAgents] = useState<AgentVersion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState("all")

  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // 获取已发布的助理列表
  useEffect(() => {
    async function fetchAgents() {
      try {
        setLoading(true)
        setError(null)

        const response = await getPublishedAgents(debouncedQuery)

        if (response.code === 200) {
          setAgents(response.data)
        } else {
          setError(response.message)
          toast({
            title: "获取助理列表失败",
            description: response.message,
            variant: "destructive",
          })
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        setError(errorMessage)
        toast({
          title: "获取助理列表失败",
          description: errorMessage,
          variant: "destructive",
        })
      } finally {
        setLoading(false)
      }
    }

    fetchAgents()
  }, [debouncedQuery])

  // 根据类型过滤助理 - 简化为显示所有助理
  const filteredAgents = agents.filter(() => true)

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">探索助理</h1>
          <p className="text-muted-foreground">发现并使用各种AI助理</p>
        </div>
      </div>

      <div className="flex flex-col gap-6">
        {/* 搜索栏 */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1">
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
          <Button variant="outline" size="icon">
            <Filter className="h-4 w-4" />
            <span className="sr-only">筛选</span>
          </Button>
        </div>

        {/* 助理列表 */}
        <div className="mt-6">
          {renderAgentList(filteredAgents, loading, error)}
        </div>
      </div>
    </div>
  )
}

// 渲染助理列表
function renderAgentList(agents: AgentVersion[], loading: boolean, error: string | null) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, index) => (
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
    )
  }

  if (error) {
    return (
      <div className="text-center py-10">
        <div className="text-red-500 mb-4">{error}</div>
        <Button variant="outline" onClick={() => window.location.reload()}>
          重试
        </Button>
      </div>
    )
  }

  if (agents.length === 0) {
    return (
      <div className="text-center py-16 border rounded-lg bg-gray-50">
        <Search className="h-12 w-12 mx-auto text-gray-400 mb-4" />
        <h3 className="text-lg font-medium mb-2">未找到助理</h3>
        <p className="text-muted-foreground mb-6">尝试使用不同的搜索词或浏览全部助理</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {agents.map((agent) => (
        <Card key={agent.id} className="overflow-hidden">
          <CardHeader className="pb-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground overflow-hidden">
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
                <div>
                  <CardTitle className="text-base">{agent.name}</CardTitle>
                  <div className="flex items-center gap-1 mt-1">
                    <Badge variant="outline" className="text-[10px]">
                      助理
                    </Badge>
                    <Badge variant="outline" className="text-[10px]">
                      v{agent.versionNumber}
                    </Badge>
                  </div>
                </div>
              </div>
              <MessageCircle className="h-4 w-4 text-blue-500" />
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground line-clamp-3">{agent.description || "无描述"}</p>
          </CardContent>
          <CardFooter className="flex justify-between">
            <Button variant="outline" size="sm">
              详情
            </Button>
            <Button size="sm" asChild>
              <Link href={`/explore/chat/${agent.agentId}`}>
                <MessageCircle className="mr-2 h-4 w-4" />
                开始对话
              </Link>
            </Button>
          </CardFooter>
        </Card>
      ))}
    </div>
  )
}

