"use client";

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { FileText, ExternalLink } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { RetrievedFileInfo } from '@/types/rag-dataset';

interface ClickableFileLinkProps {
  file: RetrievedFileInfo;
  onClick?: (file: RetrievedFileInfo) => void;
  isSelected?: boolean;
  className?: string;
}

export function ClickableFileLink({ 
  file, 
  onClick, 
  isSelected = false, 
  className 
}: ClickableFileLinkProps) {
  const handleClick = () => {
    onClick?.(file);
  };

  return (
    <Button
      variant="ghost"
      size="sm"
      className={cn(
        "h-auto p-2 text-left justify-start items-start",
        "border rounded-lg hover:bg-accent transition-colors",
        isSelected && "bg-accent border-primary",
        className
      )}
      onClick={handleClick}
    >
      <div className="flex items-start gap-2 w-full">
        <FileText className="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5 flex-shrink-0" />
        <div className="flex-1 min-w-0 space-y-1">
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-medium truncate">
              {file.fileName}
            </span>
            <ExternalLink className="h-3 w-3 text-muted-foreground flex-shrink-0" />
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs px-1 py-0">
              {(file.score * 100).toFixed(0)}%
            </Badge>
            <span className="text-xs text-muted-foreground">
              点击查看详情
            </span>
          </div>
        </div>
      </div>
    </Button>
  );
}