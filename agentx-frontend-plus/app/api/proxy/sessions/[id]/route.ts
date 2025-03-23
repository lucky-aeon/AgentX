import { type NextRequest, NextResponse } from "next/server"

// 直接使用环境变量，不再附加"/api"
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://68c8ff2.r3.cpolar.top/api"

export async function GET(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const sessionId = params.id
    if (!sessionId) {
      return NextResponse.json({ error: "Session ID is required" }, { status: 400 })
    }

    // 构建API URL
    const apiUrl = `${API_BASE_URL}/conversation/session/${sessionId}`
    console.log(`Proxying GET request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      // 增加超时时间
      signal: AbortSignal.timeout(15000), // 15秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error")
      console.error(`API request failed with status ${response.status}: ${errorText}`)

      // 返回更详细的错误信息
      return NextResponse.json(
        {
          code: response.status,
          message: `API request failed with status ${response.status}: ${errorText}`,
          data: null,
          timestamp: Date.now(),
        },
        { status: 200 }, // 返回200状态码，让前端能够处理错误
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)

    // 返回格式化的错误响应
    return NextResponse.json(
      {
        code: 500,
        message: error instanceof Error ? error.message : "Internal server error",
        data: null,
        timestamp: Date.now(),
      },
      { status: 200 }, // 返回200状态码，让前端能够处理错误
    )
  }
}

export async function PUT(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const sessionId = params.id
    if (!sessionId) {
      return NextResponse.json({ error: "Session ID is required" }, { status: 400 })
    }

    // 从请求URL中获取查询参数
    const { searchParams } = new URL(request.url)
    const title = searchParams.get("title")
    const description = searchParams.get("description")
    const archived = searchParams.get("archived")

    // 构建API URL
    let apiUrl = `${API_BASE_URL}/conversation/session/${sessionId}`
    const queryParams = new URLSearchParams()

    if (title) queryParams.append("title", title)
    if (description) queryParams.append("description", description)
    if (archived !== null) queryParams.append("archived", archived)

    if (queryParams.toString()) {
      apiUrl += `?${queryParams.toString()}`
    }

    console.log(`Proxying PUT request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      signal: AbortSignal.timeout(15000), // 15秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error")
      console.error(`API request failed with status ${response.status}: ${errorText}`)

      return NextResponse.json(
        {
          code: response.status,
          message: `API request failed with status ${response.status}: ${errorText}`,
          data: null,
          timestamp: Date.now(),
        },
        { status: 200 },
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)

    return NextResponse.json(
      {
        code: 500,
        message: error instanceof Error ? error.message : "Internal server error",
        data: null,
        timestamp: Date.now(),
      },
      { status: 200 },
    )
  }
}

export async function DELETE(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const sessionId = params.id
    if (!sessionId) {
      return NextResponse.json({ error: "Session ID is required" }, { status: 400 })
    }

    // 构建API URL
    const apiUrl = `${API_BASE_URL}/conversation/session/${sessionId}`
    console.log(`Proxying DELETE request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      signal: AbortSignal.timeout(15000), // 15秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      const errorText = await response.text().catch(() => "Unknown error")
      console.error(`API request failed with status ${response.status}: ${errorText}`)

      return NextResponse.json(
        {
          code: response.status,
          message: `API request failed with status ${response.status}: ${errorText}`,
          data: null,
          timestamp: Date.now(),
        },
        { status: 200 },
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)

    return NextResponse.json(
      {
        code: 500,
        message: error instanceof Error ? error.message : "Internal server error",
        data: null,
        timestamp: Date.now(),
      },
      { status: 200 },
    )
  }
}

