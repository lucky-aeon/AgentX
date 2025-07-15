"use client";

import { useEffect, useState } from 'react';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { AlertCircle, FileText, Calendar, HardDrive, Hash } from 'lucide-react';
import { getFileDetailWithToast } from '@/lib/services/rag-file-detail.service';
import { useFileDetail } from '@/hooks/rag-chat/useFileDetail';
import type { RetrievedFileInfo, FileContentData } from '@/types/rag-dataset';

interface FileDetailPanelProps {
  selectedFile: RetrievedFileInfo | null;
  onDataLoad?: (data: any) => void;
}

export function FileDetailPanel({ selectedFile, onDataLoad }: FileDetailPanelProps) {
  const [fileDetailData, setFileDetailData] = useState<FileContentData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { formatFileSize } = useFileDetail();

  useEffect(() => {
    if (selectedFile) {
      // 直接调用服务函数
      const fetchData = async () => {
        setLoading(true);
        setError(null);
        
        try {
          const response = await getFileDetailWithToast({
            fileId: selectedFile.fileId,
            documentId: selectedFile.documentId
          });
          
          if (response.code === 200) {
            const contentData: FileContentData = {
              fileId: response.data.fileId,
              documentId: selectedFile.documentId,
              fileName: response.data.fileName,
              content: response.data.content,
              pageCount: response.data.pageCount,
              fileSize: response.data.fileSize,
              fileType: response.data.fileType,
              createdAt: response.data.createdAt,
              updatedAt: response.data.updatedAt
            };
            setFileDetailData(contentData);
            onDataLoad?.(contentData);
          } else {
            setError(response.message || '获取文件详情失败');
          }
        } catch (err) {
          const errorMessage = err instanceof Error ? err.message : '获取文件详情失败';
          setError(errorMessage);
          console.error('获取文件详情失败:', err);
        } finally {
          setLoading(false);
        }
      };
      
      fetchData();
    } else {
      setFileDetailData(null);
      setError(null);
    }
  }, [selectedFile, onDataLoad]);

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

  if (loading) {
    return (
      <div className="p-6 space-y-6">
        {/* 文件信息骨架 */}
        <div className="space-y-4">
          <Skeleton className="h-6 w-3/4" />
          <div className="flex gap-2">
            <Skeleton className="h-6 w-20" />
            <Skeleton className="h-6 w-16" />
          </div>
        </div>
        
        {/* 文件详情骨架 */}
        <div className="space-y-3">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
        
        {/* 内容骨架 */}
        <div className="space-y-2">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="h-4 w-full" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center text-destructive">
          <AlertCircle className="h-12 w-12 mx-auto mb-4" />
          <p className="text-sm">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <ScrollArea className="h-full">
      <div className="p-6 space-y-6">
        {/* 文件基本信息 */}
        <div className="space-y-4">
          <div>
            <h3 className="text-lg font-semibold mb-2 flex items-center gap-2">
              <FileText className="h-5 w-5" />
              {selectedFile.fileName}
            </h3>
            <div className="flex items-center gap-2 mb-3">
              <Badge variant="outline" className="text-xs">
                相似度: {(selectedFile.score * 100).toFixed(0)}%
              </Badge>
              <Badge variant="secondary" className="text-xs">
                {fileDetailData?.fileType || 'PDF'}
              </Badge>
            </div>
          </div>
          
          {/* 文件详情 */}
          {fileDetailData && (
            <Card className="p-4">
              <div className="grid grid-cols-1 gap-3 text-sm">
                <div className="flex items-center gap-2">
                  <Hash className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">页数：</span>
                  <span>{fileDetailData.pageCount} 页</span>
                </div>
                
                <div className="flex items-center gap-2">
                  <HardDrive className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">大小：</span>
                  <span>{formatFileSize(fileDetailData.fileSize)}</span>
                </div>
                
                <div className="flex items-center gap-2">
                  <Calendar className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">创建时间：</span>
                  <span>{new Date(fileDetailData.createdAt).toLocaleString('zh-CN')}</span>
                </div>
                
                <div className="flex items-center gap-2">
                  <Calendar className="h-4 w-4 text-muted-foreground" />
                  <span className="text-muted-foreground">更新时间：</span>
                  <span>{new Date(fileDetailData.updatedAt).toLocaleString('zh-CN')}</span>
                </div>
              </div>
            </Card>
          )}
        </div>

        {/* 文件内容 */}
        <div className="space-y-3">
          <h4 className="text-md font-medium">文件内容</h4>
          <Card className="p-4">
            <div className="text-sm leading-relaxed whitespace-pre-wrap">
              {fileDetailData?.content || '暂无内容'}
            </div>
          </Card>
        </div>
      </div>
    </ScrollArea>
  );
}