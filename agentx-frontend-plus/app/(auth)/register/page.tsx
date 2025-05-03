"use client"

import { useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { toast } from "@/hooks/use-toast"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { registerApi } from "@/lib/api-services"

export default function RegisterPage() {
  const router = useRouter()
  const [formData, setFormData] = useState({
    email: "",
    phone: "",
    password: "",
    confirmPassword: "",
  })
  const [loading, setLoading] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const validateForm = () => {
    // 验证密码
    if (!formData.password) {
      toast({
        variant: "destructive",
        title: "错误",
        description: "请输入密码"
      })
      return false
    }
    if (formData.password !== formData.confirmPassword) {
      toast({
        variant: "destructive",
        title: "错误",
        description: "两次输入的密码不一致"
      })
      return false
    }
    
    // 验证邮箱和手机号至少填一个
    if (!formData.email && !formData.phone) {
      toast({
        variant: "destructive",
        title: "错误",
        description: "邮箱和手机号至少填写一个"
      })
      return false
    }

    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!validateForm()) {
      return
    }

    setLoading(true)
    try {
      const { email, phone, password } = formData
      const res = await registerApi({ 
        email: email || undefined, 
        phone: phone || undefined, 
        password 
      }, true)
      
      if (res.code === 200) {
        router.push("/login")
      }
    } catch (error: any) {
      // 错误已由API处理
      console.error("注册失败:", error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container max-w-[400px] py-10 h-screen flex flex-col justify-center">
      <div className="mb-8 space-y-2 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">注册</h1>
        <p className="text-sm text-muted-foreground">创建您的新账号</p>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="email">电子邮件</Label>
            <Input
              id="email"
              name="email"
              type="email"
              placeholder="请输入电子邮件"
              value={formData.email}
              onChange={handleChange}
            />
            <p className="text-xs text-muted-foreground">邮箱和手机号至少填写一个</p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="phone">手机号</Label>
            <Input
              id="phone"
              name="phone"
              type="tel"
              placeholder="请输入手机号"
              value={formData.phone}
              onChange={handleChange}
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

          <div className="space-y-2">
            <Label htmlFor="confirmPassword">确认密码</Label>
            <Input
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
            />
          </div>

          <Button type="submit" className="w-full bg-primary text-primary-foreground hover:bg-primary/90" disabled={loading}>
            {loading ? "注册中..." : "注册"}
          </Button>
          <div className="text-sm text-center text-muted-foreground">
            已有账号？{" "}
            <Link href="/login" className="text-primary hover:underline">
              立即登录
            </Link>
          </div>
        </div>
      </form>
    </div>
  )
} 