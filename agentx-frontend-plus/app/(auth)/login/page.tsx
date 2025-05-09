"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import { Toaster } from "@/components/ui/toaster"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { loginApi, getGithubAuthorizeUrlApi } from "@/lib/api-services"
import { setCookie } from "@/lib/utils"

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

export default function LoginPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    account: "",
    password: ""
  })
  const [loading, setLoading] = useState(false)
  const [githubLoading, setGithubLoading] = useState(false)

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
      const res = await getGithubAuthorizeUrlApi()
      if (res.code === 200 && res.data?.authorizeUrl) {
        window.location.href = res.data.authorizeUrl
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

  return (
    <>
      <div className="container max-w-[400px] py-10 h-screen flex flex-col justify-center">
        <div className="mb-8 space-y-2 text-center">
          <h1 className="text-2xl font-semibold tracking-tight">登录</h1>
          <p className="text-sm text-muted-foreground">欢迎回来！请输入您的账号信息。</p>
        </div>
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
            <div className="relative my-4">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-background text-muted-foreground">或者</span>
              </div>
            </div>
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
                  <span>使用 GitHub 登录</span>
                </>
              )}
            </Button>
            <div className="flex justify-between text-sm text-muted-foreground mb-2 mt-2">
              <div>
                还没有账号？{" "}
                <Link href="/register" className="text-primary hover:underline">
                  立即注册
                </Link>
              </div>
              <div>
                <Link href="/reset-password" className="text-primary hover:underline">
                  忘记密码
                </Link>
              </div>
            </div>
          </div>
        </form>
      </div>
      <Toaster />
    </>
  )
} 