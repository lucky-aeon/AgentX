import { useState, useCallback } from 'react';
import type { 
  RetrievedFileInfo, 
  FileContentData, 
  GetFileDetailRequest, 
  FileDetailResponse 
} from '@/types/rag-dataset';

// 暂时使用模拟数据的服务函数，后续需要实现真实的API调用
async function fetchFileDetail(request: GetFileDetailRequest): Promise<FileDetailResponse> {
  // 模拟API调用延迟
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  // 模拟返回数据
  return {
    fileId: request.fileId,
    fileName: '示例文件.pdf',
    content: '这是文件的内容预览...\n\n文件包含了详细的信息，包括各种技术规格和使用说明。\n\n第一章：概述\n本文档描述了系统的基本架构和核心功能...\n\n第二章：技术实现\n系统采用了现代化的技术栈...',
    pageCount: 10,
    fileSize: 1024 * 1024 * 2, // 2MB
    fileType: 'application/pdf',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  };
}

// 导出简化版的hook，主要提供工具函数
export function useFileDetail() {
  // 格式化文件大小
  const formatFileSize = useCallback((bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }, []);

  return {
    formatFileSize
  };
}