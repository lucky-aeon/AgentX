import { API_CONFIG, API_ENDPOINTS } from "@/lib/api-config"
import type { RagStreamChatRequest, SSEMessage } from "@/types/rag-dataset"

// RAG流式聊天相关API

export interface RagChatOptions {
  onThinking?: (data: any) => void;
  onThinkingContent?: (content: string, timestamp?: number) => void; // 思考过程内容
  onThinkingEnd?: () => void; // 思考结束
  onContent?: (content: string, timestamp?: number) => void;
  onError?: (error: string) => void;
  onDone?: () => void;
  signal?: AbortSignal;
}

// 基于已安装知识库的RAG流式问答
export async function ragStreamChatByUserRag(
  userRagId: string,
  request: RagStreamChatRequest,
  options: RagChatOptions = {}
): Promise<void> {
  const { onThinking, onThinkingContent, onThinkingEnd, onContent, onError, onDone, signal } = options;
  
  try {
    console.log("Starting RAG stream chat by userRag:", userRagId, request)
    
    // 构建请求URL
    const url = `${API_CONFIG.BASE_URL}${API_ENDPOINTS.RAG_STREAM_CHAT_BY_USER_RAG(userRagId)}`;
    
    // 获取认证token（与httpClient保持一致）
    const token = localStorage.getItem("auth_token");
    
    // 发起SSE请求
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error("No response body");
    }

    const decoder = new TextDecoder();
    
    while (true) {
      const { done, value } = await reader.read();
      
      if (done) {
        onDone?.();
        break;
      }
      
      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split('\n');
      
      for (const line of lines) {
        if (line.trim() === '') continue;
        
        // 处理SSE格式：data: {...}
        if (line.startsWith('data: ')) {
          const data = line.slice(6);
          
          if (data.trim() === '[DONE]') {
            onDone?.();
            return;
          }
          
          try {
            const message: SSEMessage = JSON.parse(data);
            
            // 根据消息类型处理
            switch (message.type) {
              case 'RAG_RETRIEVAL_START':
              case 'RAG_RETRIEVAL_COMPLETE':
                onThinking?.(message.data);
                break;
              case 'RAG_THINKING_START':
              case 'RAG_THINKING_COMPLETE':
                if (message.content) {
                  onThinkingContent?.(message.content, message.timestamp);
                }
                if (message.type === 'RAG_THINKING_COMPLETE') {
                  onThinkingEnd?.();
                }
                break;
              case 'RAG_ANSWER_START':
                // 开始回答
                break;
              case 'RAG_ANSWER_PART':
                onContent?.(message.content || '', message.timestamp);
                break;
              case 'RAG_ANSWER_COMPLETE':
                onDone?.();
                break;
              case 'ERROR':
                onError?.(message.content || 'Unknown error');
                break;
            }
          } catch (e) {
            console.warn('Failed to parse SSE message:', data, e);
          }
        }
      }
    }
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('RAG stream chat by userRag aborted');
    } else {
      console.error('RAG stream chat by userRag error:', error);
      onError?.(error instanceof Error ? error.message : 'Unknown error');
    }
  }
}

// RAG流式问答
export async function ragStreamChat(
  request: RagStreamChatRequest,
  options: RagChatOptions = {}
): Promise<void> {
  const { onThinking, onThinkingContent, onThinkingEnd, onContent, onError, onDone, signal } = options;
  
  try {
    console.log("Starting RAG stream chat:", request)
    
    // 构建请求URL
    const url = `${API_CONFIG.BASE_URL}${API_ENDPOINTS.RAG_STREAM_CHAT}`;
    
    // 获取认证token（与httpClient保持一致）
    const token = localStorage.getItem("auth_token");
    
    // 发起SSE请求
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Authorization': token ? `Bearer ${token}` : '',
      },
      body: JSON.stringify(request),
      signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error("No response body");
    }

    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.trim() === '') continue;
        
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim();
          
          if (data === '[DONE]') {
            onDone?.();
            return;
          }

          try {
            const message = JSON.parse(data);
            
            
            // 根据后端的messageType处理不同类型的消息
            switch (message.messageType) {
              case 'RAG_RETRIEVAL_START':
                onThinking?.({ type: 'retrieval', status: 'start', message: message.content });
                break;
              case 'RAG_RETRIEVAL_PROGRESS':
                onThinking?.({ type: 'retrieval', status: 'progress', message: message.content });
                break;
              case 'RAG_RETRIEVAL_END':
                const retrievedDocs = message.payload ? JSON.parse(message.payload) : [];
                onThinking?.({ 
                  type: 'retrieval', 
                  status: 'end', 
                  message: message.content,
                  retrievedCount: retrievedDocs.length,
                  documents: retrievedDocs
                });
                break;
              case 'RAG_ANSWER_START':
                onThinking?.({ type: 'answer', status: 'start', message: message.content });
                break;
              case 'RAG_THINKING_START':
                onThinking?.({ type: 'thinking', status: 'start', message: message.content });
                break;
              case 'RAG_THINKING_PROGRESS':
                // 思考过程的内容传递给专门的回调
                onThinkingContent?.(message.content || '', message.timestamp);
                break;
              case 'RAG_THINKING_END':
                // 思考结束
                onThinkingEnd?.();
                break;
              case 'RAG_ANSWER_PROGRESS':
                // 这是实际的回答内容
                onContent?.(message.content || '', message.timestamp);
                break;
              case 'RAG_ANSWER_END':
                onDone?.();
                break;
              case 'ERROR':
                onError?.(message.content || '未知错误');
                break;
              default:
                console.warn('Unknown message type:', message.messageType);
            }
            
            // 如果消息标记为done，也触发完成回调
            if (message.done === true) {
              onDone?.();
            }
          } catch (e) {
            console.error('Failed to parse SSE message:', e, 'Raw data:', data);
          }
        }
      }
    }
  } catch (error) {
    console.error("RAG stream chat error:", error);
    if (error instanceof Error && error.name !== 'AbortError') {
      onError?.(error.message);
    }
  }
}

// 创建一个更高级的聊天会话管理器
// 基于已安装知识库的RAG聊天会话管理器
export class UserRagChatSession {
  private abortController: AbortController | null = null;

  async start(userRagId: string, request: RagStreamChatRequest, options: RagChatOptions): Promise<void> {
    // 如果有正在进行的会话，先取消
    this.abort();
    
    // 创建新的AbortController
    this.abortController = new AbortController();
    
    // 合并signal
    const mergedOptions: RagChatOptions = {
      ...options,
      signal: this.abortController.signal,
    };
    
    try {
      await ragStreamChatByUserRag(userRagId, request, mergedOptions);
    } finally {
      this.abortController = null;
    }
  }

  abort(): void {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
  }

  isActive(): boolean {
    return this.abortController !== null;
  }
}

export class RagChatSession {
  private abortController: AbortController | null = null;

  async start(request: RagStreamChatRequest, options: RagChatOptions): Promise<void> {
    // 如果有正在进行的会话，先取消
    this.abort();
    
    // 创建新的AbortController
    this.abortController = new AbortController();
    
    // 合并signal
    const mergedOptions: RagChatOptions = {
      ...options,
      signal: this.abortController.signal,
    };
    
    try {
      await ragStreamChat(request, mergedOptions);
    } finally {
      this.abortController = null;
    }
  }

  abort(): void {
    if (this.abortController) {
      this.abortController.abort();
      this.abortController = null;
    }
  }

  isActive(): boolean {
    return this.abortController !== null;
  }
}