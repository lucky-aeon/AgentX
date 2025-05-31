"use client"

import React, { useState, useRef, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Loader2, MessageCircle, Send, Bot, User, AlertCircle } from 'lucide-react'
import { toast } from '@/hooks/use-toast'
import { previewAgent, type AgentPreviewRequest, type MessageHistoryItem } from '@/lib/agent-preview-service'

// 消息类型
interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  timestamp: number
  isStreaming?: boolean
}

// 组件属性
interface AgentPreviewChatProps {
  // Agent基本信息
  agentName: string
  agentAvatar?: string | null
  systemPrompt?: string
  welcomeMessage?: string
  
  // Agent配置
  toolIds?: string[]
  toolPresetParams?: Record<string, Record<string, Record<string, string>>>
  modelId?: string
  
  // 样式控制
  className?: string
  disabled?: boolean
  placeholder?: string
}

export default function AgentPreviewChat({
  agentName,
  agentAvatar,
  systemPrompt,
  welcomeMessage = "你好！我是你的AI助手，有什么可以帮助你的吗？",
  toolIds,
  toolPresetParams,
  modelId,
  className = "",
  disabled = false,
  placeholder = "输入消息进行预览..."
}: AgentPreviewChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // 初始化欢迎消息
  useEffect(() => {
    if (welcomeMessage) {
      setMessages([{
        id: 'welcome',
        role: 'ASSISTANT',
        content: welcomeMessage,
        timestamp: Date.now()
      }])
    }
  }, [welcomeMessage])

  // 自动滚动到底部
  useEffect(() => {
    if (scrollAreaRef.current) {
      const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
      if (scrollElement) {
        scrollElement.scrollTop = scrollElement.scrollHeight
      }
    }
  }, [messages])

  // 发送消息
  const sendMessage = async () => {
    if (!inputValue.trim() || isLoading || disabled) return

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'USER',
      content: inputValue.trim(),
      timestamp: Date.now()
    }

    // 添加用户消息
    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setIsLoading(true)

    // 创建AI响应消息
    const aiMessageId = (Date.now() + 1).toString()
    const aiMessage: ChatMessage = {
      id: aiMessageId,
      role: 'ASSISTANT',
      content: '',
      timestamp: Date.now(),
      isStreaming: true
    }

    setMessages(prev => [...prev, aiMessage])
    setStreamingMessageId(aiMessageId)

    try {
      // 构建消息历史
      const messageHistory: MessageHistoryItem[] = messages
        .filter(msg => msg.id !== 'welcome') // 排除欢迎消息
        .map(msg => ({
          id: msg.id,
          role: msg.role,
          content: msg.content,
          createdAt: new Date(msg.timestamp).toISOString()
        }))

      // 构建预览请求
      const previewRequest: AgentPreviewRequest = {
        userMessage: userMessage.content,
        systemPrompt,
        toolIds,
        toolPresetParams,
        messageHistory,
        modelId
      }

      // 发送预览请求
      await previewAgent(
        previewRequest,
        // 流式消息处理
        (content: string) => {
          console.log('Received streaming content:', content);
          setMessages(prev => prev.map(msg => 
            msg.id === aiMessageId 
              ? { ...msg, content: msg.content + content }
              : msg
          ))
        },
        // 完成处理
        (fullContent: string) => {
          console.log('Preview completed with full content:', fullContent);
          setMessages(prev => prev.map(msg => 
            msg.id === aiMessageId 
              ? { ...msg, content: fullContent, isStreaming: false }
              : msg
          ))
          setStreamingMessageId(null)
          setIsLoading(false)
        },
        // 错误处理
        (error: Error) => {
          console.error('Preview error:', error)
          setMessages(prev => prev.map(msg => 
            msg.id === aiMessageId 
              ? { 
                  ...msg, 
                  content: `预览出错: ${error.message}`, 
                  isStreaming: false 
                }
              : msg
          ))
          setStreamingMessageId(null)
          setIsLoading(false)
          
          toast({
            title: "预览失败",
            description: error.message,
            variant: "destructive"
          })
        }
      )
    } catch (error) {
      console.error('Preview request failed:', error)
      setStreamingMessageId(null)
      setIsLoading(false)
      
      toast({
        title: "预览失败", 
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive"
      })
    }
  }

  // 处理按键事件
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  // 清空对话
  const clearChat = () => {
    setMessages(welcomeMessage ? [{
      id: 'welcome',
      role: 'ASSISTANT',
      content: welcomeMessage,
      timestamp: Date.now()
    }] : [])
  }

  return (
    <Card className={`flex flex-col h-full ${className}`}>
      {/* 头部 */}
      <CardHeader className="flex-shrink-0 pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10">
              <AvatarImage src={agentAvatar || ""} alt="Agent Avatar" />
              <AvatarFallback className="bg-blue-100 text-blue-600">
                {agentName ? agentName.charAt(0).toUpperCase() : <Bot className="h-5 w-5" />}
              </AvatarFallback>
            </Avatar>
            <div>
              <h3 className="font-semibold text-lg">{agentName || "预览助理"}</h3>
              <p className="text-sm text-muted-foreground">预览模式</p>
            </div>
          </div>
          <Button 
            variant="outline" 
            size="sm" 
            onClick={clearChat}
            disabled={isLoading}
          >
            清空对话
          </Button>
        </div>
      </CardHeader>

      {/* 消息区域 */}
      <CardContent className="flex-1 p-0 overflow-hidden">
        <ScrollArea className="h-full px-6" ref={scrollAreaRef}>
          <div className="py-4 space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex gap-3 ${
                  message.role === 'USER' ? 'justify-end' : 'justify-start'
                }`}
              >
                {message.role === 'ASSISTANT' && (
                  <Avatar className="h-8 w-8 mt-1">
                    <AvatarImage src={agentAvatar || ""} alt="Agent" />
                    <AvatarFallback className="bg-blue-100 text-blue-600">
                      <Bot className="h-4 w-4" />
                    </AvatarFallback>
                  </Avatar>
                )}
                
                <div
                  className={`max-w-[80%] rounded-lg px-4 py-2 ${
                    message.role === 'USER'
                      ? 'bg-blue-500 text-white'
                      : message.content.startsWith('预览出错:')
                      ? 'bg-red-50 text-red-700 border border-red-200'
                      : 'bg-gray-100 text-gray-900'
                  }`}
                >
                  <div className="text-sm whitespace-pre-wrap">
                    {message.content}
                    {message.isStreaming && (
                      <span className="inline-block w-2 h-4 bg-current opacity-75 animate-pulse ml-1" />
                    )}
                  </div>
                  {message.content.startsWith('预览出错:') && (
                    <div className="flex items-center gap-1 mt-1 text-xs">
                      <AlertCircle className="h-3 w-3" />
                      <span>请检查Agent配置或网络连接</span>
                    </div>
                  )}
                </div>

                {message.role === 'USER' && (
                  <Avatar className="h-8 w-8 mt-1">
                    <AvatarFallback className="bg-green-100 text-green-600">
                      <User className="h-4 w-4" />
                    </AvatarFallback>
                  </Avatar>
                )}
              </div>
            ))}
          </div>
        </ScrollArea>
      </CardContent>

      {/* 输入区域 */}
      <div className="flex-shrink-0 p-4 border-t">
        <div className="flex gap-2">
          <Input
            ref={inputRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyPress}
            placeholder={disabled ? "预览功能已禁用" : placeholder}
            disabled={disabled || isLoading}
            className="flex-1"
          />
          <Button
            onClick={sendMessage}
            disabled={disabled || isLoading || !inputValue.trim()}
            size="icon"
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>
        {disabled && (
          <p className="text-xs text-muted-foreground mt-2">
            请填写必要的Agent信息后进行预览
          </p>
        )}
      </div>
    </Card>
  )
} 