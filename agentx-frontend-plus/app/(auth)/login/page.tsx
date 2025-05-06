"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import { Toaster } from "@/components/ui/toaster"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { loginApi } from "@/lib/api-services"
import { setCookie } from "@/lib/utils"

export default function LoginPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    account: "",
    password: ""
  })
  const [loading, setLoading] = useState(false)



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
              <Label htmlFor="account">账号</Label>
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
              <Label htmlFor="password">密码</Label>
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
            <div className="text-sm text-center text-muted-foreground">
              还没有账号？{" "}
              <Link href="/register" className="text-primary hover:underline">
                立即注册
              </Link>
            </div>
          </div>
        </form>
      </div>
      <Toaster />
    </>
  )
} 