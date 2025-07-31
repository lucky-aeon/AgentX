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
    
    console.log('[RAG Stream] Reader created successfully');

    const decoder = new TextDecoder();
    let buffer = '';
    let chunkCount = 0;
    
    while (true) {
      console.log('[RAG Stream] Reading chunk...');
      const { done, value } = await reader.read();
      chunkCount++;
      
      console.log('[RAG Stream] Chunk', chunkCount, '- done:', done, 'value length:', value?.length);
      
      if (done) {
        console.log('[RAG Stream] Stream finished');
        onDone?.();
        break;
      }
      
      buffer += decoder.decode(value, { stream: true });
      console.log('[RAG Stream] Buffer after decode:', buffer);
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';
      console.log('[RAG Stream] Split lines:', lines, 'remaining buffer:', buffer);
      
      for (const line of lines) {
        if (line.trim() === '') continue;
        
        // 处理SSE格式：data:{...} 或 data: {...}
        if (line.startsWith('data:')) {
          const data = line.startsWith('data: ') ? line.slice(6) : line.slice(5);
          console.log('[RAG Stream] Raw SSE data:', data);
          
          if (data.trim() === '[DONE]') {
            console.log('[RAG Stream] Received [DONE] signal');
            onDone?.();
            return;
          }
          
          try {
            const message: SSEMessage = JSON.parse(data);
            console.log('[RAG Stream] Parsed message:', message);
            
            // 兼容处理：后端可能使用 message.type 或 message.messageType
            const messageType = message.type || (message as any).messageType;
            
            // 根据消息类型处理
            console.log('[RAG Stream] Processing message:', {
              messageType,
              content: message.content,
              timestamp: message.timestamp,
              done: message.done
            });
            
            switch (messageType) {
              case 'RAG_RETRIEVAL_START':
                console.log('[RAG Stream] Handling retrieval start');
                onThinking?.({
                  type: 'retrieval',
                  status: 'start',
                  message: message.content || '开始检索相关文档...'
                });
                break;
              case 'RAG_RETRIEVAL_END':
              case 'RAG_RETRIEVAL_COMPLETE':
                console.log('[RAG Stream] Handling retrieval complete');
                const retrievalData = {
                  type: 'retrieval',
                  status: 'end',
                  message: message.content || '检索完成',
                  documents: message.payload ? JSON.parse(message.payload) : [],
                  retrievedCount: 0
                };
                // 计算检索到的文档数量
                if (retrievalData.documents && Array.isArray(retrievalData.documents)) {
                  retrievalData.retrievedCount = retrievalData.documents.length;
                }
                console.log('[RAG Stream] Retrieval completed with documents:', retrievalData);
                onThinking?.(retrievalData);
                break;
              case 'RAG_THINKING_START':
                console.log('[RAG Stream] Handling thinking start');
                onThinking?.({ type: 'thinking', status: 'start', message: message.content });
                break;
              case 'RAG_THINKING_PROGRESS':
                console.log('[RAG Stream] Handling thinking progress:', message.content);
                // 思考过程的内容传递给专门的回调
                onThinkingContent?.(message.content || '', message.timestamp);
                break;
              case 'RAG_THINKING_END':
                console.log('[RAG Stream] Handling thinking end');
                // 思考结束
                onThinkingEnd?.();
                break;
              case 'RAG_ANSWER_START':
                console.log('[RAG Stream] Answer started');
                onThinking?.({
                  type: 'answer',
                  status: 'start', 
                  message: message.content || '开始生成回答...'
                });
                break;
              case 'RAG_ANSWER_PROGRESS':
                console.log('[RAG Stream] Handling answer progress:', message.content);
                onContent?.(message.content || '', message.timestamp);
                break;
              case 'RAG_ANSWER_COMPLETE':
              case 'RAG_ANSWER_END':
                console.log('[RAG Stream] Answer completed');
                onDone?.();
                break;
              case 'ERROR':
                console.log('[RAG Stream] Handling ERROR message:', message.content);
                onError?.(message.content || 'Unknown error');
                break;
              case 'TEXT':
                console.log('[RAG Stream] Handling TEXT message as error:', message.content);
                // TEXT类型消息作为错误处理
                onError?.(message.content || 'Unknown error');
                break;
              default:
                console.warn('[RAG Stream] Unknown message type:', messageType, message);
            }
          } catch (e) {
            console.warn('Failed to parse SSE message:', data, e);
          }
        }
      }
    }
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      console.log('[RAG Stream] RAG stream chat by userRag aborted');
    } else {
      console.error('[RAG Stream] RAG stream chat by userRag error:', error);
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
              case 'TEXT':
                // TEXT类型消息作为错误处理
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