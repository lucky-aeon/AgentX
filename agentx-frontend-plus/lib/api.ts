export async function streamChat(message: string, sessionId?: string) {
  if (!sessionId) {
    throw new Error("Session ID is required")
  }

  // 构建完整的API URL - 通过本地Next.js API路由代理请求
  const url = new URL(`/api/agent/session/chat`, window.location.origin)

  // 发送请求
  try {
    console.log("发送聊天请求:", url.toString()) // 添加日志以便调试
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify({ 
        message, 
        sessionId  // 将sessionId作为请求体的一部分发送
      }),
    })

    if (!response.ok) {
      const errorData = await response.json().catch(() => null)
      throw new Error(errorData?.error || `API request failed with status ${response.status}`)
    }

    return response
  } catch (error) {
    console.error("Stream chat error:", error)
    throw error
  }
}

