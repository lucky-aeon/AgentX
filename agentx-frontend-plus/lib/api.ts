export async function streamChat(message: string, sessionId?: string) {
  if (!sessionId) {
    throw new Error("Session ID is required")
  }

  // 构建完整的API URL
  const url = new URL("/api/chat", window.location.origin)

  // 添加查询参数
  url.searchParams.append("message", message)
  url.searchParams.append("sessionId", sessionId)

  // 发送请求
  try {
    console.log("发送聊天请求:", url.toString()) // 添加日志以便调试
    const response = await fetch(url, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
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

