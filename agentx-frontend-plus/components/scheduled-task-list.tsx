"use client"

import { useState, useEffect } from "react"
import { Clock, Edit, Trash2, Play, Pause, MoreHorizontal } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
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
import { toast } from "@/hooks/use-toast"

interface ScheduledTask {
  id: string
  content: string
  relatedSession: string
  relatedSessionTitle?: string
  repeatType: string
  executeDateTime: string
  weekdays?: number[]
  monthDay?: number
  customRepeat?: {
    interval: number
    unit: string
    executeDateTime: string
    neverEnd: boolean
    endDate?: string
  }
  isActive: boolean
  nextExecution?: string
  createdAt: string
}

interface ScheduledTaskListProps {
  conversationId: string
}

export function ScheduledTaskList({ conversationId }: ScheduledTaskListProps) {
  const [tasks, setTasks] = useState<ScheduledTask[]>([])
  const [loading, setLoading] = useState(true)
  const [taskToDelete, setTaskToDelete] = useState<ScheduledTask | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // 获取定时任务列表
  const fetchTasks = async () => {
    try {
      setLoading(true)
      
      // TODO: 调用API获取定时任务列表
      console.log("获取定时任务列表:", conversationId)
      
      // 模拟数据
      const mockTasks: ScheduledTask[] = [
        {
          id: "1",
          content: "总结每天工作内容",
          relatedSession: "session-1",
          relatedSessionTitle: "工作总结会话",
          repeatType: "daily",
          executeDateTime: "2024-01-15T18:00:00",
          isActive: true,
          nextExecution: "2024-01-15 18:00:00",
          createdAt: "2024-01-14 10:30:00"
        },
        {
          id: "2", 
          content: "周报提醒",
          relatedSession: "session-2",
          relatedSessionTitle: "周报会话",
          repeatType: "weekly",
          executeDateTime: "2024-01-22T09:00:00",
          weekdays: [1], // 周一
          isActive: false,
          nextExecution: "2024-01-22 09:00:00",
          createdAt: "2024-01-10 15:20:00"
        },
        {
          id: "3",
          content: "月度汇报",
          relatedSession: "session-3", 
          relatedSessionTitle: "月度汇报会话",
          repeatType: "monthly",
          executeDateTime: "2024-02-01T10:00:00",
          monthDay: 1,
          isActive: true,
          nextExecution: "2024-02-01 10:00:00",
          createdAt: "2024-01-01 09:00:00"
        },
        {
          id: "4",
          content: "自定义提醒",
          relatedSession: "session-4",
          relatedSessionTitle: "自定义会话",
          repeatType: "custom",
          executeDateTime: "2024-01-20T14:00:00",
          customRepeat: {
            interval: 3,
            unit: "天",
            executeDateTime: "2024-01-20T14:00:00",
            neverEnd: false,
            endDate: "2024-12-31"
          },
          isActive: true,
          nextExecution: "2024-01-20 14:00:00",
          createdAt: "2024-01-15 11:00:00"
        }
      ]
      
      await new Promise(resolve => setTimeout(resolve, 500))
      setTasks(mockTasks)
      
    } catch (error) {
      console.error("获取定时任务失败:", error)
      toast({
        title: "获取任务失败",
        description: "请稍后重试",
        variant: "destructive"
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (conversationId) {
      fetchTasks()
    }
  }, [conversationId])

  // 切换任务状态
  const toggleTaskStatus = async (taskId: string, isActive: boolean) => {
    try {
      // TODO: 调用API切换任务状态
      console.log("切换任务状态:", taskId, isActive)
      
      setTasks(prev => prev.map(task => 
        task.id === taskId ? { ...task, isActive } : task
      ))
      
      toast({
        title: isActive ? "任务已启用" : "任务已暂停",
        description: isActive ? "任务将按计划执行" : "任务已暂停执行"
      })
      
    } catch (error) {
      console.error("切换任务状态失败:", error)
      toast({
        title: "操作失败",
        description: "请稍后重试",
        variant: "destructive"
      })
    }
  }

  // 删除任务
  const deleteTask = async () => {
    if (!taskToDelete) return

    try {
      setIsDeleting(true)
      
      // TODO: 调用API删除任务
      console.log("删除任务:", taskToDelete.id)
      
      await new Promise(resolve => setTimeout(resolve, 500))
      
      setTasks(prev => prev.filter(task => task.id !== taskToDelete.id))
      
      toast({
        title: "任务已删除",
        description: "定时任务已成功删除"
      })
      
    } catch (error) {
      console.error("删除任务失败:", error)
      toast({
        title: "删除失败",
        description: "请稍后重试",
        variant: "destructive"
      })
    } finally {
      setIsDeleting(false)
      setTaskToDelete(null)
    }
  }

  const getRepeatTypeText = (type: string) => {
    switch (type) {
      case "daily": return "每天"
      case "weekly": return "每周"
      case "monthly": return "每月"
      case "weekdays": return "工作日"
      case "custom": return "自定义"
      default: return "一次性"
    }
  }

  const getWeekdayName = (day: number) => {
    const names = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]
    return names[day]
  }

  const formatDateTime = (dateTimeStr: string) => {
    try {
      const date = new Date(dateTimeStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch (e) {
      return dateTimeStr
    }
  }

  const getRepeatDetails = (task: ScheduledTask) => {
    switch (task.repeatType) {
      case "weekly":
        if (task.weekdays && task.weekdays.length > 0) {
          return `（${task.weekdays.map(d => getWeekdayName(d)).join("、")}）`
        }
        return ""
      case "monthly":
        return task.monthDay ? `（每月${task.monthDay}号）` : ""
      case "custom":
        if (task.customRepeat) {
          return `（每${task.customRepeat.interval}${task.customRepeat.unit}）`
        }
        return ""
      default:
        return ""
    }
  }

  if (loading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map(i => (
          <Card key={i} className="animate-pulse">
            <CardHeader className="pb-3">
              <div className="h-4 bg-gray-200 rounded w-3/4"></div>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                <div className="h-3 bg-gray-200 rounded w-1/3"></div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (tasks.length === 0) {
    return (
      <div className="text-center py-8">
        <Clock className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">暂无定时任务</h3>
        <p className="text-gray-500">点击右上角的"新建"按钮创建您的第一个定时任务</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {tasks.map((task) => (
        <Card key={task.id} className={`transition-all ${task.isActive ? 'border-blue-200 bg-blue-50/30' : 'border-gray-200'}`}>
          <CardHeader className="pb-3">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <CardTitle className="text-base font-medium text-gray-900 mb-2">
                  {task.content}
                </CardTitle>
                <div className="flex items-center gap-2 text-sm text-gray-500 mb-2">
                  <Clock className="h-4 w-4" />
                  <span>{formatDateTime(task.executeDateTime)}</span>
                  <Badge variant="outline" className="text-xs">
                    {getRepeatTypeText(task.repeatType)}{getRepeatDetails(task)}
                  </Badge>
                </div>
                <div className="text-xs text-gray-500">
                  关联会话: {task.relatedSessionTitle || task.relatedSession}
                </div>
              </div>
              
              <div className="flex items-center gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => toggleTaskStatus(task.id, !task.isActive)}
                  className={task.isActive ? "text-orange-600 hover:text-orange-700" : "text-green-600 hover:text-green-700"}
                >
                  {task.isActive ? (
                    <>
                      <Pause className="h-4 w-4 mr-1" />
                      暂停
                    </>
                  ) : (
                    <>
                      <Play className="h-4 w-4 mr-1" />
                      启用
                    </>
                  )}
                </Button>
                
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem>
                      <Edit className="mr-2 h-4 w-4" />
                      编辑
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onClick={() => setTaskToDelete(task)}
                      className="text-red-600 focus:text-red-600"
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>
          </CardHeader>
          
          <CardContent className="pt-0">
            <div className="text-xs text-gray-500">
              {task.isActive && task.nextExecution && (
                <div>下次执行: {task.nextExecution}</div>
              )}
              <div>创建时间: {task.createdAt}</div>
              {task.customRepeat && !task.customRepeat.neverEnd && task.customRepeat.endDate && (
                <div>截止日期: {task.customRepeat.endDate}</div>
              )}
            </div>
          </CardContent>
        </Card>
      ))}

      {/* 删除确认对话框 */}
      <AlertDialog open={!!taskToDelete} onOpenChange={(open) => !open && setTaskToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              您确定要删除定时任务 "{taskToDelete?.content}" 吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={deleteTask}
              disabled={isDeleting}
              className="bg-red-600 hover:bg-red-700"
            >
              {isDeleting ? "删除中..." : "确认删除"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
} 