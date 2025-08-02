"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Activity, Clock, MessageSquare, Zap, TrendingUp, AlertCircle } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  getUserAgentTraceStatisticsWithToast,
  type AgentTraceStatistics
} from "@/lib/agent-trace-service"

export default function TracesPage() {
  const [agents, setAgents] = useState<AgentTraceStatistics[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [filteredAgents, setFilteredAgents] = useState<AgentTraceStatistics[]>([])

  // 加载 Agent 追踪统计数据
  useEffect(() => {
    async function loadAgentTraceStatistics() {
      try {
        setLoading(true)
        const response = await getUserAgentTraceStatisticsWithToast()

        if (response.code === 200) {
          setAgents(response.data)
          setFilteredAgents(response.data)
        }
      } catch (error) {
        console.error("加载 Agent 追踪统计失败:", error)
      } finally {
        setLoading(false)
      }
    }

    loadAgentTraceStatistics()
  }, [])

  // 搜索过滤
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredAgents(agents)
    } else {
      const filtered = agents.filter(agent =>
        agent.agentName.toLowerCase().includes(searchQuery.toLowerCase())
      )
      setFilteredAgents(filtered)
    }
  }, [searchQuery, agents])

  // 格式化数字显示
  const formatNumber = (num: number) => {
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'K'
    }
    return num.toString()
  }

  // 格式化成功率
  const formatSuccessRate = (rate: number) => {
    return (rate * 100).toFixed(1) + '%'
  }

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const time = new Date(timeStr)
    return time.toLocaleString('zh-CN')
  }

  // 获取状态颜色
  const getStatusColor = (success: boolean) => {
    return success ? "text-green-600" : "text-red-600"
  }

  // 获取成功率颜色
  const getSuccessRateColor = (rate: number) => {
    if (rate >= 0.9) return "text-green-600"
    if (rate >= 0.7) return "text-yellow-600"
    return "text-red-600"
  }

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <h1 className="text-3xl font-bold mb-2">执行追踪</h1>
          <p className="text-muted-foreground">
            查看您的 Agent 执行历史和性能统计
          </p>
        </div>

        <div className="mb-6">
          <Skeleton className="h-10 w-80" />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <Card key={i}>
              <CardHeader>
                <Skeleton className="h-6 w-32" />
                <Skeleton className="h-4 w-24" />
              </CardHeader>
              <CardContent className="space-y-4">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-4 w-1/2" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    )
  }

  if (agents.length === 0) {
    return (
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <h1 className="text-3xl font-bold mb-2">执行追踪</h1>
          <p className="text-muted-foreground">
            查看您的 Agent 执行历史和性能统计
          </p>
        </div>

        <div className="flex flex-col items-center justify-center py-16">
          <Activity className="h-16 w-16 text-muted-foreground mb-4" />
          <h2 className="text-xl font-semibold mb-2">暂无执行记录</h2>
          <p className="text-muted-foreground text-center max-w-md">
            您的 Agent 还没有执行记录。开始与 Agent 对话后，执行追踪信息将会在这里显示。
          </p>
          <Link href="/studio">
            <Button className="mt-4">
              前往工作室
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">执行追踪</h1>
        <p className="text-muted-foreground">
          查看您的 Agent 执行历史和性能统计
        </p>
      </div>

      {/* 搜索框 */}
      <div className="mb-6">
        <Input
          placeholder="搜索 Agent..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="max-w-md"
        />
      </div>

      {/* Agent 卡片列表 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredAgents.map((agent) => (
          <Link key={agent.agentId} href={`/traces/agents/${agent.agentId}`}>
            <Card className="h-full hover:shadow-lg transition-shadow cursor-pointer">
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <span className="truncate">{agent.agentName}</span>
                  <Badge variant={agent.lastExecutionSuccess ? "default" : "destructive"}>
                    {agent.lastExecutionSuccess ? "正常" : "异常"}
                  </Badge>
                </CardTitle>
                <p className="text-sm text-muted-foreground">
                  最后执行: {formatTime(agent.lastExecutionTime)}
                </p>
              </CardHeader>
              
              <CardContent className="space-y-4">
                {/* 执行统计 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex items-center space-x-2">
                    <Activity className="h-4 w-4 text-blue-600" />
                    <div>
                      <div className="text-sm font-medium">{formatNumber(agent.totalExecutions)}</div>
                      <div className="text-xs text-muted-foreground">总执行</div>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <TrendingUp className={`h-4 w-4 ${getSuccessRateColor(agent.successRate)}`} />
                    <div>
                      <div className={`text-sm font-medium ${getSuccessRateColor(agent.successRate)}`}>
                        {formatSuccessRate(agent.successRate)}
                      </div>
                      <div className="text-xs text-muted-foreground">成功率</div>
                    </div>
                  </div>
                </div>

                {/* Token 和工具调用统计 */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex items-center space-x-2">
                    <Zap className="h-4 w-4 text-green-600" />
                    <div>
                      <div className="text-sm font-medium">{formatNumber(agent.totalTokens)}</div>
                      <div className="text-xs text-muted-foreground">Token 数</div>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <MessageSquare className="h-4 w-4 text-purple-600" />
                    <div>
                      <div className="text-sm font-medium">{agent.totalSessions}</div>
                      <div className="text-xs text-muted-foreground">会话数</div>
                    </div>
                  </div>
                </div>

                {/* 工具调用数 */}
                {agent.totalToolCalls > 0 && (
                  <div className="pt-2 border-t">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">工具调用</span>
                      <span className="font-medium">{formatNumber(agent.totalToolCalls)} 次</span>
                    </div>
                  </div>
                )}

                {/* 失败数提示 */}
                {agent.failedExecutions > 0 && (
                  <div className="flex items-center space-x-2 text-orange-600">
                    <AlertCircle className="h-4 w-4" />
                    <span className="text-sm">
                      {agent.failedExecutions} 次执行失败
                    </span>
                  </div>
                )}
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>

      {/* 无搜索结果 */}
      {filteredAgents.length === 0 && searchQuery && (
        <div className="flex flex-col items-center justify-center py-16">
          <Activity className="h-16 w-16 text-muted-foreground mb-4" />
          <h2 className="text-xl font-semibold mb-2">未找到相关 Agent</h2>
          <p className="text-muted-foreground">
            尝试使用其他关键词搜索
          </p>
        </div>
      )}
    </div>
  )
}