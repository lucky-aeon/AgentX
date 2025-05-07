"use client"

import type React from "react"
import { useState, useEffect } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"
import { getCurrentUserId, getUserInfo, updateUserInfoWithToast, type UserUpdateRequest } from "@/lib/user-service"

export default function ProfilePage() {
  const [formData, setFormData] = useState({
    nickname: "",
    email: "",
    phone: "",
  })
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // 获取用户信息
  useEffect(() => {
    async function fetchUserInfo() {
      try {
        setLoading(true)
        setError(null)
        
        const response = await getUserInfo()
        
        if (response.code === 200 && response.data) {
          setFormData({
            nickname: response.data.nickname || "",
            email: response.data.email || "",
            phone: response.data.phone || "",
          })
        } else {
          setError(response.message)
          toast({
            title: "获取用户信息失败",
            description: response.message,
            variant: "destructive",
          })
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        setError(errorMessage)
        toast({
          title: "获取用户信息失败",
          description: errorMessage,
          variant: "destructive",
        })
      } finally {
        setLoading(false)
      }
    }

    fetchUserInfo()
  }, [])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!formData.nickname.trim()) {
      toast({
        title: "昵称不能为空",
        variant: "destructive",
      })
      return
    }
    
    try {
      setSubmitting(true)
      
      // 后端API只支持更新昵称
      const userData: UserUpdateRequest = {
        nickname: formData.nickname,
      }
      
      const response = await updateUserInfoWithToast(userData)
      
      if (response.code === 200) {
        // 更新成功，提示信息由withToast处理
      }
    } catch (error) {
      // 错误由withToast处理
      console.error("更新用户信息失败:", error)
    } finally {
      setSubmitting(false)
    }
  }

  // 渲染加载状态
  if (loading) {
    return (
      <div className="container py-6">
        <div className="mb-6">
          <Skeleton className="h-10 w-64 mb-2" />
          <Skeleton className="h-4 w-40" />
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32 mb-2" />
            <Skeleton className="h-4 w-56" />
          </CardHeader>
          <CardContent className="space-y-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-10 w-full" />
              </div>
            ))}
          </CardContent>
          <CardFooter>
            <Skeleton className="h-10 w-24" />
          </CardFooter>
        </Card>
      </div>
    )
  }

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">个人资料</h1>
        <p className="text-muted-foreground">更新您的个人信息</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>个人资料</CardTitle>
          <CardDescription>更新您的个人信息</CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="nickname">昵称</Label>
              <Input 
                id="nickname" 
                name="nickname" 
                value={formData.nickname} 
                onChange={handleChange}
                placeholder="请输入昵称" 
              />
              <p className="text-xs text-muted-foreground">昵称是您在平台上显示的名称</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone">手机号</Label>
              <Input 
                id="phone" 
                name="phone" 
                type="tel"
                value={formData.phone} 
                onChange={handleChange}
                placeholder="请输入手机号"
                disabled
              />
              <p className="text-xs text-muted-foreground">手机号不可修改</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">电子邮件</Label>
              <Input 
                id="email" 
                name="email" 
                type="email" 
                value={formData.email} 
                onChange={handleChange}
                placeholder="请输入电子邮件"
                disabled
              />
              <p className="text-xs text-muted-foreground">邮箱地址不可修改</p>
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit" disabled={submitting}>
              {submitting ? "保存中..." : "保存更改"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}

