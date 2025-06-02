"use client"

import React, { useState, useRef, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Loader2, MessageCircle, Send, Bot, User, AlertCircle, Paperclip, X } from 'lucide-react'
import { toast } from '@/hooks/use-toast'
import { previewAgent, type AgentPreviewRequest, type MessageHistoryItem } from '@/lib/agent-preview-service'
import { uploadMultipleFiles, type UploadResult, type UploadFileInfo } from '@/lib/file-upload-service'

// 文件类型 - 使用URL而不是base64内容
interface ChatFile {
  id: string
  name: string
  type: string
  size: number
  url: string // 改为使用URL
  uploadProgress?: number // 新增：上传进度
}

// 消息类型
interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  timestamp: number
  isStreaming?: boolean
  files?: ChatFile[] // 消息附带的文件
  fileUrls?: string[] // 新增：文件URL列表（用于发送给后端）
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
  multiModal?: boolean // 新增：是否启用多模态功能
  
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
  multiModal = false,
  className = "",
  disabled = false,
  placeholder = "输入消息进行预览..."
}: AgentPreviewChatProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [isThinking, setIsThinking] = useState(false)
  const [streamingMessageId, setStreamingMessageId] = useState<string | null>(null)
  const [uploadedFiles, setUploadedFiles] = useState<ChatFile[]>([]) // 新增：待发送的文件列表
  const [isUploadingFiles, setIsUploadingFiles] = useState(false) // 新增：文件上传状态
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null) // 新增：文件输入引用

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
  }, [messages, isThinking])

  // 发送消息
  const sendMessage = async () => {
    if ((!inputValue.trim() && uploadedFiles.length === 0) || isLoading || disabled) return

    // 获取已完成上传的文件URL
    const completedFiles = uploadedFiles.filter(file => file.url && file.uploadProgress === 100)
    const fileUrls = completedFiles.map(file => file.url)

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'USER',
      content: inputValue.trim(),
      timestamp: Date.now(),
      files: completedFiles.length > 0 ? [...completedFiles] : undefined,
      fileUrls: fileUrls.length > 0 ? fileUrls : undefined
    }

    // 输出文件URL到控制台
    if (fileUrls.length > 0) {
      console.log('发送消息包含的文件URL:', fileUrls)
    }

    // 添加用户消息
    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setUploadedFiles([]) // 清空已上传的文件
    setIsLoading(true)
    setIsThinking(true) // 设置思考状态

    try {
      // 构建消息历史 - 包含文件URL信息
      const messageHistory: MessageHistoryItem[] = messages
        .filter(msg => msg.id !== 'welcome') // 排除欢迎消息
        .map(msg => ({
          id: msg.id,
          role: msg.role,
          content: msg.content,
          createdAt: new Date(msg.timestamp).toISOString(),
          fileUrls: msg.fileUrls // 包含文件URL
        }))

      // 构建预览请求
      const previewRequest: AgentPreviewRequest = {
        userMessage: userMessage.content,
        systemPrompt,
        toolIds,
        toolPresetParams,
        messageHistory,
        modelId,
        fileUrls: fileUrls.length > 0 ? fileUrls : undefined // 当前消息的文件URL
      }

      // 输出完整请求到控制台
      console.log('预览请求数据:', {
        ...previewRequest,
        fileUrls: fileUrls
      })

      // 创建AI响应消息（在第一次收到内容时才添加）
      let aiMessageId: string | null = null
      let hasReceivedFirstResponse = false

      // 发送预览请求
      await previewAgent(
        previewRequest,
        // 流式消息处理
        (content: string) => {
          console.log('Received streaming content:', content);
          
          // 首次响应处理
          if (!hasReceivedFirstResponse) {
            hasReceivedFirstResponse = true
            setIsThinking(false) // 收到第一个内容时关闭思考状态
            
            // 创建AI响应消息
            aiMessageId = (Date.now() + 1).toString()
            const aiMessage: ChatMessage = {
              id: aiMessageId,
              role: 'ASSISTANT',
              content: content,
              timestamp: Date.now(),
              isStreaming: true
            }
            
            setMessages(prev => [...prev, aiMessage])
            setStreamingMessageId(aiMessageId)
          } else if (aiMessageId) {
            // 更新现有消息内容
            setMessages(prev => prev.map(msg => 
              msg.id === aiMessageId 
                ? { ...msg, content: msg.content + content }
                : msg
            ))
          }
        },
        // 完成处理
        (fullContent: string) => {
          console.log('Preview completed with full content:', fullContent);
          if (aiMessageId) {
            setMessages(prev => prev.map(msg => 
              msg.id === aiMessageId 
                ? { ...msg, content: fullContent, isStreaming: false }
                : msg
            ))
          }
          setStreamingMessageId(null)
          setIsLoading(false)
          setIsThinking(false)
        },
        // 错误处理
        (error: Error) => {
          console.error('Preview error:', error)
          
          // 如果还在思考中，先关闭思考状态并添加错误消息
          if (isThinking) {
            setIsThinking(false)
            const errorMessageId = (Date.now() + 1).toString()
            const errorMessage: ChatMessage = {
              id: errorMessageId,
              role: 'ASSISTANT',
              content: `预览出错: ${error.message}`,
              timestamp: Date.now(),
              isStreaming: false
            }
            setMessages(prev => [...prev, errorMessage])
          } else if (aiMessageId) {
            // 如果已经有消息，更新消息内容
            setMessages(prev => prev.map(msg => 
              msg.id === aiMessageId 
                ? { 
                    ...msg, 
                    content: `预览出错: ${error.message}`, 
                    isStreaming: false 
                  }
                : msg
            ))
          }
          
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
      setIsThinking(false)
      
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
    setIsThinking(false)
    setIsLoading(false)
    setStreamingMessageId(null)
  }

  // 处理文件上传
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files
    if (!files || files.length === 0) return

    if (!multiModal) {
      toast({
        title: "多模态功能未启用",
        description: "请在Agent配置中启用多模态功能",
        variant: "destructive"
      })
      return
    }

    setIsUploadingFiles(true)

    // 准备上传文件信息
    const uploadFiles: UploadFileInfo[] = Array.from(files).map(file => ({
      file,
      fileName: file.name,
      fileType: file.type,
      fileSize: file.size
    }))

    // 创建临时文件状态（显示上传进度）
    const tempFiles: ChatFile[] = uploadFiles.map((fileInfo, index) => ({
      id: Date.now().toString() + index,
      name: fileInfo.fileName,
      type: fileInfo.fileType,
      size: fileInfo.fileSize,
      url: '', // 暂时为空
      uploadProgress: 0
    }))

    try {
      // 先添加临时文件到状态中
      setUploadedFiles(prev => [...prev, ...tempFiles])

      // 批量上传文件
      const uploadResults = await uploadMultipleFiles(
        uploadFiles,
        // 进度回调
        (fileIndex, progress) => {
          const tempFileId = tempFiles[fileIndex].id
          setUploadedFiles(prev => 
            prev.map(file => 
              file.id === tempFileId 
                ? { ...file, uploadProgress: progress }
                : file
            )
          )
        },
        // 单个文件完成回调
        (fileIndex, result) => {
          const tempFileId = tempFiles[fileIndex].id
          setUploadedFiles(prev => 
            prev.map(file => 
              file.id === tempFileId 
                ? { 
                    ...file, 
                    url: result.url, 
                    uploadProgress: 100,
                    name: result.fileName,
                    type: result.fileType,
                    size: result.fileSize
                  }
                : file
            )
          )
          console.log(`文件上传完成:`, result)
        },
        // 错误回调
        (fileIndex, error) => {
          const tempFileId = tempFiles[fileIndex].id
          console.error(`文件 ${uploadFiles[fileIndex].fileName} 上传失败:`, error)
          
          // 移除失败的文件
          setUploadedFiles(prev => prev.filter(file => file.id !== tempFileId))
          
          toast({
            title: "文件上传失败",
            description: `${uploadFiles[fileIndex].fileName}: ${error.message}`,
            variant: "destructive"
          })
        }
      )

      if (uploadResults.length > 0) {
        toast({
          title: "文件上传成功",
          description: `已上传 ${uploadResults.length} 个文件`
        })
      }
    } catch (error) {
      console.error('批量文件上传失败:', error)
      
      // 清理所有临时文件
      setUploadedFiles(prev => 
        prev.filter(file => !tempFiles.some((temp: ChatFile) => temp.id === file.id))
      )
      
      toast({
        title: "文件上传失败",
        description: error instanceof Error ? error.message : "请重试",
        variant: "destructive"
      })
    } finally {
      setIsUploadingFiles(false)
      // 清空文件选择
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  // 移除文件
  const removeFile = (fileId: string) => {
    setUploadedFiles(prev => prev.filter(file => file.id !== fileId))
  }

  // 触发文件选择
  const triggerFileSelect = () => {
    if (!multiModal) {
      toast({
        title: "多模态功能未启用",
        description: "请在Agent配置中启用多模态功能",
        variant: "destructive"
      })
      return
    }
    fileInputRef.current?.click()
  }

  return (
    <Card className={`flex flex-col h-full ${className}`}>
      {/* 头部 */}
      <CardHeader className="flex-shrink-0 pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Avatar className="h-10 w-10">
              <AvatarImage src={agentAvatar || undefined} alt="Agent Avatar" />
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
                    <AvatarImage src={agentAvatar || undefined} alt="Agent" />
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
                  {/* 文件显示 */}
                  {message.files && message.files.length > 0 && (
                    <div className="mb-2 space-y-2">
                      {message.files.map((file) => (
                        <div
                          key={file.id}
                          className={`flex items-center gap-2 p-2 rounded border ${
                            message.role === 'USER'
                              ? 'bg-blue-400/20 border-blue-300/30'
                              : 'bg-white border-gray-200'
                          }`}
                        >
                          {file.type.startsWith('image/') && file.url && file.url.trim() !== '' && (
                            <img
                              src={file.url}
                              alt={file.name}
                              className="w-8 h-8 rounded object-cover"
                            />
                          )}
                          <div className="flex-1 min-w-0">
                            <p className={`text-xs font-medium truncate ${
                              message.role === 'USER' ? 'text-white' : 'text-gray-900'
                            }`}>
                              {file.name}
                            </p>
                            <p className={`text-xs ${
                              message.role === 'USER' ? 'text-blue-100' : 'text-gray-500'
                            }`}>
                              {(file.size / 1024).toFixed(1)} KB
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* 文本内容 */}
                  {message.content && (
                    <div className="text-sm whitespace-pre-wrap">
                      {message.content}
                      {message.isStreaming && (
                        <span className="inline-block w-2 h-4 bg-current opacity-75 animate-pulse ml-1" />
                      )}
                    </div>
                  )}

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

            {/* 思考中提示 - 和对话页面相同的UI */}
            {isThinking && (
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
          </div>
        </ScrollArea>
      </CardContent>

      {/* 输入区域 */}
      <div className="flex-shrink-0 p-4 border-t">
        {/* 已上传文件预览 */}
        {uploadedFiles.length > 0 && (
          <div className="mb-3 flex flex-wrap gap-2">
            {uploadedFiles.map((file) => (
              <div
                key={file.id}
                className="flex items-center gap-2 bg-gray-100 rounded-lg p-2 border relative"
              >
                {file.type.startsWith('image/') && file.url && file.url.trim() !== '' && (
                  <img
                    src={file.url}
                    alt={file.name}
                    className="w-6 h-6 rounded object-cover"
                  />
                )}
                <div className="flex-1 min-w-0">
                  <p className="text-xs font-medium truncate text-gray-900">
                    {file.name}
                  </p>
                  <div className="flex items-center gap-2">
                    <p className="text-xs text-gray-500">
                      {(file.size / 1024).toFixed(1)} KB
                    </p>
                    {typeof file.uploadProgress === 'number' && file.uploadProgress < 100 && (
                      <div className="flex items-center gap-1">
                        <div className="w-8 h-1 bg-gray-200 rounded-full overflow-hidden">
                          <div 
                            className="h-full bg-blue-500 transition-all duration-300"
                            style={{ width: `${file.uploadProgress}%` }}
                          />
                        </div>
                        <span className="text-xs text-blue-600">
                          {file.uploadProgress}%
                        </span>
                      </div>
                    )}
                    {file.uploadProgress === 100 && (
                      <span className="text-xs text-green-600">✓</span>
                    )}
                  </div>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => removeFile(file.id)}
                  disabled={typeof file.uploadProgress === 'number' && file.uploadProgress < 100}
                  className="h-6 w-6 p-0 hover:bg-red-100 hover:text-red-600"
                >
                  <X className="h-3 w-3" />
                </Button>
              </div>
            ))}
          </div>
        )}

        <div className="flex gap-2">
          {/* 文件上传按钮 */}
          {multiModal && (
            <Button
              variant="outline"
              size="icon"
              onClick={triggerFileSelect}
              disabled={disabled || isLoading || isUploadingFiles}
              className="flex-shrink-0"
            >
              {isUploadingFiles ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Paperclip className="h-4 w-4" />
              )}
            </Button>
          )}

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
            disabled={disabled || isLoading || (!inputValue.trim() && uploadedFiles.length === 0)}
            size="icon"
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>

        {/* 隐藏的文件输入 */}
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileUpload}
          accept="image/*,.pdf,.doc,.docx,.txt,.md"
          multiple
          className="hidden"
        />

        {disabled && (
          <p className="text-xs text-muted-foreground mt-2">
            请填写必要的Agent信息后进行预览
          </p>
        )}
      </div>
    </Card>
  )
} 