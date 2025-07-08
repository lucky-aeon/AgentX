"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import Image from "next/image"
import { useRouter } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import { Toaster } from "@/components/ui/toaster"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { loginApi, getSsoLoginUrlApi } from "@/lib/api-services"
import { setCookie } from "@/lib/utils"
import { getAuthConfigWithToast } from "@/lib/auth-config-service"
import type { AuthConfig } from "@/lib/types/auth-config"
import { AUTH_FEATURE_KEY } from "@/lib/types/auth-config"

// GitHub 图标组件
const GitHubIcon = ({ className }: { className?: string }) => (
  <svg 
    className={className} 
    xmlns="http://www.w3.org/2000/svg" 
    width="24" 
    height="24" 
    viewBox="0 0 24 24" 
    fill="none" 
    stroke="currentColor" 
    strokeWidth="2" 
    strokeLinecap="round" 
    strokeLinejoin="round"
  >
    <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
  </svg>
)

// 敲鸭 Logo 组件
const QiaoyaLogo = ({ className }: { className?: string }) => (
  <Image 
    src="/logo.jpg" 
    alt="敲鸭 Logo" 
    width={20} 
    height={20}
    className={`${className} rounded`}
  />
)

export default function LoginPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    account: "",
    password: ""
  })
  const [loading, setLoading] = useState(false)
  const [githubLoading, setGithubLoading] = useState(false)
  const [qiaoyaLoading, setQiaoyaLoading] = useState(false)
  const [authConfig, setAuthConfig] = useState<AuthConfig | null>(null)
  const [configLoading, setConfigLoading] = useState(true)

  // 加载认证配置
  useEffect(() => {
    async function fetchAuthConfig() {
      try {
        const response = await getAuthConfigWithToast()
        if (response.code === 200) {
          setAuthConfig(response.data)
        }
      } catch (error) {
        console.error("获取认证配置失败:", error)
      } finally {
        setConfigLoading(false)
      }
    }

    fetchAuthConfig()
  }, [])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const { account, password } = formData
      if (!account || !password) {
        toast({
          variant: "destructive",
          title: "错误",
          description: "请输入账号和密码"
        })
        setLoading(false)
        return
      }
      
      // 使用带toast参数的API
      const res = await loginApi({ account, password }, true)
      if (res.code === 200 && res.data?.token) {
        localStorage.setItem("auth_token", res.data.token)
        setCookie("token", res.data.token, 30)
        router.push("/")
      }
    } catch (error: any) {
      // 错误已由API处理
      console.error("登录失败:", error)
    } finally {
      setLoading(false)
    }
  }

  const handleGitHubLogin = async () => {
    try {
      setGithubLoading(true)
      const res = await getSsoLoginUrlApi('github')
      if (res.code === 200 && res.data?.loginUrl) {
        window.location.href = res.data.loginUrl
      } else {
        toast({
          variant: "destructive",
          title: "错误",
          description: "获取GitHub授权链接失败"
        })
      }
    } catch (error) {
      console.error("GitHub登录失败:", error)
      toast({
        variant: "destructive",
        title: "错误",
        description: "GitHub登录失败，请稍后再试"
      })
    } finally {
      setGithubLoading(false)
    }
  }

  const handleQiaoyaLogin = async () => {
    try {
      setQiaoyaLoading(true)
      const res = await getSsoLoginUrlApi('community')
      if (res.code === 200 && res.data?.loginUrl) {
        window.location.href = res.data.loginUrl
      } else {
        toast({
          variant: "destructive",
          title: "错误",
          description: "获取敲鸭授权链接失败"
        })
      }
    } catch (error) {
      console.error("敲鸭登录失败:", error)
      toast({
        variant: "destructive",
        title: "错误",
        description: "敲鸭登录失败，请稍后再试"
      })
    } finally {
      setQiaoyaLoading(false)
    }
  }

  // 配置加载中
  if (configLoading) {
    return (
      <div className="container max-w-[400px] py-10 h-screen flex flex-col justify-center">
        <div className="mb-8 space-y-2 text-center">
          <div className="h-8 bg-gray-200 rounded animate-pulse"></div>
          <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
        </div>
        <div className="space-y-4">
          <div className="h-20 bg-gray-200 rounded animate-pulse"></div>
          <div className="h-20 bg-gray-200 rounded animate-pulse"></div>
          <div className="h-10 bg-gray-200 rounded animate-pulse"></div>
        </div>
      </div>
    )
  }

  // 检查是否有可用的登录方式
  const availableLoginMethods = authConfig?.loginMethods || {}
  const hasNormalLogin = availableLoginMethods[AUTH_FEATURE_KEY.NORMAL_LOGIN]?.enabled
  const hasGitHubLogin = availableLoginMethods[AUTH_FEATURE_KEY.GITHUB_LOGIN]?.enabled
  const hasCommunityLogin = availableLoginMethods[AUTH_FEATURE_KEY.COMMUNITY_LOGIN]?.enabled
  const hasSsoLogin = hasGitHubLogin || hasCommunityLogin

  // 如果没有可用的登录方式
  if (!hasNormalLogin && !hasSsoLogin) {
    return (
      <div className="container max-w-[400px] py-10 h-screen flex flex-col justify-center">
        <div className="text-center space-y-4">
          <h1 className="text-2xl font-semibold tracking-tight">暂时无法登录</h1>
          <p className="text-sm text-muted-foreground">
            系统暂时关闭了所有登录方式，请稍后再试或联系管理员。
          </p>
        </div>
      </div>
    )
  }

  return (
    <>
      <div className="container max-w-[400px] py-10 h-screen flex flex-col justify-center">
        <div className="mb-8 space-y-2 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">登录</h1>
          <p className="text-sm text-muted-foreground">欢迎回来！请输入您的账号信息。</p>
        </div>

        <div className="space-y-4">
          {/* 普通登录表单 */}
          {hasNormalLogin && (
            <form onSubmit={handleSubmit}>
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="account">
                    账号 <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="account"
                    name="account"
                    type="text"
                    placeholder="请输入账号/邮箱/手机号"
                    value={formData.account}
                    onChange={handleChange}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">
                    密码 <span className="text-red-500">*</span>
                  </Label>
                  <Input
                    id="password"
                    name="password"
                    type="password"
                    placeholder="请输入密码"
                    value={formData.password}
                    onChange={handleChange}
                    required
                  />
                </div>
                <Button type="submit" className="w-full bg-primary text-primary-foreground hover:bg-primary/90" disabled={loading}>
                  {loading ? "登录中..." : "登录"}
                </Button>
              </div>
            </form>
          )}

          {/* SSO登录分隔线 */}
          {hasNormalLogin && hasSsoLogin && (
            <div className="relative my-4">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-background text-muted-foreground">或者</span>
              </div>
            </div>
          )}

          {/* SSO登录按钮 */}
          {hasSsoLogin && (
            <div className="space-y-2">
              {/* 敲鸭登录 */}
              {hasCommunityLogin && (
                <Button 
                  type="button" 
                  variant="outline" 
                  className="w-full flex items-center justify-center gap-2"
                  onClick={handleQiaoyaLogin}
                  disabled={qiaoyaLoading}
                >
                  {qiaoyaLoading ? (
                    <>正在跳转到敲鸭...</>
                  ) : (
                    <>
                      <QiaoyaLogo className="h-5 w-5" />
                      <span>{availableLoginMethods[AUTH_FEATURE_KEY.COMMUNITY_LOGIN]?.name || "使用敲鸭登录"}</span>
                    </>
                  )}
                </Button>
              )}

              {/* GitHub登录 */}
              {hasGitHubLogin && (
                <Button 
                  type="button" 
                  variant="outline" 
                  className="w-full flex items-center justify-center gap-2"
                  onClick={handleGitHubLogin}
                  disabled={githubLoading}
                >
                  {githubLoading ? (
                    <>正在跳转到 GitHub...</>
                  ) : (
                    <>
                      <GitHubIcon className="h-5 w-5" />
                      <span>{availableLoginMethods[AUTH_FEATURE_KEY.GITHUB_LOGIN]?.name || "使用 GitHub 登录"}</span>
                    </>
                  )}
                </Button>
              )}
            </div>
          )}

          {/* 底部链接 */}
          <div className="flex justify-between text-sm text-muted-foreground mb-2 mt-2">
            <div>
              {authConfig?.registerEnabled && (
                <>
                  还没有账号？{" "}
                  <Link href="/register" className="text-primary hover:underline">
                    立即注册
                  </Link>
                </>
              )}
            </div>
            {/* 只有普通登录启用时才显示忘记密码 */}
            {hasNormalLogin && (
              <div>
                <Link href="/reset-password" className="text-primary hover:underline">
                  忘记密码
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
      <Toaster />
    </>
  )
} 