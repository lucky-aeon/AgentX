import { type NextRequest, NextResponse } from "next/server"
import { API_CONFIG } from "@/lib/api-config"

export async function PUT(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const agentId = params.id

    // 构建API URL
    const apiUrl = `${API_CONFIG.BASE_URL}/agent/${agentId}/status`
    console.log(`Proxying PUT request to: ${apiUrl}`)

    // 获取请求体
    const body = await request.json()

    // 发送请求到外部API
    const response = await fetch(apiUrl, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Accept: "*/*",
      },
      body: JSON.stringify(body),
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
    console.error("Error in agent status proxy API route:", error)
    return NextResponse.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : String(error) },
      { status: 500 },
    )
  }
}

