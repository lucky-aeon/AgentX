"use client"

import { useState, useRef, useEffect } from "react"
import { FileText, Send, ClipboardList, Wrench, CheckCircle, ListTodo } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { streamChat } from "@/lib/api"
import { toast } from "@/components/ui/use-toast"
import { getSessionMessages, getSessionMessagesWithToast, type MessageDTO } from "@/lib/session-message-service"
import { Skeleton } from "@/components/ui/skeleton"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import { Highlight, themes } from "prism-react-renderer"
import { CurrentTaskList } from "@/components/current-task-list"
import { MessageType, type Message as MessageInterface } from "@/types/conversation"
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'

interface ChatPanelProps {
  conversationId: string
  onToggleTaskHistory?: () => void
  showTaskHistory?: boolean
  isFunctionalAgent?: boolean
  agentName?: string
}

interface Message {
  id: string
  role: "USER" | "SYSTEM" | "assistant"
  content: string
}

interface AssistantMessage {
  id: string
  hasContent: boolean
}

interface StreamData {
  content: string
  done: boolean
  sessionId: string
  provider: string
  model: string
  timestamp: number
}

export function ChatPanel({ conversationId, onToggleTaskHistory, showTaskHistory = false, isFunctionalAgent = false, agentName = "AI助手" }: ChatPanelProps) {
  const [input, setInput] = useState("")
  const [messages, setMessages] = useState<MessageInterface[]>([])
  const [isTyping, setIsTyping] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [autoScroll, setAutoScroll] = useState(true)
  const [isThinking, setIsThinking] = useState(false)
  const [currentAssistantMessage, setCurrentAssistantMessage] = useState<AssistantMessage | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const chatContainerRef = useRef<HTMLDivElement>(null)

  // 获取会话消息
  useEffect(() => {
    const fetchSessionMessages = async () => {
      if (!conversationId) return
      
      try {
        setLoading(true)
        setError(null)
        // 清空之前的消息，避免显示上一个会话的内容
        setMessages([])
        
        // 获取会话消息
        const messagesResponse = await getSessionMessagesWithToast(conversationId)
        
        if (messagesResponse.code === 200 && messagesResponse.data) {
          // 转换消息格式
          const formattedMessages = messagesResponse.data.map((msg: MessageDTO) => {
            // 将SYSTEM角色的消息视为assistant
            const normalizedRole = msg.role === "SYSTEM" ? "assistant" : msg.role as "USER" | "SYSTEM" | "assistant"
            
            // 获取消息类型，优先使用messageType字段
            let messageType = MessageType.TEXT
            if (msg.messageType) {
              // 尝试转换为枚举值
              try {
                messageType = msg.messageType as MessageType
              } catch (e) {
                console.warn("Unknown message type:", msg.messageType)
              }
            }
            
            return {
              id: msg.id,
              role: normalizedRole,
              content: msg.content,
              type: messageType,
              createdAt: msg.createdAt,
              updatedAt: msg.updatedAt
            }
          })
          
          setMessages(formattedMessages)
        } else {
          const errorMessage = messagesResponse.message || "获取会话消息失败"
          console.error(errorMessage)
          setError(errorMessage)
        }
      } catch (error) {
        console.error("获取会话消息错误:", error)
        setError(error instanceof Error ? error.message : "获取会话消息时发生未知错误")
      } finally {
        setLoading(false)
      }
    }

    fetchSessionMessages()
  }, [conversationId])

  // 滚动到底部
  useEffect(() => {
    if (autoScroll) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
    }
  }, [messages, isTyping, autoScroll])

  // 监听滚动事件
  useEffect(() => {
    const chatContainer = chatContainerRef.current
    if (!chatContainer) return

    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = chatContainer
      // 判断是否滚动到底部附近（20px误差范围）
      const isAtBottom = scrollHeight - scrollTop - clientHeight < 20
      setAutoScroll(isAtBottom)
    }

    chatContainer.addEventListener('scroll', handleScroll)
    return () => chatContainer.removeEventListener('scroll', handleScroll)
  }, [])

  // 处理用户主动发送消息时强制滚动到底部
  const scrollToBottom = () => {
    setAutoScroll(true)
    // 使用setTimeout确保在下一个渲染周期执行
    setTimeout(() => {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
    }, 100)
  }

  // 处理发送消息
  const handleSendMessage = async () => {
    if (!input.trim() || !conversationId) return

    const userMessage = input.trim()
    setInput("")
    setIsTyping(true)
    setIsThinking(true) // 设置思考状态
    setCurrentAssistantMessage(null) // 重置助手消息状态
    scrollToBottom() // 用户发送新消息时强制滚动到底部

    // 添加用户消息到消息列表
    const userMessageId = `user-${Date.now()}`
    setMessages((prev) => [
      ...prev,
      {
        id: userMessageId,
        role: "USER",
        content: userMessage,
        type: MessageType.TEXT,
        createdAt: new Date().toISOString()
      },
    ])

    try {
      // 发送消息到服务器并获取流式响应
      const response = await streamChat(userMessage, conversationId)

      if (!response.ok) {
        throw new Error(`Stream chat failed with status ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error("No reader available")
      }

      // 添加助理消息到消息列表
      const assistantMessageId = `assistant-${Date.now()}`
      setCurrentAssistantMessage({ id: assistantMessageId, hasContent: false })
      setMessages((prev) => [
        ...prev,
        {
          id: assistantMessageId,
          role: "assistant",
          content: "",
          type: MessageType.TEXT,
          createdAt: new Date().toISOString()
        },
      ])

      let accumulatedContent = ""
      const decoder = new TextDecoder()
      let hasReceivedFirstResponse = false
      
      // 用于解析SSE格式数据的变量
      let buffer = ""

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        // 解码数据块并添加到缓冲区
        buffer += decoder.decode(value, { stream: true })
        
        // 处理缓冲区中的SSE数据
        const lines = buffer.split("\n\n")
        // 保留最后一个可能不完整的行
        buffer = lines.pop() || ""
        
        for (const line of lines) {
          if (line.startsWith("data:")) {
            try {
              // 提取JSON部分（去掉前缀"data:"）
              const jsonStr = line.substring(5)
              const data = JSON.parse(jsonStr) as StreamData
              
              if (data.content) {
                // 收到第一个响应，结束思考状态
                if (!hasReceivedFirstResponse) {
                  hasReceivedFirstResponse = true
                  setIsThinking(false)
                }
                
                accumulatedContent += data.content
                
                // 更新现有的助手消息
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMessageId ? { ...msg, content: accumulatedContent } : msg,
                  ),
                )
                
                // 更新助手消息状态
                setCurrentAssistantMessage({ id: assistantMessageId, hasContent: true })
              }
              
              // 如果返回了done标记，则结束处理
              if (data.done) {
                console.log("Stream completed with done flag")
                setIsThinking(false) // 确保在完成时关闭思考状态
              }
            } catch (e) {
              console.error("Error parsing SSE data:", e, line)
            }
          }
        }
      }
    } catch (error) {
      console.error("Error in stream chat:", error)
      setIsThinking(false) // 错误发生时关闭思考状态
      toast({
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      })
    } finally {
      setIsTyping(false)
    }
  }

  // 处理按键事件
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  // 格式化消息时间
  const formatMessageTime = (timestamp?: string) => {
    if (!timestamp) return '';
    try {
      const date = new Date(timestamp);
      return date.toLocaleString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    } catch (e) {
      return '';
    }
  };

  // 根据消息类型获取图标和文本
  const getMessageTypeInfo = (type: MessageType) => {
    switch (type) {
      case MessageType.TOOL_CALL:
        return {
          icon: <Wrench className="h-5 w-5 text-blue-500" />,
          text: '工具调用'
        };
      case MessageType.TASK_EXEC:
        return {
          icon: <ListTodo className="h-5 w-5 text-purple-500" />,
          text: '任务执行'
        };
      case MessageType.TASK_STATUS:
        return {
          icon: <CheckCircle className="h-5 w-5 text-green-500" />,
          text: '任务状态'
        };
      case MessageType.TASK_IDS:
        return {
          icon: <ListTodo className="h-5 w-5 text-orange-500" />,
          text: '任务ID列表'
        };
      case MessageType.TEXT:
      default:
        return {
          icon: null,
          text: agentName
        };
    }
  };

  // 渲染消息内容
  const renderMessageContent = (message: MessageInterface) => {
    return (
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          // 代码块渲染
          code({ inline, className, children, ...props }: any) {
            const match = /language-(\w+)/.exec(className || "");
            return !inline && match ? (
              <Highlight
                theme={themes.vsDark}
                code={String(children).replace(/\n$/, "")}
                language={match[1]}
              >
                {({ className, style, tokens, getLineProps, getTokenProps }) => (
                  <pre
                    className={`${className} rounded p-2 my-2 overflow-auto text-sm`}
                    style={style}
                  >
                    {tokens.map((line, i) => (
                      <div key={i} {...getLineProps({ line, key: i })}>
                        <span className="text-gray-500 mr-2 text-right w-6 inline-block select-none">
                          {i + 1}
                        </span>
                        {line.map((token, key) => (
                          <span key={key} {...getTokenProps({ token, key })} />
                        ))}
                      </div>
                    ))}
                  </pre>
                )}
              </Highlight>
            ) : (
              <code className={`${className} bg-gray-100 px-1 py-0.5 rounded`} {...props}>
                {children}
              </code>
            );
          },
        }}
      >
        {message.content}
      </ReactMarkdown>
    );
  };

  return (
    <div className="relative flex h-full w-full flex-col overflow-hidden bg-white">
      <div className="flex items-center justify-between px-4 py-2 border-b">
        <div className="flex items-center">
          <FileText className="h-5 w-5 text-gray-500 mr-2" />
          <span className="font-medium">对话</span>
        </div>
        {isFunctionalAgent && (
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={onToggleTaskHistory}
          >
            <ClipboardList className={`h-5 w-5 ${showTaskHistory ? 'text-primary' : 'text-gray-500'}`} />
          </Button>
        )}
      </div>

      <div 
        ref={chatContainerRef}
        className="flex-1 overflow-y-auto px-4 pt-3 pb-4 w-full"
      >
        {loading ? (
          // 加载状态
          <div className="flex items-center justify-center h-full w-full">
            <div className="text-center">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-2 border-gray-200 border-t-blue-500 mb-2"></div>
              <p className="text-gray-500">正在加载消息...</p>
            </div>
          </div>
        ) : (
          <div className="space-y-4 w-full">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-600">
                {error}
              </div>
            )}
            
            {/* 消息内容 */}
            <div className="space-y-6 w-full">
              {messages.length === 0 ? (
                <div className="flex items-center justify-center h-20 w-full">
                  <p className="text-gray-400">暂无消息，开始发送消息吧</p>
                </div>
              ) : (
                messages.map((message) => (
                  <div
                    key={message.id}
                    className={`w-full`}
                  >
                    {/* 用户消息 */}
                    {message.role === "USER" ? (
                      <div className="flex justify-end">
                        <div className="max-w-[80%]">
                          <div className="bg-blue-50 text-gray-800 p-3 rounded-lg shadow-sm">
                            {message.content}
                          </div>
                          <div className="text-xs text-gray-500 mt-1 text-right">
                            {formatMessageTime(message.createdAt)}
                          </div>
                        </div>
                      </div>
                    ) : (
                      /* AI消息 */
                      <div className="flex">
                        <div className="h-8 w-8 mr-2 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                          {message.type && message.type !== MessageType.TEXT 
                            ? getMessageTypeInfo(message.type).icon 
                            : <div className="text-lg">🤖</div>
                          }
                        </div>
                        <div className="max-w-[80%]">
                          {/* 消息类型指示 */}
                          <div className="flex items-center mb-1 text-xs text-gray-500">
                            <span className="font-medium">
                              {message.type ? getMessageTypeInfo(message.type).text : agentName}
                            </span>
                            <span className="mx-1 text-gray-400">·</span>
                            <span>{formatMessageTime(message.createdAt)}</span>
                          </div>
                          
                          {/* 消息内容 */}
                          <div className="p-3 rounded-lg">
                            {renderMessageContent(message)}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                ))
              )}
              
              {/* 思考中提示 */}
              {isThinking && (!currentAssistantMessage || !currentAssistantMessage.hasContent) && (
                <div className="flex items-start">
                  <div className="h-8 w-8 mr-2 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
                    <div className="text-lg">🤖</div>
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
              
              <div ref={messagesEndRef} />
              {!autoScroll && isTyping && (
                <Button
                  variant="outline"
                  size="sm"
                  className="fixed bottom-20 right-6 rounded-full shadow-md bg-white"
                  onClick={scrollToBottom}
                >
                  <span>↓</span>
                </Button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* 输入框上方显示当前任务列表 */}
      {isFunctionalAgent && (
        <div className="px-4 py-2">
          <CurrentTaskList />
        </div>
      )}

      {/* 输入框 */}
      <div className="border-t p-2 bg-white">
        <div className="flex items-end gap-2 max-w-5xl mx-auto">
          <Textarea
            placeholder="输入消息...(Shift+Enter换行, Enter发送)"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyPress}
            className="min-h-[56px] flex-1 resize-none overflow-hidden rounded-xl bg-white px-3 py-2 font-normal border-gray-200 shadow-sm focus-visible:ring-2 focus-visible:ring-blue-400 focus-visible:ring-opacity-50"
            rows={Math.min(5, Math.max(2, input.split('\n').length))}
          />
          <Button 
            onClick={handleSendMessage} 
            disabled={!input.trim()} 
            className="h-10 w-10 rounded-xl bg-blue-500 hover:bg-blue-600 shadow-sm"
          >
            <Send className="h-5 w-5" />
          </Button>
        </div>
      </div>
    </div>
  )
}

