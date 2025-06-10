import { toast } from "@/hooks/use-toast"
import { httpClient } from "@/lib/http-client"

// 定时任务API端点
export const SCHEDULED_TASK_ENDPOINTS = {
  SCHEDULED_TASKS: "/scheduled-tasks",
  SCHEDULED_TASKS_BY_AGENT: (agentId: string) => `/scheduled-tasks/agent/${agentId}`,
  SCHEDULED_TASK_DETAIL: (id: string) => `/scheduled-tasks/${id}`,
  PAUSE_SCHEDULED_TASK: (id: string) => `/scheduled-tasks/${id}/pause`,
  RESUME_SCHEDULED_TASK: (id: string) => `/scheduled-tasks/${id}/resume`,
}

// 重复类型枚举
export enum RepeatType {
  NONE = "NONE",
  DAILY = "DAILY", 
  WEEKLY = "WEEKLY",
  MONTHLY = "MONTHLY",
  WORKDAYS = "WORKDAYS",
  CUSTOM = "CUSTOM"
}

// 任务状态枚举
export enum ScheduleTaskStatus {
  ACTIVE = "ACTIVE",
  PAUSED = "PAUSED", 
  COMPLETED = "COMPLETED"
}

// 重复配置接口
export interface RepeatConfig {
  executeDateTime?: string
  weekdays?: number[]
  monthDay?: number
  interval?: number
  timeUnit?: "DAYS" | "WEEKS" | "MONTHS" | "YEARS"
  executeTime?: string
  endDateTime?: string
}

// 创建定时任务请求
export interface CreateScheduledTaskRequest {
  agentId: string
  sessionId: string
  content: string
  repeatType: RepeatType
  repeatConfig: RepeatConfig
}

// 更新定时任务请求
export interface UpdateScheduledTaskRequest {
  id?: string
  agentId: string
  sessionId: string
  content: string
  repeatType: RepeatType
  repeatConfig: RepeatConfig
}

// 定时任务DTO
export interface ScheduledTaskDTO {
  id: string
  userId: string
  agentId: string
  sessionId: string
  content: string
  repeatType: RepeatType
  repeatConfig: RepeatConfig
  status: ScheduleTaskStatus
  lastExecuteTime?: string
  nextExecuteTime?: string
  createdAt: string
  updatedAt: string
}

// API响应格式
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 创建定时任务
export async function createScheduledTask(request: CreateScheduledTaskRequest): Promise<ApiResponse<ScheduledTaskDTO>> {
  return httpClient.post<ApiResponse<ScheduledTaskDTO>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASKS, request)
}

