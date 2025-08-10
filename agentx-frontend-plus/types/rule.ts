// 规则相关类型定义

import { RuleHandlerKey } from './billing';

// 规则接口
export interface Rule {
  id: string;
  name: string;
  handlerKey: string; // RuleHandlerKey枚举值的字符串形式
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// 创建规则请求
export interface CreateRuleRequest {
  name: string;
  handlerKey: string;
  description?: string;
}

// 更新规则请求
export interface UpdateRuleRequest {
  name?: string;
  handlerKey?: string;
  description?: string;
}

// 查询规则请求
export interface QueryRuleRequest {
  keyword?: string;
  handlerKey?: string;
  page?: number;
  pageSize?: number;
}

// 规则表单数据接口
export interface RuleFormData {
  name: string;
  handlerKey: RuleHandlerKey;
  description: string;
}

// 规则选项接口（用于下拉选择）
export interface RuleOption {
  value: string;
  label: string;
  description?: string;
}