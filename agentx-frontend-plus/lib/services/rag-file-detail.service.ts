import { API_ENDPOINTS } from '@/lib/api-config';
import { httpClient } from '@/lib/http-client';
import { withToast } from '@/lib/toast-utils';
import type { 
  GetFileDetailRequest, 
  FileDetailResponse, 
  ApiResponse 
} from '@/types/rag-dataset';

// 获取文件详情（使用现有的后端接口）
export async function getFileDetail(request: GetFileDetailRequest): Promise<ApiResponse<FileDetailResponse>> {
  try {
    console.log('获取文件详情:', request);
    
    // 1. 获取文件基本信息
    const fileInfoResponse = await httpClient.get<ApiResponse<any>>(
      API_ENDPOINTS.RAG_FILE_INFO(request.fileId)
    );
    
    if (fileInfoResponse.code !== 200) {
      throw new Error(fileInfoResponse.message || '获取文件信息失败');
    }
    
    // 2. 获取文件语料内容
    const documentUnitsResponse = await httpClient.post<ApiResponse<any>>(
      API_ENDPOINTS.RAG_DOCUMENT_UNITS,
      {
        fileId: request.fileId,
        page: 1,
        pageSize: 999, // 获取所有语料
        keyword: ''
      }
    );
    
    let content = '';
    if (documentUnitsResponse.code === 200 && documentUnitsResponse.data.records) {
      // 按页码排序并合并内容
      const sortedUnits = documentUnitsResponse.data.records.sort((a: any, b: any) => a.page - b.page);
      content = sortedUnits.map((unit: any) => unit.content).join('\n\n');
    }
    
    // 3. 构建FileDetailResponse格式的数据
    const fileInfo = fileInfoResponse.data;
    const fileDetailResponse: FileDetailResponse = {
      fileId: fileInfo.fileId,
      fileName: fileInfo.originalFilename,
      content: content,
      pageCount: fileInfo.filePageSize || 0,
      fileSize: fileInfo.size || 0,
      fileType: fileInfo.ext || 'pdf',
      createdAt: new Date().toISOString(), // 暂时使用当前时间
      updatedAt: new Date().toISOString()
    };
    
    return {
      code: 200,
      message: '获取文件详情成功',
      data: fileDetailResponse,
      timestamp: Date.now(),
    };
  } catch (error) {
    console.error('获取文件详情错误:', error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : '获取文件详情失败',
      data: null as unknown as FileDetailResponse,
      timestamp: Date.now(),
    };
  }
}

// 获取文件内容（用于预览）
export async function getFileContent(fileId: string, documentId?: string): Promise<ApiResponse<string>> {
  try {
    console.log('获取文件内容:', { fileId, documentId });
    
    const response = await httpClient.get<ApiResponse<string>>(
      API_ENDPOINTS.RAG_FILE_CONTENT,
      { 
        params: {
          fileId,
          documentId
        }
      }
    );
    
    return response;
  } catch (error) {
    console.error('获取文件内容错误:', error);
    return {
      code: 500,
      message: error instanceof Error ? error.message : '获取文件内容失败',
      data: '',
      timestamp: Date.now(),
    };
  }
}

// 模拟API调用（在真实API实现前使用）
export async function mockGetFileDetail(request: GetFileDetailRequest): Promise<ApiResponse<FileDetailResponse>> {
  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // 根据fileId生成不同的模拟数据
  const mockData: FileDetailResponse = {
    fileId: request.fileId,
    fileName: request.fileId.includes('simple') ? '我的简历-JfMvSY (3).pdf' : '臧世龙的简历.pdf',
    content: generateMockContent(request.fileId),
    pageCount: Math.floor(Math.random() * 20) + 1,
    fileSize: Math.floor(Math.random() * 5000000) + 100000, // 100KB - 5MB
    fileType: 'application/pdf',
    createdAt: new Date(Date.now() - Math.random() * 86400000 * 30).toISOString(), // 30天内
    updatedAt: new Date().toISOString()
  };
  
  return {
    code: 200,
    message: '获取文件详情成功',
    data: mockData,
    timestamp: Date.now()
  };
}

