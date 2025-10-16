// 记忆管理相关类型

export type MemoryType = "PROFILE" | "TASK" | "FACT" | "EPISODIC";

export interface MemoryItem {
  id: string;
  type: MemoryType;
  text: string;
  importance?: number;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface QueryMemoryRequest {
  page?: number;
  pageSize?: number;
  type?: MemoryType | string; // 服务端允许可选
}

export interface CreateMemoryRequest {
  type: MemoryType | string;
  text: string;
  importance?: number; // 0~1
  tags?: string[];
  data?: Record<string, any> | null;
}

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
}

