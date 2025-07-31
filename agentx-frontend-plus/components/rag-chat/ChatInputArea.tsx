"use client";

import { useState } from "react";
import { Send, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Separator } from "@/components/ui/separator";

interface ChatInputAreaProps {
  onSend: (message: string) => void;
  onStop?: () => void;
  onClear?: () => void;
  isLoading?: boolean;
  disabled?: boolean;
  hasMessages?: boolean;
  className?: string;
}

export function ChatInputArea({
  onSend,
  onStop,
  onClear,
  isLoading = false,
  disabled = false,
  hasMessages = false,
  className
}: ChatInputAreaProps) {
  const [input, setInput] = useState("");

  const handleSend = () => {
    const message = input.trim();
    if (!message || isLoading) return;
    
    onSend(message);
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className={className}>
      <Separator />
      
      <div className="px-6 py-4">
        {/* 清空对话按钮 */}
        {hasMessages && (
          <div className="flex justify-end mb-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={onClear}
              disabled={isLoading}
            >
              清空对话
            </Button>
          </div>
        )}
        
        {/* 输入区域 */}
        <div className="flex gap-2">
          <Textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入您的问题..."
            className="flex-1 min-h-[60px] max-h-[120px] resize-none"
            disabled={isLoading || disabled}
          />
          <div className="flex flex-col gap-2">
            {isLoading ? (
              <Button
                variant="outline"
                size="icon"
                onClick={onStop}
                className="h-[60px]"
              >
                <X className="h-4 w-4" />
              </Button>
            ) : (
              <Button
                size="icon"
                onClick={handleSend}
                disabled={!input.trim()}
                className="h-[60px]"
              >
                <Send className="h-4 w-4" />
              </Button>
            )}
          </div>
        </div>
        
        <p className="text-xs text-muted-foreground mt-2">
          按 Enter 发送，Shift + Enter 换行
        </p>
      </div>
    </div>
  );
}