import { httpClient } from "@/lib/http-client";

export async function streamChat(message: string, sessionId?: string) {
  if (!sessionId) {
    throw new Error("Session ID is required");
  }

  // 使用 httpClient 但启用 raw 模式，返回原始 Response 对象
  try {
    const response = await httpClient.post<Response>(
      "/agent/session/chat",
      { message, sessionId },
      {}, // 请求配置，保持默认
      { raw: true } // 启用原始响应模式，不自动JSON解析，保留流
    );

    // raw 模式下 response 就是原始 Response 对象，可以直接使用 response.body 等
    return response;
  } catch (error) {
    console.error("Stream chat error:", error);
    throw error;
  }
}

