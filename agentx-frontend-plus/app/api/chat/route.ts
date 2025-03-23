import { type NextRequest, NextResponse } from "next/server"

// 直接使用环境变量，不再附加"/api"
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://68c8ff2.r3.cpolar.top/api"

export async function GET(request: NextRequest) {
  try {
    // 从请求 URL 中获取查询参数
    const { searchParams } = new URL(request.url)
    const message = searchParams.get("message")
    const sessionId = searchParams.get("sessionId")

    if (!message) {
      return NextResponse.json({ error: "Message is required" }, { status: 400 })
    }

    if (!sessionId) {
      return NextResponse.json({ error: "Session ID is required" }, { status: 400 })
    }

    // 构建 API URL - 使用新的接口格式
    const apiUrl = `${API_BASE_URL}/conversation/chat/${sessionId}?content=${encodeURIComponent(message)}`

    console.log(`Proxying chat request to: ${apiUrl}`)

    // 发送请求到外部 API
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
        Connection: "keep-alive",
      },
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`Chat API request failed with status ${response.status}`)
      return NextResponse.json(
        { error: `API request failed with status ${response.status}` },
        { status: response.status },
      )
    }

    // 获取响应数据
    const data = await response.json()

    // 模拟流式响应
    const encoder = new TextEncoder()
    const stream = new ReadableStream({
      async start(controller) {
        // 将响应内容分成多个小块，模拟流式传输
        const content = data.data?.content || "抱歉，没有收到有效的响应。"
        const chunkSize = 3 // 每个块的字符数

        // 发送开始标记
        controller.enqueue(
          encoder.encode(
            `data:${JSON.stringify({
              content: "",
              done: false,
              sessionId,
              provider: "api",
              model: "gpt-3.5-turbo",
              timestamp: Date.now(),
            })}\n\n`,
          ),
        )

        // 分块发送内容
        for (let i = 0; i < content.length; i += chunkSize) {
          const chunk = content.substring(i, i + chunkSize)
          const isDone = i + chunkSize >= content.length

          // 构建SSE格式的数据
          const sseData = {
            content: chunk,
            done: isDone,
            sessionId,
            provider: "api",
            model: "gpt-3.5-turbo",
            timestamp: Date.now(),
          }

          controller.enqueue(encoder.encode(`data:${JSON.stringify(sseData)}\n\n`))

          // 添加延迟以模拟真实的流式传输
          if (!isDone) {
            await new Promise((resolve) => setTimeout(resolve, 50))
          }
        }

        controller.close()
      },
    })

    // 返回流式响应
    return new NextResponse(stream, {
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

