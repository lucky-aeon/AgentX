// 认证配置相关类型定义

// 常量定义（大写）
export const AUTH_FEATURE_KEY = {
  NORMAL_LOGIN: 'NORMAL_LOGIN',
  GITHUB_LOGIN: 'GITHUB_LOGIN',
  COMMUNITY_LOGIN: 'COMMUNITY_LOGIN',
  USER_REGISTER: 'USER_REGISTER'
} as const;

export const FEATURE_TYPE = {
  LOGIN: 'LOGIN',
  REGISTER: 'REGISTER'
} as const;

// 类型定义
export type AuthFeatureKey = typeof AUTH_FEATURE_KEY[keyof typeof AUTH_FEATURE_KEY];
export type FeatureType = typeof FEATURE_TYPE[keyof typeof FEATURE_TYPE];

// 认证配置响应接口
export interface AuthConfig {
  loginMethods: Record<string, LoginMethod>;
  registerEnabled: boolean;
}

// 登录方式接口
export interface LoginMethod {
  enabled: boolean;
  name: string;
  provider?: string;
}

// 认证设置接口
export interface AuthSetting {
  id: string;
  featureType: string;
  featureKey: string;
  featureName: string;
  enabled: boolean;
  displayOrder: number;
  description: string;
  configData?: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

// 更新认证配置请求接口
export interface UpdateAuthSettingRequest {
  featureName?: string;
  enabled?: boolean;
  displayOrder?: number;
  description?: string;
  configData?: Record<string, any>;
}

// API响应接口
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}