"use client"

import { useEffect, useState } from "react"
import { MoreHorizontal, Plus, Edit, Trash, Power, PowerOff, Loader2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Badge } from "@/components/ui/badge"
import { getProviders } from "@/lib/api-services"

// 服务提供商接口
interface Model {
  id: string
  userId: string
  providerId: string
  providerName: string | null
  modelId: string
  name: string
  description: string
  type: string
  config: any
  isOfficial: boolean | null
  status: boolean
  createdAt: string
  updatedAt: string
}

interface Provider {
  id: string
  protocol: string
  name: string
  description?: string
  config: any
  isOfficial: boolean
  status: boolean
  createdAt: string
  updatedAt: string
  models: Model[]
}

export default function ProvidersPage() {
  const [activeTab, setActiveTab] = useState("全部")
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null)
  const [showDetailDialog, setShowDetailDialog] = useState(false)
  const [providers, setProviders] = useState<Provider[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  // 加载服务提供商数据
  useEffect(() => {
    async function loadProviders() {
      setLoading(true)
      try {
        const response = await getProviders()
        if (response.code === 200) {
          console.log("服务提供商数据:", response.data)
          setProviders(response.data)
        } else {
          setError(response.message || "获取服务提供商列表失败")
        }
      } catch (err) {
        console.error("获取服务提供商错误:", err)
        setError("获取服务提供商数据失败")
      } finally {
        setLoading(false)
      }
    }
    
    loadProviders()
  }, [])
  
  // 根据标签筛选服务提供商
  const filteredProviders = activeTab === "全部"
    ? providers
    : activeTab === "官方服务"
      ? providers.filter(p => p.isOfficial)
      : providers.filter(p => !p.isOfficial)

  // 打开详情弹窗
  const openDetail = (provider: Provider) => {
    setSelectedProvider(provider)
    setShowDetailDialog(true)
  }
  
  // 关闭详情弹窗
  const closeDetail = () => {
    setShowDetailDialog(false)
    setSelectedProvider(null)
  }
  
  // 显示加载中状态
  if (loading) {
    return (
      <div className="container py-6 flex flex-col items-center justify-center min-h-[400px]">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="mt-4 text-muted-foreground">加载服务提供商...</p>
      </div>
    )
  }
  
  // 显示错误状态
  if (error) {
    return (
      <div className="container py-6">
        <div className="bg-red-50 border border-red-200 rounded-md p-4 mb-6">
          <h3 className="text-red-800 font-medium">加载失败</h3>
          <p className="text-red-600">{error}</p>
          <Button 
            variant="outline" 
            className="mt-2" 
            onClick={() => window.location.reload()}
          >
            重试
          </Button>
        </div>
      </div>
    )
  }
  
  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">服务提供商</h1>
          <p className="text-muted-foreground">管理您的AI服务提供商和API密钥</p>
        </div>
        <Button className="flex items-center gap-2">
          <Plus className="h-4 w-4" />
          添加服务提供商
        </Button>
      </div>
      
      <Tabs defaultValue="全部" className="space-y-6" value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="全部">全部</TabsTrigger>
          <TabsTrigger value="官方服务">官方服务</TabsTrigger>
          <TabsTrigger value="自定义服务">自定义服务</TabsTrigger>
        </TabsList>
        
        <TabsContent value={activeTab} className="space-y-6">
          {filteredProviders.length === 0 ? (
            <div className="text-center py-10 border rounded-md bg-gray-50">
              <p className="text-muted-foreground">暂无服务提供商数据</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredProviders.map((provider) => (
                <Card 
                  key={provider.id} 
                  className="overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
                  onClick={() => openDetail(provider)}
                >
                  <CardHeader className="pb-2 relative">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="flex h-10 w-10 items-center justify-center rounded-md bg-blue-100 text-blue-600">
                          {provider.protocol.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <CardTitle className="text-base">{provider.name}</CardTitle>
                          <CardDescription className="text-xs">
                            {provider.protocol}
                            {provider.isOfficial && (
                              <Badge variant="outline" className="ml-2 text-[10px]">
                                官方
                              </Badge>
                            )}
                          </CardDescription>
                        </div>
                      </div>
                      {!provider.isOfficial && (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button variant="ghost" size="icon" className="absolute top-2 right-2">
                              <MoreHorizontal className="h-4 w-4" />
                              <span className="sr-only">打开菜单</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={(e) => e.stopPropagation()}>
                              <Edit className="mr-2 h-4 w-4" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => e.stopPropagation()}>
                              <Trash className="mr-2 h-4 w-4" />
                              删除
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => e.stopPropagation()}>
                              {provider.status ? (
                                <>
                                  <PowerOff className="mr-2 h-4 w-4" />
                                  禁用
                                </>
                              ) : (
                                <>
                                  <Power className="mr-2 h-4 w-4" />
                                  启用
                                </>
                              )}
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </div>
                  </CardHeader>
                  <CardContent>
                    <p className="text-sm text-muted-foreground mb-3">
                      {provider.description || "无描述"}
                    </p>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {provider.status ? (
                        <Badge variant="outline" className="bg-green-50 text-green-600 border-green-200">
                          已启用
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="bg-red-50 text-red-600 border-red-200">
                          已禁用
                        </Badge>
                      )}
                      {provider.models && provider.models.length > 0 && (
                        <div className="w-full mt-2">
                          <p className="text-xs text-muted-foreground mb-1">可用模型:</p>
                          <div className="flex flex-wrap gap-1">
                            {provider.models.slice(0, 3).map((model, index) => (
                              <Badge key={index} variant="outline" className="bg-blue-50 text-blue-600 border-blue-200">
                                {model.name}
                              </Badge>
                            ))}
                            {provider.models.length > 3 && (
                              <Badge variant="outline" className="bg-blue-50 text-blue-600 border-blue-200">
                                +{provider.models.length - 3}
                              </Badge>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
      
      {/* 服务提供商详情弹窗 */}
      {selectedProvider && (
        <Dialog open={showDetailDialog} onOpenChange={setShowDetailDialog}>
          <DialogContent className="max-w-3xl">
            <DialogHeader>
              <DialogTitle>服务提供商详情</DialogTitle>
              <DialogDescription>
                查看服务提供商的详细配置和可用模型
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-6 py-4">
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-md bg-blue-100 text-blue-600">
                  {selectedProvider.protocol.charAt(0).toUpperCase()}
                </div>
                <div>
                  <h3 className="text-xl font-semibold">{selectedProvider.name}</h3>
                  <div className="flex items-center gap-2 mt-1">
                    <span className="text-sm text-muted-foreground">{selectedProvider.protocol}</span>
                    {selectedProvider.isOfficial && (
                      <Badge variant="outline">官方</Badge>
                    )}
                    {selectedProvider.status ? (
                      <Badge variant="outline" className="bg-green-50 text-green-600 border-green-200">
                        已启用
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="bg-red-50 text-red-600 border-red-200">
                        已禁用
                      </Badge>
                    )}
                  </div>
                </div>
                
                {!selectedProvider.isOfficial && (
                  <div className="ml-auto">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="h-4 w-4" />
                          <span className="sr-only">打开菜单</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem>
                          <Edit className="mr-2 h-4 w-4" />
                          编辑
                        </DropdownMenuItem>
                        <DropdownMenuItem>
                          <Trash className="mr-2 h-4 w-4" />
                          删除
                        </DropdownMenuItem>
                        <DropdownMenuItem>
                          {selectedProvider.status ? (
                            <>
                              <PowerOff className="mr-2 h-4 w-4" />
                              禁用
                            </>
                          ) : (
                            <>
                              <Power className="mr-2 h-4 w-4" />
                              启用
                            </>
                          )}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                )}
              </div>
              
              <div className="space-y-2">
                <h4 className="font-medium">描述</h4>
                <p className="text-sm text-muted-foreground">
                  {selectedProvider.description || "无描述"}
                </p>
              </div>
              
              <div className="space-y-2">
                <h4 className="font-medium">可用模型</h4>
                {selectedProvider.models && selectedProvider.models.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                    {selectedProvider.models.map((model, index) => (
                      <div key={index} className="p-3 bg-gray-50 rounded-md text-sm flex items-center gap-2">
                        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-100 text-blue-600 text-xs">
                          {model.name.charAt(0).toUpperCase()}
                        </div>
                        {model.name}
                        {model.type && <span className="text-xs text-muted-foreground ml-auto">{model.type}</span>}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">暂无可用模型</p>
                )}
              </div>
              
              {!selectedProvider.isOfficial && (
                <div className="space-y-2">
                  <h4 className="font-medium">API配置</h4>
                  <div className="p-3 bg-gray-50 rounded-md">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium">API Key:</span>
                      <span className="text-sm text-muted-foreground">******************************</span>
                    </div>
                  </div>
                </div>
              )}
              
              <div className="space-y-2">
                <h4 className="font-medium">创建时间</h4>
                <p className="text-sm text-muted-foreground">
                  {new Date(selectedProvider.createdAt).toLocaleString("zh-CN")}
                </p>
              </div>
            </div>
            
            <DialogFooter>
              <Button variant="outline" onClick={closeDetail}>
                关闭
              </Button>
              {!selectedProvider.isOfficial && (
                <Button variant="default">
                  编辑配置
                </Button>
              )}
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
} 