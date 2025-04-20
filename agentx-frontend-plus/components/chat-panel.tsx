"use client"

import { useState, useRef, useEffect, useCallback } from "react"
import { FileText, Send, ClipboardList, Wrench, CheckCircle, ListTodo, Circle, AlertCircle } from 'lucide-react'
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { streamChat } from "@/lib/api"
import { toast } from "@/components/ui/use-toast"
import { getSessionMessages, getSessionMessagesWithToast, type MessageDTO } from "@/lib/session-message-service"
import { getSessionTasksWithToast } from "@/lib/task-service"
import { Skeleton } from "@/components/ui/skeleton"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import { Highlight, themes } from "prism-react-renderer"
import { CurrentTaskList } from "@/components/current-task-list"
import { MessageType, type Message as MessageInterface } from "@/types/conversation"
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { nanoid } from 'nanoid'

interface ChatPanelProps {
  conversationId: string
  onToggleTaskHistory?: () => void
  showTaskHistory?: boolean
  isFunctionalAgent?: boolean
  agentName?: string
  agentType?: number // 新增：助理类型，2表示功能性Agent
}

interface Message {
  id: string
  role: "USER" | "SYSTEM" | "assistant"
  content: string
  tasks?: TaskDTO[] // 任务列表，仅对Agent消息有效
  taskId?: string // 任务ID
  messageType?: string // 消息类型
  type?: MessageType // 消息类型枚举
  createdAt?: string
  updatedAt?: string
}

interface AssistantMessage {
  id: string
  hasContent: boolean
}

interface StreamData {
  content: string
  done: boolean
  sessionId: string
  provider?: string
  model?: string
  timestamp: number
  messageType?: string // 消息类型
  taskId?: string // 任务ID
  tasks?: TaskDTO[] // 任务数据
}

interface TaskAggregate {
  task: TaskDTO      // 父任务
  subTasks: TaskDTO[] // 子任务列表
}

// 定义消息类型为字符串字面量类型
type MessageTypeValue = 
  | "TEXT" 
  | "TASK_IDS" 
  | "TASK_EXEC" 
  | "TASK_STATUS" 
  | "TOOL_CALL"
  | "TASK_SPLIT_FINISH"
  | "TASK_STATUS_TO_FINISH";

// 定义任务状态为字符串字面量类型
type TaskStatusValue = 
  | "WAITING"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "FAILED";

// 任务数据传输对象
interface TaskDTO {
  id: string
  taskName: string
  status: string
  progress: number
  parentTaskId: string
  taskResult?: string // 可选，任务执行结果
  startTime?: string  // 可选，任务开始时间
  endTime?: string    // 可选，任务结束时间
}

