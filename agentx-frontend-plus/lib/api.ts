import { streamChat as streamChatService } from "@/lib/stream-service";

export async function streamChat(message: string, sessionId?: string) {
  if (!sessionId) {
    throw new Error("Session ID is required");
  }

  try {
    // 使用新的stream-service调用流式聊天API
    const response = await streamChatService(sessionId, message);
    return response;
  } catch (error) {
    console.error("Stream chat error:", error);
    throw error;
  }
}

