import { type NextRequest, NextResponse } from "next/server"

// 直接使用环境变量，不再附加"/api"
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "https://68c8ff2.r3.cpolar.top/api"

export async function GET(request: NextRequest) {
  try {
    // 从请求URL中获取查询参数
    const { searchParams } = new URL(request.url)
    const userId = searchParams.get("userId") || "1"
    const archived = searchParams.get("archived")

    // 构建API URL
    let apiUrl = `${API_BASE_URL}/conversation/session?userId=${userId}`
    if (archived !== null) {
      apiUrl += `&archived=${archived}`
    }

    console.log(`Proxying request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
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

export async function POST(request: NextRequest) {
  try {
    // 从请求URL中获取查询参数
    const { searchParams } = new URL(request.url)
    const title = searchParams.get("title")
    const userId = searchParams.get("userId") || "1"
    const description = searchParams.get("description")

    if (!title) {
      return NextResponse.json({ error: "title is required" }, { status: 400 })
    }

    // 构建API URL
    let apiUrl = `${API_BASE_URL}/conversation/session?title=${encodeURIComponent(title)}&userId=${userId}`
    if (description) {
      apiUrl += `&description=${encodeURIComponent(description)}`
    }

    console.log(`Proxying POST request to: ${apiUrl}`)

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
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

