import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"

// 不需要登录就可以访问的路由
const publicRoutes = ["/login", "/register"]

export function middleware(request: NextRequest) {
  const token = request.cookies.get("token")?.value
  const { pathname } = request.nextUrl

  // 如果是登录/注册页面且已登录，重定向到首页
  if (publicRoutes.includes(pathname) && token) {
    return NextResponse.redirect(new URL("/", request.url))
  }

  return NextResponse.next()
}

// 只处理登录和注册页面
export const config = {
  matcher: ["/login", "/register"]
} 