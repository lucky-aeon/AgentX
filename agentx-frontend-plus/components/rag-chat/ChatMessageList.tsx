"use client";

import { useState } from "react";
import { Bot, ArrowDown } from "lucide-react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import { MessageItem } from "./MessageItem";
import { useSmartScroll } from "@/hooks/rag-chat/useSmartScroll";
import type { Message } from '@/hooks/rag-chat/useRagChatSession';
import type { RetrievedFileInfo } from '@/types/rag-dataset';

interface ChatMessageListProps {
  messages: Message[];
  onFileClick?: (file: RetrievedFileInfo) => void;
  selectedFileId?: string;
  className?: string;
}

export function ChatMessageList({ 
  messages, 
  onFileClick, 
  selectedFileId,
  className 
}: ChatMessageListProps) {
  console.log('[ChatMessageList] Rendering with messages:', messages.length, messages);
  const [expandedThinking, setExpandedThinking] = useState<Record<string, boolean>>({});
  const [showScrollToBottom, setShowScrollToBottom] = useState(false);
  
  const { 
    scrollAreaRef, 
    handleScrollToBottom, 
    shouldShowScrollToBottom 
  } = useSmartScroll();

  // 切换思考过程展开状态
  const toggleThinking = (messageId: string) => {
    setExpandedThinking(prev => ({
      ...prev,
      [messageId]: !prev[messageId]
    }));
  };

  // 检查是否应该显示滚动到底部按钮
  const checkScrollButton = () => {
    const shouldShow = shouldShowScrollToBottom();
    setShowScrollToBottom(shouldShow);
  };

  return (
    <div className={`relative flex-1 overflow-hidden ${className}`}>
      <ScrollArea 
        ref={scrollAreaRef} 
        className="h-full px-6 py-4"
        onScrollCapture={checkScrollButton}
      >
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="rounded-full bg-primary/10 p-4 mb-4">
              <Bot className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-lg font-semibold mb-2">开始对话</h3>
            <p className="text-muted-foreground max-w-sm">
              我可以帮您快速检索和理解知识库中的内容。请输入您的问题开始对话。
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {messages.map((message) => (
              <MessageItem
                key={message.id}
                message={message}
                onFileClick={onFileClick}
                selectedFileId={selectedFileId}
                expandedThinking={expandedThinking[message.id] !== false}
                onToggleThinking={() => toggleThinking(message.id)}
              />
            ))}
          </div>
        )}
      </ScrollArea>
      
      {/* 回到底部按钮 */}
      {showScrollToBottom && messages.length > 0 && (
        <Button
          onClick={handleScrollToBottom}
          size="sm"
          className="absolute bottom-4 right-6 rounded-full shadow-lg"
          variant="secondary"
        >
          <ArrowDown className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}