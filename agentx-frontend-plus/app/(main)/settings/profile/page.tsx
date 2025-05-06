"use client"

import type React from "react"
import { useState } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function ProfilePage() {
  const [formData, setFormData] = useState({
    nickname: "张三",
    email: "zhangsan@example.com",
    phone: "",
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // TODO: 调用更新用户信息的API
    console.log("保存个人资料:", formData)
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
              />
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
              />
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit">保存更改</Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
}

