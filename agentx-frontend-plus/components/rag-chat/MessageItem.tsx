"use client";

import ReactMarkdown from "react-markdown";
import { Bot, User, Loader2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { RetrievalProcess } from "./RetrievalProcess";
import { ThinkingProcess } from "./ThinkingProcess";
import type { Message } from '@/hooks/rag-chat/useRagChatSession';
import type { RetrievedFileInfo } from '@/types/rag-dataset';

interface MessageItemProps {
  message: Message;
  onFileClick?: (file: RetrievedFileInfo) => void;
  selectedFileId?: string;
  expandedThinking?: boolean;
  onToggleThinking?: () => void;
}

// 检测是否为错误消息
const isErrorMessage = (content: string): boolean => {
  const errorKeywords = [
    '错误', '失败', '无法', '未配置', '抱歉', 
    '出现了错误', '请重试', '处理失败', '未找到',
    '不存在', '配置错误', '连接失败'
  ];
  return errorKeywords.some(keyword => content.includes(keyword));
};

export function MessageItem({ 
  message, 
  onFileClick, 
  selectedFileId, 
  expandedThinking = true,
  onToggleThinking 
}: MessageItemProps) {
  console.log('[MessageItem] Rendering message:', {
    id: message.id,
    role: message.role,
    content: message.content,
    isStreaming: message.isStreaming,
    isError: isErrorMessage(message.content)
  });
  return (
    <div
      className={`flex gap-3 ${
        message.role === 'user' ? 'justify-end' : 'justify-start'
      }`}
    >
      {message.role === 'assistant' && (
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
          <Bot className="h-5 w-5 text-primary" />
        </div>
      )}
      
      <div className={`flex flex-col gap-2 max-w-[70%] ${
        message.role === 'user' ? 'items-end' : 'items-start'
      }`}>
        {/* 用户消息 */}
        {message.role === 'user' && (
          <Card className="px-4 py-2 bg-primary text-primary-foreground">
            <div className="text-sm whitespace-pre-wrap">
              {message.content}
            </div>
          </Card>
        )}
        
        {/* 助手消息：检索过程 */}
        {message.role === 'assistant' && message.retrieval && (
          <RetrievalProcess
            retrieval={message.retrieval}
            onFileClick={onFileClick}
            selectedFileId={selectedFileId}
          />
        )}
        
        {/* 助手消息：思考过程 */}
        {message.role === 'assistant' && (message.thinking || message.thinkingContent) && (
          <ThinkingProcess
            thinking={message.thinking}
            thinkingContent={message.thinkingContent}
            isThinkingComplete={message.isThinkingComplete}
            isStreaming={message.isStreaming}
            expanded={expandedThinking}
            onToggle={onToggleThinking}
          />
        )}
        
        {/* 助手消息：回答内容 */}
        {message.role === 'assistant' && message.content && (
          <Card className={`px-4 py-2 ${
            isErrorMessage(message.content)
              ? 'bg-destructive/10 border-destructive/20' 
              : 'bg-muted'
          }`} key={`${message.id}-content`}>
            <div className={`prose prose-sm dark:prose-invert max-w-none ${
              isErrorMessage(message.content)
                ? 'text-destructive' 
                : ''
            }`}>
              <ReactMarkdown>
                {message.content}
              </ReactMarkdown>
              {message.isStreaming && (
                <span className="inline-block w-1 h-4 ml-1 bg-current animate-pulse" />
              )}
            </div>
          </Card>
        )}
        
        {/* 正在生成回答的提示 */}
        {message.role === 'assistant' && 
         message.isStreaming && 
         !message.content && 
         !message.retrieval && 
         !message.thinking && (
          <Card className="px-4 py-2 bg-muted">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>正在生成回答...</span>
            </div>
          </Card>
        )}
        
        <span className="text-xs text-muted-foreground px-2">
          {message.timestamp.toLocaleTimeString('zh-CN')}
        </span>
      </div>
      
      {message.role === 'user' && (
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-secondary flex items-center justify-center">
          <User className="h-5 w-5" />
        </div>
      )}
    </div>
  );
}