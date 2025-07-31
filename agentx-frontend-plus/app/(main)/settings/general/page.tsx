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
  getChatModelsWithToast,
  getOcrModelsWithToast,
  getEmbeddingModelsWithToast,
  type UserSettings,
  type UserSettingsConfig, 
  type UserSettingsUpdateRequest, 
  type Model,
  type FallbackConfig 
} from "@/lib/user-settings-service"
import { FallbackConfigComponent } from "@/components/settings/fallback-config"

export default function GeneralSettingsPage() {
  const [settings, setSettings] = useState<UserSettings>({
    settingConfig: {
      defaultModel: null,
      defaultOcrModel: null,
      defaultEmbeddingModel: null,
      fallbackConfig: {
        enabled: false,
        fallbackChain: []
      }
    }
  })
  const [models, setModels] = useState<Model[]>([])
  const [ocrModels, setOcrModels] = useState<Model[]>([])
  const [embeddingModels, setEmbeddingModels] = useState<Model[]>([])
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
          setSettings({
            ...response.data,
            settingConfig: {
              ...response.data.settingConfig,
              defaultOcrModel: response.data.settingConfig.defaultOcrModel || null,
              defaultEmbeddingModel: response.data.settingConfig.defaultEmbeddingModel || null,
              fallbackConfig: response.data.settingConfig.fallbackConfig || {
                enabled: false,
                fallbackChain: []
              }
            }
          })
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
        
        // 并行获取所有类型的模型
        const [chatResponse, ocrResponse, embeddingResponse] = await Promise.all([
          getChatModelsWithToast(),
          getOcrModelsWithToast(),
          getEmbeddingModelsWithToast()
        ])
        
        if (chatResponse.code === 200 && chatResponse.data) {
          const activeModels = chatResponse.data.filter((model: Model) => model.status)
          setModels(activeModels)
        }
        
        if (ocrResponse.code === 200 && ocrResponse.data) {
          const activeOcrModels = ocrResponse.data.filter((model: Model) => model.status)
          setOcrModels(activeOcrModels)
        }
        
        if (embeddingResponse.code === 200 && embeddingResponse.data) {
          const activeEmbeddingModels = embeddingResponse.data.filter((model: Model) => model.status)
          setEmbeddingModels(activeEmbeddingModels)
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

  const handleDefaultOcrModelChange = (modelId: string) => {
    setSettings(prev => ({
      ...prev,
      settingConfig: {
        ...prev.settingConfig,
        defaultOcrModel: modelId
      }
    }))
  }

  const handleDefaultEmbeddingModelChange = (modelId: string) => {
    setSettings(prev => ({
      ...prev,
      settingConfig: {
        ...prev.settingConfig,
        defaultEmbeddingModel: modelId
      }
    }))
  }

  const handleFallbackConfigChange = (fallbackConfig: FallbackConfig) => {
    setSettings(prev => ({
      ...prev,
      settingConfig: {
        ...prev.settingConfig,
        fallbackConfig
      }
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      setSubmitting(true)
      
      const updateData: UserSettingsUpdateRequest = {
        settingConfig: {
          defaultModel: settings.settingConfig.defaultModel,
          defaultOcrModel: settings.settingConfig.defaultOcrModel,
          defaultEmbeddingModel: settings.settingConfig.defaultEmbeddingModel,
          fallbackConfig: settings.settingConfig.fallbackConfig
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

  if (loading) {
    return (
      <div className="container mx-auto py-6 space-y-6">
        <div>
          <h1 className="text-3xl font-bold">通用设置</h1>
          <p className="text-muted-foreground">管理您的账户偏好设置</p>
        </div>
        
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-64" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">通用设置</h1>
        <p className="text-muted-foreground">管理您的账户偏好设置</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* 默认模型设置 */}
        <Card>
          <CardHeader>
            <CardTitle>默认模型</CardTitle>
            <CardDescription>
              选择您的默认AI模型，这将作为新对话的默认选择
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="default-model">默认模型</Label>
              {modelsLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Select 
                  value={settings.settingConfig.defaultModel || ""} 
                  onValueChange={handleDefaultModelChange}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择默认模型" />
                  </SelectTrigger>
                  <SelectContent>
                    {models.map((model) => (
                      <SelectItem key={model.id} value={model.id}>
                        <div className="flex items-center gap-2">
                          <span>{model.name}</span>
                          {model.providerName && (
                            <Badge variant="secondary" className="text-xs">
                              {model.providerName}
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          </CardContent>
        </Card>

        {/* 默认OCR模型设置 */}
        <Card>
          <CardHeader>
            <CardTitle>默认OCR模型</CardTitle>
            <CardDescription>
              选择用于文档OCR识别的默认模型，用于RAG系统的文档处理
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="default-ocr-model">默认OCR模型</Label>
              {modelsLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Select 
                  value={settings.settingConfig.defaultOcrModel || ""} 
                  onValueChange={handleDefaultOcrModelChange}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择默认OCR模型" />
                  </SelectTrigger>
                  <SelectContent>
                    {ocrModels.map((model) => (
                      <SelectItem key={model.id} value={model.id}>
                        <div className="flex items-center gap-2">
                          <span>{model.name}</span>
                          {model.providerName && (
                            <Badge variant="secondary" className="text-xs">
                              {model.providerName}
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          </CardContent>
        </Card>

        {/* 默认嵌入模型设置 */}
        <Card>
          <CardHeader>
            <CardTitle>默认嵌入模型</CardTitle>
            <CardDescription>
              选择用于向量化的默认嵌入模型，用于RAG系统的语义搜索
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="default-embedding-model">默认嵌入模型</Label>
              {modelsLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Select 
                  value={settings.settingConfig.defaultEmbeddingModel || ""} 
                  onValueChange={handleDefaultEmbeddingModelChange}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择默认嵌入模型" />
                  </SelectTrigger>
                  <SelectContent>
                    {embeddingModels.map((model) => (
                      <SelectItem key={model.id} value={model.id}>
                        <div className="flex items-center gap-2">
                          <span>{model.name}</span>
                          {model.providerName && (
                            <Badge variant="secondary" className="text-xs">
                              {model.providerName}
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            </div>
          </CardContent>
        </Card>

        {/* 降级配置 */}
        {!modelsLoading && (
          <FallbackConfigComponent
            fallbackConfig={settings.settingConfig.fallbackConfig || { enabled: false, fallbackChain: [] }}
            models={models}
            onConfigChange={handleFallbackConfigChange}
          />
        )}

        {/* 保存按钮 */}
        <Card>
          <CardFooter className="pt-6">
            <Button 
              type="submit" 
              disabled={submitting}
              className="w-full"
            >
              {submitting ? "保存中..." : "保存设置"}
            </Button>
          </CardFooter>
        </Card>
      </form>
    </div>
  )
} 