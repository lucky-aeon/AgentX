"use client";

import { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Skeleton } from '@/components/ui/skeleton';
import { 
  AlertCircle, 
  FileText, 
  Calendar, 
  HardDrive, 
  Hash, 
  Search, 
  X,
  Loader2,
  FileSearch,
  CheckCircle
} from 'lucide-react';
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import { getFileInfoWithToast, getDocumentUnitsWithToast } from '@/lib/rag-file-service';
import { getInstalledRagFileDocumentsWithToast, getInstalledRagFileInfoWithToast } from '@/lib/rag-publish-service';
import { useFileDetail } from '@/hooks/rag-chat/useFileDetail';
import type { 
  RetrievedFileInfo, 
  FileDetailInfoDTO, 
  DocumentUnitDTO, 
  PageResponse 
} from '@/types/rag-dataset';

interface FileDetailPanelProps {
  selectedFile: RetrievedFileInfo | null;
  onDataLoad?: (data: any) => void;
}

export function FileDetailPanel({ selectedFile, onDataLoad }: FileDetailPanelProps) {
  const [fileInfo, setFileInfo] = useState<FileDetailInfoDTO | null>(null);
  const [documentUnits, setDocumentUnits] = useState<DocumentUnitDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const { formatFileSize } = useFileDetail();

  // 分页状态
  const [pageData, setPageData] = useState<PageResponse<DocumentUnitDTO>>({
    records: [],
    total: 0,
    size: 10, // RAG对话中使用较小的分页大小
    current: 1,
    pages: 0
  });

  // 防抖处理搜索
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // 加载文件信息和文档单元
  useEffect(() => {
    if (selectedFile) {
      loadFileInfo();
      loadDocumentUnits(1, debouncedQuery);
    } else {
      setFileInfo(null);
      setDocumentUnits([]);
      setError(null);
    }
  }, [selectedFile, debouncedQuery]);

  // 加载文件信息
  const loadFileInfo = async () => {
    if (!selectedFile) return;
    
    console.log('[FileDetailPanel] Loading file info for:', selectedFile);
    
    // 对于已安装RAG的文件，调用专门的API获取文件信息
    if (selectedFile.isInstalledRag && selectedFile.userRagId) {
      console.log('[FileDetailPanel] Processing as installed RAG file, loading from API');
      
      try {
        const response = await getInstalledRagFileInfoWithToast(selectedFile.userRagId, selectedFile.fileId);
        
        if (response.code === 200 && response.data) {
          // 使用从API获取的实际文件信息
          const installedFileInfo: FileDetailInfoDTO = {
            id: selectedFile.fileId,
            originalFilename: selectedFile.fileName,
            filename: selectedFile.fileName,
            url: response.data.url || selectedFile.filePath || '',
            size: response.data.size || 0, // 使用API返回的文件大小
            ext: response.data.ext || '',
            contentType: response.data.contentType || '',
            filePageSize: response.data.filePageSize || 0, // 使用API返回的页数信息
            isInitialize: response.data.processingStatus === 2 ? 1 : 0,
            isEmbedding: response.data.processingStatus === 2 ? 1 : 0,
            dataSetId: selectedFile.userRagId,
            userId: response.data.userId || '',
            createdAt: response.data.createdAt || '',
            updatedAt: response.data.updatedAt || ''
          };
          
          setFileInfo(installedFileInfo);
          onDataLoad?.(installedFileInfo);
          return;
        } else {
          throw new Error(response.message || '获取文件信息失败');
        }
      } catch (error) {
        console.error('[FileDetailPanel] Failed to load installed RAG file info:', error);
        // 如果API调用失败，仍然显示基本信息
        const fallbackFileInfo: FileDetailInfoDTO = {
          id: selectedFile.fileId,
          originalFilename: selectedFile.fileName,
          filename: selectedFile.fileName,
          url: selectedFile.filePath || '',
          size: 0,
          ext: '',
          contentType: '',
          filePageSize: 0,
          isInitialize: 1,
          isEmbedding: 1,
          dataSetId: selectedFile.userRagId,
          userId: '',
          createdAt: '',
          updatedAt: ''
        };
        
        setFileInfo(fallbackFileInfo);
        onDataLoad?.(fallbackFileInfo);
        return;
      }
    }
    
    try {
      const response = await getFileInfoWithToast(selectedFile.fileId);
      if (response.code === 200) {
        setFileInfo(response.data);
        onDataLoad?.(response.data);
      } else {
        setError(response.message || '获取文件信息失败');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取文件信息失败';
      setError(errorMessage);
      console.error('获取文件信息失败:', err);
    }
  };

  // 加载文档单元列表
  const loadDocumentUnits = async (page: number = 1, keyword?: string) => {
    if (!selectedFile) return;
    
    try {
      setLoading(true);
      setError(null);
      
      let response;
      
      if (selectedFile.isInstalledRag && selectedFile.userRagId) {
        // 已安装RAG：使用快照感知API
        console.log('[FileDetailPanel] Loading documents for installed RAG:', { userRagId: selectedFile.userRagId, fileId: selectedFile.fileId });
        const documentsResponse = await getInstalledRagFileDocumentsWithToast(
          selectedFile.userRagId, 
          selectedFile.fileId
        );
        console.log('[FileDetailPanel] Documents response:', documentsResponse);
        
        if (documentsResponse.code === 200) {
          let documents = documentsResponse.data || [];
          
          // 客户端过滤（如果有搜索查询）
          if (keyword?.trim()) {
            const query = keyword.trim().toLowerCase();
            documents = documents.filter(doc => 
              doc.content?.toLowerCase().includes(query)
            );
          }
          
          // 客户端分页
          const startIndex = (page - 1) * 10;
          const endIndex = startIndex + 10;
          const paginatedDocs = documents.slice(startIndex, endIndex);
          
          // 构造分页响应格式
          response = {
            code: 200,
            data: {
              records: paginatedDocs,
              total: documents.length,
              size: 10,
              current: page,
              pages: Math.ceil(documents.length / 10)
            }
          };
        } else {
          response = documentsResponse;
        }
      } else {
        // 原始RAG：使用原有API
        response = await getDocumentUnitsWithToast({
          fileId: selectedFile.fileId,
          page,
          pageSize: 10,
          keyword: keyword?.trim() || undefined
        });
      }
      
      if (response.code === 200) {
        setPageData(response.data);
        setDocumentUnits(response.data.records || []);
      } else {
        setError(response.message || '获取文档单元失败');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取文档单元失败';
      setError(errorMessage);
      console.error('获取文档单元失败:', err);
    } finally {
      setLoading(false);
    }
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadDocumentUnits(page, debouncedQuery);
  };

  // 生成分页数字
  const generatePageNumbers = () => {
    const pages: (number | string)[] = [];
    const current = pageData.current;
    const total = pageData.pages;

    if (total <= 5) {
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      if (current <= 3) {
        for (let i = 1; i <= 4; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      } else if (current >= total - 2) {
        pages.push(1);
        pages.push('...');
        for (let i = total - 3; i <= total; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push('...');
        pages.push(current - 1);
        pages.push(current);
        pages.push(current + 1);
        pages.push('...');
        pages.push(total);
      }
    }

    return pages;
  };

  if (!selectedFile) {
    return (
      <div className="flex items-center justify-center h-full text-muted-foreground">
        <div className="text-center">
          <FileText className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>请选择一个文件查看详情</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full text-muted-foreground">
        <div className="text-center">
          <AlertCircle className="h-12 w-12 mx-auto mb-4 text-red-500" />
          <p className="text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      {/* 头部信息 */}
      <div className="p-4 border-b shrink-0">
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <FileText className="h-5 w-5 text-blue-600" />
            <div>
              <h3 className="text-lg font-medium">{selectedFile.fileName}</h3>
              {selectedFile.score !== undefined && (
                <p className="text-sm text-muted-foreground">
                  相似度: {(selectedFile.score * 100).toFixed(0)}%
                </p>
              )}
              {selectedFile.isInstalledRag && (
                <p className="text-sm text-muted-foreground">
                  已安装知识库文件
                </p>
              )}
            </div>
          </div>
          
          {/* 文件详情 - 只有当有可显示的信息时才显示 */}
          {fileInfo && ((fileInfo.filePageSize && fileInfo.filePageSize > 0) || (fileInfo.size && fileInfo.size > 0)) && (
            <div className="grid grid-cols-2 gap-3 text-sm">
              {/* 只有页数大于0时才显示 */}
              {fileInfo.filePageSize && fileInfo.filePageSize > 0 && (
                <div className="flex items-center gap-2">
                  <Hash className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">页数：</span>
                  <span>{fileInfo.filePageSize} 页</span>
                </div>
              )}
              {/* 只有大小大于0时才显示 */}
              {fileInfo.size && fileInfo.size > 0 && (
                <div className="flex items-center gap-2">
                  <HardDrive className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">大小：</span>
                  <span>{formatFileSize(fileInfo.size)}</span>
                </div>
              )}
            </div>
          )}

          {/* 统计信息 */}
          <div className="flex items-center gap-4">
            <Badge variant="outline" className="text-xs">
              共 {pageData.total} 个语料
            </Badge>
            {pageData.pages > 1 && (
              <Badge variant="outline" className="text-xs">
                第 {pageData.current}/{pageData.pages} 页
              </Badge>
            )}
          </div>

          {/* 搜索框 */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="搜索语料内容..."
              className="pl-10 pr-10"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
                onClick={() => setSearchQuery("")}
              >
                <X className="h-4 w-4" />
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* 内容区域 */}
      <ScrollArea className="flex-1 p-4">
        {loading ? (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <Card key={i} className="p-4">
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-5 w-16" />
                    <Skeleton className="h-5 w-20" />
                  </div>
                  <Skeleton className="h-20 w-full" />
                </div>
              </Card>
            ))}
          </div>
        ) : documentUnits.length === 0 ? (
          <div className="text-center py-8">
            <FileSearch className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium mb-2">
              {searchQuery ? "未找到匹配的语料" : "暂无语料数据"}
            </h3>
            <p className="text-muted-foreground">
              {searchQuery ? "尝试使用不同的搜索词" : "请先对文件进行初始化处理"}
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {documentUnits.map((unit) => (
              <Card key={unit.id} className="p-4">
                <div className="space-y-3">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">
                        第 {unit.page + 1} 页
                      </Badge>
                      {unit.isVector && (
                        <Badge variant="default" className="text-xs">
                          <CheckCircle className="h-3 w-3 mr-1" />
                          已向量化
                        </Badge>
                      )}
                      {unit.isOcr && (
                        <Badge variant="secondary" className="text-xs">
                          OCR处理
                        </Badge>
                      )}
                    </div>
                  </div>

                  <div className="text-sm">
                    <div className="leading-tight">
                      <ReactMarkdown 
                        components={{
                          h1: ({ children }) => <h1 className="text-lg font-bold mb-1 leading-tight">{children}</h1>,
                          h2: ({ children }) => <h2 className="text-md font-semibold mb-1 leading-tight">{children}</h2>,
                          h3: ({ children }) => <h3 className="text-sm font-medium mb-0.5 leading-tight">{children}</h3>,
                          p: ({ children }) => <p className="mb-0.5 leading-tight">{children}</p>,
                          ul: ({ children }) => <ul className="list-disc list-inside mb-0.5 space-y-0">{children}</ul>,
                          ol: ({ children }) => <ol className="list-decimal list-inside mb-0.5 space-y-0">{children}</ol>,
                          li: ({ children }) => <li className="leading-tight">{children}</li>,
                          strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                          em: ({ children }) => <em className="italic">{children}</em>,
                          code: ({ children }) => <code className="bg-gray-100 dark:bg-gray-800 px-1 py-0.5 rounded text-xs font-mono">{children}</code>,
                          pre: ({ children }) => <pre className="bg-gray-100 dark:bg-gray-800 p-2 rounded text-xs font-mono overflow-x-auto mb-1">{children}</pre>,
                          blockquote: ({ children }) => <blockquote className="border-l-4 border-gray-300 pl-4 italic mb-0.5 leading-tight">{children}</blockquote>,
                          a: ({ children, href }) => <a href={href} className="text-blue-600 hover:underline" target="_blank" rel="noopener noreferrer">{children}</a>,
                          hr: () => <hr className="my-4 border-gray-200" />,
                          table: ({ children }) => <table className="border-collapse border border-gray-300 w-full mb-1 text-xs">{children}</table>,
                          th: ({ children }) => <th className="border border-gray-300 px-2 py-1 bg-gray-50 dark:bg-gray-800 font-semibold">{children}</th>,
                          td: ({ children }) => <td className="border border-gray-300 px-2 py-1">{children}</td>,
                        }}
                      >
                        {unit.content}
                      </ReactMarkdown>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t">
                    <span className="text-xs text-muted-foreground">
                      ID: {unit.id.substring(0, 8)}...
                    </span>
                    <span className="text-xs text-muted-foreground">
                      更新: {new Date(unit.updatedAt).toLocaleString('zh-CN')}
                    </span>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )}
      </ScrollArea>

      {/* 分页 */}
      {pageData.pages > 1 && (
        <div className="p-4 border-t shrink-0">
          <Pagination>
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious 
                  onClick={() => handlePageChange(pageData.current - 1)}
                  className={pageData.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                />
              </PaginationItem>
              
              {generatePageNumbers().map((page, index) => (
                <PaginationItem key={index}>
                  {page === '...' ? (
                    <PaginationEllipsis />
                  ) : (
                    <PaginationLink
                      onClick={() => handlePageChange(page as number)}
                      isActive={page === pageData.current}
                      className="cursor-pointer"
                    >
                      {page}
                    </PaginationLink>
                  )}
                </PaginationItem>
              ))}
              
              <PaginationItem>
                <PaginationNext 
                  onClick={() => handlePageChange(pageData.current + 1)}
                  className={pageData.current >= pageData.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      )}
    </div>
  );
}