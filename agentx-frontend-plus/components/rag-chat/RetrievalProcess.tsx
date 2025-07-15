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
  if (!retrieval || retrieval.type !== 'retrieval') {
    return null;
  }

  return (
    <Card className="px-4 py-2 bg-blue-50 dark:bg-blue-950/20">
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <FileSearch className="h-4 w-4 text-blue-600 dark:text-blue-400" />
          <span className="text-sm font-medium">文档检索</span>
          {retrieval.status === 'end' && (
            <Badge variant="secondary" className="text-xs">
              找到 {retrieval.retrievedCount || 0} 个文档
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
        {retrieval.documents && retrieval.documents.length > 0 && (
          <div className="mt-2 space-y-1">
            {retrieval.documents.map((doc, idx) => (
              <ClickableFileLink
                key={`${doc.fileId}-${doc.documentId}-${idx}`}
                file={doc}
                onClick={onFileClick}
                isSelected={selectedFileId === doc.fileId}
              />
            ))}
          </div>
        )}
      </div>
    </Card>
  );
}