export function ChatPanel({ conversationId, onToggleTaskHistory, showTaskHistory = false, isFunctionalAgent = false, agentName = "AI助手", agentType = 1 }: ChatPanelProps) {
  const [input, setInput] = useState("")
  const [messages, setMessages] = useState<MessageInterface[]>([])
  const [isTyping, setIsTyping] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [autoScroll, setAutoScroll] = useState(true)
  const [isThinking, setIsThinking] = useState(false)
  const [currentAssistantMessage, setCurrentAssistantMessage] = useState<AssistantMessage | null>(null)
  const [tasks, setTasks] = useState<Map<string, TaskDTO>>(new Map()) // 任务映射
  const [tasksMessageId, setTasksMessageId] = useState<string | null>(null) // 存储任务列表消息的ID
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const chatContainerRef = useRef<HTMLDivElement>(null)
  const [taskFetchingInProgress, setTaskFetchingInProgress] = useState(false);
  const taskFetchTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  
  // 新增：使用useRef保存不需要触发重新渲染的状态
  const hasReceivedFirstResponse = useRef(false);
  const messageContentAccumulator = useRef({
    content: "",
    type: MessageType.TEXT as MessageType,
    taskId: null as string | null
  });

  // 在组件顶部添加状态来跟踪已完成的TEXT消息
  const [completedTextMessages, setCompletedTextMessages] = useState<Set<string>>(new Set());
  // 添加消息序列计数器
  const messageSequenceNumber = useRef(0);

  // 在组件初始化和conversationId变更时重置状态
  useEffect(() => {
    hasReceivedFirstResponse.current = false;
    messageContentAccumulator.current = {
      content: "",
      type: MessageType.TEXT,
      taskId: null
    };
    setCompletedTextMessages(new Set());
    messageSequenceNumber.current = 0;
  }, [conversationId]);

  // 添加消息到列表的辅助函数
  const addMessage = (message: {
    id: string;
    role: "USER" | "SYSTEM" | "assistant";
    content: string;
    type?: MessageType;
    taskId?: string;
    createdAt?: string | Date;
  }) => {
    const messageObj: MessageInterface = {
      id: message.id,
      role: message.role,
      content: message.content,
      type: message.type || MessageType.TEXT,
      taskId: message.taskId,
      createdAt: message.createdAt instanceof Date 
        ? message.createdAt.toISOString() 
        : message.createdAt || new Date().toISOString()
    };
    
    setMessages(prev => [...prev, messageObj]);
  };

  // 获取会话消息
  useEffect(() => {
    const fetchSessionMessages = async () => {
      if (!conversationId) return
      
      try {
        setLoading(true)
        setError(null)
        // 清空之前的消息，避免显示上一个会话的内容
        setMessages([])
        setTasks(new Map())
        
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

  // 获取会话任务列表 (使用useCallback包装并添加防抖)
  const fetchSessionTasks = useCallback(async (sessionId: string, retryCount = 0) => {
    // 防止重复获取
    if (taskFetchingInProgress) {
      console.log("任务获取正在进行中，跳过重复请求");
      return;
    }
    
    // 清除之前的超时
    if (taskFetchTimeoutRef.current) {
      clearTimeout(taskFetchTimeoutRef.current);
    }
    
    console.log(`开始获取会话任务 (尝试 ${retryCount + 1})`, sessionId);
    
    // 设置延迟时间根据重试次数增加
    const delay = retryCount === 0 ? 300 : Math.min(1000 * retryCount, 5000);
    
    // 设置防抖
    taskFetchTimeoutRef.current = setTimeout(async () => {
      try {
        setTaskFetchingInProgress(true);
        
        const tasksResponse = await getSessionTasksWithToast(sessionId);
        
        if (tasksResponse.code === 200 && tasksResponse.data) {
          // 提取主任务和子任务
          const parentTask = tasksResponse.data.task;
          const subTasks = tasksResponse.data.subTasks || [];
          
          console.log("获取到任务数据:", parentTask, subTasks);
          
          // 检查是否有父任务
          if (!parentTask || !parentTask.id) {
            console.warn("API返回的父任务数据为空");
            // 如果未获取到任务且重试次数小于5，则重试
            if (retryCount < 5) {
              console.log(`未获取到任务，将在${delay}ms后重试...`);
              setTimeout(() => fetchSessionTasks(sessionId, retryCount + 1), delay);
            } else {
              console.error("获取任务数据失败，已达最大重试次数");
            }
            return;
          }
          
          // 创建新的任务Map
          const taskMap = new Map<string, TaskDTO>();
          
          // 添加父任务
          console.log(`添加父任务: ${parentTask.id}, ${parentTask.taskName}, 状态=${parentTask.status}`);
          taskMap.set(parentTask.id, {
            id: parentTask.id,
            taskName: parentTask.taskName,
            status: parentTask.status || "IN_PROGRESS",
            progress: parentTask.progress || 0,
            parentTaskId: parentTask.parentTaskId || "0"
          });
          
          // 添加子任务
          subTasks.forEach((task: any) => {
            if (task && task.id) {
              console.log(`添加子任务: ${task.id}, ${task.taskName}, 状态=${task.status || "WAITING"}`);
              taskMap.set(task.id, {
                id: task.id,
                taskName: task.taskName,
                status: task.status || "WAITING",
                progress: task.progress || 0,
                parentTaskId: task.parentTaskId || parentTask.id || "0"
              });
            }
          });
          
          // 更新任务状态 - 直接替换，但保留现有任务的进行状态
          setTasks(currentTasks => {
            // 只保留API返回的任务，但可能保留一些特殊状态
            
            // 1. 找出当前正在进行中的任务
            const inProgressTaskIds = new Set<string>();
            for (const [id, task] of currentTasks.entries()) {
              if (task.status === "IN_PROGRESS") {
                inProgressTaskIds.add(id);
              }
            }
            
            // 2. 以API返回的任务为基础
            const newTaskMap = new Map<string, TaskDTO>();
            
            // 3. 处理每个任务
            for (const [id, task] of taskMap.entries()) {
              // 如果任务在API中存在，添加到新Map中
              if (inProgressTaskIds.has(id) && task.status === "WAITING") {
                // 保留进行中状态
                newTaskMap.set(id, {
                  ...task,
                  status: "IN_PROGRESS"
                });
              } else {
                // 使用API返回的状态
                newTaskMap.set(id, task);
              }
            }
            
            console.log(`任务更新: ${currentTasks.size} -> ${newTaskMap.size} (替换模式)`);
            return newTaskMap;
          });
          
          // 添加任务完成检查
          setTimeout(() => {
            console.log("检查并自动标记已完成任务");
            // 获取最新任务状态
            setTasks(currentMap => {
              const newMap = new Map(currentMap);
              // 检查所有任务是否已完成 (根据content或progress判断)
              for (const [id, task] of newMap.entries()) {
                if (task.progress >= 100 && task.status !== "COMPLETED") {
                  console.log(`检测到任务[${id}] ${task.taskName} 进度已达100%，自动标记为已完成`);
                  newMap.set(id, {
                    ...task,
                    status: "COMPLETED"
                  });
                }
                
                if (task.taskResult && task.status !== "COMPLETED") {
                  console.log(`检测到任务[${id}] ${task.taskName} 含有结果，自动标记为已完成`);
                  newMap.set(id, {
                    ...task,
                    status: "COMPLETED",
                    progress: 100
                  });
                }
              }
              return newMap;
            });
          }, 200);
        } else {
          console.warn("获取任务列表API返回错误:", tasksResponse);
          
          // 如果API调用失败且重试次数小于5，则重试
          if (retryCount < 5) {
            setTimeout(() => fetchSessionTasks(sessionId, retryCount + 1), delay);
          }
        }
      } catch (error) {
        console.error("获取会话任务失败:", error);
        
        // 如果发生异常且重试次数小于5，则重试
        if (retryCount < 5) {
          setTimeout(() => fetchSessionTasks(sessionId, retryCount + 1), delay);
        }
      } finally {
        setTaskFetchingInProgress(false);
        taskFetchTimeoutRef.current = null;
      }
    }, delay);
  }, [currentAssistantMessage, taskFetchingInProgress]);
  
  // 更新任务状态
  const updateTaskStatus = (taskId: string, status: string) => {
    console.log(`尝试更新任务状态: ${taskId} -> ${status}`);
    
    // 立即尝试更新
    setTasks(prev => {
      const newMap = new Map(prev);
      const task = newMap.get(taskId);
      
      if (task) {
        console.log(`找到任务: ${taskId}，当前状态: ${task.status}，更新为: ${status}`);
        
        // 创建新的任务对象而不是修改原对象，确保状态变更触发重新渲染
        newMap.set(taskId, {
          ...task,
          status: status
        });
        
        // 如果状态变为完成，自动设置进度为100%
        if (status === "COMPLETED" && task.progress < 100) {
          console.log(`任务${taskId}状态变为COMPLETED，自动设置进度为100%`);
          const updatedTask = newMap.get(taskId);
          if (updatedTask) {
            newMap.set(taskId, {
              ...updatedTask,
              progress: 100
            });
          }
        }
        
        console.log(`任务${taskId}更新后状态:`, newMap.get(taskId));
      } else {
        console.warn(`尝试更新不存在的任务: ${taskId}，当前任务Map大小: ${prev.size}`);
        if (prev.size > 0) {
          console.debug("现有任务ID:", Array.from(prev.keys()));
        }
      }
      
      return newMap;
    });
    
    // 使用多阶段更新策略，确保状态确实被更新
    // 1. 初次延迟检查 (100ms)
    setTimeout(() => {
      setTasks(prev => {
        const task = prev.get(taskId);
        if (!task) return prev; // 任务不存在
        
        if (task.status !== status) {
          console.log(`[检查1] 任务${taskId}状态未更新，再次尝试: ${task.status} -> ${status}`);
          const newMap = new Map(prev);
          newMap.set(taskId, {
            ...task,
            status: status,
            // 如果是完成状态，确保进度为100%
            progress: status === "COMPLETED" ? 100 : task.progress
          });
          return newMap;
        }
        return prev; // 状态已是期望值，无需更新
      });
    }, 100);
    
    // 2. 二次延迟检查 (500ms)
    setTimeout(() => {
      setTasks(prev => {
        // 先检查是否需要获取任务列表
        let needFetch = true;
        const task = prev.get(taskId);
        
        if (task) {
          needFetch = false; // 任务存在，不需要获取
          
          if (task.status !== status) {
            console.log(`[检查2] 任务${taskId}状态仍未更新，最后尝试: ${task.status} -> ${status}`);
            const newMap = new Map(prev);
            newMap.set(taskId, {
              ...task,
              status: status,
              progress: status === "COMPLETED" ? 100 : task.progress
            });
            return newMap;
          }
        }
        
        // 如果任务不存在且conversationId存在，尝试获取任务列表
        if (needFetch && conversationId) {
          console.log(`任务${taskId}不存在，尝试获取任务列表`);
          fetchSessionTasks(conversationId);
        }
        
        return prev;
      });
    }, 500);
  }
  
  // 更新任务进度
  const updateTaskProgress = (taskId: string, progress: number) => {
    console.log(`更新任务进度: ${taskId} -> ${progress}%`);
    setTasks(prev => {
      const newMap = new Map(prev);
      const task = newMap.get(taskId);
      
      if (task) {
        // 如果进度达到100%，自动设置状态为COMPLETED
        const status = progress >= 100 ? "COMPLETED" : task.status;
        
        // 创建新对象以确保状态更新触发UI刷新
        newMap.set(taskId, {
          ...task,
          progress: progress,
          status: status
        });
        
        console.log(`任务${taskId}进度更新为${progress}%，状态为${status}`);
      } else {
        console.warn(`尝试更新不存在的任务进度: ${taskId}`);
        // 如果任务不存在，可能是因为任务还未加载，尝试重新获取任务列表
        if (conversationId) {
          fetchSessionTasks(conversationId);
        }
      }
      
      return newMap;
    });
  }

  // 处理发送消息
  const handleSendMessage = async () => {
    if (!input.trim() || !conversationId) return

    // 添加调试信息
    console.log("当前聊天模式:", agentType === 2 ? "功能性Agent" : "普通对话")
    
    const userMessage = input.trim()
    setInput("")
    setIsTyping(true)
    setIsThinking(true) // 设置思考状态
    setCurrentAssistantMessage(null) // 重置助手消息状态
    scrollToBottom() // 用户发送新消息时强制滚动到底部
    
    // 重置所有状态
    setCompletedTextMessages(new Set())
    resetMessageAccumulator()
    hasReceivedFirstResponse.current = false
    messageSequenceNumber.current = 0; // 重置消息序列计数器

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

      // 生成基础消息ID，作为所有消息序列的前缀
      const baseMessageId = Date.now().toString()
      
      // 重置状态
      hasReceivedFirstResponse.current = false;
      messageContentAccumulator.current = {
        content: "",
        type: MessageType.TEXT,
        taskId: null
      };
      
      const decoder = new TextDecoder()
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
              // 提取JSON部分（去掉前缀"data:"，处理可能的重复前缀情况）
              let jsonStr = line.substring(5);
              // 处理可能存在的重复data:前缀
              if (jsonStr.startsWith("data:")) {
                jsonStr = jsonStr.substring(5);
              }
              console.log("收到SSE消息:", jsonStr);
              
              const data = JSON.parse(jsonStr) as StreamData
              console.log("解析后的消息:", data, "消息类型:", data.messageType);
              
              // 处理消息 - 传递baseMessageId作为前缀
              handleStreamDataMessage(data, baseMessageId);
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

  // 消息处理主函数 - 完全重构
  const handleStreamDataMessage = (data: StreamData, baseMessageId: string) => {
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
    const messageType = data.messageType as MessageType || MessageType.TEXT;
    
    // 生成当前消息序列的唯一ID
    const currentMessageId = `assistant-${messageType}-${baseMessageId}-seq${messageSequenceNumber.current}`;
    
    console.log(`处理消息: 类型=${messageType}, 序列=${messageSequenceNumber.current}, ID=${currentMessageId}, done=${data.done}`);
    
    // 处理消息功能（任务状态更新等）
    if (data.messageType) {
      handleMessageTypeForTaskUpdate(data);
    }
    
    // 处理消息内容（用于UI显示）
    const displayableTypes = [undefined, "TEXT", "TOOL_CALL", "TASK_EXEC"];
    const isDisplayableType = displayableTypes.includes(data.messageType);
    
    if (isDisplayableType && data.content) {
      // 累积消息内容
      messageContentAccumulator.current.content += data.content;
      messageContentAccumulator.current.type = messageType;
      messageContentAccumulator.current.taskId = data.taskId || null;
      
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
  }
  
  // 更新或创建UI消息
  const updateOrCreateMessageInUI = (messageId: string, messageData: {
    content: string;
    type: MessageType;
    taskId: string | null;
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
          content: messageData.content
        };
        return newMessages;
      } else {
        // 消息不存在，创建新消息
        console.log(`创建新消息: ${messageId}, 类型: ${messageData.type}`);
        return [
          ...prev,
          {
            id: messageId,
            role: "assistant",
            content: messageData.content,
            type: messageData.type,
            taskId: messageData.taskId || undefined,
            createdAt: new Date().toISOString()
          }
        ];
      }
    });
    
    // 更新当前助手消息状态
    setCurrentAssistantMessage({ id: messageId, hasContent: true });
  }
  
  // 完成消息处理
  const finalizeMessage = (messageId: string, messageData: {
    content: string;
    type: MessageType;
    taskId: string | null;
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
          content: messageData.content
        };
        return newMessages;
      } else {
        // 消息不存在，创建新消息
        console.log(`创建并完成新消息: ${messageId}`);
        return [
          ...prev,
          {
            id: messageId,
            role: "assistant",
            content: messageData.content,
            type: messageData.type,
            taskId: messageData.taskId || undefined,
            createdAt: new Date().toISOString()
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
  }

  // 重置消息累积器
  const resetMessageAccumulator = () => {
    console.log("重置消息累积器");
    messageContentAccumulator.current = {
      content: "",
      type: MessageType.TEXT,
      taskId: null
    };
  };

  // 更新主任务状态为已完成
  const updateMainTaskStatusToCompleted = () => {
    setTasks(prev => {
      const newMap = new Map(prev);
      for (const [id, task] of newMap.entries()) {
        if (task.parentTaskId === "0") {
          console.log("更新主任务状态为已完成:", id);
          newMap.set(id, {
            ...task,
            status: "COMPLETED",
            progress: 100
          });
        }
      }
      return newMap;
    });
  };

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
          text: '任务列表'
        };
      case MessageType.TASK_SPLIT_FINISH:
        return {
          icon: <CheckCircle className="h-5 w-5 text-green-500" />,
          text: '任务拆分完成'
        };
      case MessageType.TASK_IN_PROGRESS:
        return {
          icon: <Circle className="h-5 w-5 text-blue-500 animate-pulse" />,
          text: '任务进行中'
        };
      case MessageType.TASK_COMPLETED:
        return {
          icon: <CheckCircle className="h-5 w-5 text-green-600 font-bold" />,
          text: '任务完成通知'
        };
      case MessageType.TASK_STATUS_TO_FINISH:
        return {
          icon: <CheckCircle className="h-5 w-5 text-green-500" />,
          text: '子任务完成'
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
      <div className="react-markdown">
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
                    <div className="code-block-container">
                      <pre
                        className={`${className} rounded p-2 my-2 overflow-x-auto max-w-full text-sm`}
                        style={{...style, wordBreak: 'break-all', overflowWrap: 'break-word'}}
                      >
                        {tokens.map((line, i) => (
                          <div key={i} {...getLineProps({ line, key: i })} style={{whiteSpace: 'pre-wrap', wordBreak: 'break-all'}}>
                            <span className="text-gray-500 mr-2 text-right w-6 inline-block select-none">
                              {i + 1}
                            </span>
                            {line.map((token, tokenIndex) => {
                              // 获取token props但不包含key
                              const tokenProps = getTokenProps({ token, key: tokenIndex });
                              // 删除key属性
                              const { key, ...restTokenProps } = tokenProps;
                              // 单独传递key属性，并添加样式确保长字符串能换行
                              return <span 
                                key={tokenIndex} 
                                {...restTokenProps} 
                                style={{
                                  ...restTokenProps.style,
                                  wordBreak: 'break-all',
                                  overflowWrap: 'break-word'
                                }}
                              />;
                            })}
                          </div>
                        ))}
                      </pre>
                    </div>
                  )}
                </Highlight>
              ) : (
                <code className={`${className} bg-gray-100 px-1 py-0.5 rounded break-all`} {...props}>
                  {children}
                </code>
              );
            },
          }}
        >
          {message.content}
        </ReactMarkdown>
      </div>
    );
  };

  // 渲染任务状态图标
  const renderTaskStatusIcon = (status: string) => {
    switch(status) {
      case "COMPLETED":
        return <CheckCircle className="w-5 h-5 text-green-500" />
      case "IN_PROGRESS":
        return <Circle className="w-5 h-5 text-blue-500 animate-pulse" />
      case "FAILED":
        return <AlertCircle className="w-5 h-5 text-red-500" />
      case "WAITING":
      default:
        return <Circle className="w-5 h-5 text-gray-300" />
    }
  }

  // 判断是否为错误消息
  const isErrorMessage = (data: StreamData): boolean => {
    return !!data.content && (
      data.content.includes("Error updating database") || 
      data.content.includes("PSQLException") || 
      data.content.includes("任务执行过程中发生错误")
    );
  };

  // 处理错误消息
  const handleErrorMessage = (data: StreamData) => {
    console.error("检测到后端错误:", data.content);
    toast({
      title: "任务执行错误",
      description: "服务器处理任务时遇到问题，请稍后再试",
      variant: "destructive",
    });
  };

  // 处理消息类型函数 - 任务状态更新
  const handleMessageTypeForTaskUpdate = (data: StreamData) => {
    if (!data.messageType) return;
    
    console.log(`处理任务消息: 类型=${data.messageType}, 任务ID=${data.taskId}, 内容=${data.content?.substring(0, 20)}...`);
    
    switch(data.messageType) {
      case "TASK_SPLIT_FINISH":
        // 任务拆分完成 - 获取任务列表
        console.log("收到任务拆分完成消息, 开始获取任务列表");
        setTimeout(() => {
          fetchSessionTasks(conversationId);
          updateMainTaskStatusToCompleted();
        }, 1000);
        break;
        
      case "TASK_STATUS_TO_LOADING":
        // 更新任务状态为进行中
        if (data.taskId) {
          console.log(`任务状态更新为进行中: ${data.taskId}`);
          
          // 直接更新state，确保立即反映在UI上
          setTasks(prev => {
            const newMap = new Map(prev);
            const task = newMap.get(data.taskId as string);
            
            if (task) {
              console.log(`直接设置任务[${data.taskId}] ${task.taskName} 为进行中状态`);
              newMap.set(data.taskId as string, {
                ...task,
                status: "IN_PROGRESS"
              });
            } else {
              console.warn(`找不到要更新的任务: ${data.taskId}，将通过API获取`);
              // 任务不存在，尝试获取
              if (conversationId) {
                setTimeout(() => fetchSessionTasks(conversationId), 500);
              }
            }
            
            return newMap;
          });
          
          // 同时调用状态更新函数
          updateTaskStatus(data.taskId, "IN_PROGRESS");
          
          // 处理进度信息
          if (data.content && !isNaN(parseInt(data.content))) {
            updateTaskProgress(data.taskId, parseInt(data.content));
          }
          
          // 额外检查，确保状态更新成功
          setTimeout(() => {
            setTasks(prev => {
              const task = prev.get(data.taskId as string);
              if (task && task.status !== "IN_PROGRESS") {
                console.log(`检测到任务[${data.taskId}]状态不是IN_PROGRESS，强制更新`);
                const newMap = new Map(prev);
                newMap.set(data.taskId as string, {
                  ...task,
                  status: "IN_PROGRESS"
                });
                return newMap;
              }
              return prev;
            });
          }, 300);
        }
        break;
        
      case "TASK_STATUS_TO_FINISH":
        // 更新任务状态为已完成
        if (data.taskId) {
          console.log(`任务状态更新为已完成: ${data.taskId}`);
          updateTaskStatus(data.taskId, "COMPLETED");
          updateTaskProgress(data.taskId, 100);
          
          // 直接修改tasks状态，确保任务结果被记录
          setTasks(currentMap => {
            const newMap = new Map(currentMap);
            const task = newMap.get(data.taskId as string);
            
            if (task) {
              console.log(`直接设置任务[${data.taskId}] ${task.taskName} 为已完成状态`);
              newMap.set(data.taskId as string, {
                ...task,
                status: "COMPLETED",
                progress: 100,
                taskResult: data.content || task.taskResult
              });
            } else {
              console.warn(`找不到要完成的任务: ${data.taskId}`);
            }
            
            return newMap;
          });
        }
        break;
        
      case "TASK_IDS":
        // 任务ID列表 - 只更新任务状态
        console.log("收到任务列表消息", data.tasks?.length || 0, "个任务");
        if (data.tasks && data.tasks.length > 0) {
          const taskMap = new Map<string, TaskDTO>();
          
          // 添加父任务
          if (data.taskId) {
            const parentTaskName = "任务处理中...";
            console.log(`添加父任务: ${data.taskId}, ${parentTaskName}`);
            taskMap.set(data.taskId, {
              id: data.taskId,
              taskName: parentTaskName,
              status: "IN_PROGRESS",
              progress: 0,
              parentTaskId: "0"
            });
          }
          
          // 添加子任务
          data.tasks.forEach((task: any) => {
            if (task && task.id) {
              console.log(`添加子任务: ${task.id}, ${task.taskName}, 状态=${task.status || "WAITING"}`);
              taskMap.set(task.id, {
                id: task.id,
                taskName: task.taskName,
                status: task.status || "WAITING",
                progress: task.progress || 0,
                parentTaskId: task.parentTaskId || data.taskId || "0"
              });
            }
          });
          
          // 更新任务状态
          setTasks(prev => {
            const merged = new Map(prev);
            taskMap.forEach((value, key) => {
              merged.set(key, value);
            });
            console.log(`任务Map更新后大小: ${merged.size}`);
            return merged;
          });
        }
        break;
        
      case "TASK_STATUS":
        // 任务状态更新 - 只更新进度
        if(data.taskId && data.content) {
          console.log(`任务进度更新: ${data.taskId} -> ${data.content}%`);
          updateTaskProgress(data.taskId, parseInt(data.content));
        }
        break;
        
      case "TASK_IN_PROGRESS":
        // 任务进行中状态 - 只更新状态
        if(data.taskId) {
          console.log(`任务状态设置为进行中: ${data.taskId}`);
          updateTaskStatus(data.taskId, "IN_PROGRESS");
        }
        break;
        
      case "TASK_COMPLETED":
        // 任务完成状态 - 更新状态并可能创建通知
        if(data.taskId) {
          console.log(`任务状态设置为已完成: ${data.taskId}`);
          updateTaskStatus(data.taskId, "COMPLETED");
          updateTaskProgress(data.taskId, 100);
          
          // 检查是否为父任务
          const task = Array.from(tasks.values()).find(t => t.id === data.taskId);
          const isParentTask = task?.parentTaskId === "0";
          
          // 仅为父任务创建完成消息
          if (isParentTask && data.content) {
            console.log(`创建父任务完成消息: ${data.taskId}`);
            const taskCompletedMessageId = `task-completed-${Date.now()}`;
            addMessage({
              id: taskCompletedMessageId,
              role: 'assistant',
              content: data.content || `✅ 任务处理完成`,
              type: MessageType.TASK_COMPLETED,
              taskId: data.taskId,
              createdAt: new Date()
            });
          }
        }
        break;
    }
  }

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
          <CurrentTaskList 
            taskName={Array.from(tasks.values())
              .find(task => task.parentTaskId === "0")?.taskName || "任务处理中..."}
            tasks={Array.from(tasks.values())
              .filter(task => task.parentTaskId !== "0")}
            isLoading={isTyping && tasks.size === 0}
          />
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

