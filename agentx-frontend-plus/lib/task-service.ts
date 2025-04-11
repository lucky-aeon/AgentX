import { API_CONFIG, API_ENDPOINTS } from "./api-config";
import { withToast } from "./toast-utils";
import type { ApiResponse } from "@/types/agent";

// 任务数据结构
export interface TaskDTO {
  id: string;
  taskName: string;
  status: string;
  progress: number;
  parentTaskId: string;
  taskResult?: string;
  startTime?: string;
  endTime?: string;
}

// 任务聚合结构
export interface TaskAggregate {
  task: TaskDTO;
  subTasks: TaskDTO[];
}

// 获取会话最新任务列表
export async function getSessionTasks(sessionId: string): Promise<ApiResponse<TaskAggregate>> {
  try {
    const response = await fetch(`${API_CONFIG.BASE_URL}${API_ENDPOINTS.SESSION_TASKS(sessionId)}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    });

    if (!response.ok) {
      throw new Error(`获取会话任务失败: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("获取会话任务错误:", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取会话任务失败",
      data: { task: {} as TaskDTO, subTasks: [] },
      timestamp: Date.now(),
    };
  }
}

// 获取任务详情
export async function getTaskDetail(taskId: string): Promise<ApiResponse<TaskDTO>> {
  try {
    const response = await fetch(`${API_CONFIG.BASE_URL}${API_ENDPOINTS.SESSION_TASK_DETAIL(taskId)}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    });

    if (!response.ok) {
      throw new Error(`获取任务详情失败: ${response.status}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error("获取任务详情错误:", error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : "获取任务详情失败",
      data: {} as TaskDTO,
      timestamp: Date.now(),
    };
  }
}

// 使用toast包装的API函数
export const getSessionTasksWithToast = withToast(getSessionTasks, {
  showSuccessToast: false,
  errorTitle: "获取会话任务失败"
});

export const getTaskDetailWithToast = withToast(getTaskDetail, {
  showSuccessToast: false,
  errorTitle: "获取任务详情失败"
}); 