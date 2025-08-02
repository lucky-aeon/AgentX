"use client"

import { useState, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, Clock, Zap, AlertCircle, CheckCircle, XCircle, Tool, MessageSquare, Activity } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Separator } from "@/components/ui/separator"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { toast } from "@/hooks/use-toast"
import {
  getTraceDetailWithToast,
  getExecutionDetailsWithToast
} from "@/lib/agent-trace-service"

// 追踪详情类型定义
interface TraceDetail {
  summary: {
    traceId: string
    agentId: string
    sessionId: string
    executionStartTime: string
    executionEndTime: string
    totalExecutionTime: number
    totalTokens: number
    totalInputTokens: number
    totalOutputTokens: number
    toolCallCount: number
    totalCost: number
    executionSuccess: boolean
    errorPhase?: string
    errorMessage?: string
  }
  details: ExecutionDetail[]
}

interface ExecutionDetail {
  id: string
  traceId: string
  sequenceNo: number
  stepType: string
  messageType: string
  content: string
  modelId?: string
  providerName?: string
  tokenCount?: number
  executionTime?: number
  cost?: number
  success: boolean
  errorMessage?: string
  toolName?: string
  createdAt: string
}

export default function TraceDetailPage() {
  const params = useParams()
  const router = useRouter()
  const agentId = params.agentId as string
  const sessionId = params.sessionId as string

  const [traceDetails, setTraceDetails] = useState<TraceDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedTrace, setSelectedTrace] = useState<TraceDetail | null>(null)

  // 加载追踪详情数据
  useEffect(() => {
    async function loadTraceDetails() {
      if (!sessionId) return

      try {
        setLoading(true)
        
        // 首先获取会话的所有追踪记录
        // 这里应该调用获取会话追踪列表的接口，但当前后端接口返回的是单个追踪
        // 暂时使用模拟数据结构
        toast({
          title: "提示",
          description: "追踪详情页面正在开发中，当前显示模拟数据"
        })

        // 模拟数据
        const mockTrace: TraceDetail = {
          summary: {
            traceId: "trace_123456",
            agentId,
            sessionId,
            executionStartTime: new Date().toISOString(),
            executionEndTime: new Date().toISOString(),
            totalExecutionTime: 2500,
            totalTokens: 150,
            totalInputTokens: 80,
            totalOutputTokens: 70,
            toolCallCount: 2,
            totalCost: 0.05,
            executionSuccess: true
          },
          details: [
            {
              id: "1",
              traceId: "trace_123456",
              sequenceNo: 1,
              stepType: "USER_MESSAGE",
              messageType: "TEXT",
              content: "用户输入消息",
              success: true,
              createdAt: new Date().toISOString()
            },
            {
              id: "2",
              traceId: "trace_123456",
              sequenceNo: 2,
              stepType: "AI_RESPONSE",
              messageType: "TEXT",
              content: "AI 响应消息",
              modelId: "gpt-4",
              providerName: "OpenAI",
              tokenCount: 45,
              executionTime: 1200,
              cost: 0.03,
              success: true,
              createdAt: new Date().toISOString()
            },
            {
              id: "3",
              traceId: "trace_123456",
              sequenceNo: 3,
              stepType: "TOOL_CALL",
              messageType: "TOOL_CALL",
              content: "工具调用结果",
              toolName: "search_tool",
              executionTime: 800,
              success: true,
              createdAt: new Date().toISOString()
            }
          ]
        }

        setTraceDetails([mockTrace])
        setSelectedTrace(mockTrace)

      } catch (error) {
        console.error("加载追踪详情失败:", error)
        toast({
          title: "加载失败",
          description: "无法加载追踪详情",
          variant: "destructive"
        })
      } finally {
        setLoading(false)
      }
    }

    loadTraceDetails()
  }, [sessionId, agentId])

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const time = new Date(timeStr)
    return time.toLocaleString('zh-CN')
  }

  // 格式化执行时间
  const formatExecutionTime = (timeMs: number) => {
    if (timeMs < 1000) {
      return timeMs + 'ms'
    }
    return (timeMs / 1000).toFixed(1) + 's'
  }

  // 格式化费用
  const formatCost = (cost: number) => {
    return '$' + cost.toFixed(4)
  }

  // 获取步骤图标
  const getStepIcon = (stepType: string, success: boolean) => {
    const iconClass = `h-4 w-4 ${success ? 'text-green-600' : 'text-red-600'}`
    
    switch (stepType) {
      case 'USER_MESSAGE':
        return <MessageSquare className={iconClass} />
      case 'AI_RESPONSE':
        return <Activity className={iconClass} />
      case 'TOOL_CALL':
        return <Tool className={iconClass} />
      default:
        return success ? <CheckCircle className={iconClass} /> : <XCircle className={iconClass} />
    }
  }

  // 获取步骤类型名称
  const getStepTypeName = (stepType: string) => {
    switch (stepType) {
      case 'USER_MESSAGE':
        return '用户消息'
      case 'AI_RESPONSE':
        return 'AI 响应'
      case 'TOOL_CALL':
        return '工具调用'
      default:
        return stepType
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <Skeleton className="h-10 w-32 mb-4" />
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-64" />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1">
            <Skeleton className="h-64 w-full" />
          </div>
          <div className="lg:col-span-2">
            <Skeleton className="h-96 w-full" />
          </div>
        </div>
      </div>
    )
  }

  if (!selectedTrace) {
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
          <h1 className="text-3xl font-bold mb-2">追踪详情</h1>
        </div>

        <div className="flex flex-col items-center justify-center py-16">
          <Activity className="h-16 w-16 text-muted-foreground mb-4" />
          <h2 className="text-xl font-semibold mb-2">无追踪记录</h2>
          <p className="text-muted-foreground">
            未找到相关的执行追踪记录
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
        <h1 className="text-3xl font-bold mb-2">执行链路详情</h1>
        <p className="text-muted-foreground">
          查看详细的执行步骤和性能数据
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 左侧：执行概要 */}
        <div className="lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>执行概要</span>
                <Badge variant={selectedTrace.summary.executionSuccess ? "default" : "destructive"}>
                  {selectedTrace.summary.executionSuccess ? "成功" : "失败"}
                </Badge>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <div className="text-sm text-muted-foreground">追踪 ID</div>
                <div className="font-mono text-sm">{selectedTrace.summary.traceId}</div>
              </div>
              
              <Separator />
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">开始时间</div>
                  <div className="text-sm">{formatTime(selectedTrace.summary.executionStartTime)}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">结束时间</div>
                  <div className="text-sm">{formatTime(selectedTrace.summary.executionEndTime)}</div>
                </div>
              </div>
              
              <div>
                <div className="text-sm text-muted-foreground">总执行时间</div>
                <div className="flex items-center space-x-1">
                  <Clock className="h-4 w-4 text-gray-600" />
                  <span>{formatExecutionTime(selectedTrace.summary.totalExecutionTime)}</span>
                </div>
              </div>
              
              <Separator />
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">总 Token</div>
                  <div className="flex items-center space-x-1">
                    <Zap className="h-4 w-4 text-green-600" />
                    <span>{selectedTrace.summary.totalTokens}</span>
                  </div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">工具调用</div>
                  <div className="flex items-center space-x-1">
                    <Tool className="h-4 w-4 text-purple-600" />
                    <span>{selectedTrace.summary.toolCallCount} 次</span>
                  </div>
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">输入 Token</div>
                  <div>{selectedTrace.summary.totalInputTokens}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">输出 Token</div>
                  <div>{selectedTrace.summary.totalOutputTokens}</div>
                </div>
              </div>
              
              <div>
                <div className="text-sm text-muted-foreground">总费用</div>
                <div>{formatCost(selectedTrace.summary.totalCost)}</div>
              </div>
              
              {!selectedTrace.summary.executionSuccess && selectedTrace.summary.errorMessage && (
                <>
                  <Separator />
                  <div>
                    <div className="text-sm text-muted-foreground text-red-600">错误信息</div>
                    <div className="text-sm text-red-600">{selectedTrace.summary.errorMessage}</div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        </div>

        {/* 右侧：执行步骤详情 */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>执行步骤</CardTitle>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>序号</TableHead>
                    <TableHead>步骤类型</TableHead>
                    <TableHead>内容</TableHead>
                    <TableHead>详情</TableHead>
                    <TableHead>耗时</TableHead>
                    <TableHead>状态</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {selectedTrace.details.map((detail) => (
                    <TableRow key={detail.id}>
                      <TableCell>{detail.sequenceNo}</TableCell>
                      
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          {getStepIcon(detail.stepType, detail.success)}
                          <span>{getStepTypeName(detail.stepType)}</span>
                        </div>
                      </TableCell>
                      
                      <TableCell>
                        <div className="max-w-xs truncate" title={detail.content}>
                          {detail.content}
                        </div>
                      </TableCell>
                      
                      <TableCell>
                        <div className="text-sm space-y-1">
                          {detail.modelId && (
                            <div>模型: {detail.modelId}</div>
                          )}
                          {detail.providerName && (
                            <div>提供商: {detail.providerName}</div>
                          )}
                          {detail.toolName && (
                            <div>工具: {detail.toolName}</div>
                          )}
                          {detail.tokenCount && (
                            <div>Token: {detail.tokenCount}</div>
                          )}
                          {detail.cost && (
                            <div>费用: {formatCost(detail.cost)}</div>
                          )}
                        </div>
                      </TableCell>
                      
                      <TableCell>
                        {detail.executionTime ? (
                          <div className="flex items-center space-x-1">
                            <Clock className="h-3 w-3 text-gray-600" />
                            <span className="text-sm">{formatExecutionTime(detail.executionTime)}</span>
                          </div>
                        ) : (
                          <span className="text-muted-foreground">-</span>
                        )}
                      </TableCell>
                      
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          {detail.success ? (
                            <CheckCircle className="h-4 w-4 text-green-600" />
                          ) : (
                            <XCircle className="h-4 w-4 text-red-600" />
                          )}
                          {detail.errorMessage && (
                            <div className="flex items-center space-x-1 text-red-600">
                              <AlertCircle className="h-3 w-3" />
                              <span className="text-xs" title={detail.errorMessage}>错误</span>
                            </div>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}