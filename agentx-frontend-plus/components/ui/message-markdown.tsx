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

// 预处理文本内容，标准化反引号和特殊字符
const preprocessContent = (content: string): string => {
  if (!content) return content;

  // 替换各种可能的引号字符为标准反引号
  let processedContent = content
    // 全角反引号 ｀ -> 标准反引号 `
    .replace(/｀/g, '`')
    // 左右单引号 ' ' -> 标准反引号 `（如果它们是成对出现的，可能是代码标记）
    .replace(/['']([^'']*?)[''] /g, '`$1` ')
    // 其他可能的引号字符
    .replace(/‛/g, '`')
    .replace(/′/g, '`')
    // 修复可能的零宽字符和不可见字符
    .replace(/[\u200B-\u200D\uFEFF]/g, '')
    // 标准化换行符
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n');

  return processedContent;
};

export function MessageMarkdown({ 
  content, 
  showCopyButton = true,
  isStreaming = false, 
  isError = false,
  className 
}: MessageMarkdownProps) {
  const { copyMarkdown } = useCopy();
  
  // 预处理内容
  const processedContent = preprocessContent(content);

  // 自动检测错误消息或使用传入的 isError
  const shouldShowAsError = isError || isErrorMessage(content);
  
  const handleCopyMessage = () => {
    copyMarkdown(content);
  };

  return (
    <div className={cn("relative group overflow-x-auto min-w-0", className)}>
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
          "prose prose-sm dark:prose-invert w-full min-w-0 max-w-none",
          "prose-pre:bg-white prose-pre:border prose-pre:border-gray-200 prose-pre:text-gray-900",
          shouldShowAsError && "text-destructive"
        )}>
          <ReactMarkdown 
            remarkPlugins={[remarkGfm]}
            components={{
              code: ({ inline, children, className, ...props }: any) => {
 
                
                // 多重检测内联代码：inline 属性或者没有 language- 前缀
                const isInline = inline || !className?.includes('language-');
                
                if (isInline) {
                  // 内联代码样式 - 使用 ! 前缀强制优先级
                  return (
                    <code 
                      className="!bg-gray-100 dark:!bg-gray-800 !text-red-600 dark:!text-red-400 !px-1 !py-0.5 !rounded !text-sm !font-mono !border-0" 
                      {...props}
                    >
                      {children}
                    </code>
                  );
                }
                // 代码块中的代码保持原样
                return <code className={className} {...props}>{children}</code>;
              },
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
              },
              table: ({ children, ...props }) => (
                <div className="w-full my-4 rounded-lg border border-gray-200 dark:border-gray-700">
                  <table 
                    className="w-full table-auto divide-y divide-gray-200 dark:divide-gray-700" 
                    {...props}
                  >
                    {children}
                  </table>
                </div>
              ),
              thead: ({ children, ...props }) => (
                <thead className="bg-gray-50 dark:bg-gray-800" {...props}>
                  {children}
                </thead>
              ),
              tbody: ({ children, ...props }) => (
                <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700" {...props}>
                  {children}
                </tbody>
              ),
              th: ({ children, ...props }) => (
                <th 
                  className="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider break-words" 
                  {...props}
                >
                  {children}
                </th>
              ),
              td: ({ children, ...props }) => (
                <td 
                  className="px-3 py-2 text-sm text-gray-900 dark:text-gray-100 break-words" 
                  {...props}
                >
                  {children}
                </td>
              )
            }}
          >
            {processedContent}
          </ReactMarkdown>
          {isStreaming && (
            <span className="inline-block w-1 h-4 ml-1 bg-current animate-pulse" />
          )}
        </div>
      )}
      
      {/* 底部复制按钮 */}
      {showCopyButton && (
        <div className="flex items-center gap-1 mt-1 opacity-60 hover:opacity-100 transition-opacity">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleCopyMessage}
            className="h-6 w-6 p-0 hover:bg-gray-100 rounded"
            aria-label="复制消息"
          >
            <Copy className="h-3 w-3" />
          </Button>
        </div>
      )}
    </div>
  );
}