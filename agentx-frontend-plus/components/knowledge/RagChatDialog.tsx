"use client"

import { useState, useRef, useEffect } from "react"
import ReactMarkdown from "react-markdown"
import { 
  Send, 
  Bot, 
  User, 
  Loader2, 
  X, 
  MessageSquare, 
  Sparkles,
  ChevronDown,
  ChevronRight,
  Brain,
  FileSearch,
  ArrowDown
} from "lucide-react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { RagChatSession } from "@/lib/rag-chat-service"
import { toast } from "@/hooks/use-toast"
import type { RagDataset, RagThinkingData } from "@/types/rag-dataset"
import { cn } from "@/lib/utils"

interface RagChatDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  dataset: RagDataset
}

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  retrieval?: RagThinkingData // 检索过程信息
  thinking?: RagThinkingData // 思考过程信息
  thinkingContent?: string // 思考过程的内容
  timestamp: Date
  isStreaming?: boolean
  isThinkingComplete?: boolean
  isRetrievalComplete?: boolean
}

export function RagChatDialog({ open, onOpenChange, dataset }: RagChatDialogProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [currentThinking, setCurrentThinking] = useState<RagThinkingData | null>(null)
  const [currentThinkingContent, setCurrentThinkingContent] = useState<string>("")
  const thinkingContentRef = useRef<string>("")
  const [expandedThinking, setExpandedThinking] = useState<Record<string, boolean>>({})
  const [showScrollToBottom, setShowScrollToBottom] = useState(false)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const chatSessionRef = useRef<RagChatSession | null>(null)
  const processedTimestamps = useRef<Set<number>>(new Set())
  const isUserScrolling = useRef(false)
  const lastScrollTop = useRef(0)
  const scrollingTimeout = useRef<NodeJS.Timeout>()
  const autoScrollTimeout = useRef<NodeJS.Timeout>()
  const isAutoScrolling = useRef(false)

  // 检查是否在底部附近（阈值30px）
  const isNearBottom = (element: Element) => {
    const threshold = 30
    return element.scrollHeight - element.scrollTop - element.clientHeight < threshold
  }

  // 智能滚动到底部
  const scrollToBottom = () => {
   
    
    if (scrollAreaRef.current && !isUserScrolling.current) {
      const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
      if (scrollElement) {
        const beforeScroll = {
          scrollTop: scrollElement.scrollTop,
          scrollHeight: scrollElement.scrollHeight,
          clientHeight: scrollElement.clientHeight
        };
        
       
        
        // 设置自动滚动标志
        isAutoScrolling.current = true
        scrollElement.scrollTop = scrollElement.scrollHeight
        
        // 验证滚动是否成功
        setTimeout(() => {
          const afterScroll = {
            scrollTop: scrollElement.scrollTop,
            scrollHeight: scrollElement.scrollHeight,
            clientHeight: scrollElement.clientHeight
          };
         
          isAutoScrolling.current = false
        }, 100)
      } else {
       
      }
    } else {
     
    }
  }

  // 处理滚动事件
  const handleScroll = (event: Event) => {
    const scrollElement = event.target as Element
    const currentScrollTop = scrollElement.scrollTop
    
   
    
    // 如果是自动滚动触发的事件，忽略
    if (isAutoScrolling.current) {
     
      lastScrollTop.current = currentScrollTop
      return
    }
    
    // 防抖处理，避免频繁触发
    if (scrollingTimeout.current) {
      clearTimeout(scrollingTimeout.current)
    }
    
    scrollingTimeout.current = setTimeout(() => {
      // 检测用户主动滚动的几种情况：
      // 1. 向上滚动
      // 2. 不在底部附近
      // 3. 滚动速度较快（表示用户主动操作）
      const scrollDelta = Math.abs(currentScrollTop - lastScrollTop.current)
      const isScrollingUp = currentScrollTop < lastScrollTop.current
      const isAwayFromBottom = !isNearBottom(scrollElement)
      const isFastScrolling = scrollDelta > 10 // 快速滚动阈值
      
     
      
      if (isScrollingUp || isAwayFromBottom || isFastScrolling) {
        // 用户主动滚动
       
        isUserScrolling.current = true
        setShowScrollToBottom(true)
      } else if (isNearBottom(scrollElement)) {
        // 用户滚动到底部附近，恢复自动滚动
       
        isUserScrolling.current = false
        setShowScrollToBottom(false)
      }
      
      lastScrollTop.current = currentScrollTop
    }, 100) // 100ms防抖延迟
  }

  // 手动滚动到底部
  const handleScrollToBottom = () => {
    isUserScrolling.current = false
    setShowScrollToBottom(false)
    if (scrollAreaRef.current) {
      const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]')
      if (scrollElement) {
       
        scrollElement.scrollTo({
          top: scrollElement.scrollHeight,
          behavior: 'smooth'
        })
      }
    }
  }

  // 移除全局的messages监听，改为精确控制滚动时机

  // 设置滚动事件监听器
  useEffect(() => {
    const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]')
    if (scrollElement) {
      scrollElement.addEventListener('scroll', handleScroll)
      return () => {
        scrollElement.removeEventListener('scroll', handleScroll)
      }
    }
  }, [])

  // 清理聊天会话和定时器
  useEffect(() => {
    return () => {
      if (chatSessionRef.current) {
        chatSessionRef.current.abort()
      }
      // 清理所有定时器
      if (scrollingTimeout.current) {
        clearTimeout(scrollingTimeout.current)
      }
      if (autoScrollTimeout.current) {
        clearTimeout(autoScrollTimeout.current)
      }
    }
  }, [])

  // 切换思考过程展开状态
  const toggleThinking = (messageId: string) => {
    setExpandedThinking(prev => ({
      ...prev,
      [messageId]: !prev[messageId]
    }))
  }

  // 发送消息
  const handleSend = async () => {
    const question = input.trim()
    if (!question || isLoading) return
    
    

    // 创建用户消息
    const userMessage: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: question,
      timestamp: new Date()
    }

    // 创建助手消息占位符
    const assistantMessage: Message = {
      id: `assistant-${Date.now()}`,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    }

    setMessages(prev => [...prev, userMessage, assistantMessage])
    setInput("")
    setIsLoading(true)
    setCurrentThinking(null)
    setCurrentThinkingContent("")
    thinkingContentRef.current = ""
    processedTimestamps.current.clear()
    
    // 重置滚动状态，确保新对话会自动滚动
    isUserScrolling.current = false
    // 新对话的滚动将由首次内容到达时触发

    // 确保聊天会话已初始化
    if (!chatSessionRef.current) {
      chatSessionRef.current = new RagChatSession()
    }
    
    // 如果有正在进行的会话，先中止
    if (chatSessionRef.current.isActive()) {
      chatSessionRef.current.abort()
    }

    try {
      await chatSessionRef.current.start(
        {
          datasetIds: [dataset.id],
          question,
          stream: true
        },
        {
          onThinking: (data) => {
            setCurrentThinking(data)
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  const updatedMessage = { ...lastMessage };
                  // 根据类型分别存储
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
            })
          },
          onThinkingContent: (content, timestamp) => {
            // 使用ref累积内容，避免闭包问题
            thinkingContentRef.current += content
            setCurrentThinkingContent(thinkingContentRef.current)
            
           
            
            // 思考内容更新时的滚动策略：只有当用户确实在底部时才滚动
            if (!isUserScrolling.current) {
              const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]');
              if (scrollElement) {
                const nearBottom = isNearBottom(scrollElement);
                
                if (nearBottom) {
                  
                  // 清除之前的自动滚动定时器
                  if (autoScrollTimeout.current) {
                    clearTimeout(autoScrollTimeout.current);
                  }
                  // 思考内容滚动延迟稍长，避免过于频繁
                  autoScrollTimeout.current = setTimeout(() => {
                    scrollToBottom();
                  }, 100);
                } else {
                 
                }
              }
            } else {
              
            }
            
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
            })
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
            })
          },
          onContent: (content, timestamp) => {
            // 检查是否已处理过这个时间戳的消息
            if (timestamp && processedTimestamps.current.has(timestamp)) {
             
              return;
            }
            if (timestamp) {
              processedTimestamps.current.add(timestamp);
            }
            
            setMessages(prev => {
              // 直接修改最后一条消息，React会检测到变化
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  const isFirstContent = !lastMessage.content; // 判断是否是第一次接收到正式回答内容
                  const updatedMessage = {
                    ...lastMessage,
                    content: lastMessage.content + content
                  };
                  
                  // 优化后的滚动策略：智能判断是否应该自动滚动
                 
                  
                  if (!isUserScrolling.current) {
                    // 清除之前的自动滚动定时器
                    if (autoScrollTimeout.current) {
                      clearTimeout(autoScrollTimeout.current);
                    }
                    
                    if (isFirstContent) {
                      // 首次内容到达时，立即滚动到底部
                     
                      scrollToBottom();
                    } else {
                      // 后续内容到达时，只有当用户在底部附近时才自动滚动
                      const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]');
                      if (scrollElement) {
                        const nearBottom = isNearBottom(scrollElement);
                        
                        
                        if (nearBottom) {
                         
                          autoScrollTimeout.current = setTimeout(() => {
                            scrollToBottom();
                          }, 50);
                        } else {
                          
                        }
                      }
                    }
                  } else {
                      
                  }
                  
                  return [
                    ...prev.slice(0, -1),
                    updatedMessage
                  ];
                }
              }
              return prev;
            })
          },
          onError: (error) => {
            toast({
              title: "对话出错",
              description: error,
              variant: "destructive"
            })
            setMessages(prev => {
              if (prev.length > 0) {
                const lastMessage = prev[prev.length - 1];
                if (lastMessage && lastMessage.role === 'assistant') {
                  return [
                    ...prev.slice(0, -1),
                    {
                      ...lastMessage,
                      content: "抱歉，处理您的请求时出现了错误。请重试。",
                      isStreaming: false
                    }
                  ];
                }
              }
              return prev;
            })
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
            })
          }
        }
      )
    } catch (error) {
      console.error("Chat error:", error)
    } finally {
      setIsLoading(false)
    }
  }

  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  // 停止生成
  const handleStop = () => {
    if (chatSessionRef.current) {
      chatSessionRef.current.abort()
    }
    setIsLoading(false)
    setMessages(prev => {
      const newMessages = [...prev]
      const lastMessage = newMessages[newMessages.length - 1]
      if (lastMessage && lastMessage.role === 'assistant' && lastMessage.isStreaming) {
        lastMessage.isStreaming = false
        if (!lastMessage.content) {
          lastMessage.content = "生成已停止。"
        }
      }
      return newMessages
    })
  }

  // 清空对话
  const handleClear = () => {
    if (chatSessionRef.current) {
      chatSessionRef.current.abort()
    }
    // 清理所有定时器
    if (scrollingTimeout.current) {
      clearTimeout(scrollingTimeout.current)
    }
    if (autoScrollTimeout.current) {
      clearTimeout(autoScrollTimeout.current)
    }
    setMessages([])
    setCurrentThinking(null)
    setCurrentThinkingContent("")
    thinkingContentRef.current = ""
    processedTimestamps.current.clear()
    setIsLoading(false)
    setExpandedThinking({})
    setShowScrollToBottom(false)
    isUserScrolling.current = false
    isAutoScrolling.current = false
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl h-[80vh] p-0 flex flex-col">
        <DialogHeader className="px-6 py-4 border-b">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <MessageSquare className="h-5 w-5" />
              <DialogTitle>RAG 智能问答</DialogTitle>
              <Badge variant="secondary">{dataset.name}</Badge>
            </div>
            <div className="flex items-center gap-2">
              {messages.length > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleClear}
                  disabled={isLoading}
                >
                  清空对话
                </Button>
              )}
            </div>
          </div>
        </DialogHeader>

        <div className="relative flex-1 overflow-hidden">
          <ScrollArea ref={scrollAreaRef} className="h-full px-6 py-4">
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
                <div
                  key={message.id}
                  className={`flex gap-3 ${
                    message.role === 'user' ? 'justify-end' : 'justify-start'
                  }`}
                >
                  {message.role === 'assistant' && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                      <Bot className="h-5 w-5 text-primary" />
                    </div>
                  )}
                  
                  <div className={`flex flex-col gap-2 max-w-[70%] ${
                    message.role === 'user' ? 'items-end' : 'items-start'
                  }`}>
                    {/* 用户消息 */}
                    {message.role === 'user' && (
                      <Card className="px-4 py-2 bg-primary text-primary-foreground">
                        <div className="text-sm whitespace-pre-wrap">
                          {message.content}
                        </div>
                      </Card>
                    )}
                    
                    {/* 助手消息：检索过程 */}
                    {message.role === 'assistant' && message.retrieval && (
                      <Card className="px-4 py-2 bg-blue-50 dark:bg-blue-950/20">
                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <FileSearch className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                            <span className="text-sm font-medium">文档检索</span>
                            {message.retrieval.status === 'end' && (
                              <Badge variant="secondary" className="text-xs">
                                找到 {message.retrieval.retrievedCount || 0} 个文档
                              </Badge>
                            )}
                          </div>
                          
                          {/* 检索状态 */}
                          <div className="text-xs text-muted-foreground">
                            {message.retrieval.status === 'start' && '开始检索相关文档...'}
                            {message.retrieval.status === 'progress' && '正在数据集中检索...'}
                            {message.retrieval.status === 'end' && message.retrieval.message}
                          </div>
                          
                          {/* 检索到的文档 */}
                          {message.retrieval.documents && message.retrieval.documents.length > 0 && (
                            <div className="mt-2 space-y-1">
                              {message.retrieval.documents.map((doc, idx) => (
                                <div key={idx} className="text-xs flex items-center gap-2 pl-6">
                                  <span className="text-blue-600 dark:text-blue-400">•</span>
                                  <span className="flex-1">{doc.fileName}</span>
                                  <Badge variant="outline" className="text-xs px-1 py-0">
                                    {(doc.score * 100).toFixed(0)}%
                                  </Badge>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </Card>
                    )}
                    
                    {/* 助手消息：思考过程 */}
                    {message.role === 'assistant' && (message.thinking || message.thinkingContent) && (
                      <Collapsible 
                        open={expandedThinking[message.id] !== false}
                        onOpenChange={() => toggleThinking(message.id)}
                        className="w-full"
                      >
                        <Card className="px-4 py-2 bg-purple-50 dark:bg-purple-950/20">
                          <CollapsibleTrigger className="flex items-center justify-between w-full text-left">
                            <div className="flex items-center gap-2">
                              <Brain className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                              <span className="text-sm font-medium">思考过程</span>
                              {message.isThinkingComplete && (
                                <Badge variant="secondary" className="text-xs">
                                  已完成
                                </Badge>
                              )}
                            </div>
                            {expandedThinking[message.id] !== false ? (
                              <ChevronDown className="h-4 w-4 text-muted-foreground" />
                            ) : (
                              <ChevronRight className="h-4 w-4 text-muted-foreground" />
                            )}
                          </CollapsibleTrigger>
                          
                          <CollapsibleContent className="mt-3">
                            <div className="prose prose-sm dark:prose-invert max-w-none pl-6 text-sm">
                              {message.thinkingContent ? (
                                <ReactMarkdown>
                                  {message.thinkingContent}
                                </ReactMarkdown>
                              ) : (
                                <span className="text-muted-foreground">思考中...</span>
                              )}
                            </div>
                            
                            {/* 思考进行中状态 */}
                            {!message.isThinkingComplete && message.isStreaming && (
                              <div className="flex items-center gap-2 text-xs text-muted-foreground mt-2 pl-6">
                                <Loader2 className="h-3 w-3 animate-spin" />
                                <span>正在思考中...</span>
                              </div>
                            )}
                          </CollapsibleContent>
                        </Card>
                      </Collapsible>
                    )}
                    
                    {/* 助手消息：回答内容 */}
                    {message.role === 'assistant' && message.content && (
                      <Card className="px-4 py-2 bg-muted" key={`${message.id}-content`}>
                        <div className="prose prose-sm dark:prose-invert max-w-none">
                          <ReactMarkdown>
                            {message.content}
                          </ReactMarkdown>
                          {message.isStreaming && (
                            <span className="inline-block w-1 h-4 ml-1 bg-current animate-pulse" />
                          )}
                        </div>
                      </Card>
                    )}
                    
                    {/* 正在生成回答的提示 */}
                    {message.role === 'assistant' && message.isStreaming && !message.content && !message.retrieval && !message.thinking && (
                      <Card className="px-4 py-2 bg-muted">
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          <Loader2 className="h-4 w-4 animate-spin" />
                          <span>正在生成回答...</span>
                        </div>
                      </Card>
                    )}
                    
                    <span className="text-xs text-muted-foreground px-2">
                      {message.timestamp.toLocaleTimeString('zh-CN')}
                    </span>
                  </div>
                  
                  {message.role === 'user' && (
                    <div className="flex-shrink-0 w-8 h-8 rounded-full bg-secondary flex items-center justify-center">
                      <User className="h-5 w-5" />
                    </div>
                  )}
                </div>
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

        <Separator />

        <div className="px-6 py-4">
          <div className="flex gap-2">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入您的问题..."
              className="flex-1 min-h-[60px] max-h-[120px] resize-none"
              disabled={isLoading}
            />
            <div className="flex flex-col gap-2">
              {isLoading ? (
                <Button
                  variant="outline"
                  size="icon"
                  onClick={handleStop}
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
      </DialogContent>
    </Dialog>
  )
}