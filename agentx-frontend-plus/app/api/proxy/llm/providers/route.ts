import { API_CONFIG } from "@/lib/api-config"
import { NextResponse } from "next/server"

export async function GET() {
  try {
    const apiUrl = `${API_CONFIG.BASE_URL}/llm/providers`
    
    console.log(`Proxying GET request to: ${apiUrl}`)
    
    const response = await fetch(apiUrl, {
      method: "GET",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })
    
    const data = await response.json()
    
    if (!response.ok) {
      return NextResponse.json(
        {
          code: response.status,
          message: "获取服务提供商列表失败",
          data: null,
          timestamp: Date.now(),
        },
        { status: response.status }
      )
    }
    
    return NextResponse.json(data)
  } catch (error) {
    console.error("代理服务提供商列表请求错误:", error)
    
    return NextResponse.json(
      {
        code: 500,
        message: error instanceof Error ? error.message : "未知错误",
        data: null,
        timestamp: Date.now(),
      },
      { status: 500 }
    )
  }
}