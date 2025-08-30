"use client"

import { useState, useEffect } from "react"
import { Clock, Play, Pause, MoreHorizontal, Edit, Trash2, Calendar } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { 
  getScheduledTasksByAgentIdWithToast,
  pauseScheduledTaskWithToast,
  resumeScheduledTaskWithToast,
  deleteScheduledTaskWithToast,
  mapBackendRepeatTypeToFrontend,
  mapBackendTimeUnitToFrontend,
  mapBackendWeekdaysToFrontend,
  type ScheduledTaskDTO,
  ScheduleTaskStatus,
  RepeatType
} from "@/lib/scheduled-task-service"
import { getAgentSessionsWithToast, type SessionDTO } from "@/lib/agent-session-service"
import { formatDisplayDateTime } from "@/lib/date-utils"

interface ScheduledTaskListProps {
  onTaskUpdate?: () => void
  onEditTask?: (task: ScheduledTaskDTO) => void
  agentId?: string
}

export function ScheduledTaskList({ onTaskUpdate, onEditTask, agentId }: ScheduledTaskListProps) {
  const [tasks, setTasks] = useState<ScheduledTaskDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [deleteTaskId, setDeleteTaskId] = useState<string | null>(null)
  const [operatingTaskId, setOperatingTaskId] = useState<string | null>(null)
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [sessionsMap, setSessionsMap] = useState<Map<string, string>>(new Map())

  // 获取任务列表
  const fetchTasks = async () => {
    if (!agentId) {
      setLoading(false)
      return
    }
    
    try {
      setLoading(true)
      const response = await getScheduledTasksByAgentIdWithToast(agentId)
      
      if (response.code === 200 && response.data) {
        setTasks(response.data)
      }
    } catch (error) {
 
    } finally {
      setLoading(false)
    }
  }

  // 获取会话列表
  const fetchSessions = async () => {
    if (!agentId) return
    
    try {
      const response = await getAgentSessionsWithToast(agentId)
      
      if (response.code === 200 && response.data) {
        setSessions(response.data)
        // 创建会话ID到名称的映射
        const map = new Map<string, string>()
        response.data.forEach(session => {
          map.set(session.id, session.title)
        })
        setSessionsMap(map)
      }
    } catch (error) {
 
    }
  }

  useEffect(() => {
    fetchTasks()
  }, [agentId])

  useEffect(() => {
    fetchSessions()
  }, [agentId])

  // 切换任务状态
  const handleToggleStatus = async (task: ScheduledTaskDTO) => {
    try {
      setOperatingTaskId(task.id)
      
      let response
      if (task.status === ScheduleTaskStatus.ACTIVE) {
        response = await pauseScheduledTaskWithToast(task.id)
      } else {
        response = await resumeScheduledTaskWithToast(task.id)
      }
      
      if (response.code === 200) {
        // 更新本地状态
        setTasks(prev => prev.map(t => 
          t.id === task.id ? { ...t, status: response.data.status } : t
        ))
        onTaskUpdate?.()
      }
    } catch (error) {
 
    } finally {
      setOperatingTaskId(null)
    }
  }

  // 删除任务
  const handleDeleteTask = async (taskId: string) => {
    try {
      setOperatingTaskId(taskId)
      const response = await deleteScheduledTaskWithToast(taskId)
      
      if (response.code === 200) {
        setTasks(prev => prev.filter(t => t.id !== taskId))
        onTaskUpdate?.()
      }
    } catch (error) {
 
    } finally {
      setOperatingTaskId(null)
      setDeleteTaskId(null)
    }
  }

  // 格式化重复类型显示
  const formatRepeatType = (task: ScheduledTaskDTO) => {
    const frontendType = mapBackendRepeatTypeToFrontend(task.repeatType)
    
    switch (frontendType) {
      case "none":
        return "不重复"
      case "daily":
        return "每天"
      case "weekly":
        if (task.repeatConfig.weekdays && task.repeatConfig.weekdays.length > 0) {
          const frontendWeekdays = mapBackendWeekdaysToFrontend(task.repeatConfig.weekdays)
          const weekdayNames = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]
          const names = frontendWeekdays.map(day => weekdayNames[day]).join("、")
          return `每周（${names}）`
        }
        return "每周"
      case "monthly":
        if (task.repeatConfig.monthDay) {
          return `每月${task.repeatConfig.monthDay}号`
        }
        return "每月"
      case "weekdays":
        return "工作日"
      case "custom":
        if (task.repeatConfig.interval && task.repeatConfig.timeUnit) {
          const unit = mapBackendTimeUnitToFrontend(task.repeatConfig.timeUnit)
          return `每${task.repeatConfig.interval}${unit}`
        }
        return "自定义"
      default:
        return "不重复"
    }
  }

  // 格式化执行时间
  const formatExecuteTime = (task: ScheduledTaskDTO) => {
    if (task.nextExecuteTime) {
      return formatDisplayDateTime(new Date(task.nextExecuteTime))
    }
    
    if (task.repeatConfig.executeDateTime) {
      return formatDisplayDateTime(new Date(task.repeatConfig.executeDateTime))
    }
    
    return "未设置"
  }

  // 获取状态显示
  const getStatusDisplay = (status: ScheduleTaskStatus) => {
    switch (status) {
      case ScheduleTaskStatus.ACTIVE:
        return { text: "运行中", variant: "default" as const }
      case ScheduleTaskStatus.PAUSED:
        return { text: "已暂停", variant: "secondary" as const }
      case ScheduleTaskStatus.COMPLETED:
        return { text: "已完成", variant: "outline" as const }
      default:
        return { text: "未知", variant: "destructive" as const }
    }
  }

  // 获取会话名称
  const getSessionName = (sessionId: string) => {
    const sessionName = sessionsMap.get(sessionId)
    if (sessionName) {
      return sessionName
    }
    // 如果找不到会话名称，显示会话ID的前8位
    return `会话 ${sessionId.substring(0, 8)}`
  }

  if (loading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-6 w-16" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
                <div className="flex justify-between items-center pt-2">
                  <Skeleton className="h-4 w-24" />
                  <div className="flex gap-2">
                    <Skeleton className="h-8 w-8" />
                    <Skeleton className="h-8 w-8" />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (tasks.length === 0) {
    return (
      <div className="text-center py-12">
        <Clock className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">暂无定时任务</h3>
        <p className="text-gray-500">点击上方按钮创建您的第一个定时任务</p>
      </div>
    )
  }

  return (
    <>
      <div className="space-y-4">
        {tasks.map((task) => {
          const statusDisplay = getStatusDisplay(task.status)
          const isOperating = operatingTaskId === task.id
          
          return (
            <Card key={task.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Clock className="h-4 w-4 text-gray-500" />
                    <span className="font-medium text-sm text-gray-600">
                      {formatRepeatType(task)}
                    </span>
                  </div>
                  <Badge variant={statusDisplay.variant}>
                    {statusDisplay.text}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <p className="text-sm text-gray-900 leading-relaxed">
                    {task.content}
                  </p>
                  
                  <div className="flex items-center gap-2 text-xs text-gray-500">
                    <Calendar className="h-3 w-3" />
                    <span>下次执行: {formatExecuteTime(task)}</span>
                  </div>
                  
                  {task.lastExecuteTime && (
                    <div className="text-xs text-gray-400">
                      上次执行: {formatDisplayDateTime(new Date(task.lastExecuteTime))}
                    </div>
                  )}
                  
                  <div className="flex justify-between items-center pt-2">
                    <span className="text-xs text-gray-400">
                      关联会话: {getSessionName(task.sessionId)}
                    </span>
                    
                    <div className="flex items-center gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleToggleStatus(task)}
                        disabled={isOperating}
                        className="h-8 w-8 p-0"
                      >
                        {task.status === ScheduleTaskStatus.ACTIVE ? (
                          <Pause className="h-3 w-3" />
                        ) : (
                          <Play className="h-3 w-3" />
                        )}
                      </Button>
                      
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button 
                            variant="outline" 
                            size="sm" 
                            className="h-8 w-8 p-0"
                            disabled={isOperating}
                          >
                            <MoreHorizontal className="h-3 w-3" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => onEditTask?.(task)}>
                            <Edit className="h-4 w-4 mr-2" />
                            编辑
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={() => setDeleteTaskId(task.id)}
                            className="text-red-600"
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            删除
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      {/* 删除确认对话框 */}
      <AlertDialog open={!!deleteTaskId} onOpenChange={() => setDeleteTaskId(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              此操作将永久删除该定时任务，无法撤销。确定要继续吗？
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => deleteTaskId && handleDeleteTask(deleteTaskId)}
              className="bg-red-600 hover:bg-red-700"
            >
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
} 