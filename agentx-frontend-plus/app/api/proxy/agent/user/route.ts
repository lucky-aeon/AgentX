import { type NextRequest, NextResponse } from "next/server"
import { API_CONFIG } from "@/lib/api-config"

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url)
    const name = searchParams.get("name")

    // 构建API URL
    let apiUrl = `${API_CONFIG.BASE_URL}/agent/user`
    const queryParams: Record<string, string> = {}

    if (name) {
      queryParams.name = name
    }

    // 添加查询参数
    if (Object.keys(queryParams).length > 0) {
      const queryString = new URLSearchParams(queryParams).toString()
      apiUrl += `?${queryString}`
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
    console.error("Error in agent proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
    )
  }
}

