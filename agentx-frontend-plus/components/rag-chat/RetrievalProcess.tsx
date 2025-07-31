"use client";

import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { FileSearch } from 'lucide-react';
import { ClickableFileLink } from './ClickableFileLink';
import type { RagThinkingData, RetrievedFileInfo } from '@/types/rag-dataset';

interface RetrievalProcessProps {
  retrieval: RagThinkingData;
  onFileClick?: (file: RetrievedFileInfo) => void;
  selectedFileId?: string;
}

export function RetrievalProcess({ 
  retrieval, 
  onFileClick, 
  selectedFileId 
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
            <Badge variant="secondary" className="text-xs">
              找到 {(() => {
                // 计算唯一文件数量
                const uniqueFileIds = new Set(retrieval.documents.map(doc => doc.fileId));
                return uniqueFileIds.size;
              })()} 个文件
            </Badge>
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

          return (
            <div className="mt-2 space-y-1">
              {uniqueFiles.map((doc, idx) => (
                <ClickableFileLink
                  key={`${doc.fileId}-${idx}`}
                  file={doc}
                  onClick={onFileClick}
                  isSelected={selectedFileId === doc.fileId}
                />
              ))}
            </div>
          );
        })()}
      </div>
    </Card>
  );
}