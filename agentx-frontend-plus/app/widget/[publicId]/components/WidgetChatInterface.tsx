"use client";

import React, { useState, useRef, useEffect, useCallback } from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Send, MessageCircle, User, Loader2, Bot } from "lucide-react";
import { cn } from "@/lib/utils";
import { widgetChatStream, handleWidgetStream, type WidgetChatRequest, type WidgetChatResponse } from '@/lib/widget-chat-service';
import { MessageType } from '@/types/conversation';
import { MessageMarkdown } from '@/components/ui/message-markdown';

interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  timestamp: number;
  isStreaming?: boolean;
  type?: MessageType;
  payload?: string; // 用于存储RAG检索结果等额外数据
}

interface WidgetChatInterfaceProps {
  publicId: string;
  agentName: string;
  agentAvatar?: string;
  welcomeMessage?: string;
  systemPrompt?: string;
  toolIds?: string[];
  knowledgeBaseIds?: string[];
}

// 生成UUID的简单函数
const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};

export function WidgetChatInterface({ 
  publicId, 
  agentName, 
  agentAvatar,
  welcomeMessage,
  systemPrompt,
  toolIds,
  knowledgeBaseIds
}: WidgetChatInterfaceProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isThinking, setIsThinking] = useState(false);
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null);
  const [sessionId] = useState<string>(generateUUID()); // 生成并保持会话ID
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  
  // 消息处理状态管理（复用预览聊天逻辑）
  const hasReceivedFirstResponse = useRef(false);
  const messageContentAccumulator = useRef({
    content: "",
    type: MessageType.TEXT as MessageType,
    payload: undefined as string | undefined
  });
  const messageSequenceNumber = useRef(0);
  const [completedTextMessages, setCompletedTextMessages] = useState<Set<string>>(new Set());
  const [currentAssistantMessage, setCurrentAssistantMessage] = useState<{ id: string; hasContent: boolean } | null>(null);
  const [autoScroll, setAutoScroll] = useState(true);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const isUserScrolling = useRef(false);
  const scrollTimer = useRef<NodeJS.Timeout | null>(null);

  // 检测用户是否正在手动滚动
  useEffect(() => {
    // 查找ScrollArea内部的实际滚动容器
    const scrollContainer = scrollContainerRef.current?.querySelector('[data-radix-scroll-area-viewport]') as HTMLElement;
    if (!scrollContainer) return;

    const handleScroll = () => {
      isUserScrolling.current = true;
      
      // 清除之前的定时器
      if (scrollTimer.current) {
        clearTimeout(scrollTimer.current);
      }
      
      // 设置定时器，500ms后认为用户停止滚动
      scrollTimer.current = setTimeout(() => {
        isUserScrolling.current = false;
      }, 500);

      // 检查是否滚动到底部
      const { scrollTop, scrollHeight, clientHeight } = scrollContainer;
      const isAtBottom = Math.abs(scrollHeight - clientHeight - scrollTop) < 10;
      
      if (isAtBottom) {
        setAutoScroll(true);
      } else {
        setAutoScroll(false);
      }
    };

    scrollContainer.addEventListener('scroll', handleScroll, { passive: true });
    
    return () => {
      scrollContainer.removeEventListener('scroll', handleScroll);
      if (scrollTimer.current) {
        clearTimeout(scrollTimer.current);
      }
    };
  }, []);

  // 智能滚动到底部 - 只在用户不在滚动且开启自动滚动时滚动
  useEffect(() => {
    if (autoScroll && !isUserScrolling.current && messagesEndRef.current) {
      // 使用requestAnimationFrame确保DOM更新完成
      requestAnimationFrame(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
      });
    }
  }, [messages, isThinking, autoScroll]);

  // 处理用户主动发送消息时强制滚动到底部
  const scrollToBottom = useCallback(() => {
    setAutoScroll(true);
    // 立即滚动，不等待用户停止滚动
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, 100);
  }, []);

  // 初始化时添加欢迎消息和状态重置
  useEffect(() => {
    // 重置消息处理状态
    hasReceivedFirstResponse.current = false;
    messageContentAccumulator.current = {
      content: "",
      type: MessageType.TEXT,
      payload: undefined
    };
    setCompletedTextMessages(new Set());
    messageSequenceNumber.current = 0;
    setCurrentAssistantMessage(null);
    
    if (welcomeMessage && messages.length === 0) {
      const welcomeMsg: Message = {
        id: 'welcome',
        role: 'ASSISTANT',
        content: welcomeMessage,
        timestamp: Date.now(),
        type: MessageType.TEXT
      };
      setMessages([welcomeMsg]);
    }
  }, [welcomeMessage]);

  // 发送消息 - 使用Widget专用聊天API，无需认证
  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'USER',
      content: inputValue.trim(),
      timestamp: Date.now(),
      type: MessageType.TEXT
    };

    // 添加用户消息
    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setIsLoading(true);
    setIsThinking(true); // 设置思考状态
    setCurrentAssistantMessage(null); // 重置助手消息状态
    scrollToBottom(); // 用户发送新消息时强制滚动到底部
    
    // 重置所有状态
    setCompletedTextMessages(new Set());
    resetMessageAccumulator();
    hasReceivedFirstResponse.current = false;
    messageSequenceNumber.current = 0;

    try {
      // 构建Widget聊天请求
      const chatRequest: WidgetChatRequest = {
        message: userMessage.content,
        sessionId: sessionId, // 使用生成的会话ID
        fileUrls: [] // 暂不支持文件
      };

      console.log('Widget聊天请求数据:', chatRequest);

      // 使用Widget聊天流式处理
      const stream = await widgetChatStream(publicId, chatRequest);
      if (!stream) {
        throw new Error('Failed to get widget stream');
      }

      // 生成基础消息ID，作为所有消息序列的前缀
      const baseMessageId = Date.now().toString();
      
      // 重置状态
      hasReceivedFirstResponse.current = false;
      messageContentAccumulator.current = {
        content: "",
        type: MessageType.TEXT,
        payload: undefined
      };

      await handleWidgetStream(
        stream,
        (response: WidgetChatResponse) => {
          console.log('收到Widget流式响应:', response);
          // 处理消息 - 传递baseMessageId作为前缀
          handleStreamDataMessage(response, baseMessageId);
        },
        (error: Error) => {
          console.error('Widget stream error:', error);
          handleStreamError(error);
        },
        () => {
          console.log('Widget stream completed');
          setIsLoading(false);
          setIsThinking(false);
        }
      );
    } catch (error) {
      console.error('Widget聊天请求失败:', error);
      handleStreamError(error instanceof Error ? error : new Error('未知错误'));
    }
  };

  // 消息处理主函数 - 处理Widget聊天响应
  const handleStreamDataMessage = (data: WidgetChatResponse, baseMessageId: string) => {
    // 首次响应处理
    if (!hasReceivedFirstResponse.current) {
      hasReceivedFirstResponse.current = true;
      setIsThinking(false);
    }
    
    // 处理错误消息
    if (isErrorMessage(data)) {
      handleErrorMessage(data);
      return;
    }
    
    // 获取消息类型，默认为TEXT
    const messageType = (data.messageType as MessageType) || MessageType.TEXT;
    
    // 生成当前消息序列的唯一ID
    const currentMessageId = `assistant-${messageType}-${baseMessageId}-seq${messageSequenceNumber.current}`;
    
    console.log(`处理消息: 类型=${messageType}, 序列=${messageSequenceNumber.current}, ID=${currentMessageId}, done=${data.done}`);
    
    // 处理消息内容（用于UI显示）- 简化版本，只显示最终回答
    const displayableTypes = [
      undefined, 
      "TEXT", 
      "RAG_ANSWER_PROGRESS"  // 只处理RAG回答内容，不显示中间过程
    ];
    const isDisplayableType = displayableTypes.includes(data.messageType);
    
    if (isDisplayableType && data.content) {
      // 累积消息内容
      messageContentAccumulator.current.content += data.content;
      messageContentAccumulator.current.type = messageType;
      
      // 更新UI显示
      updateOrCreateMessageInUI(currentMessageId, messageContentAccumulator.current);
    }
    
    // 消息结束信号处理
    if (data.done) {
      console.log(`消息完成 (done=true), 类型: ${messageType}, 序列: ${messageSequenceNumber.current}`);
      
      // 如果是可显示类型且有内容，完成该消息
      if (isDisplayableType && messageContentAccumulator.current.content) {
        finalizeMessage(currentMessageId, messageContentAccumulator.current);
      }
      
      // 无论如何，都重置消息累积器，准备接收下一条消息
      resetMessageAccumulator();
      
      // 增加消息序列计数
      messageSequenceNumber.current += 1;
      
      console.log(`消息序列增加到: ${messageSequenceNumber.current}`);
    }
  };
  
  // 更新或创建UI消息
  const updateOrCreateMessageInUI = (messageId: string, messageData: {
    content: string;
    type: MessageType;
    payload?: string;
  }) => {
    // 使用函数式更新，在一次原子操作中检查并更新/创建消息
    setMessages(prev => {
      // 检查消息是否已存在
      const messageIndex = prev.findIndex(msg => msg.id === messageId);
      
      if (messageIndex >= 0) {
        // 消息已存在，只需更新内容
        console.log(`更新现有消息: ${messageId}, 内容长度: ${messageData.content.length}`);
        const newMessages = [...prev];
        newMessages[messageIndex] = {
          ...newMessages[messageIndex],
          content: messageData.content,
          payload: messageData.payload || newMessages[messageIndex].payload
        };
        return newMessages;
      } else {
        // 消息不存在，创建新消息
        console.log(`创建新消息: ${messageId}, 类型: ${messageData.type}`);
        return [
          ...prev,
          {
            id: messageId,
            role: "ASSISTANT" as const,
            content: messageData.content,
            type: messageData.type,
            timestamp: Date.now(),
            isStreaming: true,
            payload: messageData.payload
          }
        ];
      }
    });
    
    // 更新当前助手消息状态
    setCurrentAssistantMessage({ id: messageId, hasContent: true });
    setStreamingMessageId(messageId);
  };
  
  // 完成消息处理
  const finalizeMessage = (messageId: string, messageData: {
    content: string;
    type: MessageType;
    payload?: string;
  }) => {
    console.log(`完成消息: ${messageId}, 类型: ${messageData.type}, 内容长度: ${messageData.content.length}`);
    
    // 如果消息内容为空，不处理
    if (!messageData.content || messageData.content.trim() === "") {
      console.log("消息内容为空，不处理");
      return;
    }
    
    // 确保UI已更新到最终状态，使用相同的原子操作模式
    setMessages(prev => {
      // 检查消息是否已存在
      const messageIndex = prev.findIndex(msg => msg.id === messageId);
      
      if (messageIndex >= 0) {
        // 消息已存在，更新内容
        console.log(`完成现有消息: ${messageId}`);
        const newMessages = [...prev];
        newMessages[messageIndex] = {
          ...newMessages[messageIndex],
          content: messageData.content,
          isStreaming: false,
          payload: messageData.payload || newMessages[messageIndex].payload
        };
        return newMessages;
      } else {
        // 消息不存在，创建新消息
        console.log(`创建并完成新消息: ${messageId}`);
        return [
          ...prev,
          {
            id: messageId,
            role: "ASSISTANT" as const,
            content: messageData.content,
            type: messageData.type,
            timestamp: Date.now(),
            isStreaming: false,
            payload: messageData.payload
          }
        ];
      }
    });
    
    // 标记消息为已完成
    setCompletedTextMessages(prev => {
      const newSet = new Set(prev);
      newSet.add(messageId);
      return newSet;
    });
    
    setStreamingMessageId(null);
  };

  // 重置消息累积器
  const resetMessageAccumulator = () => {
    console.log("重置消息累积器");
    messageContentAccumulator.current = {
      content: "",
      type: MessageType.TEXT,
      payload: undefined
    };
  };

  // 判断是否为错误消息
  const isErrorMessage = (data: WidgetChatResponse): boolean => {
    return !!data.content && (
      data.content.includes("Error updating database") || 
      data.content.includes("PSQLException") || 
      data.content.includes("任务执行过程中发生错误")
    );
  };

  // 处理错误消息
  const handleErrorMessage = (data: WidgetChatResponse) => {
    console.error("检测到后端错误:", data.content);
    // 这里可以添加 toast 通知，但先保持简单
  };

  // 处理流处理错误
  const handleStreamError = (error: Error) => {
    setIsThinking(false);
    setIsLoading(false);
    setStreamingMessageId(null);
    
    // 添加错误消息到聊天
    const errorMessage: Message = {
      id: Date.now().toString(),
      role: 'ASSISTANT',
      content: '抱歉，发生了一些错误，请稍后再试。',
      timestamp: Date.now(),
      type: MessageType.TEXT
    };
    setMessages(prev => [...prev, errorMessage]);
    
    // 重新聚焦输入框
    inputRef.current?.focus();
  };

  // 根据消息类型获取图标和文本 - 简化版本
  const getMessageTypeInfo = (type?: MessageType) => {
    // 统一使用普通的Assistant样式，不区分消息类型
    return {
      icon: <Bot className="h-4 w-4" />,
      text: agentName
    };
  };

  // 格式化消息时间
  const formatMessageTime = (timestamp?: number | string) => {
    if (!timestamp) return '刚刚';
    try {
      const date = typeof timestamp === 'number' ? new Date(timestamp) : new Date(timestamp);
      return date.toLocaleString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return '刚刚';
    }
  };


  // 处理回车发送
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="flex flex-col h-[500px] bg-white rounded-lg border">
      {/* 消息区域 */}
      <ScrollArea className="flex-1 p-4" ref={scrollContainerRef}>
        <div className="space-y-4">
          {messages.map((message) => (
            <div key={message.id} className="w-full">
              {/* 用户消息 */}
              {message.role === 'USER' ? (
                <div className="flex justify-end">
                  <div className="max-w-[80%]">
                    {/* 消息内容 */}
                    {message.content && (
                      <div className="bg-blue-50 text-gray-800 p-3 rounded-lg shadow-sm">
                        <div className="text-sm whitespace-pre-wrap">
                          {message.content}
                        </div>
                      </div>
                    )}
                    
                    <div className="text-xs text-gray-500 mt-1 text-right">
                      {formatMessageTime(message.timestamp)}
                    </div>
                  </div>
                </div>
              ) : (
                /* AI消息 */
                <div className="flex items-start">
                  <div className="h-8 w-8 mr-2 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                    {message.type && message.type !== MessageType.TEXT 
                      ? getMessageTypeInfo(message.type).icon 
                      : (agentAvatar ? (
                          <img src={agentAvatar} alt={agentName} className="h-8 w-8 rounded-full object-cover" />
                        ) : (
                          <div className="text-lg">🤖</div>
                        ))
                    }
                  </div>
                  <div className="max-w-[80%]">
                    {/* 消息类型指示 */}
                    <div className="flex items-center mb-1 text-xs text-gray-500">
                      <span className="font-medium">
                        {message.type ? getMessageTypeInfo(message.type).text : agentName}
                      </span>
                      <span className="mx-1 text-gray-400">·</span>
                      <span>{formatMessageTime(message.timestamp)}</span>
                    </div>
                    
                    {/* 消息内容 */}
                    {message.content && (
                      <div className={`p-3 rounded-lg ${
                        message.content.startsWith('抱歉，发生了一些错误')
                          ? 'bg-red-50 text-red-700 border border-red-200'
                          : ''
                      }`}>
                        {message.content.startsWith('抱歉，发生了一些错误') ? (
                          // 错误消息使用简单文本显示
                          <div className="text-sm whitespace-pre-wrap">
                            {message.content}
                            {message.isStreaming && (
                              <span className="inline-block w-2 h-4 bg-current opacity-75 animate-pulse ml-1" />
                            )}
                          </div>
                        ) : (
                          // 所有正常消息统一使用Markdown渲染
                          <div className="markdown-content">
                            <MessageMarkdown 
                              content={message.content + (message.isStreaming ? ' ▌' : '')}
                              showCopyButton={true}
                            />
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}

          {/* 思考中提示 - 和预览聊天保持一致 */}
          {isThinking && (!currentAssistantMessage || !currentAssistantMessage.hasContent) && (
            <div className="flex items-start">
              <div className="h-8 w-8 mr-2 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                {agentAvatar ? (
                  <img src={agentAvatar} alt={agentName} className="h-8 w-8 rounded-full object-cover" />
                ) : (
                  <div className="text-lg">🤖</div>
                )}
              </div>
              <div className="max-w-[80%]">
                <div className="flex items-center mb-1 text-xs text-gray-500">
                  <span className="font-medium">{agentName}</span>
                  <span className="mx-1 text-gray-400">·</span>
                  <span>刚刚</span>
                </div>
                <div className="space-y-2 p-3 rounded-lg">
                  <div className="flex space-x-2 items-center">
                    <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></div>
                    <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse delay-75"></div>
                    <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse delay-150"></div>
                    <div className="text-sm text-gray-500 animate-pulse">思考中...</div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 滚动到底部按钮 - 当用户手动滚动离开底部时显示 */}
          {!autoScroll && (
            <div className="sticky bottom-0 flex justify-center py-2">
              <Button
                variant="outline"
                size="sm"
                className="rounded-full shadow-md bg-white hover:bg-gray-50"
                onClick={scrollToBottom}
              >
                <span className="text-sm">↓ 回到底部</span>
              </Button>
            </div>
          )}
        </div>
        <div ref={messagesEndRef} />
      </ScrollArea>

      {/* 输入区域 */}
      <div className="border-t p-4">
        <div className="flex gap-2">
          <Input
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder={`与${agentName}对话...`}
            disabled={isLoading}
            className="flex-1"
          />
          <Button 
            onClick={handleSendMessage}
            disabled={!inputValue.trim() || isLoading}
            size="icon"
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>
        <div className="mt-2 text-xs text-muted-foreground">
          按 Enter 发送消息，Shift + Enter 换行
        </div>
      </div>
    </div>
  );
}