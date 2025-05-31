import { API_CONFIG } from "@/lib/api-config"

// 预览请求类型
export interface AgentPreviewRequest {
  userMessage: string
  systemPrompt?: string
  toolIds?: string[]
  toolPresetParams?: Record<string, Record<string, Record<string, string>>>
  messageHistory?: MessageHistoryItem[]
  modelId?: string // 可选，不传则使用用户默认模型
}

// 消息历史项
export interface MessageHistoryItem {
  id?: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createdAt?: string
}

// 聊天响应类型（流式）
export interface AgentChatResponse {
  content: string
  done: boolean
  messageType: 'TEXT' | 'TASK_IDS' | 'ERROR'
  taskId?: string
  payload?: string
  timestamp: number
  tasks?: any[]
}

/**
 * 使用 fetch 方式发送预览请求（返回 ReadableStream）
 * 这是推荐的方式，支持流式响应
 */
export async function previewAgentStream(request: AgentPreviewRequest): Promise<ReadableStream<Uint8Array> | null> {
  try {
    const url = `${API_CONFIG.BASE_URL}/agent/session/preview`
    
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
    })

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
    }

    return response.body
  } catch (error) {
    console.error('Preview request failed:', error)
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
      
      console.log('Parsing JSON string:', jsonStr);
      
      // 解析JSON数据
      const parsed = JSON.parse(jsonStr) as AgentChatResponse;
      console.log('Parsed result:', parsed);
      
      return parsed;
    }
    return null
  } catch (error) {
    console.error('Failed to parse stream data:', error, 'Line:', line)
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
      
      console.log('Received chunk:', newText);
      console.log('Current buffer:', buffer);
      
      // 按双换行符分割SSE数据块
      const blocks = buffer.split('\n\n');
      // 保留最后一个可能不完整的块
      buffer = blocks.pop() || '';
      
      console.log('Split blocks:', blocks);
      console.log('Remaining buffer:', buffer);
      
      // 返回完整的数据块，并过滤空块
      return blocks.filter(block => block.trim() !== '');
    }
  }
}

/**
 * 处理预览响应流 - 简化版本，专注于TEXT消息类型
 */
export async function handlePreviewStream(
  stream: ReadableStream<Uint8Array>, 
  onData: (response: AgentChatResponse) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> {
  const reader = stream.getReader()
  const streamDecoder = createStreamDecoder()

  try {
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        console.log('Stream reading completed');
        onComplete?.()
        break
      }

      if (value) {
        const dataBlocks = streamDecoder.decode(value)
        
        for (const block of dataBlocks) {
          console.log('Processing block:', block);
          
          // 每个数据块可能包含多行，分别处理
          const lines = block.split('\n').filter(line => line.trim() !== '');
          
          for (const line of lines) {
            console.log('Processing line:', line);
            
            const response = parseStreamData(line)
            if (response) {
              console.log('Parsed preview response:', response)
              onData(response)
              
              // 如果收到结束标记，退出
              if (response.done) {
                console.log('Received done signal, completing stream');
                onComplete?.()
                return
              }
            }
          }
        }
      }
    }
  } catch (error) {
    console.error('Stream processing error:', error)
    onError?.(error as Error)
  } finally {
    reader.releaseLock()
  }
}

/**
 * 简化的预览函数，自动处理流式响应
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
        // 只处理TEXT类型的消息内容
        if (response.messageType === 'TEXT' && response.content) {
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