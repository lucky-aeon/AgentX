"use client"

import { useState, useRef, useEffect } from "react"
import { FileText, Send } from "lucide-react"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { streamChat } from "@/lib/api"
import { toast } from "@/components/ui/use-toast"

// 对话数据
const conversations = [
  {
    id: "conv-1",
    workspaceId: "workspace-3",
    name: "聊天测试",
    icon: "📝",
    messages: [{ id: "m1", role: "assistant", content: "你好！我是你的 AI 助手。有什么可以帮助你的吗？" }],
  },
  {
    id: "conv-2",
    workspaceId: "workspace-3",
    name: "1",
    icon: "📝",
    messages: [{ id: "m2", role: "assistant", content: "这是测试助手1。请问有什么需要帮助的吗?" }],
  },
  {
    id: "conv-3",
    workspaceId: "workspace-3",
    name: "测试工具",
    icon: "🔧",
    messages: [{ id: "m3", role: "assistant", content: "这是测试工具助手。我可以帮助您测试各种功能。" }],
  },
  {
    id: "conv-4",
    workspaceId: "workspace-1",
    name: "图像生成",
    icon: "🖼️",
    messages: [{ id: "m4", role: "assistant", content: "你好！我是文生图助理。请告诉我你想要生成什么样的图像。" }],
  },
  {
    id: "conv-5",
    workspaceId: "workspace-2",
    name: "网络搜索",
    icon: "🔍",
    messages: [{ id: "m5", role: "assistant", content: "你好！我是深度搜索助理。我可以帮你搜索和分析网络上的信息。" }],
  },
]

interface ChatPanelProps {
  conversationId: string
}

interface Message {
  id: string
  role: "user" | "assistant"
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
  const [isTyping, setIsTyping] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  const [streamingContent, setStreamingContent] = useState("")
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  // 获取当前对话
  const conversation = conversations.find((c) => c.id === conversationId)

  // 当对话ID变化时，更新消息列表
  useEffect(() => {
    if (conversation) {
      setMessages([...conversation.messages])
    } else {
      setMessages([])
    }
  }, [conversationId, conversation])

  // 自动滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages, streamingContent])

