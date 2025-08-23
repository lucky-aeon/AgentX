"use client";

import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { FileSearch, Files, FileText } from 'lucide-react';
import { ClickableFileLink } from './ClickableFileLink';
import { DocumentSegmentCard } from './DocumentSegmentCard';
import type { RagThinkingData, RetrievedFileInfo, DocumentSegment } from '@/types/rag-dataset';

interface RetrievalProcessProps {
  retrieval: RagThinkingData;
  onFileClick?: (file: RetrievedFileInfo) => void;
  onSegmentClick?: (segment: DocumentSegment) => void;
  selectedFileId?: string;
  selectedSegmentId?: string;
}

export function RetrievalProcess({ 
  retrieval, 
  onFileClick,
  onSegmentClick, 
  selectedFileId,
  selectedSegmentId 
}: RetrievalProcessProps) {
  console.log('[RetrievalProcess] Rendering with retrieval:', retrieval);
  
  if (!retrieval || retrieval.type !== 'retrieval') {
    return null;
  }

  return (
    <Card className="px-4 py-2 bg-blue-50 dark:bg-blue-950/20">
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <FileSearch className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          <span className="text-sm font-medium">文档检索</span>
          {retrieval.status === 'end' && retrieval.documents && (
            <>
              <Badge variant="secondary" className="text-xs">
                找到 {(() => {
                  // 计算唯一文件数量
                  const uniqueFileIds = new Set(retrieval.documents.map(doc => doc.fileId));
                  return uniqueFileIds.size;
                })()} 个文件
              </Badge>
              <Badge variant="outline" className="text-xs">
                {retrieval.documents.length} 个相关文档
              </Badge>
            </>
          )}
        </div>
        
        {/* 检索状态 */}
        <div className="text-xs text-muted-foreground">
          {retrieval.status === 'start' && '开始检索相关文档...'}
          {retrieval.status === 'progress' && '正在数据集中检索...'}
          {retrieval.status === 'end' && retrieval.message}
        </div>
        
        {/* 检索到的文档 */}
        {retrieval.documents && retrieval.documents.length > 0 && (() => {
          console.log('[RetrievalProcess] Processing documents:', retrieval.documents);
          
          // 按fileId去重，保留每个文件的最高分文档
          const uniqueFiles = retrieval.documents.reduce((acc, doc) => {
            const existing = acc.find(item => item.fileId === doc.fileId);
            if (!existing || doc.score > existing.score) {
              // 如果是新文件或当前分数更高，则更新
              const filtered = acc.filter(item => item.fileId !== doc.fileId);
              filtered.push(doc);
              return filtered;
            }
            return acc;
          }, [] as typeof retrieval.documents);

          // 按分数降序排列
          uniqueFiles.sort((a, b) => b.score - a.score);

          // 转换所有文档为DocumentSegment格式
          const sortedDocs = [...retrieval.documents].sort((a, b) => b.score - a.score);
          const documentSegments: DocumentSegment[] = sortedDocs
            .map((doc, index) => ({
              fileId: doc.fileId,
              fileName: doc.fileName,
              documentId: doc.documentId,
              score: doc.score,
              index: index + 1, // 从1开始的序号，基于排序后的顺序
              contentPreview: `来自${doc.fileName}的相关内容` // 简单的内容预览
            }));

          return (
            <div className="mt-3 space-y-3">
              {/* 文件卡片区域 */}
              {uniqueFiles.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <Files className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                    <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                      相关文件 ({uniqueFiles.length})
                    </span>
                  </div>
                  <div className="space-y-1">
                    {uniqueFiles.map((doc, idx) => (
                      <ClickableFileLink
                        key={`file-${doc.fileId}-${idx}`}
                        file={doc}
                        onClick={onFileClick}
                        isSelected={selectedFileId === doc.fileId}
                      />
                    ))}
                  </div>
                </div>
              )}

              {/* 文档片段区域 */}
              {documentSegments.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                    <span className="text-xs font-medium text-gray-700 dark:text-gray-300">
                      相关文档 ({documentSegments.length})
                    </span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                    {documentSegments.map((segment, idx) => (
                      <DocumentSegmentCard
                        key={`segment-${segment.documentId}-${idx}`}
                        segment={segment}
                        onClick={onSegmentClick}
                        isSelected={selectedSegmentId === segment.documentId}
                      />
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })()}
      </div>
    </Card>
  );
}