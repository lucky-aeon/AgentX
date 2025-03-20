"use client"

import type React from "react"

import { useState } from "react"
import { User } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

export default function ProfilePage() {
  const [formData, setFormData] = useState({
    name: "张三",
    email: "zhangsan@example.com",
    username: "zhangsan",
    bio: "AI 爱好者，喜欢探索新技术。",
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // 模拟保存
    console.log("保存个人资料:", formData)
    // 这里可以添加保存成功的提示
  }

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">个人设置</h1>
        <p className="text-muted-foreground">管理您的个人信息和账户设置</p>
      </div>

      <Tabs defaultValue="profile" className="space-y-6">
        <TabsList>
          <TabsTrigger value="profile">个人资料</TabsTrigger>
          <TabsTrigger value="account">账户设置</TabsTrigger>
          <TabsTrigger value="appearance">外观</TabsTrigger>
        </TabsList>

        <TabsContent value="profile">
          <Card>
            <CardHeader>
              <CardTitle>个人资料</CardTitle>
              <CardDescription>更新您的个人信息</CardDescription>
            </CardHeader>
            <form onSubmit={handleSubmit}>
              <CardContent className="space-y-6">
                <div className="flex items-center gap-4">
                  <Avatar className="h-20 w-20">
                    <AvatarImage src="/placeholder.svg?height=80&width=80" alt="用户头像" />
                    <AvatarFallback className="text-2xl">
                      <User className="h-8 w-8" />
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <Button variant="outline" size="sm">
                      更改头像
                    </Button>
                    <p className="text-xs text-muted-foreground mt-1">支持 JPG, PNG 或 GIF，最大 2MB</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">姓名</Label>
                    <Input id="name" name="name" value={formData.name} onChange={handleChange} />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="username">用户名</Label>
                    <Input id="username" name="username" value={formData.username} onChange={handleChange} />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email">电子邮件</Label>
                  <Input id="email" name="email" type="email" value={formData.email} onChange={handleChange} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="bio">个人简介</Label>
                  <Textarea id="bio" name="bio" value={formData.bio} onChange={handleChange} rows={4} />
                </div>
              </CardContent>
              <CardFooter>
                <Button type="submit">保存更改</Button>
              </CardFooter>
            </form>
          </Card>
        </TabsContent>

        <TabsContent value="account">
          <Card>
            <CardHeader>
              <CardTitle>账户设置</CardTitle>
              <CardDescription>管理您的账户安全和通知设置</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h3 className="text-lg font-medium">密码</h3>
                <p className="text-sm text-muted-foreground">上次更改密码时间: 2024-01-15</p>
                <Button variant="outline">更改密码</Button>
              </div>

              <div className="space-y-2">
                <h3 className="text-lg font-medium">双因素认证</h3>
                <p className="text-sm text-muted-foreground">增强您账户的安全性</p>
                <Button variant="outline">设置双因素认证</Button>
              </div>

              <div className="space-y-2">
                <h3 className="text-lg font-medium">通知设置</h3>
                <p className="text-sm text-muted-foreground">管理您接收的通知类型</p>
                <Button variant="outline">管理通知</Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="appearance">
          <Card>
            <CardHeader>
              <CardTitle>外观设置</CardTitle>
              <CardDescription>自定义界面外观</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <h3 className="text-lg font-medium">主题</h3>
                <div className="grid grid-cols-3 gap-4">
                  <Button variant="outline" className="justify-start">
                    浅色模式
                  </Button>
                  <Button variant="outline" className="justify-start">
                    深色模式
                  </Button>
                  <Button variant="default" className="justify-start">
                    跟随系统
                  </Button>
                </div>
              </div>

              <div className="space-y-2">
                <h3 className="text-lg font-medium">字体大小</h3>
                <div className="grid grid-cols-3 gap-4">
                  <Button variant="outline" className="justify-start">
                    小
                  </Button>
                  <Button variant="default" className="justify-start">
                    中
                  </Button>
                  <Button variant="outline" className="justify-start">
                    大
                  </Button>
                </div>
              </div>
            </CardContent>
            <CardFooter>
              <Button>保存设置</Button>
            </CardFooter>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}

