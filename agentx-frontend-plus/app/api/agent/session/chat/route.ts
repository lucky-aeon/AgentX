import { type NextRequest } from "next/server"

// 直接使用明确的后端地址，避免从环境变量中可能带有/api的BASE_URL
const BACKEND_URL = "http://127.0.0.1:8080"

export async function POST(request: NextRequest) {
  try {
    // 获取请求体
    const requestBody = await request.json()
    const { sessionId, message } = requestBody

    if (!sessionId) {
      return new Response(JSON.stringify({ error: "Session ID is required" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      })
    }

    // 构建明确的API URL，直接请求后端8080端口
    const apiUrl = `${BACKEND_URL}/api/agent/session/chat`
    console.log(`直接请求后端API: ${apiUrl}`)

    // 发送请求到后端API
    const response = await fetch(apiUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
        Connection: "keep-alive",
      },
      body: JSON.stringify({ sessionId, message }),
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`Chat API request failed with status ${response.status}`)
      
      // 尝试解析错误响应
      try {
        const errorText = await response.text()
        return new Response(
          JSON.stringify({ error: `API request failed with status ${response.status}`, details: errorText }),
          { status: response.status, headers: { "Content-Type": "application/json" } }
        )
      } catch (parseError) {
        return new Response(
          JSON.stringify({ error: `API request failed with status ${response.status}` }),
          { status: response.status, headers: { "Content-Type": "application/json" } }
        )
      }
    }

    // 直接将流式响应传递给客户端
    return new Response(response.body, {
      headers: {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        "Connection": "keep-alive",
      },
    })
  } catch (error) {
    console.error("Error in chat API route:", error)
    return new Response(
      JSON.stringify({
        error: "Internal server error",
        details: error instanceof Error ? error.message : String(error),
      }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    )
  }
} 