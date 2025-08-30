"use client"

import { useState, useEffect } from "react"
import { Clock, Calendar, Repeat, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Checkbox } from "@/components/ui/checkbox"
import { toast } from "@/hooks/use-toast"
import { getAgentSessionsWithToast, type SessionDTO } from "@/lib/agent-session-service"
import { DateTimePicker } from "@/components/ui/datetime-picker"
import { 
  createScheduledTaskWithToast,
  updateScheduledTaskWithToast,
  mapFrontendRepeatTypeToBackend,
  mapFrontendTimeUnitToBackend,
  mapFrontendWeekdaysToBackend,
  mapBackendRepeatTypeToFrontend,
  mapBackendTimeUnitToFrontend,
  mapBackendWeekdaysToFrontend,
  type CreateScheduledTaskRequest,
  type UpdateScheduledTaskRequest,
  type RepeatConfig,
  type ScheduledTaskDTO
} from "@/lib/scheduled-task-service"
import { 
  toBackendDateTimeString, 
  toBackendTimeString,
  saveLocalDateTime
} from "@/lib/date-utils"

interface ScheduledTaskDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  conversationId: string
  agentId?: string
  onTaskCreated?: () => void
  editingTask?: ScheduledTaskDTO | null
  onTaskUpdated?: () => void
}

type RepeatType = "none" | "daily" | "weekly" | "monthly" | "weekdays" | "custom"

interface CustomRepeatConfig {
  interval: number
  unit: "天" | "周" | "月" | "年"
  executeDateTime: string
  neverEnd: boolean
  endDate?: string
}

interface ScheduledTaskData {
  content: string
  relatedSession: string
  repeatType: RepeatType
  executeDateTime: string
  weekdays?: number[] // 每周重复时选择的星期几 (0=周日, 1=周一, ..., 6=周六)
  monthDay?: number // 每月重复时选择的几号 (1-31)
  customRepeat?: CustomRepeatConfig
}

