import { publicHttpClient } from './public-http-client';
import { API_CONFIG } from "@/lib/api-config";

// Widget聊天请求类型
export interface WidgetChatRequest {
  message: string;
  sessionId: string; // 后端要求sessionId必须提供
  fileUrls?: string[];
}

// Widget聊天响应类型
export interface WidgetChatResponse {
  content: string;
  done: boolean;
  messageType?: string;
  taskId?: string;
  payload?: string;
  timestamp: number;
  tasks?: any[];
  sessionId?: string;
  provider?: string;
  model?: string;
  files?: string[];
}

/**
 * Widget聊天流式请求 - 使用公开HTTP客户端，无需认证
 */
export async function widgetChatStream(publicId: string, request: WidgetChatRequest, signal?: AbortSignal): Promise<ReadableStream<Uint8Array> | null> {
  try {
    const url = `${API_CONFIG.BASE_URL}/widget/${publicId}/chat`;
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Referer': typeof window !== 'undefined' ? window.location.origin : '',
      },
      body: JSON.stringify(request),
      signal // 添加AbortSignal支持
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }

    return response.body;
  } catch (error) {
 
    throw error;
  }
}

/**
 * 解析Widget流式响应数据
 */
export function parseWidgetStreamData(line: string): WidgetChatResponse | null {
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
        return { content: '', done: true, messageType: 'TEXT', timestamp: Date.now() };
      }
      
 
      
      // 解析JSON数据
      const parsed = JSON.parse(jsonStr) as WidgetChatResponse;
 
      
      return parsed;
    }
  } catch (error) {
 
  }
  
  return null;
}

/**
 * 处理Widget流式响应
 */
export async function handleWidgetStream(
  stream: ReadableStream<Uint8Array>,
  onData: (data: WidgetChatResponse) => void,
  onError: (error: Error) => void,
  onComplete: () => void
): Promise<void> {
  if (!stream) {
    onError(new Error('No stream provided'));
    return;
  }

  try {
    const reader = stream.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      
      if (done) break;
      
      buffer += decoder.decode(value, { stream: true });
      
      // 按行分割数据
      const lines = buffer.split('\n');
      // 保留最后一行（可能不完整）
      buffer = lines.pop() || '';
      
      // 处理完整的行
      for (const line of lines) {
        if (line.trim()) {
          const parsed = parseWidgetStreamData(line);
          if (parsed) {
            onData(parsed);
            
            // 如果收到完成标记，结束流处理
            if (parsed.done) {
              onComplete();
              return;
            }
          }
        }
      }
    }
    
    // 处理剩余缓冲区内容
    if (buffer.trim()) {
      const parsed = parseWidgetStreamData(buffer);
      if (parsed) {
        onData(parsed);
      }
    }
    
    onComplete();
  } catch (error) {
 
    onError(error instanceof Error ? error : new Error('Widget stream processing error'));
  }
}