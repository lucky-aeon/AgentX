// 用量记录相关类型定义

// 用量记录接口
export interface UsageRecord {
  id: string;
  userId: string;
  productId: string;
  quantityData: Record<string, any>;
  cost: number;
  requestId: string;
  billedAt: string;
  createdAt: string;
  updatedAt: string;
  // 业务信息字段（后端填充）
  serviceName?: string;        // 业务服务名称（如：GPT-4 模型调用）
  serviceType?: string;        // 服务类型（如：模型服务）
  serviceDescription?: string; // 服务描述
  pricingRule?: string;       // 定价规则说明（如：输入 ¥0.002/1K tokens，输出 ¥0.006/1K tokens）
  relatedEntityName?: string; // 关联实体名称（如：具体的模型名称或Agent名称）
}

// 查询用量记录请求（匹配后端QueryUsageRecordRequest）
export interface QueryUsageRecordRequest {
  userId?: string;
  productId?: string;
  requestId?: string;
  startTime?: string; // 后端使用startTime/endTime而不是startDate/endDate
  endTime?: string;
  page?: number;
  pageSize?: number;
}

// 用量记录统计接口
export interface UsageStats {
  totalRecords: number;
  totalCost: number;
  averageCost: number;
  periodCost: number; // 指定时间段内的费用
}

// 用量记录详情（包含关联的商品信息）
export interface UsageRecordDetail extends UsageRecord {
  productName?: string;
  productType?: string;
  serviceName?: string;
}

// 用量数据类型（针对不同计费类型）
export interface ModelUsageData {
  input: number;
  output: number;
  model?: string;
}

export interface AgentUsageData {
  calls: number;
  agentId?: string;
}

export interface ApiCallUsageData {
  calls: number;
  endpoint?: string;
}

export interface StorageUsageData {
  bytes: number;
  files?: number;
}

// 用量图表数据
export interface UsageChartData {
  date: string;
  cost: number;
  records: number;
}