export function ScheduledTaskDialog({ 
  open, 
  onOpenChange, 
  conversationId,
  agentId,
  onTaskCreated,
  editingTask,
  onTaskUpdated
}: ScheduledTaskDialogProps) {
  // 初始化编辑数据的辅助函数
  const initializeTaskData = (): ScheduledTaskData => {
    if (!editingTask) {
      return {
        content: "",
        relatedSession: conversationId,
        repeatType: "none",
        executeDateTime: "",
        weekdays: [],
        monthDay: 1,
        customRepeat: {
          interval: 1,
          unit: "天",
          executeDateTime: "",
          neverEnd: true,
          endDate: ""
        }
      }
    }

    // 转换后端类型到前端类型
    const frontendRepeatType = mapBackendRepeatTypeToFrontend(editingTask.repeatType)
    
    // 从repeatConfig中提取数据
    const config = editingTask.repeatConfig
    const executeDateTime = config.executeDateTime || ""
    const weekdays = config.weekdays ? mapBackendWeekdaysToFrontend(config.weekdays) : []
    const monthDay = config.monthDay || 1

    // 处理自定义重复配置
    let customRepeat: CustomRepeatConfig = {
      interval: 1,
      unit: "天",
      executeDateTime: "",
      neverEnd: true,
      endDate: ""
    }

    if (frontendRepeatType === "custom" && config.interval && config.timeUnit) {
      customRepeat = {
        interval: config.interval,
        unit: mapBackendTimeUnitToFrontend(config.timeUnit) as "天" | "周" | "月" | "年",
        executeDateTime: executeDateTime,
        neverEnd: !config.endDateTime,
        endDate: config.endDateTime || ""
      }
    }

    return {
      content: editingTask.content,
      relatedSession: editingTask.sessionId,
      repeatType: frontendRepeatType as RepeatType,
      executeDateTime,
      weekdays,
      monthDay,
      customRepeat
    }
  }

  const [taskData, setTaskData] = useState<ScheduledTaskData>(initializeTaskData())
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [loadingSessions, setLoadingSessions] = useState(false)

  // 当编辑任务变化时，重新初始化表单数据
  useEffect(() => {
    setTaskData(initializeTaskData())
  }, [editingTask])

  // 获取会话列表
  useEffect(() => {
    const fetchSessions = async () => {
      if (!agentId || !open) return
      
      try {
        setLoadingSessions(true)
        const response = await getAgentSessionsWithToast(agentId)
        
        if (response.code === 200 && response.data) {
          setSessions(response.data)
        }
      } catch (error) {
 
        toast({
          title: "获取会话列表失败",
          description: "请稍后重试",
          variant: "destructive"
        })
      } finally {
        setLoadingSessions(false)
      }
    }

    fetchSessions()
  }, [agentId, open])

  // 重置表单数据
  const resetForm = () => {
    setTaskData(initializeTaskData())
  }

  // 构建重复配置
  const buildRepeatConfig = (): RepeatConfig => {
    const config: RepeatConfig = {}

    switch (taskData.repeatType) {
      case "none":
      case "daily":
      case "weekdays":
        if (taskData.executeDateTime) {
          const date = new Date(taskData.executeDateTime)
          config.executeDateTime = toBackendDateTimeString(date)
          config.executeTime = toBackendTimeString(date)
        }
        break

      case "weekly":
        if (taskData.executeDateTime) {
          const date = new Date(taskData.executeDateTime)
          config.executeDateTime = toBackendDateTimeString(date)
          config.executeTime = toBackendTimeString(date)
        }
        if (taskData.weekdays && taskData.weekdays.length > 0) {
          config.weekdays = mapFrontendWeekdaysToBackend(taskData.weekdays)
        }
        break

      case "monthly":
        if (taskData.executeDateTime) {
          const date = new Date(taskData.executeDateTime)
          config.executeDateTime = toBackendDateTimeString(date)
          config.executeTime = toBackendTimeString(date)
        }
        if (taskData.monthDay) {
          config.monthDay = taskData.monthDay
        }
        break

      case "custom":
        if (taskData.customRepeat) {
          const custom = taskData.customRepeat
          config.interval = custom.interval
          config.timeUnit = mapFrontendTimeUnitToBackend(custom.unit)
          if (custom.executeDateTime) {
            const date = new Date(custom.executeDateTime)
            config.executeDateTime = toBackendDateTimeString(date)
            config.executeTime = toBackendTimeString(date)
          }
          if (!custom.neverEnd && custom.endDate) {
            const endDate = new Date(custom.endDate)
            config.endDateTime = toBackendDateTimeString(endDate)
          }
        }
        break
    }

    return config
  }

  const handleSubmit = async () => {
    if (!taskData.content.trim()) {
      toast({
        title: "请输入任务内容",
        variant: "destructive"
      })
      return
    }

    if (!agentId) {
      toast({
        title: "Agent ID不能为空",
        variant: "destructive"
      })
      return
    }

    if (!taskData.executeDateTime && taskData.repeatType !== "custom") {
      toast({
        title: "请选择执行时间",
        variant: "destructive"
      })
      return
    }

    if (taskData.repeatType === "custom") {
      if (!taskData.customRepeat?.executeDateTime) {
        toast({
          title: "请选择自定义重复的执行时间",
          variant: "destructive"
        })
        return
      }
      if (!taskData.customRepeat.neverEnd && !taskData.customRepeat.endDate) {
        toast({
          title: "请选择截止日期",
          variant: "destructive"
        })
        return
      }
    }

    if (taskData.repeatType === "weekly" && (!taskData.weekdays || taskData.weekdays.length === 0)) {
      toast({
        title: "请选择每周执行的日期",
        variant: "destructive"
      })
      return
    }

    try {
      setIsSubmitting(true)
      
      const repeatConfig = buildRepeatConfig()
      
      if (editingTask) {
        // 编辑模式 - 更新任务
        const updateRequest: UpdateScheduledTaskRequest = {
          id: editingTask.id,
          agentId,
          sessionId: taskData.relatedSession,
          content: taskData.content,
          repeatType: mapFrontendRepeatTypeToBackend(taskData.repeatType),
          repeatConfig
        }
        
        const response = await updateScheduledTaskWithToast(editingTask.id, updateRequest)
        
        if (response.code === 200) {
          onTaskUpdated?.()
          onOpenChange(false)
          resetForm()
        }
      } else {
        // 创建模式 - 创建新任务
        const createRequest: CreateScheduledTaskRequest = {
          agentId,
          sessionId: taskData.relatedSession,
          content: taskData.content,
          repeatType: mapFrontendRepeatTypeToBackend(taskData.repeatType),
          repeatConfig
        }
        
        const response = await createScheduledTaskWithToast(createRequest)
        
        if (response.code === 200) {
          onTaskCreated?.()
          onOpenChange(false)
          resetForm()
        }
      }
      
    } catch (error) {
 
    } finally {
      setIsSubmitting(false)
    }
  }

  const getRepeatDescription = (type: RepeatType) => {
    switch (type) {
      case "none": return "不重复"
      case "daily": return "每天重复"
      case "weekly": return "每周重复"
      case "monthly": return "每月重复"
      case "weekdays": return "每个工作日重复（星期一至星期五）"
      case "custom": return "自定义重复"
      default: return "不重复"
    }
  }

  const getWeekdayName = (day: number) => {
    const names = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]
    return names[day]
  }

  const handleWeekdayToggle = (day: number, checked: boolean) => {
    setTaskData(prev => ({
      ...prev,
      weekdays: checked 
        ? [...(prev.weekdays || []), day].sort()
        : (prev.weekdays || []).filter(d => d !== day)
    }))
  }

  const handleCustomRepeatChange = (field: keyof CustomRepeatConfig, value: any) => {
    setTaskData(prev => ({
      ...prev,
      customRepeat: {
        ...prev.customRepeat!,
        [field]: value
      } as CustomRepeatConfig
    }))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            {editingTask ? "编辑定时任务" : "创建定时任务"}
          </DialogTitle>
          <DialogDescription>
            {editingTask ? "修改定时任务设置" : "设置定时任务，系统将根据时间自动向会话发送消息"}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* 任务内容 */}
          <div className="space-y-2">
            <Label htmlFor="content">任务内容</Label>
            <Textarea
              id="content"
              placeholder="如：总结每天工作内容"
              value={taskData.content}
              onChange={(e) => setTaskData(prev => ({ ...prev, content: e.target.value }))}
              className="min-h-[80px]"
            />
          </div>

          {/* 关联会话 */}
          <div className="space-y-2">
            <Label htmlFor="session">关联会话</Label>
            <Select 
              value={taskData.relatedSession} 
              onValueChange={(value) => setTaskData(prev => ({ ...prev, relatedSession: value }))}
              disabled={loadingSessions}
            >
              <SelectTrigger>
                <SelectValue placeholder={loadingSessions ? "加载中..." : "选择会话"} />
              </SelectTrigger>
              <SelectContent>
                {sessions.map((session) => (
                  <SelectItem key={session.id} value={session.id}>
                    {session.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* 重复设置 */}
          <div className="space-y-3">
            <Label>重复</Label>
            <RadioGroup 
              value={taskData.repeatType} 
              onValueChange={(value: RepeatType) => setTaskData(prev => ({ ...prev, repeatType: value }))}
              className="space-y-2"
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="none" id="none" />
                <Label htmlFor="none" className="font-normal">不重复</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="daily" id="daily" />
                <Label htmlFor="daily" className="font-normal">每天重复</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="weekly" id="weekly" />
                <Label htmlFor="weekly" className="font-normal">每周重复</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="monthly" id="monthly" />
                <Label htmlFor="monthly" className="font-normal">每月重复</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="weekdays" id="weekdays" />
                <Label htmlFor="weekdays" className="font-normal">每个工作日重复（星期一至星期五）</Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="custom" id="custom" />
                <Label htmlFor="custom" className="font-normal">自定义重复</Label>
              </div>
            </RadioGroup>
          </div>

          {/* 每周重复 - 选择星期几 */}
          {taskData.repeatType === "weekly" && (
            <div className="space-y-3">
              <Label>选择每周执行日期</Label>
              <div className="grid grid-cols-7 gap-2">
                {[0, 1, 2, 3, 4, 5, 6].map((day) => (
                  <div key={day} className="flex items-center space-x-2">
                    <Checkbox
                      id={`weekday-${day}`}
                      checked={taskData.weekdays?.includes(day) || false}
                      onCheckedChange={(checked) => handleWeekdayToggle(day, checked as boolean)}
                    />
                    <Label htmlFor={`weekday-${day}`} className="text-sm">
                      {getWeekdayName(day)}
                    </Label>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* 每月重复 - 选择几号 */}
          {taskData.repeatType === "monthly" && (
            <div className="space-y-2">
              <Label htmlFor="monthDay">每月几号执行</Label>
              <Select 
                value={taskData.monthDay?.toString() || "1"} 
                onValueChange={(value) => setTaskData(prev => ({ ...prev, monthDay: parseInt(value) }))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Array.from({ length: 31 }, (_, i) => i + 1).map((day) => (
                    <SelectItem key={day} value={day.toString()}>
                      {day}号
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {/* 自定义重复设置 */}
          {taskData.repeatType === "custom" && (
            <div className="space-y-4 p-4 border rounded-lg bg-gray-50">
              <Label className="text-base font-medium">自定义重复设置</Label>
              
              {/* 间隔和时间单位 */}
              <div className="flex items-center gap-2">
                <Label className="text-sm">每</Label>
                <Input
                  type="number"
                  min="1"
                  max="999"
                  value={taskData.customRepeat?.interval || 1}
                  onChange={(e) => handleCustomRepeatChange('interval', parseInt(e.target.value) || 1)}
                  className="w-20"
                />
                <Select 
                  value={taskData.customRepeat?.unit || "天"} 
                  onValueChange={(value: "天" | "周" | "月" | "年") => handleCustomRepeatChange('unit', value)}
                >
                  <SelectTrigger className="w-24">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="天">天</SelectItem>
                    <SelectItem value="周">周</SelectItem>
                    <SelectItem value="月">月</SelectItem>
                    <SelectItem value="年">年</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* 执行时间 */}
              <div className="space-y-2">
                <Label htmlFor="customDateTime">执行时间</Label>
                <DateTimePicker
                  value={taskData.customRepeat?.executeDateTime ? new Date(taskData.customRepeat.executeDateTime) : undefined}
                  onChange={(date: Date | undefined) => {
                    if (date) {
                      handleCustomRepeatChange('executeDateTime', saveLocalDateTime(date))
                    }
                  }}
                  placeholder="选择执行日期和时间"
                />
              </div>

              {/* 截止设置 */}
              <div className="space-y-3">
                <div className="flex items-center space-x-2">
                  <Checkbox
                    id="neverEnd"
                    checked={taskData.customRepeat?.neverEnd || false}
                    onCheckedChange={(checked) => handleCustomRepeatChange('neverEnd', checked)}
                  />
                  <Label htmlFor="neverEnd">永不截止</Label>
                </div>

                {!taskData.customRepeat?.neverEnd && (
                  <div className="space-y-2">
                    <Label htmlFor="endDate">截止日期</Label>
                    <DateTimePicker
                      value={taskData.customRepeat?.endDate ? new Date(taskData.customRepeat.endDate) : undefined}
                      onChange={(date: Date | undefined) => {
                        if (date) {
                          handleCustomRepeatChange('endDate', saveLocalDateTime(date))
                        }
                      }}
                      placeholder="选择截止日期和时间"
                    />
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 执行时间 - 非自定义重复时显示 */}
          {taskData.repeatType !== "custom" && (
            <div className="space-y-2">
              <Label htmlFor="executeDateTime" className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                执行时间
              </Label>
              <DateTimePicker
                value={taskData.executeDateTime ? new Date(taskData.executeDateTime) : undefined}
                onChange={(date: Date | undefined) => {
                  if (date) {
                    setTaskData(prev => ({ ...prev, executeDateTime: saveLocalDateTime(date) }))
                  }
                }}
                placeholder="选择执行日期和时间"
              />
            </div>
          )}

          {/* 重复说明 */}
          {taskData.repeatType !== "none" && (
            <div className="bg-blue-50 p-3 rounded-lg">
              <div className="flex items-center gap-2 text-sm text-blue-700">
                <Repeat className="h-4 w-4" />
                <span>{getRepeatDescription(taskData.repeatType)}</span>
                {taskData.repeatType === "weekly" && taskData.weekdays && taskData.weekdays.length > 0 && (
                  <span>（{taskData.weekdays.map(d => getWeekdayName(d)).join("、")}）</span>
                )}
                {taskData.repeatType === "monthly" && taskData.monthDay && (
                  <span>（每月{taskData.monthDay}号）</span>
                )}
                {taskData.repeatType === "custom" && taskData.customRepeat && (
                  <span>（每{taskData.customRepeat.interval}{taskData.customRepeat.unit}）</span>
                )}
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? (editingTask ? "更新中..." : "创建中...") : (editingTask ? "更新" : "确认")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
} 