"use client"

import { useState, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, Clock, Zap, AlertCircle, CheckCircle, XCircle, Wrench, MessageSquare, Activity, RefreshCw } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Separator } from "@/components/ui/separator"
import { SmartContentDisplay } from "@/components/ui/content-display"
import { toast } from "@/hooks/use-toast"
import {
  getSessionExecutionDetailsWithToast,
  SessionExecutionDetail
} from "@/lib/agent-trace-service"



export default function TraceDetailPage() {
  const params = useParams()
  const router = useRouter()
  const agentId = params.agentId as string
  const sessionId = params.sessionId as string

  const [executionDetails, setExecutionDetails] = useState<SessionExecutionDetail[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // 加载会话执行详情数据
  useEffect(() => {
    async function loadExecutionDetails() {
      if (!sessionId) return

      try {
        setLoading(true)
        setError(null)
        
        const response = await getSessionExecutionDetailsWithToast(sessionId)
        
        if (response.code === 200) {
          setExecutionDetails(response.data || [])
        } else {
          setError(response.message)
        }

      } catch (error) {
 
        setError("加载执行详情失败")
      } finally {
        setLoading(false)
      }
    }

    loadExecutionDetails()
  }, [sessionId])

  // 格式化时间
  const formatTime = (timeStr: string | null | undefined) => {
    if (!timeStr) return '-'
    try {
      const time = new Date(timeStr)
      return time.toLocaleString('zh-CN')
    } catch (error) {
      return timeStr // 如果无法解析，直接返回原字符串
    }
  }

  // 格式化执行时间
  const formatExecutionTime = (timeMs: number | null | undefined) => {
    if (!timeMs) return '-'
    if (timeMs < 1000) {
      return timeMs + 'ms'
    }
    return (timeMs / 1000).toFixed(1) + 's'
  }

  // 格式化费用
  const formatCost = (cost: number | string | null | undefined) => {
    if (!cost) return '-'
    const numCost = typeof cost === 'string' ? parseFloat(cost) : cost
    return '$' + numCost.toFixed(4)
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
        return <Wrench className={iconClass} />
      case 'ERROR_MESSAGE':
        return <AlertCircle className="h-4 w-4 text-red-600" />
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
      case 'ERROR_MESSAGE':
        return '异常信息'
      default:
        return stepType
    }
  }

  // 获取步骤样式
  const getStepStyle = (stepType: string, success: boolean) => {
    // ERROR_MESSAGE 类型始终使用红色错误样式
    if (stepType === 'ERROR_MESSAGE') {
      return 'border-red-500 bg-red-50'
    }
    
    if (!success) {
      return 'border-red-500 bg-red-50'
    }
    
    switch (stepType) {
      case 'USER_MESSAGE':
        return 'border-blue-500 bg-blue-50'
      case 'AI_RESPONSE':
        return 'border-green-500 bg-green-50'
      case 'TOOL_CALL':
        return 'border-orange-500 bg-orange-50'
      default:
        return 'border-gray-500 bg-gray-50'
    }
  }

  // 获取卡片样式
  const getCardStyle = (stepType: string, success: boolean) => {
    // ERROR_MESSAGE 类型始终使用红色错误样式
    if (stepType === 'ERROR_MESSAGE') {
      return 'border-red-200 bg-red-50'
    }
    
    if (!success) {
      return 'border-red-200 bg-red-50'
    }
    
    switch (stepType) {
      case 'USER_MESSAGE':
        return 'border-blue-200 bg-blue-50'
      case 'AI_RESPONSE':
        return 'border-green-200 bg-green-50'
      case 'TOOL_CALL':
        return 'border-orange-200 bg-orange-50'
      default:
        return 'border-gray-200 bg-gray-50'
    }
  }

  // 获取步骤对齐方式
  const getStepAlignment = (stepType: string) => {
    switch (stepType) {
      case 'USER_MESSAGE':
        return 'justify-start' // 用户消息左对齐
      case 'AI_RESPONSE':
        return 'justify-start' // AI响应也左对齐
      case 'TOOL_CALL':
        return 'justify-start' // 工具调用左对齐
      default:
        return 'justify-start'
    }
  }

  // 获取卡片位置
  const getCardPosition = (stepType: string) => {
    return '' // 统一左对齐，通过图标颜色区分
  }

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <Skeleton className="h-10 w-32 mb-4" />
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-64" />
        </div>

        <div className="w-full">
          <Skeleton className="h-96 w-full" />
        </div>
      </div>
    )
  }

  if (error) {
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
        </div>

        <div className="flex flex-col items-center justify-center py-16">
          <AlertCircle className="h-16 w-16 text-red-500 mb-4" />
          <h2 className="text-xl font-semibold mb-2">加载失败</h2>
          <p className="text-muted-foreground">
            {error}
          </p>
        </div>
      </div>
    )
  }

  if (executionDetails.length === 0) {
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
        </div>

        <div className="flex flex-col items-center justify-center py-16">
          <Activity className="h-16 w-16 text-muted-foreground mb-4" />
          <h2 className="text-xl font-semibold mb-2">无执行记录</h2>
          <p className="text-muted-foreground">
            该会话暂无执行链路记录
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
        <h1 className="text-3xl font-bold mb-2">会话执行链路详情</h1>
        <p className="text-muted-foreground">
          查看该会话的详细执行步骤和日志信息
        </p>
        <p className="text-sm text-muted-foreground mt-2">
          会话ID: {sessionId} | 共 {executionDetails.length} 条执行记录
        </p>
      </div>

      {/* 执行步骤日志 - 时间线样式 */}
      <div className="space-y-6 max-w-4xl mx-auto">
        {executionDetails.map((detail, index) => (
          <div key={detail.id} className="relative">
            {/* 时间线连接线 */}
            {index < executionDetails.length - 1 && (
              <div className="absolute left-6 top-12 w-0.5 h-16 bg-gradient-to-b from-border to-transparent"></div>
            )}
            
            {/* 步骤卡片 */}
            <div className={`flex gap-4 ${getStepAlignment(detail.stepType)}`}>
              {/* 时间线图标 */}
              <div className="flex-shrink-0 flex flex-col items-center">
                <div className={`w-12 h-12 rounded-full border-2 flex items-center justify-center ${getStepStyle(detail.stepType, detail.success)}`}>
                  {getStepIcon(detail.stepType, detail.success)}
                </div>
              </div>
              
              {/* 内容卡片 */}
              <div className={`flex-1 max-w-2xl ${getCardPosition(detail.stepType)}`}>
                <Card className={`${getCardStyle(detail.stepType, detail.success)}`}>
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-2">
                        <span className="font-medium text-sm">{getStepTypeName(detail.stepType)}</span>
                        <Badge variant={detail.stepType === 'ERROR_MESSAGE' ? "destructive" : (detail.success ? "default" : "destructive")} className="text-xs">
                          {detail.stepType === 'ERROR_MESSAGE' ? "失败" : (detail.success ? "成功" : "失败")}
                        </Badge>
                      </div>
                      <div className="text-xs text-muted-foreground">
                        {formatTime(detail.createdAt)}
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent className="pt-0">
                    {/* 主要内容 */}
                    <div className="mb-3">
                      <SmartContentDisplay 
                        content={detail.content || "无内容"}
                        maxLength={150}
                        showCopy={true}
                        className="text-sm"
                      />
                    </div>
                    
                    {/* 详细信息 */}
                    <div className="space-y-2">
                      {(detail.modelId || detail.providerName || detail.toolName || detail.tokenCount || detail.cost || detail.executionTime) && (
                        <Separator />
                      )}
                      
                      <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
                        {detail.modelId && (
                          <div className="flex items-center space-x-1">
                            <span className="font-medium">模型:</span>
                            <span>{detail.modelId}</span>
                            {detail.isFallbackUsed && (
                              <RefreshCw className="h-3 w-3 text-orange-600" title="模型已切换" />
                            )}
                          </div>
                        )}
                        {detail.providerName && (
                          <div className="flex items-center space-x-1">
                            <span className="font-medium">提供商:</span>
                            <span>{detail.providerName}</span>
                          </div>
                        )}
                        {detail.toolName && (
                          <div className="flex items-center space-x-1">
                            <Wrench className="h-3 w-3" />
                            <span className="font-medium">工具:</span>
                            <span>{detail.toolName}</span>
                          </div>
                        )}
                        {detail.tokenCount && (
                          <div className="flex items-center space-x-1">
                            <Zap className="h-3 w-3" />
                            <span className="font-medium">Token:</span>
                            <span>{detail.tokenCount}</span>
                          </div>
                        )}
                        {detail.executionTime && (
                          <div className="flex items-center space-x-1">
                            <Clock className="h-3 w-3" />
                            <span className="font-medium">耗时:</span>
                            <span>{formatExecutionTime(detail.executionTime)}</span>
                          </div>
                        )}
                        {detail.cost && (
                          <div className="flex items-center space-x-1">
                            <span className="font-medium">费用:</span>
                            <span>{formatCost(detail.cost)}</span>
                          </div>
                        )}
                      </div>
                      
                      {/* 工具调用详情 */}
                      {detail.stepType === 'TOOL_CALL' && (detail.toolRequestArgs || detail.toolResponseData) && (
                        <div className="mt-3 space-y-3">
                          <Separator />
                          
                          {detail.toolRequestArgs && (
                            <div>
                              <div className="flex items-center space-x-1 mb-2">
                                <Wrench className="h-3 w-3 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground">工具调用入参:</span>
                              </div>
                              <SmartContentDisplay 
                                content={detail.toolRequestArgs}
                                maxLength={100}
                                showCopy={true}
                                className="text-xs"
                              />
                            </div>
                          )}
                          
                          {detail.toolResponseData && (
                            <div>
                              <div className="flex items-center space-x-1 mb-2">
                                <Activity className="h-3 w-3 text-muted-foreground" />
                                <span className="text-xs font-medium text-muted-foreground">工具响应数据:</span>
                              </div>
                              <SmartContentDisplay 
                                content={detail.toolResponseData}
                                maxLength={100}
                                showCopy={true}
                                className="text-xs"
                              />
                            </div>
                          )}
                        </div>
                      )}
                      
                      {/* 模型切换信息 */}
                      {detail.isFallbackUsed && (detail.fallbackFromProvider || detail.fallbackFromEndpoint) && (
                        <div className="mt-3">
                          <Separator />
                          <div className="mt-2 p-2 bg-orange-50 border border-orange-200 rounded text-xs text-orange-700">
                            <div className="flex items-center space-x-1 mb-1">
                              <RefreshCw className="h-3 w-3" />
                              <span className="font-medium">模型已切换</span>
                            </div>
                            <div className="text-xs">
                              {detail.fallbackFromProvider && detail.fallbackToProvider && (
                                <div>
                                  从 <span className="font-medium">{detail.fallbackFromProvider}</span> 切换到 <span className="font-medium">{detail.fallbackToProvider}</span>
                                </div>
                              )}
                              {detail.fallbackFromEndpoint && detail.fallbackToEndpoint && (
                                <div className="mt-1">
                                  模型: <span className="font-medium">{detail.fallbackFromEndpoint}</span> → <span className="font-medium">{detail.fallbackToEndpoint}</span>
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      )}

                      {/* 错误信息 - 只有在非 ERROR_MESSAGE 类型时才显示，避免重复 */}
                      {detail.errorMessage && detail.stepType !== 'ERROR_MESSAGE' && (
                        <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-600">
                          <div className="flex items-center space-x-1">
                            <AlertCircle className="h-3 w-3" />
                            <span className="font-medium">错误:</span>
                          </div>
                          <p className="mt-1">{detail.errorMessage}</p>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}