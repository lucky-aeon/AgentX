"use client";

import ReactMarkdown from "react-markdown";
import { Brain, ChevronDown, ChevronRight, Loader2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import type { RagThinkingData } from '@/types/rag-dataset';

interface ThinkingProcessProps {
  thinking?: RagThinkingData;
  thinkingContent?: string;
  isThinkingComplete?: boolean;
  isStreaming?: boolean;
  expanded?: boolean;
  onToggle?: () => void;
}

export function ThinkingProcess({ 
  thinking, 
  thinkingContent, 
  isThinkingComplete, 
  isStreaming, 
  expanded = true,
  onToggle 
}: ThinkingProcessProps) {
  // 只要有thinking状态就显示，无论是否有实际内容
  if (!thinking && !thinkingContent) {
    return null;
  }

  return (
    <Collapsible 
      open={expanded}
      onOpenChange={onToggle}
      className="w-full"
    >
      <Card className="px-4 py-2 bg-purple-50 dark:bg-purple-950/20">
        <CollapsibleTrigger className="flex items-center justify-between w-full text-left">
          <div className="flex items-center gap-2">
            <Brain className="h-4 w-4 text-purple-600 dark:text-purple-400" />
            <span className="text-sm font-medium">思考过程</span>
            {isThinkingComplete && (
              <Badge variant="secondary" className="text-xs">
                已完成
              </Badge>
            )}
          </div>
          {expanded ? (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronRight className="h-4 w-4 text-muted-foreground" />
          )}
        </CollapsibleTrigger>
        
        <CollapsibleContent className="mt-3">
          <div className="prose prose-sm dark:prose-invert max-w-none pl-6 text-sm">
            {thinkingContent ? (
              <ReactMarkdown>
                {thinkingContent}
              </ReactMarkdown>
            ) : (
              <span className="text-muted-foreground">思考中...</span>
            )}
          </div>
          
          {/* 思考进行中状态 */}
          {!isThinkingComplete && isStreaming && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground mt-2 pl-6">
              <Loader2 className="h-3 w-3 animate-spin" />
              <span>正在思考中...</span>
            </div>
          )}
        </CollapsibleContent>
      </Card>
    </Collapsible>
  );
}