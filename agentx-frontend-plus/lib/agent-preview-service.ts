import { API_CONFIG } from "@/lib/api-config"

// 预览请求类型
export interface AgentPreviewRequest {
  userMessage: string
  systemPrompt?: string
  toolIds?: string[]
  toolPresetParams?: Record<string, Record<string, Record<string, string>>>
  messageHistory?: MessageHistoryItem[]
  modelId?: string // 可选，不传则使用用户默认模型
  fileUrls?: string[] // 新增：文件URL列表
  knowledgeBaseIds?: string[] // 新增：知识库ID列表，用于RAG功能
}

// 消息历史项
export interface MessageHistoryItem {
  id?: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt?: string
  fileUrls?: string[] // 新增：文件URL列表
}

// 聊天响应类型（流式）- 扩展支持更多消息类型
export interface AgentChatResponse {
  content: string
  done: boolean
  messageType?: string // 支持所有消息类型字符串
  taskId?: string
  payload?: string
  timestamp: number
  tasks?: any[]
  sessionId?: string
  provider?: string
  model?: string
  files?: string[]
}

/**
 * 使用 fetch 方式发送预览请求（返回 ReadableStream）
 * 这是推荐的方式，支持流式响应
 */
export async function previewAgentStream(request: AgentPreviewRequest, signal?: AbortSignal): Promise<ReadableStream<Uint8Array> | null> {
  try {
    const url = `${API_CONFIG.BASE_URL}/agents/sessions/preview`
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // 添加认证header，如果有token的话
        ...(typeof window !== 'undefined' && localStorage.getItem('auth_token') 
          ? { 'Authorization': `Bearer ${localStorage.getItem('auth_token')}` }
          : {}
        )
      },
      body: JSON.stringify(request),
      credentials: 'include',
      signal // 添加AbortSignal支持
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
    }

    return response.body
  } catch (error) {
 
    throw error
  }
}

/**
 * 解析流式响应数据 - 与ChatPanel保持一致的解析逻辑
 */
export function parseStreamData(line: string): AgentChatResponse | null {
  try {
    // 检查是否为data:格式的SSE数据
    if (line.startsWith('data:')) {
      // 提取JSON部分（去掉前缀"data:"）
      let jsonStr = line.substring(5).trim();
      
      // 跳过空数据
      if (!jsonStr) {
        return null;
      }
      
      // 处理结束标记
      if (jsonStr === '[DONE]') {
        return { content: '', done: true, messageType: 'TEXT', timestamp: Date.now() }
      }
      
 
      
      // 解析JSON数据
      const parsed = JSON.parse(jsonStr) as AgentChatResponse;
 
      
      return parsed;
    }
    return null
  } catch (error) {
 
    return null
  }
}

/**
 * 创建流式文本解码器 - 与ChatPanel保持一致的解码逻辑
 */
export function createStreamDecoder(): {
  decode: (chunk: Uint8Array) => string[]
} {
  const decoder = new TextDecoder()
  let buffer = ''

  return {
    decode: (chunk: Uint8Array): string[] => {
      // 解码数据块并添加到缓冲区
      const newText = decoder.decode(chunk, { stream: true });
      buffer += newText;
      
 
 
      
      // 按双换行符分割SSE数据块
      const blocks = buffer.split('\n\n');
      // 保留最后一个可能不完整的块
      buffer = blocks.pop() || '';
      
 
 
      
      // 返回完整的数据块，并过滤空块
      return blocks.filter(block => block.trim() !== '');
    }
  }
}

/**
 * 处理预览响应流 - 与chat-panel保持一致的流处理逻辑
 */
export async function handlePreviewStream(
  stream: ReadableStream<Uint8Array>, 
  onData: (response: AgentChatResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
 
        onComplete?.()
        break
      }

      if (value) {
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
              let jsonStr = line.substring(5)
              // 处理可能存在的重复data:前缀
              if (jsonStr.startsWith("data:")) {
                jsonStr = jsonStr.substring(5)
              }
 
              
              const data = JSON.parse(jsonStr) as AgentChatResponse
 
              
              onData(data)
            } catch (e) {
 
            }
          }
        }
      }
    }
  } catch (error) {
 
    onError?.(error as Error)
  } finally {
    reader.releaseLock()
  }
}

/**
 * 简化的预览函数 - 保持向后兼容，但现在支持所有消息类型 
 */
export async function previewAgent(
  request: AgentPreviewRequest,
  onMessage: (content: string) => void,
  onComplete: (fullContent: string) => void,
  onError?: (error: Error) => void
): Promise<void> {
  try {
    const stream = await previewAgentStream(request)
    if (!stream) {
      throw new Error('Failed to get preview stream')
    }

    let fullContent = ''

    await handlePreviewStream(
      stream,
      (response) => {
        // 处理可显示的消息类型内容
        const displayableTypes = [undefined, "TEXT", "TOOL_CALL"]
        const isDisplayableType = displayableTypes.includes(response.messageType)
        
        if (isDisplayableType && response.content) {
          fullContent += response.content
          onMessage(response.content) // 发送增量内容给UI
        }
      },
      onError,
      () => onComplete(fullContent) // 发送完整内容给UI
    )
  } catch (error) {
    onError?.(error as Error)
  }
} 