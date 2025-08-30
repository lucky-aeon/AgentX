"use client"

import { useState, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import Link from "next/link"
import { ArrowLeft, MessageSquare, Clock, Zap, TrendingUp, AlertCircle, Archive } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  getAgentSessionTraceStatisticsWithToast,
  type SessionTraceStatistics
} from "@/lib/agent-trace-service"

export default function AgentSessionsPage() {
  const params = useParams()
  const router = useRouter()
  const agentId = params.agentId as string

  const [sessions, setSessions] = useState<SessionTraceStatistics[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [showArchived, setShowArchived] = useState(false)
  const [filteredSessions, setFilteredSessions] = useState<SessionTraceStatistics[]>([])
  const [agentName, setAgentName] = useState("")

  // 加载会话追踪统计数据
  useEffect(() => {
    async function loadSessionTraceStatistics() {
      if (!agentId) return

      try {
        setLoading(true)
        const response = await getAgentSessionTraceStatisticsWithToast(agentId, {
          includeArchived: true // 获取所有会话，前端控制显示
        })

        if (response.code === 200) {
          setSessions(response.data)
          // 设置 Agent 名称（从第一个会话中获取）
          if (response.data.length > 0) {
            setAgentName(response.data[0].agentName)
          }
        }
      } catch (error) {
 
      } finally {
        setLoading(false)
      }
    }

    loadSessionTraceStatistics()
  }, [agentId])

  // 搜索和归档过滤
  useEffect(() => {
    let filtered = sessions

    // 归档过滤
    if (!showArchived) {
      filtered = filtered.filter(session => !session.isArchived)
    }

    // 搜索过滤
    if (searchQuery.trim()) {
      filtered = filtered.filter(session =>
        session.sessionTitle.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }

    setFilteredSessions(filtered)
  }, [searchQuery, showArchived, sessions])

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

  // 格式化执行时间（毫秒转秒）
  const formatExecutionTime = (timeMs: number) => {
    if (timeMs < 1000) {
      return timeMs + 'ms'
    }
    return (timeMs / 1000).toFixed(1) + 's'
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
          <Skeleton className="h-10 w-32 mb-4" />
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-64" />
        </div>

        <div className="mb-6 space-y-4">
          <Skeleton className="h-10 w-80" />
          <Skeleton className="h-5 w-32" />
        </div>

        <div className="space-y-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      </div>
    )
  }

  if (sessions.length === 0) {
    return (
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <Button 
            variant="ghost" 
            onClick={() => router.back()}
            className="mb-4"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            返回
          </Button>
          <h1 className="text-3xl font-bold mb-2">{agentName || "Agent"} 会话追踪</h1>
          <p className="text-muted-foreground">
            查看该 Agent 的所有会话执行统计
          </p>
        </div>

        <div className="flex flex-col items-center justify-center py-16">
          <MessageSquare className="h-16 w-16 text-muted-foreground mb-4" />
          <h2 className="text-xl font-semibold mb-2">暂无会话记录</h2>
          <p className="text-muted-foreground text-center max-w-md">
            该 Agent 还没有会话执行记录。
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <Button 
          variant="ghost" 
          onClick={() => router.back()}
          className="mb-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          返回
        </Button>
        <h1 className="text-3xl font-bold mb-2">{agentName} 会话追踪</h1>
        <p className="text-muted-foreground">
          查看该 Agent 的所有会话执行统计
        </p>
      </div>

      {/* 搜索和过滤 */}
      <div className="mb-6 space-y-4">
        <Input
          placeholder="搜索会话..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="max-w-md"
        />
        
        <div className="flex items-center space-x-2">
          <Checkbox
            id="show-archived"
            checked={showArchived}
            onCheckedChange={setShowArchived}
          />
          <label htmlFor="show-archived" className="text-sm">
            显示已归档会话
          </label>
        </div>
      </div>

      {/* 会话列表表格 */}
      <Card>
        <CardHeader>
          <CardTitle>会话列表 ({filteredSessions.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>会话名称</TableHead>
                <TableHead>执行次数</TableHead>
                <TableHead>成功率</TableHead>
                <TableHead>Token 数</TableHead>
                <TableHead>工具调用</TableHead>
                <TableHead>执行时间</TableHead>
                <TableHead>最后执行</TableHead>
                <TableHead>状态</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredSessions.map((session) => (
                <TableRow 
                  key={session.sessionId}
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => router.push(`/traces/agents/${agentId}/sessions/${session.sessionId}`)}
                >
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      {session.isArchived && (
                        <Archive className="h-4 w-4 text-muted-foreground" />
                      )}
                      <span className={session.isArchived ? "text-muted-foreground" : ""}>
                        {session.sessionTitle}
                      </span>
                    </div>
                  </TableCell>
                  
                  <TableCell>
                    <div className="flex items-center space-x-1">
                      <MessageSquare className="h-4 w-4 text-blue-600" />
                      <span>{formatNumber(session.totalExecutions)}</span>
                    </div>
                  </TableCell>
                  
                  <TableCell>
                    <div className="flex items-center space-x-1">
                      <TrendingUp className={`h-4 w-4 ${getSuccessRateColor(session.successRate)}`} />
                      <span className={getSuccessRateColor(session.successRate)}>
                        {formatSuccessRate(session.successRate)}
                      </span>
                    </div>
                  </TableCell>
                  
                  <TableCell>
                    <div className="flex items-center space-x-1">
                      <Zap className="h-4 w-4 text-green-600" />
                      <span>{formatNumber(session.totalTokens)}</span>
                    </div>
                  </TableCell>
                  
                  <TableCell>
                    {session.totalToolCalls > 0 ? (
                      <span>{formatNumber(session.totalToolCalls)} 次</span>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  
                  <TableCell>
                    <div className="flex items-center space-x-1">
                      <Clock className="h-4 w-4 text-gray-600" />
                      <span>{formatExecutionTime(session.totalExecutionTime)}</span>
                    </div>
                  </TableCell>
                  
                  <TableCell className="text-sm">
                    {formatTime(session.lastExecutionTime)}
                  </TableCell>
                  
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Badge variant={session.lastExecutionSuccess ? "default" : "destructive"}>
                        {session.lastExecutionSuccess ? "正常" : "异常"}
                      </Badge>
                      {session.failedExecutions > 0 && (
                        <div className="flex items-center space-x-1 text-orange-600">
                          <AlertCircle className="h-3 w-3" />
                          <span className="text-xs">{session.failedExecutions}</span>
                        </div>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* 无搜索结果 */}
          {filteredSessions.length === 0 && (searchQuery || !showArchived) && (
            <div className="flex flex-col items-center justify-center py-8">
              <MessageSquare className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-medium mb-2">未找到相关会话</h3>
              <p className="text-muted-foreground">
                {searchQuery ? "尝试使用其他关键词搜索" : "尝试显示已归档会话"}
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}