import { useState, useRef, useCallback } from "react";
import { toast } from "@/hooks/use-toast";
import { AgentSessionService } from "@/lib/agent-session-service";

export interface UseInterruptableChatOptions {
  /** 成功中断时的回调 */
  onInterruptSuccess?: () => void;
  /** 中断失败时的回调 */
  onInterruptError?: (error: string) => void;
  /** 自定义toast配置 */
  showToast?: boolean;
}

export interface UseInterruptableChatReturn {
  /** 是否可以中断 */
  canInterrupt: boolean;
  /** 是否正在中断 */
  isInterrupting: boolean;
  /** AbortController引用 */
  abortControllerRef: React.MutableRefObject<AbortController | null>;
  /** 开始对话 */
  startChat: () => void;
  /** 中断对话 */
  handleInterrupt: (sessionId: string) => Promise<void>;
  /** 重置状态 */
  reset: () => void;
}

/**
 * 可中断对话Hook
 * 提供对话中断功能的通用逻辑
 */
export function useInterruptableChat(options: UseInterruptableChatOptions = {}): UseInterruptableChatReturn {
  const {
    onInterruptSuccess,
    onInterruptError,
    showToast = true
  } = options;

  const [canInterrupt, setCanInterrupt] = useState(false);
  const [isInterrupting, setIsInterrupting] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);

  /** 开始对话 */
  const startChat = useCallback(() => {
    // 创建新的AbortController
    abortControllerRef.current = new AbortController();
    setCanInterrupt(true);
    setIsInterrupting(false);
  }, []);

  /** 中断对话 */
  const handleInterrupt = useCallback(async (sessionId: string) => {
    if (isInterrupting || !canInterrupt) {
      return;
    }

    setIsInterrupting(true);
    
    try {
      // 1. 取消前端请求
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }

      // 2. 调用后端中断API
      const response = await AgentSessionService.interruptSession(sessionId);
      
      if (response.code === 200) {
        if (showToast) {
          toast({
            title: "对话已中断",
            variant: "default",
          });
        }
        onInterruptSuccess?.();
      } else {
        throw new Error(response.message || "中断失败");
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : "中断对话失败";
      
      if (showToast) {
        toast({
          title: "中断失败",
          description: errorMessage,
          variant: "destructive",
        });
      }
      
      onInterruptError?.(errorMessage);
    } finally {
      setIsInterrupting(false);
      setCanInterrupt(false);
    }
  }, [isInterrupting, canInterrupt, showToast, onInterruptSuccess, onInterruptError]);

  /** 重置状态 */
  const reset = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setCanInterrupt(false);
    setIsInterrupting(false);
  }, []);

  return {
    canInterrupt,
    isInterrupting,
    abortControllerRef,
    startChat,
    handleInterrupt,
    reset,
  };
}