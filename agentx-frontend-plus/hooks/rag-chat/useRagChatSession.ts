import { useCallback, useRef, useState } from 'react';
import { RagChatSession } from '@/lib/rag-chat-service';
import type { RagStreamChatRequest, RagThinkingData } from '@/types/rag-dataset';

// 消息类型定义
export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  retrieval?: RagThinkingData;
  thinking?: RagThinkingData;
  thinkingContent?: string;
  timestamp: Date;
  isStreaming?: boolean;
  isThinkingComplete?: boolean;
  isRetrievalComplete?: boolean;
}

interface UseRagChatSessionOptions {
  onError?: (error: string) => void;
  onDone?: () => void;
}

export function useRagChatSession(options: UseRagChatSessionOptions = {}) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [currentThinking, setCurrentThinking] = useState<RagThinkingData | null>(null);
  const [currentThinkingContent, setCurrentThinkingContent] = useState<string>('');
  
  const chatSessionRef = useRef<RagChatSession | null>(null);
  const thinkingContentRef = useRef<string>('');
  const isDoneCalledRef = useRef<boolean>(false);

  // 清空对话
  const clearMessages = useCallback(() => {
    if (chatSessionRef.current) {
      chatSessionRef.current.abort();
    }
    setMessages([]);
    setCurrentThinking(null);
    setCurrentThinkingContent('');
    thinkingContentRef.current = '';
    isDoneCalledRef.current = false;
    setIsLoading(false);
  }, []);

  // 停止生成
  const stopGeneration = useCallback(() => {
    try {
      if (chatSessionRef.current) {
        chatSessionRef.current.abort();
      }
      setIsLoading(false);
      setMessages(prev => {
        const newMessages = [...prev];
        const lastMessage = newMessages[newMessages.length - 1];
        if (lastMessage && lastMessage.role === 'assistant' && lastMessage.isStreaming) {
          lastMessage.isStreaming = false;
          if (!lastMessage.content) {
            lastMessage.content = '生成已停止。';
          }
        }
        return newMessages;
      });
    } catch (error) {
      // 静默处理停止过程中的错误，确保 UI 状态正确更新
 
      setIsLoading(false);
    }
  }, []);

  // 发送消息
  const sendMessage = useCallback(async (question: string, datasetIds: string[]) => {
    if (!question.trim() || isLoading) return;

    // 创建用户消息
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: new Date()
    };

    // 创建助手消息占位符
    const assistantMessage: Message = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    };

    setMessages(prev => [...prev, userMessage, assistantMessage]);
    setIsLoading(true);
    setCurrentThinking(null);
    setCurrentThinkingContent('');
    thinkingContentRef.current = '';
    isDoneCalledRef.current = false;

    // 确保聊天会话已初始化
    if (!chatSessionRef.current) {
      chatSessionRef.current = new RagChatSession();
    }

    // 如果有正在进行的会话，先中止
    if (chatSessionRef.current.isActive()) {
      chatSessionRef.current.abort();
    }

    try {
      await chatSessionRef.current.start(
        {
          datasetIds,
          question,
          stream: true
        },
        {
          onThinking: (data) => {
            setCurrentThinking(data);
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  const updatedMessage = { ...lastMessage };
                  if (data.type === 'retrieval') {
                    updatedMessage.retrieval = data;
                    if (data.status === 'end') {
                      updatedMessage.isRetrievalComplete = true;
                    }
                  } else if (data.type === 'thinking' || data.type === 'answer') {
                    updatedMessage.thinking = data;
                  }
                  return [...prev.slice(0, -1), updatedMessage];
                }
              }
              return prev;
            });
          },
          onThinkingContent: (content, timestamp) => {
            thinkingContentRef.current += content;
            setCurrentThinkingContent(thinkingContentRef.current);
            
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  return [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      thinkingContent: thinkingContentRef.current
                    }
                  ];
                }
              }
              return prev;
            });
          },
          onThinkingEnd: () => {
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  return [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      isThinkingComplete: true
                    }
                  ];
                }
              }
              return prev;
            });
          },
          onContent: (content, timestamp) => {
            // 移除时间戳去重逻辑：SSE 是有序流，不会有重复消息
            // 之前的去重导致相同毫秒内的多个片段被跳过，造成内容丢失

            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  return [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      content: lastMessage.content + content
                    }
                  ];
                }
              }
              return prev;
            });
          },
          onError: (error) => {
            options.onError?.(error);
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  return [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      content: '抱歉，处理您的请求时出现了错误。请重试。',
                      isStreaming: false
                    }
                  ];
                }
              }
              return prev;
            });
          },
          onDone: () => {
            // 防止重复调用
            if (isDoneCalledRef.current) {
              return;
            }
            isDoneCalledRef.current = true;

            try {
              setMessages(prev => {
                if (prev.length > 0) {
                  const lastMessage = prev[prev.length - 1];
                  if (lastMessage && lastMessage.role === 'assistant') {
                    return [
                      ...prev.slice(0, -1),
                      {
                        ...lastMessage,
                        isStreaming: false
                      }
                    ];
                  }
                }
                return prev;
              });

              // 确保状态总是被重置
              setIsLoading(false);
              options.onDone?.();
            } catch (error) {
              // 即使回调出错，也要确保状态重置
              setIsLoading(false);
              console.error('onDone callback error:', error);
            }
          }
        }
      );
    } catch (error) {
 
      options.onError?.(error instanceof Error ? error.message : '发送消息失败');
    } finally {
      // 作为最后的安全保障，确保 isLoading 状态被重置
      setIsLoading(false);
    }
  }, [isLoading, options]);

  return {
    messages,
    isLoading,
    currentThinking,
    currentThinkingContent,
    sendMessage,
    clearMessages,
    stopGeneration
  };
}