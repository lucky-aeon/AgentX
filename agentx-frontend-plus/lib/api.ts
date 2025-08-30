import { streamChat as streamChatService } from "@/lib/stream-service";

export async function streamChat(message: string, sessionId?: string, fileUrls?: string[]) {
  if (!sessionId) {
    throw new Error("Session ID is required");
  }

  try {
    // 使用新的stream-service调用流式聊天API，传递文件URL
    const response = await streamChatService(sessionId, message, fileUrls);
    return response;
  } catch (error) {
 
    throw error;
  }
}

