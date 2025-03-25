import { type NextRequest } from "next/server"

// API基础URL
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://127.0.0.1:8080"

export async function POST(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    // 使用 await 解包 params 对象
    const resolvedParams = await Promise.resolve(params)
    const sessionId = resolvedParams.id
    if (!sessionId) {
      return new Response(JSON.stringify({ error: "Session ID is required" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      })
    }

    // 获取请求体
    const requestBody = await request.json()

    // 构建API URL - 去掉多余的 /api 前缀
    const apiUrl = `${API_BASE_URL}/agent/session/${sessionId}/message`
    console.log(`Proxying POST request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
        Connection: "keep-alive",
      },
      body: JSON.stringify(requestBody),
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`Message API request failed with status ${response.status}`)
      
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
    console.error("Error in message proxy API route:", error)
    return new Response(
      JSON.stringify({
        error: "Internal server error",
        details: error instanceof Error ? error.message : String(error),
      }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    )
  }
} 