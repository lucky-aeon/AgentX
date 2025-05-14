// 基础工具类型，用于市场工具
export interface MarketTool {
  id: string;
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
    type: string;
    url: string;
  };
  is_office?: boolean;
  installCount: number;
  status: ToolStatus;
  createdAt: string;
  updatedAt: string;
  tool_list?: ToolFunction[];
}

// 用户工具类型，包含后端API返回的字段
export interface UserTool {
  id: string;
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
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

// 弹窗状态类型
export interface DialogState {
  detailOpen: boolean;
  installOpen: boolean;
  deleteOpen: boolean;
  selectedTool: MarketTool | UserTool | null;
  toolToDelete: UserTool | null;
} 