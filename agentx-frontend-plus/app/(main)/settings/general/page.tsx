"use client"

import type React from "react"
import { useState, useEffect } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"
import { 
  getUserSettingsWithToast, 
  updateUserSettingsWithToast, 
  getAllModelsWithToast,
  type UserSettings,
  type UserSettingsConfig, 
  type UserSettingsUpdateRequest, 
  type Model 
} from "@/lib/user-settings-service"

export default function GeneralSettingsPage() {
  const [settings, setSettings] = useState<UserSettings>({
    settingConfig: {
      defaultModel: null
    }
  })
  const [models, setModels] = useState<Model[]>([])
  const [loading, setLoading] = useState(true)
  const [modelsLoading, setModelsLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  // 获取用户设置
  useEffect(() => {
    async function fetchUserSettings() {
      try {
        setLoading(true)
        const response = await getUserSettingsWithToast()
        
        if (response.code === 200 && response.data) {
          setSettings(response.data)
        }
      } catch (error) {
        console.error("获取用户设置失败:", error)
      } finally {
        setLoading(false)
      }
    }

    fetchUserSettings()
  }, [])

  // 获取模型列表
  useEffect(() => {
    async function fetchModels() {
      try {
        setModelsLoading(true)
        const response = await getAllModelsWithToast()
        
        if (response.code === 200 && response.data) {
          // 只显示激活的聊天模型
          const activeModels = response.data.filter((model: Model) => 
            model.status && model.type === 'CHAT'
          )
          setModels(activeModels)
        }
      } catch (error) {
        console.error("获取模型列表失败:", error)
      } finally {
        setModelsLoading(false)
      }
    }

    fetchModels()
  }, [])

  const handleDefaultModelChange = (modelId: string) => {
    setSettings(prev => ({
      ...prev,
      settingConfig: {
        ...prev.settingConfig,
        defaultModel: modelId
      }
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!settings.settingConfig.defaultModel) {
      toast({
        title: "请选择默认模型",
        variant: "destructive",
      })
      return
    }
    
    try {
      setSubmitting(true)
      
      const updateData: UserSettingsUpdateRequest = {
        settingConfig: {
          defaultModel: settings.settingConfig.defaultModel
        }
      }
      
      const response = await updateUserSettingsWithToast(updateData)
      
      if (response.code === 200) {
        // 更新成功，提示信息由withToast处理
        if (response.data) {
          setSettings(response.data)
        }
      }
    } catch (error) {
      console.error("更新用户设置失败:", error)
    } finally {
      setSubmitting(false)
    }
  }

  // 渲染加载状态
  if (loading || modelsLoading) {
    return (
      <div className="container max-w-2xl py-6">
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
            <div className="space-y-2">
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-3 w-48" />
            </div>
          </CardContent>
          <CardFooter>
            <Skeleton className="h-10 w-24" />
          </CardFooter>
        </Card>
      </div>
    )
  }

  return (
    <div className="container max-w-2xl py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">通用设置</h1>
        <p className="text-muted-foreground">配置您的默认选项和偏好</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>默认模型设置</CardTitle>
          <CardDescription>设置系统在创建Agent和生成内容时使用的默认模型</CardDescription>
        </CardHeader>
        <form onSubmit={handleSubmit}>
          <CardContent className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="defaultModel">默认模型</Label>
              <Select
                value={settings.settingConfig.defaultModel || ""}
                onValueChange={handleDefaultModelChange}
              >
                <SelectTrigger className="w-full max-w-md">
                  <SelectValue placeholder="请选择默认模型" />
                </SelectTrigger>
                <SelectContent className="max-w-md">
                  {models.map((model) => (
                    <SelectItem key={model.id} value={model.id} className="cursor-pointer">
                      <div className="flex items-center justify-between w-full">
                        <span className="font-medium text-sm">{model.name}</span>
                        {model.isOfficial && (
                          <Badge variant="secondary" className="ml-2 text-xs">
                            官方
                          </Badge>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                此模型将用于生成Agent系统提示词、代码补全等功能
              </p>
            </div>
          </CardContent>
          <CardFooter>
            <Button type="submit" disabled={submitting || !settings.settingConfig.defaultModel}>
              {submitting ? "保存中..." : "保存设置"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  )
} 