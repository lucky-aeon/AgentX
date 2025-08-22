"use client";

import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { FileText } from 'lucide-react';
import type { DocumentSegment } from '@/types/rag-dataset';

interface DocumentSegmentCardProps {
  segment: DocumentSegment;
  onClick?: (segment: DocumentSegment) => void;
  isSelected?: boolean;
}

export function DocumentSegmentCard({ 
  segment, 
  onClick, 
  isSelected = false 
}: DocumentSegmentCardProps) {
  const handleClick = () => {
    onClick?.(segment);
  };

  // 格式化相似度分数为百分比
  const formatScore = (score: number) => {
    return `${Math.round(score * 100)}%`;
  };

  // 生成内容预览
  const getContentPreview = () => {
    if (segment.contentPreview) {
      return segment.contentPreview.length > 50 
        ? segment.contentPreview.substring(0, 50) + '...'
        : segment.contentPreview;
    }
    return '文档内容预览';
  };

  return (
    <Card 
      className={`p-3 cursor-pointer transition-all duration-200 hover:shadow-md ${
        isSelected 
          ? 'ring-2 ring-blue-500 bg-blue-50 dark:bg-blue-950/20' 
          : 'hover:bg-gray-50 dark:hover:bg-gray-800/50'
      }`}
      onClick={handleClick}
    >
      <div className="flex items-center gap-3">
        {/* 文档图标 */}
        <div className="flex-shrink-0">
          <FileText className="h-4 w-4 text-gray-600 dark:text-gray-400" />
        </div>
        
        {/* 文档信息 */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
              文档{segment.index || 1}
            </span>
            <Badge variant="outline" className="text-xs">
              {formatScore(segment.score)}
            </Badge>
          </div>
          
          {/* 内容预览 */}
          <p className="text-xs text-gray-600 dark:text-gray-400 truncate">
            {getContentPreview()}
          </p>
          
          {/* 文件名 */}
          <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
            来自: {segment.fileName}
          </p>
        </div>
      </div>
    </Card>
  );
}