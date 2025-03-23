import { type NextRequest, NextResponse } from "next/server"

// API基础URL
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://127.0.0.1:8080"

export async function GET(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const sessionId = params.id
    if (!sessionId) {
      return NextResponse.json({ error: "Session ID is required" }, { status: 400 })
    }

    // 构建API URL
    const apiUrl = `${API_BASE_URL}/api/conversation/session/${sessionId}`
    console.log(`Proxying GET request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      signal: AbortSignal.timeout(10000), // 10秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`API request failed with status ${response.status}`)
      return NextResponse.json(
        { error: `API request failed with status ${response.status}` },
        { status: response.status },
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
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
    let apiUrl = `${API_BASE_URL}/api/conversation/session/${sessionId}`
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
      signal: AbortSignal.timeout(10000), // 10秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`API request failed with status ${response.status}`)
      return NextResponse.json(
        { error: `API request failed with status ${response.status}` },
        { status: response.status },
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
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
    const apiUrl = `${API_BASE_URL}/api/conversation/session/${sessionId}`
    console.log(`Proxying DELETE request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "DELETE",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      signal: AbortSignal.timeout(10000), // 10秒超时
    })

    // 检查响应状态
    if (!response.ok) {
      console.error(`API request failed with status ${response.status}`)
      return NextResponse.json(
        { error: `API request failed with status ${response.status}` },
        { status: response.status },
      )
    }

    // 获取响应数据
    const data = await response.json()
    return NextResponse.json(data)
  } catch (error) {
    console.error("Error in session proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
    )
  }
}

