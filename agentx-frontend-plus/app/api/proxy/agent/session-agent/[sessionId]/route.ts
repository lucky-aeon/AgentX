import { type NextRequest, NextResponse } from "next/server"
import { API_CONFIG } from "@/lib/api-config"

export async function GET(request: NextRequest, { params }: { params: { sessionId: string } }) {
  try {
    const sessionId = params.sessionId

    // 构建API URL
    const apiUrl = `${API_CONFIG.BASE_URL}/agent/session-agent/${sessionId}`

    console.log(`Proxying GET request to: ${apiUrl}`)

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
    console.error("Error in agent session-agent proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
    )
  }
}

