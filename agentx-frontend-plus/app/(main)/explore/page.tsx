"use client"

import { useEffect, useState } from "react"
import { Bot, Search, Plus } from "lucide-react"
import { Metadata } from "next"
import { redirect } from "next/navigation"

import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"
import { getPublishedAgents } from "@/lib/agent-service"
import type { AgentVersion } from "@/types/agent"
import { Sidebar } from "@/components/sidebar"
import Link from "next/link"

export default function ExplorePage() {
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [agents, setAgents] = useState<AgentVersion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState("推荐")

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

  // 根据类型过滤助理
  const getFilteredAgents = (tab: string) => {
    if (tab === "推荐") return agents

    // 将标签名称映射到 agentType 或其他属性
    return agents.filter((agent) => {
      if (tab === "Agent" && agent.agentType === 2) return true
      if (tab === "助手" && agent.agentType === 1) return true
      // 其他标签可以根据需要添加更多过滤条件
      return false
    })
  }

  return (
    <div className="flex h-[calc(100vh-3.5rem)] w-full">
      {/* 左侧边栏 */}
      <Sidebar />

      {/* 右侧内容区域 */}
      <div className="flex-1 overflow-auto">
        <div className="container py-6 px-4">
          <div className="mb-6">
            <h1 className="text-2xl font-bold tracking-tight text-blue-600">探索 AgentX 的应用</h1>
            <p className="text-muted-foreground mt-1">使用这些模板应用程序，或根据模板自定义您自己的应用程序。</p>
          </div>

          <Tabs defaultValue="推荐" className="space-y-6" value={activeTab} onValueChange={setActiveTab}>
            <div className="flex justify-between items-center flex-wrap gap-4">
              <TabsList className="overflow-x-auto flex-nowrap max-w-full">
                <TabsTrigger value="推荐" className="flex items-center gap-1 whitespace-nowrap">
                  <Search className="h-4 w-4" />
                  推荐
                </TabsTrigger>
                <TabsTrigger value="Agent" className="whitespace-nowrap">
                  Agent
                </TabsTrigger>
                <TabsTrigger value="助手" className="whitespace-nowrap">
                  助手
                </TabsTrigger>
                <TabsTrigger value="DeepSeek" className="whitespace-nowrap">
                  DeepSeek
                </TabsTrigger>
                <TabsTrigger value="媒体" className="whitespace-nowrap">
                  媒体
                </TabsTrigger>
                <TabsTrigger value="工作流" className="whitespace-nowrap">
                  工作流
                </TabsTrigger>
                <TabsTrigger value="写作" className="whitespace-nowrap">
                  写作
                </TabsTrigger>
              </TabsList>

              <div className="relative w-full md:w-auto">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  type="search"
                  placeholder="搜索..."
                  className="pl-8 w-full md:w-[250px]"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </div>

            {["推荐", "Agent", "助手", "DeepSeek", "媒体", "工作流", "写作"].map((tab) => (
              <TabsContent key={tab} value={tab} className="space-y-6">
                {loading ? (
                  // 加载状态
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {Array.from({ length: 6 }).map((_, index) => (
                      <Card key={index} className="p-4">
                        <div className="flex items-start gap-3 mb-3">
                          <Skeleton className="h-12 w-12 rounded-lg" />
                          <div className="flex-1">
                            <Skeleton className="h-5 w-32 mb-1" />
                            <Skeleton className="h-4 w-16" />
                          </div>
                        </div>
                        <Skeleton className="h-4 w-full mb-2" />
                        <Skeleton className="h-4 w-3/4" />
                      </Card>
                    ))}
                  </div>
                ) : error ? (
                  // 错误状态
                  <div className="text-center py-10">
                    <div className="text-red-500 mb-4">{error}</div>
                    <Button variant="outline" onClick={() => window.location.reload()}>
                      重试
                    </Button>
                  </div>
                ) : getFilteredAgents(tab).length === 0 ? (
                  // 空状态
                  <div className="text-center py-16 border rounded-lg bg-gray-50">
                    <Search className="h-12 w-12 mx-auto text-gray-400 mb-4" />
                    <h3 className="text-lg font-medium mb-2">
                      {searchQuery ? "未找到匹配的助理" : `暂无${tab}类型的助理`}
                    </h3>
                    <p className="text-muted-foreground mb-6">
                      {searchQuery ? "尝试使用不同的搜索词" : "敬请期待更多内容"}
                    </p>
                  </div>
                ) : (
                  // 助理列表
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {getFilteredAgents(tab).map((agent) => (
                      <Card key={agent.id} className="group relative">
                        <div className="p-5">
                          <div className="flex items-start gap-3 mb-3">
                            <div className="w-12 h-12 rounded-lg overflow-hidden bg-amber-100 flex items-center justify-center shrink-0">
                              {agent.avatar ? (
                                <img
                                  src={agent.avatar || "/placeholder.svg"}
                                  alt={agent.name}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <Bot className="h-6 w-6 text-amber-500" />
                              )}
                            </div>
                            <div>
                              <h3 className="text-lg font-semibold leading-tight">{agent.name}</h3>
                              <div className="text-xs text-muted-foreground uppercase font-medium mt-1">AGENT</div>
                            </div>
                          </div>
                          <p className="text-sm text-gray-600 line-clamp-3">{agent.description || "无描述"}</p>

                          <div className="mt-4 opacity-0 group-hover:opacity-100 transition-opacity">
                            <Button className="w-full bg-blue-500 hover:bg-blue-600 text-white" asChild>
                              <Link href={`/explore/chat/${agent.agentId}`}>
                                <Plus className="h-4 w-4 mr-2" />
                                添加到工作区
                              </Link>
                            </Button>
                          </div>
                        </div>
                      </Card>
                    ))}
                  </div>
                )}
              </TabsContent>
            ))}
          </Tabs>
        </div>
      </div>
    </div>
  )
}

