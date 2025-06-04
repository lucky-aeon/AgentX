import { streamChat as streamChatService } from "@/lib/stream-service";

export async function streamChat(message: string, sessionId?: string, signal?: AbortSignal , fileUrls?: string[]) {
  if (!sessionId) {
    throw new Error("Session ID is required");
  }

  try {
    // 使用新的stream-service调用流式聊天API，传递文件URL
    const response = await streamChatService(sessionId, message, signal, fileUrls);
    return response;
  } catch (error) {
    console.error("Stream chat error:", error);
    throw error;
  }
}





