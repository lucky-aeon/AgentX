"use client"

import { useState, useRef, useEffect } from "react"
import { FileText, Send } from 'lucide-react'
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

interface ChatPanelProps {
  conversationId: string
}

interface Message {
  id: string
  role: "USER" | "SYSTEM" | "assistant"
  content: string
}

interface StreamData {
  content: string
  done: boolean
  sessionId: string
  provider: string
  model: string
  timestamp: number
}

export function ChatPanel({ conversationId }: ChatPanelProps) {
  const [input, setInput] = useState("")
  const [messages, setMessages] = useState<Message[]>([])
  const [isTyping, setIsTyping] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [autoScroll, setAutoScroll] = useState(true)
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
          const formattedMessages = messagesResponse.data.map((msg: MessageDTO) => ({
            id: msg.id,
            role: msg.role as "USER" | "SYSTEM" | "assistant",
            content: msg.content,
          }))
          
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
    scrollToBottom() // 用户发送新消息时强制滚动到底部

    // 添加用户消息到消息列表
    const userMessageId = `user-${Date.now()}`
    setMessages((prev) => [
      ...prev,
      {
        id: userMessageId,
        role: "USER",
        content: userMessage,
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

      // 添加助理消息到消息列表 - 使用固定的ID以便于更新
      const assistantMessageId = `assistant-${Date.now()}`
      setMessages((prev) => [
        ...prev,
        {
          id: assistantMessageId,
          role: "assistant",
          content: "",
        },
      ])

      let accumulatedContent = ""
      const decoder = new TextDecoder()
      
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
                accumulatedContent += data.content
                
                // 更新现有的助手消息
                setMessages((prev) =>
                  prev.map((msg) =>
                    msg.id === assistantMessageId ? { ...msg, content: accumulatedContent } : msg,
                  ),
                )
              }
              
              // 如果返回了done标记，则结束处理
              if (data.done) {
                console.log("Stream completed with done flag")
              }
            } catch (e) {
              console.error("Error parsing SSE data:", e, line)
            }
          }
        }
      }
    } catch (error) {
      console.error("Error in stream chat:", error)
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

  return (
    <div className="flex flex-col h-full w-full">
      {/* 消息列表 */}
      <div className="flex-1 overflow-y-auto p-2 bg-white" ref={chatContainerRef}>
        {loading ? (
          // 加载状态
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <div className="inline-block animate-spin rounded-full h-8 w-8 border-2 border-gray-200 border-t-blue-500 mb-2"></div>
              <p className="text-gray-500">正在加载消息...</p>
            </div>
          </div>
        ) : (
          <div className="space-y-4 max-w-5xl mx-auto">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-600">
                {error}
              </div>
            )}

            {/* 消息内容 */}
            <div className="space-y-3">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.role === "USER" ? "justify-end" : "justify-start"} mb-3`}
                >
                  {message.role !== "USER" && (
                    <div className="mr-2 h-8 w-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm shadow-sm flex-shrink-0">
                      A
                    </div>
                  )}
                  <div
                    className={`rounded-2xl px-3.5 py-2.5 ${
                      message.role === "USER"
                        ? "bg-blue-500 text-white shadow-sm"
                        : "bg-gray-100 border border-gray-200 shadow-sm"
                    }`}
                    style={{ 
                      wordWrap: 'break-word', 
                      overflowWrap: 'break-word', 
                      maxWidth: 'min(90%, 800px)',
                      position: 'relative'
                    }}
                  >
                    {message.content ? (
                      <div 
                        className={`prose prose-sm max-w-none break-words overflow-hidden ${
                          message.role === "USER" 
                            ? "prose-invert" 
                            : "prose-headings:text-gray-800"
                        }`} 
                        style={{ wordBreak: 'break-word', overflowWrap: 'break-word' }}
                      >
                        <ReactMarkdown 
                          remarkPlugins={[remarkGfm]}
                          components={{
                            code({node, inline, className, children, ...props}: any) {
                              const match = /language-(\w+)/.exec(className || '')
                              const language = match ? match[1] : ''
                              
                              return !inline ? (
                                <div className="overflow-x-auto my-3 rounded-lg" style={{ maxWidth: '100%' }}>
                                  <Highlight
                                    theme={message.role === "USER" ? themes.vsLight : themes.github}
                                    code={String(children).replace(/\n$/, '')}
                                    language={language || 'text'}
                                  >
                                    {({className, style, tokens, getLineProps, getTokenProps}) => (
                                      <pre className="p-3 rounded-lg" style={{
                                        ...style,
                                        overflowX: 'auto',
                                        margin: 0,
                                        maxWidth: '100%',
                                        whiteSpace: 'pre-wrap',
                                        wordBreak: 'break-word',
                                        backgroundColor: message.role === "USER" ? 'rgba(59, 130, 246, 0.15)' : 'rgba(0, 0, 0, 0.04)'
                                      }}>
                                        {tokens.map((line, i) => (
                                          <div key={i} {...getLineProps({line})} style={{ overflowWrap: 'break-word', wordBreak: 'break-all' }}>
                                            {line.map((token, key) => (
                                              <span key={key} {...getTokenProps({token})} />
                                            ))}
                                          </div>
                                        ))}
                                      </pre>
                                    )}
                                  </Highlight>
                                </div>
                              ) : (
                                <code className={`${message.role === "USER" ? "bg-blue-400/30" : "bg-gray-200"} px-1.5 py-0.5 rounded-md text-sm font-mono`} {...props}>
                                  {children}
                                </code>
                              )
                            },
                            pre({children}: any) {
                              return <div className="overflow-x-auto" style={{ maxWidth: '100%' }}>{children}</div>
                            },
                            p({children}: any) {
                              return <div className="break-words whitespace-normal my-2" style={{ overflowWrap: 'break-word', wordBreak: 'break-word' }}>{children}</div>
                            },
                            li({children}: any) {
                              return <li className="my-1">{children}</li>
                            },
                            ul({children}: any) {
                              return <ul className="list-disc pl-5 my-2">{children}</ul>
                            },
                            ol({children}: any) {
                              return <ol className="list-decimal pl-5 my-2">{children}</ol>
                            },
                            blockquote({children}: any) {
                              return <div className="border-l-4 border-gray-200 pl-4 my-2 italic" style={{ overflowWrap: 'break-word', wordBreak: 'break-word' }}>{children}</div>
                            },
                            table({children}: any) {
                              return <div className="overflow-x-auto my-2" style={{ maxWidth: '100%' }}><table className="border-collapse border border-gray-300">{children}</table></div>
                            },
                            a({node, children, href, ...props}: any) {
                              return <a href={href} className="break-all" style={{ wordBreak: 'break-all' }} {...props}>{children}</a>
                            }
                          }}
                        >
                          {message.content}
                        </ReactMarkdown>
                      </div>
                    ) : (
                      message.role === "SYSTEM" && isTyping ? (
                        <div className="flex space-x-2">
                          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" />
                          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce delay-100" />
                          <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce delay-200" />
                        </div>
                      ) : null
                    )}
                  </div>
                  {message.role === "USER" && (
                    <div className="ml-2 h-8 w-8 rounded-full bg-blue-500 flex items-center justify-center text-white text-sm shadow-sm flex-shrink-0">
                      U
                    </div>
                  )}
                </div>
              ))}
              <div ref={messagesEndRef} />
              {!autoScroll && isTyping && (
                <button
                  onClick={scrollToBottom}
                  className="fixed bottom-20 right-5 bg-blue-500 text-white rounded-full p-2 shadow-lg hover:bg-blue-600 transition-colors"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* 输入框 */}
      <div className="border-t p-2 bg-white">
        <div className="flex items-end gap-2 max-w-5xl mx-auto">
          <Textarea
            placeholder="输入消息...(Shift+Enter换行, Enter发送)"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSendMessage();
              }
            }}
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