// 创建定时任务（带toast提示）
export async function createScheduledTaskWithToast(request: CreateScheduledTaskRequest): Promise<ApiResponse<ScheduledTaskDTO>> {
  try {
    const response = await createScheduledTask(request)
    
    if (response.code === 200) {
      toast({
        title: "定时任务创建成功",
        description: "任务将按设定时间执行"
      })
    } else {
      toast({
        title: "创建失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "创建失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 更新定时任务
export async function updateScheduledTask(taskId: string, request: UpdateScheduledTaskRequest): Promise<ApiResponse<ScheduledTaskDTO>> {
  return httpClient.put<ApiResponse<ScheduledTaskDTO>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASK_DETAIL(taskId), request)
}

// 更新定时任务（带toast提示）
export async function updateScheduledTaskWithToast(taskId: string, request: UpdateScheduledTaskRequest): Promise<ApiResponse<ScheduledTaskDTO>> {
  try {
    const response = await updateScheduledTask(taskId, request)
    
    if (response.code === 200) {
      toast({
        title: "任务更新成功"
      })
    } else {
      toast({
        title: "更新失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "更新失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 删除定时任务
export async function deleteScheduledTask(taskId: string): Promise<ApiResponse<void>> {
  return httpClient.delete<ApiResponse<void>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASK_DETAIL(taskId))
}

// 删除定时任务（带toast提示）
export async function deleteScheduledTaskWithToast(taskId: string): Promise<ApiResponse<void>> {
  try {
    const response = await deleteScheduledTask(taskId)
    
    if (response.code === 200) {
      toast({
        title: "任务删除成功"
      })
    } else {
      toast({
        title: "删除失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "删除失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 获取定时任务列表
export async function getScheduledTasks(): Promise<ApiResponse<ScheduledTaskDTO[]>> {
  return httpClient.get<ApiResponse<ScheduledTaskDTO[]>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASKS)
}

// 获取定时任务列表（带toast提示）
export async function getScheduledTasksWithToast(): Promise<ApiResponse<ScheduledTaskDTO[]>> {
  try {
    const response = await getScheduledTasks()
    
    if (response.code !== 200) {
      toast({
        title: "获取任务列表失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "获取任务列表失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 获取单个定时任务
export async function getScheduledTask(taskId: string): Promise<ApiResponse<ScheduledTaskDTO>> {
  return httpClient.get<ApiResponse<ScheduledTaskDTO>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASK_DETAIL(taskId))
}

// 暂停定时任务
export async function pauseScheduledTask(taskId: string): Promise<ApiResponse<ScheduledTaskDTO>> {
  return httpClient.post<ApiResponse<ScheduledTaskDTO>>(SCHEDULED_TASK_ENDPOINTS.PAUSE_SCHEDULED_TASK(taskId))
}

// 暂停定时任务（带toast提示）
export async function pauseScheduledTaskWithToast(taskId: string): Promise<ApiResponse<ScheduledTaskDTO>> {
  try {
    const response = await pauseScheduledTask(taskId)
    
    if (response.code === 200) {
      toast({
        title: "任务已暂停"
      })
    } else {
      toast({
        title: "暂停失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "暂停失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 恢复定时任务
export async function resumeScheduledTask(taskId: string): Promise<ApiResponse<ScheduledTaskDTO>> {
  return httpClient.post<ApiResponse<ScheduledTaskDTO>>(SCHEDULED_TASK_ENDPOINTS.RESUME_SCHEDULED_TASK(taskId))
}

// 恢复定时任务（带toast提示）
export async function resumeScheduledTaskWithToast(taskId: string): Promise<ApiResponse<ScheduledTaskDTO>> {
  try {
    const response = await resumeScheduledTask(taskId)
    
    if (response.code === 200) {
      toast({
        title: "任务已恢复"
      })
    } else {
      toast({
        title: "恢复失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "恢复失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 工具函数：前端重复类型转换为后端枚举
export function mapFrontendRepeatTypeToBackend(frontendType: string): RepeatType {
  const mapping: Record<string, RepeatType> = {
    'none': RepeatType.NONE,
    'daily': RepeatType.DAILY,
    'weekly': RepeatType.WEEKLY,
    'monthly': RepeatType.MONTHLY,
    'weekdays': RepeatType.WORKDAYS,
    'custom': RepeatType.CUSTOM
  }
  
  return mapping[frontendType] || RepeatType.NONE
}

// 工具函数：后端重复类型转换为前端类型
export function mapBackendRepeatTypeToFrontend(backendType: RepeatType): string {
  const mapping: Record<RepeatType, string> = {
    [RepeatType.NONE]: 'none',
    [RepeatType.DAILY]: 'daily',
    [RepeatType.WEEKLY]: 'weekly',
    [RepeatType.MONTHLY]: 'monthly',
    [RepeatType.WORKDAYS]: 'weekdays',
    [RepeatType.CUSTOM]: 'custom'
  }
  
  return mapping[backendType] || 'none'
}

// 工具函数：前端时间单位转换为后端格式
export function mapFrontendTimeUnitToBackend(frontendUnit: string): "DAYS" | "WEEKS" | "MONTHS" | "YEARS" {
  const mapping: Record<string, "DAYS" | "WEEKS" | "MONTHS" | "YEARS"> = {
    '天': 'DAYS',
    '周': 'WEEKS', 
    '月': 'MONTHS',
    '年': 'YEARS'
  }
  
  return mapping[frontendUnit] || 'DAYS'
}

// 工具函数：后端时间单位转换为前端格式
export function mapBackendTimeUnitToFrontend(backendUnit: string): string {
  const mapping: Record<string, string> = {
    'DAYS': '天',
    'WEEKS': '周',
    'MONTHS': '月', 
    'YEARS': '年'
  }
  
  return mapping[backendUnit] || '天'
}

// 工具函数：前端星期几转换为后端格式（前端0=周日，后端1=周一）
export function mapFrontendWeekdaysToBackend(frontendWeekdays: number[]): number[] {
  return frontendWeekdays.map(day => {
    // 前端：0=周日, 1=周一, ..., 6=周六
    // 后端：1=周一, 2=周二, ..., 7=周日
    return day === 0 ? 7 : day
  })
}

// 工具函数：后端星期几转换为前端格式
export function mapBackendWeekdaysToFrontend(backendWeekdays: number[]): number[] {
  return backendWeekdays.map(day => {
    // 后端：1=周一, 2=周二, ..., 7=周日
    // 前端：0=周日, 1=周一, ..., 6=周六
    return day === 7 ? 0 : day
  })
}

/**
 * 根据会话ID获取定时任务列表
 */
export const getScheduledTasksBySessionId = async (sessionId: string): Promise<ApiResponse<ScheduledTaskDTO[]>> => {
  return httpClient.get<ApiResponse<ScheduledTaskDTO[]>>(
    `${SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASKS}?sessionId=${sessionId}`
  )
}

/**
 * 根据会话ID获取定时任务列表（带错误提示）
 */
export const getScheduledTasksBySessionIdWithToast = async (sessionId: string): Promise<ApiResponse<ScheduledTaskDTO[]>> => {
  try {
    const response = await getScheduledTasksBySessionId(sessionId)
    
    if (response.code !== 200) {
      toast({
        title: "获取会话定时任务失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "获取会话定时任务失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
}

// 根据Agent ID获取定时任务列表
export async function getScheduledTasksByAgentId(agentId: string): Promise<ApiResponse<ScheduledTaskDTO[]>> {
  return httpClient.get<ApiResponse<ScheduledTaskDTO[]>>(SCHEDULED_TASK_ENDPOINTS.SCHEDULED_TASKS_BY_AGENT(agentId))
}

// 根据Agent ID获取定时任务列表（带toast提示）
export async function getScheduledTasksByAgentIdWithToast(agentId: string): Promise<ApiResponse<ScheduledTaskDTO[]>> {
  try {
    const response = await getScheduledTasksByAgentId(agentId)
    
    if (response.code !== 200) {
      toast({
        title: "获取Agent定时任务失败",
        description: response.message || "请稍后重试",
        variant: "destructive"
      })
    }
    
    return response
  } catch (error) {
    toast({
      title: "获取Agent定时任务失败",
      description: "网络错误，请稍后重试",
      variant: "destructive"
    })
    throw error
  }
} 