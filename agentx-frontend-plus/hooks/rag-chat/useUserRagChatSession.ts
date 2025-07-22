import { useCallback, useRef, useState } from 'react';
import { UserRagChatSession } from '@/lib/rag-chat-service';
import type { RagStreamChatRequest, RagThinkingData } from '@/types/rag-dataset';

// 消息类型定义（复用现有的）
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

interface UseUserRagChatSessionOptions {
  onError?: (error: string) => void;
  onDone?: () => void;
}

export function useUserRagChatSession(options: UseUserRagChatSessionOptions = {}) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [currentThinking, setCurrentThinking] = useState<RagThinkingData | null>(null);
  const [currentThinkingContent, setCurrentThinkingContent] = useState<string>('');
  
  const chatSessionRef = useRef<UserRagChatSession | null>(null);
  const thinkingContentRef = useRef<string>('');
  const processedTimestamps = useRef<Set<number>>(new Set());

  // 清空对话
  const clearMessages = useCallback(() => {
    if (chatSessionRef.current) {
      chatSessionRef.current.abort();
    }
    setMessages([]);
    setCurrentThinking(null);
    setCurrentThinkingContent('');
    thinkingContentRef.current = '';
    processedTimestamps.current.clear();
    setIsLoading(false);
  }, []);

  // 停止生成
  const stopGeneration = useCallback(() => {
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
  }, []);

  // 发送消息 - 基于用户安装的RAG
  const sendMessage = useCallback(async (question: string, userRagId: string) => {
    console.log('[Chat Hook] sendMessage called:', { question, userRagId, isLoading });
    if (!question.trim() || isLoading || !userRagId) return;

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

    const initialMessages = [userMessage, assistantMessage];
    console.log('[Chat Hook] Adding initial messages:', initialMessages);
    setMessages(prev => [...prev, userMessage, assistantMessage]);
    setIsLoading(true);
    setCurrentThinking(null);
    setCurrentThinkingContent('');
    thinkingContentRef.current = '';
    processedTimestamps.current.clear();

    // 确保聊天会话已初始化
    if (!chatSessionRef.current) {
      chatSessionRef.current = new UserRagChatSession();
    }

    // 如果有正在进行的会话，先中止
    if (chatSessionRef.current.isActive()) {
      chatSessionRef.current.abort();
    }

    try {
      await chatSessionRef.current.start(
        userRagId,
        {
          question,
          stream: true
        },
        {
          onThinking: (data) => {
            console.log('[Chat Hook] onThinking called with:', data);
            // 检查 data 是否存在且有 type 属性
            if (!data || typeof data !== 'object') {
              console.warn('[Chat Hook] onThinking received invalid data:', data);
              return;
            }
            
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
            console.log('[Chat Hook] onContent called with:', { content, timestamp });
            // 移除时间戳去重逻辑，因为可能会导致内容丢失
            
            setMessages(prev => {
              console.log('[Chat Hook] Current messages before content update:', prev.length);
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                console.log('[Chat Hook] Last message before content update:', lastMessage);
                if (lastMessage && lastMessage.role === 'assistant') {
                  const updatedMessage = {
                    ...lastMessage,
                    content: lastMessage.content + content
                  };
                  console.log('[Chat Hook] Updated message with new content:', updatedMessage);
                  return [
                    ...prev.slice(0, -1),
                    updatedMessage
                  ];
                }
              }
              console.log('[Chat Hook] No assistant message to update with content');
              return prev;
            });
          },
          onError: (error) => {
            console.log('[Chat Hook] onError called with:', error);
            // 移除Toast错误通知，只通过聊天界面显示错误
            setMessages(prev => {
              console.log('[Chat Hook] Current messages before error update:', prev.length);
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                console.log('[Chat Hook] Last message:', lastMessage);
                if (lastMessage && lastMessage.role === 'assistant') {
                  const updatedMessages = [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      content: error || '抱歉，处理您的请求时出现了错误。请重试。',
                      isStreaming: false
                    }
                  ];
                  console.log('[Chat Hook] Updated messages with error:', updatedMessages);
                  return updatedMessages;
                }
              }
              console.log('[Chat Hook] No messages to update with error');
              return prev;
            });
          },
          onDone: () => {
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
            options.onDone?.();
            setIsLoading(false);
          }
        }
      );
    } catch (error) {
      console.error('User RAG chat error:', error);
      const errorMessage = error instanceof Error ? error.message : '发送消息失败';
      
      // 将错误消息显示在聊天界面中
      setMessages(prev => {
        if (prev.length > 0) {
          const lastMessage = prev[prev.length - 1];
          if (lastMessage && lastMessage.role === 'assistant') {
            return [
              ...prev.slice(0, -1),
              {
                ...lastMessage,
                content: errorMessage,
                isStreaming: false
              }
            ];
          }
        }
        return prev;
      });
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