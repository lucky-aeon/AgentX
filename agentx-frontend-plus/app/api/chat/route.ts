import { type NextRequest, NextResponse } from "next/server"

export async function GET(request: NextRequest) {
  try {
    // 从请求 URL 中获取查询参数
    const { searchParams } = new URL(request.url)
    const message = searchParams.get("message")
    const sessionId = searchParams.get("sessionId")

    if (!message) {
      return NextResponse.json({ error: "Message is required" }, { status: 400 })
    }

    // 构建 API URL
    const apiUrl = new URL("https://68c8ff2.r3.cpolar.top/api/conversation/chat/stream")
    apiUrl.searchParams.append("message", message)
    if (sessionId) {
      apiUrl.searchParams.append("sessionId", sessionId)
    }

    // 发送请求到外部 API
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
    })

    // 检查响应状态
    if (!response.ok) {
      return NextResponse.json(
        { error: `API request failed with status ${response.status}` },
        { status: response.status },
      )
    }

    // 直接传递原始响应
    return new NextResponse(response.body, {
      headers: {
        "Content-Type": "text/event-stream",
        "Cache-Control": "no-cache",
        Connection: "keep-alive",
      },
    })
  } catch (error) {
    console.error("Error in chat API route:", error)
    return NextResponse.json({ error: "Internal server error" }, { status: 500 })
  }
}

