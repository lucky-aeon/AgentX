import type {
  ApiResponse,
  CreateSessionParams,
  GetSessionsParams,
  Session,
  UpdateSessionParams,
} from "@/types/conversation"

// 构建查询字符串
function buildQueryString(params: Record<string, any>): string {
  const query = Object.entries(params)
    .filter(([_, value]) => value !== undefined && value !== null)
    .map(([key, value]) => {
      if (typeof value === "boolean") {
        // 如果是布尔值，只传递键名
        return value ? key : null
      }
      return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`
    })
    .filter(Boolean)
    .join("&")

  return query ? `?${query}` : ""
}

// 创建会话
export async function createSession(params: CreateSessionParams): Promise<ApiResponse<Session>> {
  try {
    const queryString = buildQueryString(params)
    const url = `/api/proxy/sessions${queryString}`

    console.log(`Creating session with URL: ${url}`)

    const response = await fetch(url, {
      method: "POST",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })

    const data = await response.json()

    if (!response.ok && !data.code) {
      throw new Error(`创建会话失败: ${response.status}, ${data.error || "Unknown error"}`)
    }

    return data
  } catch (error) {
    console.error("创建会话错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as Session[],
      timestamp: Date.now(),
    }
  }
}

// 获取会话列表
export async function getSessions(params: GetSessionsParams): Promise<ApiResponse<Session[]>> {
  try {
    const queryString = buildQueryString(params)
    const url = `/api/proxy/sessions${queryString}`

    console.log(`Fetching sessions with URL: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })

    const data = await response.json()

    if (!response.ok && !data.code) {
      throw new Error(`获取会话列表失败: ${response.status}, ${data.error || "Unknown error"}`)
    }

    return data
  } catch (error) {
    console.error("获取会话列表错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: [] as Session[],
      timestamp: Date.now(),
    }
  }
}

// 获取单个会话详情
export async function getSession(sessionId: string): Promise<ApiResponse<Session>> {
  try {
    const url = `/api/proxy/sessions/${sessionId}`

    console.log(`Fetching session with URL: ${url}`)

    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })

    const data = await response.json()

    if (!response.ok && !data.code) {
      throw new Error(`获取会话详情失败: ${response.status}, ${data.error || "Unknown error"}`)
    }

    return data
  } catch (error) {
    console.error("获取会话详情错误:", error)
    // 返回格式化的错误响应，确保与API响应格式一致
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Session, // 类型转换以满足返回类型
      timestamp: Date.now(),
    }
  }
}

// 更新会话
export async function updateSession(sessionId: string, params: UpdateSessionParams): Promise<ApiResponse<Session>> {
  try {
    const queryString = buildQueryString(params)
    const url = `/api/proxy/sessions/${sessionId}${queryString}`

    console.log(`Updating session with URL: ${url}`)

    const response = await fetch(url, {
      method: "PUT",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })

    const data = await response.json()

    if (!response.ok && !data.code) {
      throw new Error(`更新会话失败: ${response.status}, ${data.error || "Unknown error"}`)
    }

    return data
  } catch (error) {
    console.error("更新会话错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null as unknown as Session,
      timestamp: Date.now(),
    }
  }
}

// 删除会话
export async function deleteSession(sessionId: string): Promise<ApiResponse<null>> {
  try {
    const url = `/api/proxy/sessions/${sessionId}`

    console.log(`Deleting session with URL: ${url}`)

    const response = await fetch(url, {
      method: "DELETE",
      headers: {
        Accept: "*/*",
        "Content-Type": "application/json",
      },
    })

    const data = await response.json()

    if (!response.ok && !data.code) {
      throw new Error(`删除会话失败: ${response.status}, ${data.error || "Unknown error"}`)
    }

    return data
  } catch (error) {
    console.error("删除会话错误:", error)
    // 返回格式化的错误响应
    return {
      code: 500,
      message: error instanceof Error ? error.message : "未知错误",
      data: null,
      timestamp: Date.now(),
    }
  }
}

