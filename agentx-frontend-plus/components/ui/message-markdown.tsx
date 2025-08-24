"use client";

import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Button } from '@/components/ui/button';
import { Copy } from 'lucide-react';
import { useCopy } from '@/hooks/use-copy';
import { CodeBlock } from '@/components/ui/code-block';
import { cn } from '@/lib/utils';

interface MessageMarkdownProps {
  content: string;
  showCopyButton?: boolean;
  isStreaming?: boolean;
  isError?: boolean;
  className?: string;
}

// 检测是否为错误消息
const isErrorMessage = (content: string): boolean => {
  const errorKeywords = [
    '错误', '失败', '无法', '未配置', '抱歉', 
    '出现了错误', '请重试', '处理失败', '未找到',
    '不存在', '配置错误', '连接失败', '预览出错:'
  ];
  return errorKeywords.some(keyword => content.includes(keyword));
};

export function MessageMarkdown({ 
  content, 
  showCopyButton = true, 
  isStreaming = false, 
  isError = false,
  className 
}: MessageMarkdownProps) {
  const { copyMarkdown } = useCopy();
  
  // 自动检测错误消息或使用传入的 isError
  const shouldShowAsError = isError || isErrorMessage(content);
  
  const handleCopyMessage = () => {
    copyMarkdown(content);
  };

  return (
    <div className={cn("relative group", className)}>
      {/* 复制按钮 */}
      {showCopyButton && (
        <Button
          variant="ghost"
          size="sm"
          onClick={handleCopyMessage}
          className="absolute top-2 right-2 h-8 w-8 p-0 z-10"
          aria-label="复制消息"
        >
          <Copy className="h-4 w-4" />
        </Button>
      )}
      
      {/* Markdown 内容 */}
      {shouldShowAsError ? (
        // 错误消息使用简单文本显示
        <div className={cn(
          "text-sm whitespace-pre-wrap p-3 rounded-lg",
          "bg-red-50 text-red-700 border border-red-200"
        )}>
          {content}
          {isStreaming && (
            <span className="inline-block w-2 h-4 bg-current opacity-75 animate-pulse ml-1" />
          )}
        </div>
      ) : (
        // 正常消息使用 Markdown 渲染
        <div className={cn(
          "prose prose-sm dark:prose-invert max-w-none",
          "prose-pre:bg-white prose-pre:border prose-pre:border-gray-200 prose-pre:text-gray-900",
          shouldShowAsError && "text-destructive"
        )}>
          <ReactMarkdown 
            remarkPlugins={[remarkGfm]}
            components={{
              pre: ({ children, ...props }) => {
                // 提取代码内容
                const codeElement = children as React.ReactElement;
                const code = typeof codeElement?.props?.children === 'string' 
                  ? codeElement.props.children 
                  : '';
                
                return (
                  <CodeBlock code={code}>
                    <pre {...props}>{children}</pre>
                  </CodeBlock>
                );
              }
            }}
          >
            {content}
          </ReactMarkdown>
          {isStreaming && (
            <span className="inline-block w-1 h-4 ml-1 bg-current animate-pulse" />
          )}
        </div>
      )}
    </div>
  );
}