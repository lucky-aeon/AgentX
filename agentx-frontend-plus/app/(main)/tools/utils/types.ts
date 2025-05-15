// 基础工具类型，用于市场工具
export interface MarketTool {
  id: string;
  toolId?: string;         // 工具ID，用于API调用（新增）
  name: string;
  icon: string | null;
  subtitle: string;
  description: string;
  user_id: string;
  author: string;
  labels: string[];
  tool_type: string;
  upload_type: string;
  upload_url: string;
  install_command: {
    type: 'sse'; // 明确指定为sse类型，兼容全局Tool类型
    url: string;
  };
  is_office?: boolean;
  installCount: number;
  status: ToolStatus;
  current_version?: string; // 当前版本（新增）
  createdAt: string;
  updatedAt: string;
  tool_list?: ToolFunction[];
}

// 用户工具类型，包含后端API返回的字段
export interface UserTool {
  id: string;
  toolId?: string;         // 工具ID，用于API调用（新增）
  name: string;
  icon: string | null;
  subtitle: string;
  description: string;
  userId?: string;         // 后端返回的userId
  userName?: string | null; // 后端返回的用户名
  author?: string;         // 兼容旧字段
  labels: string[];
  toolType?: string;       // 后端返回的toolType
  tool_type?: string;      // 兼容旧字段
  uploadType?: string;     // 后端返回的uploadType
  upload_type?: string;    // 兼容旧字段
  uploadUrl?: string;      // 后端返回的uploadUrl
  upload_url?: string;     // 兼容旧字段
  toolList?: ToolFunction[]; // 后端返回的toolList
  tool_list?: ToolFunction[]; // 兼容旧字段
  status: ToolStatus;
  failedStepStatus?: ToolStatus; // 审核失败的具体环节
  rejectReason?: string;   // 审核失败原因
  isOffice?: boolean;      // 后端返回的isOffice
  is_office?: boolean;     // 兼容旧字段
  office?: boolean;        // 后端返回的office
  installCount?: number | null;
  currentVersion?: string | null; // 后端返回的currentVersion
  current_version?: string; // 兼容旧字段
  installCommand?: string;  // 后端返回的installCommand
  install_command?: {
    type: string;
    url: string;
  };
  usageCount?: number;
  isOwner?: boolean;       // 是否为用户自己创建的工具
  createdAt: string;
  updatedAt: string;
}

// 工具功能定义
export interface ToolFunction {
  name: string;
  description: string;
  parameters?: {
    type?: string;
    properties: Record<string, any>;
    required?: string[];
  };
  inputSchema?: {
    type: string;
    properties: Record<string, any>;
    required?: string[];
  };
}

// 工具状态枚举
export enum ToolStatus {
  WAITING_REVIEW = 'WAITING_REVIEW', // 等待审核
  DEPLOYING = 'DEPLOYING',           // 部署中
  FETCHING_TOOLS = 'FETCHING_TOOLS', // 获取工具列表
  MANUAL_REVIEW = 'MANUAL_REVIEW',   // 人工审核
  APPROVED = 'APPROVED',             // 已通过
  FAILED = 'FAILED',                 // 失败
  PENDING = 'PENDING',               // 兼容旧状态
  REJECTED = 'REJECTED'              // 兼容旧状态
}

// 弹窗状态类型
export interface DialogState {
  detailOpen: boolean;
  installOpen: boolean;
  deleteOpen: boolean;
  selectedTool: MarketTool | UserTool | null;
  toolToDelete: UserTool | null;
}

// 工具版本数据类型
export interface VersionData {
  id: string;
  name: string;
  icon: string | null;
  subtitle: string;
  description: string;
  userId: string;
  version: string;
  toolId: string;
  uploadType: string | null;
  uploadUrl: string | null;
  toolList: ToolFunction[];
  labels: string[];
  publicStatus: boolean;
  changeLog: string;
  createdAt: string;
  updatedAt: string;
  userName: string | null;
  installCount: number | null;
  office: boolean;
} 