  // 处理SSE格式的流式响应
  const handleSSEResponse = async (response: Response) => {
    if (!response.body) {
      throw new Error("Response body is null")
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ""
    let streamedText = ""

    setStreamingContent("")

    try {
      while (true) {
        const { value, done } = await reader.read()

        if (done) break

        // 解码当前块并添加到缓冲区
        buffer += decoder.decode(value, { stream: true })

        // 处理缓冲区中的所有完整行
        const lines = buffer.split("\n")
        buffer = lines.pop() || "" // 保留最后一个不完整的行

        for (const line of lines) {
          if (line.startsWith("data:")) {
            try {
              // 提取JSON部分
              const jsonStr = line.slice(5)
              const data: StreamData = JSON.parse(jsonStr)

              // 只处理内容部分
              if (data.content) {
                streamedText += data.content
                setStreamingContent(streamedText)
              }

              // 如果done为true，表示流结束
              if (data.done) {
                // 流式响应结束，将内容添加到消息列表
                if (streamedText) {
                  const assistantMessage: Message = {
                    id: `m${Date.now() + 1}`,
                    role: "assistant",
                    content: streamedText,
                  }

                  setMessages((prev) => [...prev, assistantMessage])
                  setStreamingContent("")
                }
                setIsTyping(false)
                return
              }
            } catch (e) {
              console.error("Error parsing SSE data:", e, line)
            }
          }
        }
      }

      // 处理最后可能的不完整行
      if (buffer.startsWith("data:")) {
        try {
          const jsonStr = buffer.slice(5)
          const data: StreamData = JSON.parse(jsonStr)

          if (data.content) {
            streamedText += data.content
            setStreamingContent(streamedText)
          }

          if (data.done && streamedText) {
            const assistantMessage: Message = {
              id: `m${Date.now() + 1}`,
              role: "assistant",
              content: streamedText,
            }

            setMessages((prev) => [...prev, assistantMessage])
            setStreamingContent("")
          }
        } catch (e) {
          console.error("Error parsing final SSE data:", e, buffer)
        }
      }

      // 如果到这里还没有结束，也将累积的内容添加到消息列表
      if (streamedText) {
        const assistantMessage: Message = {
          id: `m${Date.now() + 1}`,
          role: "assistant",
          content: streamedText,
        }

        setMessages((prev) => [...prev, assistantMessage])
        setStreamingContent("")
      }
    } catch (error) {
      console.error("Error reading stream:", error)
      // 如果是用户主动中断，不显示错误
      if (error.name !== "AbortError") {
        toast({
          title: "读取响应时出错",
          description: "请稍后再试",
          variant: "destructive",
        })
      }
    } finally {
      setIsTyping(false)
      abortControllerRef.current = null
    }
  }

  // 发送消息
  const sendMessage = async () => {
    if (!input.trim() || !conversationId || isTyping) return

    // 添加用户消息
    const userMessage: Message = {
      id: `m${Date.now()}`,
      role: "user",
      content: input,
    }

    setMessages((prev) => [...prev, userMessage])
    const sentMessage = input
    setInput("")
    setIsTyping(true)

    try {
      // 创建 AbortController 用于取消请求
      abortControllerRef.current = new AbortController()

      // 调用API获取流式响应
      const response = await streamChat(sentMessage, conversationId)
      await handleSSEResponse(response)
    } catch (error) {
      console.error("Error sending message:", error)

      // 如果不是用户主动中断，则显示错误消息
      if (error.name !== "AbortError") {
        // 显示错误提示
        toast({
          title: "发送消息失败",
          description: error.message || "请检查网络连接并稍后再试",
          variant: "destructive",
        })

        // 添加错误消息到对话
        const errorMessage: Message = {
          id: `m${Date.now() + 1}`,
          role: "assistant",
          content: "抱歉，发生了错误，请稍后再试。",
        }

        setMessages((prev) => [...prev, errorMessage])
      }
    } finally {
      setIsTyping(false)
      abortControllerRef.current = null
    }
  }

  // 停止响应
  const stopResponse = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      abortControllerRef.current = null
    }
    setIsTyping(false)
    setStreamingContent("")
  }

  if (!conversation) return null

  return (
    <div className="flex-1 flex flex-col">
      <div className="flex items-center justify-between border-b px-4 py-2 bg-gray-50">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center">{conversation.icon}</div>
          <div>
            <h1 className="text-lg font-medium">{conversation.name}</h1>
          </div>
        </div>
        <Button variant="ghost" size="icon" className="h-8 w-8">
          <FileText className="h-4 w-4" />
        </Button>
      </div>

      <div className="flex-1 overflow-auto p-4 bg-gray-50">
        <div className="mx-auto max-w-3xl space-y-6">
          {messages.map((message) => (
            <div key={message.id} className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}>
              {message.role === "assistant" && (
                <div className="mr-2 h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center text-sm">
                  {conversation.icon}
                </div>
              )}
              <div
                className={`rounded-lg px-4 py-2 max-w-[80%] ${
                  message.role === "user" ? "bg-blue-100 text-blue-900" : "bg-white border"
                }`}
              >
                {message.content}
              </div>
              {message.role === "user" && (
                <Avatar className="ml-2 h-8 w-8">
                  <AvatarImage src="/placeholder.svg?height=32&width=32" alt="User" />
                  <AvatarFallback className="bg-blue-500 text-white">U</AvatarFallback>
                </Avatar>
              )}
            </div>
          ))}

          {/* 流式响应显示 */}
          {streamingContent && (
            <div className="flex justify-start">
              <div className="mr-2 h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center text-sm">
                {conversation.icon}
              </div>
              <div className="rounded-lg px-4 py-2 max-w-[80%] bg-white border">{streamingContent}</div>
            </div>
          )}

          {/* 打字指示器 */}
          {isTyping && !streamingContent && (
            <div className="flex justify-start">
              <div className="mr-2 h-8 w-8 rounded-full bg-blue-100 flex items-center justify-center text-sm">
                {conversation.icon}
              </div>
              <div className="rounded-lg px-4 py-2 bg-white border">
                <div className="flex space-x-1">
                  <div
                    className="h-2 w-2 rounded-full bg-gray-300 animate-bounce"
                    style={{ animationDelay: "0ms" }}
                  ></div>
                  <div
                    className="h-2 w-2 rounded-full bg-gray-300 animate-bounce"
                    style={{ animationDelay: "150ms" }}
                  ></div>
                  <div
                    className="h-2 w-2 rounded-full bg-gray-300 animate-bounce"
                    style={{ animationDelay: "300ms" }}
                  ></div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
      </div>

      <div className="border-t p-4 bg-white">
        <div className="mx-auto max-w-3xl">
          <form
            onSubmit={(e) => {
              e.preventDefault()
              sendMessage()
            }}
            className="flex items-center gap-2"
          >
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="和机器人聊天"
              className="flex-1 border-gray-300"
              disabled={isTyping}
            />
            <Button
              type="submit"
              size="icon"
              className="bg-blue-500 hover:bg-blue-600 text-white rounded-full h-10 w-10"
              disabled={!input.trim() || isTyping}
            >
              <Send className="h-5 w-5" />
              <span className="sr-only">发送</span>
            </Button>
          </form>
          <div className="mt-2 text-center">
            {isTyping && (
              <Button variant="outline" size="sm" className="text-xs text-gray-500" onClick={stopResponse}>
                停止响应
              </Button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