// 生成模拟内容
function generateMockContent(fileId: string): string {
  const templates = [
    `个人简历

基本信息：
- 姓名：张三
- 性别：男
- 年龄：28岁
- 联系电话：138****8888
- 邮箱：zhangsan@example.com
- 地址：北京市朝阳区

教育背景：
2014-2018    北京理工大学    计算机科学与技术    本科

工作经历：
2020-至今    ABC科技有限公司    高级前端开发工程师
- 负责公司核心产品的前端架构设计和开发
- 参与产品需求分析和技术方案制定
- 指导初级开发人员，提升团队整体技术水平

2018-2020    XYZ软件公司    前端开发工程师
- 负责Web应用程序的前端开发
- 参与产品功能优化和用户体验改进
- 协助测试团队完成产品测试工作

专业技能：
- 熟练掌握HTML、CSS、JavaScript
- 精通React、Vue.js等前端框架
- 熟悉Node.js、Python等后端技术
- 具备良好的代码规范和团队协作能力

项目经验：
1. 企业级管理系统前端重构
   - 技术栈：React + TypeScript + Ant Design
   - 项目周期：6个月
   - 负责内容：架构设计、核心功能开发、性能优化

2. 移动端H5应用开发
   - 技术栈：Vue.js + Vant + Webpack
   - 项目周期：3个月
   - 负责内容：页面开发、交互实现、适配优化`,

    `技术文档

系统架构说明

概述：
本系统采用前后端分离的架构设计，前端使用React技术栈，后端使用Java Spring Boot框架。

技术选型：
- 前端：React 18 + TypeScript + Tailwind CSS
- 后端：Spring Boot 3.0 + MyBatis-Plus + PostgreSQL
- 部署：Docker + Kubernetes
- 监控：Prometheus + Grafana

系统模块：
1. 用户管理模块
   - 用户注册登录
   - 权限管理
   - 个人信息维护

2. 数据管理模块
   - 数据录入
   - 数据查询
   - 数据导出

3. 报表模块
   - 图表展示
   - 数据分析
   - 定时任务

安全考虑：
- 使用JWT进行用户认证
- 实现RBAC权限控制
- 数据传输加密
- SQL注入防护

性能优化：
- 前端代码分割
- 图片懒加载
- 数据库索引优化
- 缓存策略

部署说明：
1. 环境准备
2. 数据库初始化
3. 应用部署
4. 监控配置`,

    `产品需求文档

项目背景：
随着公司业务的快速发展，现有的管理系统已无法满足业务需求，需要开发一套新的综合管理系统。

产品目标：
1. 提高工作效率
2. 降低运营成本
3. 提升用户体验
4. 增强数据安全性

功能需求：
1. 首页仪表板
   - 数据概览
   - 快捷操作
   - 消息通知

2. 用户管理
   - 用户列表
   - 角色管理
   - 权限分配

3. 内容管理
   - 内容发布
   - 内容审核
   - 内容统计

4. 系统设置
   - 基础配置
   - 日志管理
   - 备份恢复

非功能需求：
1. 性能要求
   - 页面加载时间 < 2秒
   - 并发用户数 > 1000
   - 系统可用性 > 99.9%

2. 安全要求
   - 数据加密传输
   - 访问权限控制
   - 操作日志记录

3. 兼容性要求
   - 支持主流浏览器
   - 响应式设计
   - 移动端适配

项目计划：
第一阶段（1-2个月）：基础框架搭建
第二阶段（3-4个月）：核心功能开发
第三阶段（5-6个月）：测试和优化
第四阶段（7个月）：上线和运维`
  ];
  
  const index = Math.abs(hashCode(fileId)) % templates.length;
  return templates[index];
}

// 简单的hash函数
function hashCode(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // 转为32位整数
  }
  return hash;
}

// 使用 withToast 包装器的API函数
export const getFileDetailWithToast = withToast(getFileDetail, {
  showSuccessToast: false,
  errorTitle: '获取文件详情失败'
});

export const getFileContentWithToast = withToast(getFileContent, {
  showSuccessToast: false,
  errorTitle: '获取文件内容失败'
});