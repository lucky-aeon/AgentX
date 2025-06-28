import { httpClient } from './http-client';
import { ApiResponse } from './types/api';
import { withToast } from './toast-utils';

export interface Container {
  id: string;
  name: string;
  userId: string;
  userNickname?: string;
  type: ContainerType;
  status: ContainerStatus;
  dockerContainerId?: string;
  image: string;
  internalPort: number;
  externalPort?: number;
  ipAddress?: string;
  cpuUsage?: number;
  memoryUsage?: number;
  volumePath?: string;
  errorMessage?: string;
  lastAccessedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ContainerType {
  code: number;
  description: string;
}

export interface ContainerStatus {
  code: number;
  description: string;
}

export interface ContainerStatistics {
  totalContainers: number;
  runningContainers: number;
}

export interface GetContainersParams {
  keyword?: string;
  status?: ContainerStatus;
  type?: ContainerType;
  page?: number;
  pageSize?: number;
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface CreateContainerRequest {
  name: string;
  type: ContainerType;
  image?: string;
  internalPort?: number;
  description?: string;
}

export class AdminContainerService {
  /** 分页获取容器列表 */
  static async getContainers(params?: GetContainersParams): Promise<ApiResponse<PageResponse<Container>>> {
    try {
      return await httpClient.get<ApiResponse<PageResponse<Container>>>(
        '/admin/containers', 
        { params }
      );
    } catch (error) {
      return {
        code: 500,
        message: "获取容器列表失败",
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  /** 获取容器统计信息 */
  static async getContainerStatistics(): Promise<ApiResponse<ContainerStatistics>> {
    try {
      return await httpClient.get<ApiResponse<ContainerStatistics>>('/admin/containers/statistics');
    } catch (error) {
      return {
        code: 500,
        message: "获取容器统计失败",
        data: { totalContainers: 0, runningContainers: 0 },
        timestamp: Date.now()
      };
    }
  }

  /** 创建审核容器 */
  static async createReviewContainer(data: CreateContainerRequest): Promise<ApiResponse<Container>> {
    return await httpClient.post<ApiResponse<Container>>('/admin/containers/review', data);
  }

  /** 启动容器 */
  static async startContainer(containerId: string): Promise<ApiResponse<void>> {
    return await httpClient.post<ApiResponse<void>>(`/admin/containers/${containerId}/start`);
  }

  /** 停止容器 */
  static async stopContainer(containerId: string): Promise<ApiResponse<void>> {
    return await httpClient.post<ApiResponse<void>>(`/admin/containers/${containerId}/stop`);
  }

  /** 删除容器 */
  static async deleteContainer(containerId: string): Promise<ApiResponse<void>> {
    return await httpClient.delete<ApiResponse<void>>(`/admin/containers/${containerId}`);
  }

  /** 获取容器日志 */
  static async getContainerLogs(containerId: string, lines?: number): Promise<ApiResponse<string>> {
    const params = lines ? { lines } : {};
    return await httpClient.get<ApiResponse<string>>(`/admin/containers/${containerId}/logs`, { params });
  }

  /** 在容器中执行命令 */
  static async executeCommand(containerId: string, command: string): Promise<ApiResponse<string>> {
    return await httpClient.post<ApiResponse<string>>(`/admin/containers/${containerId}/exec`, null, {
      params: { command }
    });
  }

  /** 获取容器系统信息 */
  static async getSystemInfo(containerId: string): Promise<ApiResponse<string>> {
    return await httpClient.get<ApiResponse<string>>(`/admin/containers/${containerId}/system-info`);
  }

  /** 获取容器进程信息 */
  static async getProcessInfo(containerId: string): Promise<ApiResponse<string>> {
    return await httpClient.get<ApiResponse<string>>(`/admin/containers/${containerId}/processes`);
  }

  /** 获取容器网络信息 */
  static async getNetworkInfo(containerId: string): Promise<ApiResponse<string>> {
    return await httpClient.get<ApiResponse<string>>(`/admin/containers/${containerId}/network`);
  }

  /** 检查容器内MCP网关状态 */
  static async getMcpGatewayStatus(containerId: string): Promise<ApiResponse<string>> {
    return await httpClient.get<ApiResponse<string>>(`/admin/containers/${containerId}/mcp-status`);
  }
}

// 带Toast提示的API方法
export const getContainersWithToast = withToast(AdminContainerService.getContainers, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取容器列表失败"
});

export const getContainerStatisticsWithToast = withToast(AdminContainerService.getContainerStatistics, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取容器统计失败"
});

export const createReviewContainerWithToast = withToast(AdminContainerService.createReviewContainer, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "创建审核容器成功",
  errorTitle: "创建审核容器失败"
});

export const startContainerWithToast = withToast(AdminContainerService.startContainer, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "启动容器成功",
  errorTitle: "启动容器失败"
});

export const stopContainerWithToast = withToast(AdminContainerService.stopContainer, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "停止容器成功",
  errorTitle: "停止容器失败"
});

export const deleteContainerWithToast = withToast(AdminContainerService.deleteContainer, {
  showSuccessToast: true,
  showErrorToast: true,
  successTitle: "删除容器成功",
  errorTitle: "删除容器失败"
});

export const getContainerLogsWithToast = withToast(AdminContainerService.getContainerLogs, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取容器日志失败"
});

export const executeCommandWithToast = withToast(AdminContainerService.executeCommand, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "执行命令失败"
});

export const getSystemInfoWithToast = withToast(AdminContainerService.getSystemInfo, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取系统信息失败"
});

export const getProcessInfoWithToast = withToast(AdminContainerService.getProcessInfo, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取进程信息失败"
});

export const getNetworkInfoWithToast = withToast(AdminContainerService.getNetworkInfo, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取网络信息失败"
});

export const getMcpGatewayStatusWithToast = withToast(AdminContainerService.getMcpGatewayStatus, {
  showSuccessToast: false,
  showErrorToast: true,
  errorTitle: "获取MCP网关状态失败"
});

// 容器状态和类型常量
export const CONTAINER_STATUSES = {
  CREATING: { code: 1, description: "创建中" },
  RUNNING: { code: 2, description: "运行中" },
  STOPPED: { code: 3, description: "已停止" },
  ERROR: { code: 4, description: "错误状态" },
  DELETING: { code: 5, description: "删除中" },
  DELETED: { code: 6, description: "已删除" },
  SUSPENDED: { code: 7, description: "已暂停" }
};

export const CONTAINER_TYPES = {
  USER: { code: 1, description: "用户容器" },
  REVIEW: { code: 2, description: "审核容器" }
};