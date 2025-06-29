"use client"

import { useEffect, useState } from "react"
import { MoreHorizontal, Plus, Edit, Trash, Power, PowerOff, Loader2, RefreshCw, PlusCircle, Settings2 } from "lucide-react"
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
import { 
  getProviders, 
  getProviderDetail, 
  deleteProviderWithToast, 
  toggleProviderStatusWithToast,
  deleteModelWithToast,
  toggleModelStatusWithToast
} from "@/lib/api-services"
import { ProviderDialog } from "@/components/provider-dialog"
import { ModelDialog } from "@/components/model-dialog"
import { toast } from "@/hooks/use-toast"
import { Separator } from "@/components/ui/separator"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Switch } from "@/components/ui/switch"
import { Metadata } from "next"
import { redirect } from "next/navigation"
import Image from "next/image"

// 服务商接口
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
  const [detailLoading, setDetailLoading] = useState(false)
  const [showProviderDialog, setShowProviderDialog] = useState(false)
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null)
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState(false)
  
  // 模型管理相关状态
  const [selectedModel, setSelectedModel] = useState<Model | null>(null)
  const [showModelDialog, setShowModelDialog] = useState(false)
  const [editingModel, setEditingModel] = useState<Model | null>(null)
  const [deleteModelConfirmOpen, setDeleteModelConfirmOpen] = useState(false)
  const [isDeletingModel, setIsDeletingModel] = useState(false)
  const [isTogglingModelStatus, setIsTogglingModelStatus] = useState(false)
  
  // 加载服务商数据
  const loadProviders = async () => {
    setLoading(true)
    try {
      let type: string | undefined;
      if (activeTab === "官方服务") {
        type = "official";
      } else if (activeTab === "自定义服务") {
        type = "custom";
      }
      
      const response = await getProviders(type);
      if (response.code === 200) {
        console.log("服务商数据:", response.data)
        setProviders(response.data)
      } else {
        setError(response.message || "获取服务商列表失败")
      }
    } catch (err) {
      console.error("获取服务商错误:", err)
      setError("获取服务商数据失败")
    } finally {
      setLoading(false)
    }
  }
  
  // 当标签变化时重新加载数据
  useEffect(() => {
    loadProviders()
  }, [activeTab])
  
  // 根据标签筛选服务商（已通过API过滤，无需本地再次过滤）
  const filteredProviders = providers;

  // 打开详情弹窗并获取详细信息
  const openDetail = async (provider: Provider) => {
    setSelectedProvider(provider)
    setShowDetailDialog(true)
    
    // 获取服务商详情
    setDetailLoading(true)
    try {
      const response = await getProviderDetail(provider.id);
      if (response.code === 200) {
        setSelectedProvider(response.data);
        // 不再需要单独加载模型列表，因为服务商详情已包含models数组
      }
    } catch (err) {
      console.error("获取服务商详情错误:", err);
    } finally {
      setDetailLoading(false);
    }
  }
  
  // 关闭详情弹窗
  const closeDetail = () => {
    setShowDetailDialog(false)
    setSelectedProvider(null)
  }
  
  // 打开编辑弹窗
  const openEditDialog = async (provider: Provider, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
      e.preventDefault();
    }
    
    // 获取服务商详情
    try {
      const response = await getProviderDetail(provider.id);
      if (response.code === 200) {
        setEditingProvider(response.data);
        setShowProviderDialog(true);
      } else {
        toast({
          title: "获取提供商详情失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (err) {
      console.error("获取服务商详情错误:", err);
      toast({
        title: "获取提供商详情失败",
        description: "请稍后重试",
        variant: "destructive"
      });
    }
  }
  
  // 打开添加弹窗
  const openAddDialog = () => {
    setEditingProvider(null);
    setShowProviderDialog(true);
  }
  
  // 打开删除确认
  const openDeleteConfirm = (provider: Provider, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedProvider(provider);
    setDeleteConfirmOpen(true);
  }
  
  // 确认删除
  const confirmDelete = async () => {
    if (!selectedProvider) return;
    
    setIsDeleting(true);
    try {
      const response = await deleteProviderWithToast(selectedProvider.id);
      if (response.code === 200) {
        setDeleteConfirmOpen(false);
        setShowDetailDialog(false);
        loadProviders();
      }
    } catch (error) {
      console.error("删除服务商失败:", error);
    } finally {
      setIsDeleting(false);
    }
  }
  
  // 切换服务商状态
  const toggleProviderStatus = async (provider: Provider, e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    
    setIsTogglingStatus(true);
    try {
      const response = await toggleProviderStatusWithToast(provider.id);
      if (response.code === 200) {
        // 更新本地状态
        if (selectedProvider && selectedProvider.id === provider.id) {
          setSelectedProvider({
            ...selectedProvider,
            status: !selectedProvider.status
          });
        }
        // 更新列表
        setProviders(prev => prev.map(p => {
          if (p.id === provider.id) {
            return { ...p, status: !p.status };
          }
          return p;
        }));
      }
    } catch (error) {
      console.error("切换服务商状态失败:", error);
    } finally {
      setIsTogglingStatus(false);
    }
  }
  
  // 打开添加模型对话框
  const openAddModelDialog = () => {
    setEditingModel(null);
    setShowModelDialog(true);
  }
  
  // 打开编辑模型对话框
  const openEditModelDialog = (model: Model) => {
    setEditingModel(model);
    setShowModelDialog(true);
  }
  
  // 打开删除模型确认
  const openDeleteModelConfirm = (model: Model) => {
    setSelectedModel(model);
    setDeleteModelConfirmOpen(true);
  }
  
  // 确认删除模型
  const confirmDeleteModel = async () => {
    if (!selectedModel || !selectedProvider) return;
    
    setIsDeletingModel(true);
    try {
      const response = await deleteModelWithToast(selectedModel.id);
      if (response.code === 200) {
        setDeleteModelConfirmOpen(false);
        
        // 更新服务商详情中的模型列表
        try {
          const detailResponse = await getProviderDetail(selectedProvider.id);
          if (detailResponse.code === 200) {
            const updatedProvider = detailResponse.data;
            setSelectedProvider(updatedProvider);
            
            // 局部更新providers数组中的对应服务商
            setProviders(prev => prev.map(p => {
              if (p.id === selectedProvider.id) {
                return {
                  ...p,
                  models: updatedProvider.models || []
                };
              }
              return p;
            }));
          }
        } catch (error) {
          console.error("刷新服务商详情失败:", error);
        }
      }
    } catch (error) {
      console.error("删除模型失败:", error);
    } finally {
      setIsDeletingModel(false);
    }
  }
  
  // 切换模型状态
  const toggleModelStatus = async (model: Model) => {
    if (!selectedProvider) return;
    
    setIsTogglingModelStatus(true);
    try {
      const response = await toggleModelStatusWithToast(model.id);
      if (response.code === 200) {
        const updatedStatus = !model.status;
        
        // 更新详情页模型状态
        const updatedModels = selectedProvider.models.map(m => 
          m.id === model.id ? { ...m, status: updatedStatus } : m
        );
        
        // 更新选中的服务商
        setSelectedProvider(prev => {
          if (!prev) return prev;
          return {
            ...prev,
            models: updatedModels
          };
        });
        
        // 局部更新providers数组中的对应服务商
        setProviders(prev => prev.map(p => {
          if (p.id === selectedProvider.id) {
            return {
              ...p,
              models: updatedModels
            };
          }
          return p;
        }));
      }
    } catch (error) {
      console.error("切换模型状态失败:", error);
    } finally {
      setIsTogglingModelStatus(false);
    }
  }
  
  // 显示加载中状态
  if (loading) {
    return (
      <div className="container py-6 flex flex-col items-center justify-center min-h-[400px]">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="mt-4 text-muted-foreground">加载服务商...</p>
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
          <h1 className="text-3xl font-bold tracking-tight">服务商</h1>
          <p className="text-muted-foreground">管理您的AI服务商和API密钥</p>
        </div>
        <Button className="flex items-center gap-2" onClick={openAddDialog}>
          <Plus className="h-4 w-4" />
          添加服务商
        </Button>
      </div>
      
      <Tabs defaultValue="全部" className="space-y-6" value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="全部">全部</TabsTrigger>
          <TabsTrigger value="官方服务">官方服务</TabsTrigger>
          <TabsTrigger value="自定义服务">自定义服务</TabsTrigger>
        </TabsList>
        
        <TabsContent value="全部" className="space-y-6">
          {filteredProviders.length === 0 ? (
            <div className="text-center py-10 border rounded-md bg-gray-50">
              <p className="text-muted-foreground">暂无服务商数据</p>
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
                            <DropdownMenuItem onClick={(e) => openEditDialog(provider, e)}>
                              <Edit className="mr-2 h-4 w-4" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => openDeleteConfirm(provider, e)}>
                              <Trash className="mr-2 h-4 w-4" />
                              删除
                            </DropdownMenuItem>
                            <DropdownMenuItem 
                              onClick={(e) => toggleProviderStatus(provider, e)}
                              disabled={isTogglingStatus}
                            >
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
        
        <TabsContent value="官方服务" className="space-y-6">
          {filteredProviders.length === 0 ? (
            <div className="text-center py-10 border rounded-md bg-gray-50">
              <p className="text-muted-foreground">暂无官方服务商数据</p>
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
        
        <TabsContent value="自定义服务" className="space-y-6">
          {filteredProviders.length === 0 ? (
            <div className="text-center py-10 border rounded-md bg-gray-50">
              <p className="text-muted-foreground">暂无自定义服务商数据</p>
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
                            <DropdownMenuItem onClick={(e) => openEditDialog(provider, e)}>
                              <Edit className="mr-2 h-4 w-4" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={(e) => openDeleteConfirm(provider, e)}>
                              <Trash className="mr-2 h-4 w-4" />
                              删除
                            </DropdownMenuItem>
                            <DropdownMenuItem 
                              onClick={(e) => toggleProviderStatus(provider, e)}
                              disabled={isTogglingStatus}
                            >
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
      
      {/* 服务商详情弹窗 */}
      {selectedProvider && (
        <Dialog open={showDetailDialog} onOpenChange={setShowDetailDialog}>
          <DialogContent className="max-w-4xl max-h-[80vh] flex flex-col overflow-hidden">
            <DialogHeader>
              <DialogTitle className="flex justify-between items-center">
                <span>服务商详情</span>
              </DialogTitle>
              <DialogDescription>
                查看和管理服务商的详细信息和模型配置
              </DialogDescription>
            </DialogHeader>
            
            {detailLoading ? (
              <div className="flex justify-center py-10">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            ) : selectedProvider ? (
              <div className="flex flex-col space-y-4 h-full overflow-hidden">
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
                          <DropdownMenuItem onClick={() => openEditDialog(selectedProvider)}>
                            <Edit className="mr-2 h-4 w-4" />
                            编辑
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={(e) => openDeleteConfirm(selectedProvider, e)}>
                            <Trash className="mr-2 h-4 w-4" />
                            删除
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={(e) => toggleProviderStatus(selectedProvider, e)}
                            disabled={isTogglingStatus}
                          >
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
                
                <Separator />
                
                {/* 模型列表 */}
                <div className="flex-1 overflow-hidden flex flex-col">
                  <div className="flex justify-between items-center mb-2">
                    <h3 className="text-lg font-semibold">模型列表</h3>
                    {!selectedProvider.isOfficial && (
                      <Button variant="outline" size="sm" onClick={openAddModelDialog}>
                        <PlusCircle className="h-4 w-4 mr-1" />
                        添加模型
                      </Button>
                    )}
                  </div>
                  
                  <ScrollArea className="flex-1">
                    {!selectedProvider.models || selectedProvider.models.length === 0 ? (
                      <div className="text-center py-6 text-muted-foreground">
                        暂无模型{!selectedProvider.isOfficial && "，点击添加按钮创建模型"}
                      </div>
                    ) : (
                      <div className="space-y-2">
                        {selectedProvider.models.map((model: Model) => (
                          <Card key={model.id} className="p-3">
                            <div className="flex justify-between items-start">
                              <div>
                                <div className="font-medium">{model.name}</div>
                                <div className="text-sm text-muted-foreground flex items-center space-x-2">
                                  <span>ID: {model.modelId}</span>
                                  <span>·</span>
                                  <span>类型: {model.type}</span>
                                </div>
                                {model.description && (
                                  <div className="text-sm mt-1">{model.description}</div>
                                )}
                              </div>
                              <div className="flex items-center space-x-2">
                                {!selectedProvider.isOfficial && (
                                  <Switch 
                                    checked={model.status}
                                    onCheckedChange={() => {}}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      toggleModelStatus(model);
                                    }}
                                  />
                                )}
                                {!selectedProvider.isOfficial && (
                                  <>
                                    <Button 
                                      variant="ghost" 
                                      size="icon"
                                      className="h-8 w-8" 
                                      onClick={() => openEditModelDialog(model)}
                                    >
                                      <Settings2 className="h-4 w-4" />
                                    </Button>
                                    <Button 
                                      variant="ghost" 
                                      size="icon"
                                      className="h-8 w-8 text-destructive" 
                                      onClick={() => openDeleteModelConfirm(model)}
                                    >
                                      <svg
                                        width="15"
                                        height="15"
                                        viewBox="0 0 15 15"
                                        fill="none"
                                        xmlns="http://www.w3.org/2000/svg"
                                        className="h-4 w-4"
                                      >
                                        <path
                                          d="M5.5 1C5.22386 1 5 1.22386 5 1.5C5 1.77614 5.22386 2 5.5 2H9.5C9.77614 2 10 1.77614 10 1.5C10 1.22386 9.77614 1 9.5 1H5.5ZM3 3.5C3 3.22386 3.22386 3 3.5 3H11.5C11.7761 3 12 3.22386 12 3.5C12 3.77614 11.7761 4 11.5 4H3.5C3.22386 4 3 3.77614 3 3.5ZM3.5 5C3.22386 5 3 5.22386 3 5.5C3 5.77614 3.22386 6 3.5 6H4V12C4 12.5523 4.44772 13 5 13H10C10.5523 13 11 12.5523 11 12V6H11.5C11.7761 6 12 5.77614 12 5.5C12 5.22386 11.7761 5 11.5 5H3.5ZM5 6H10V12H5V6Z"
                                          fill="currentColor"
                                          fillRule="evenodd"
                                          clipRule="evenodd"
                                        ></path>
                                      </svg>
                                    </Button>
                                  </>
                                )}
                              </div>
                            </div>
                          </Card>
                        ))}
                      </div>
                    )}
                  </ScrollArea>
                </div>
              </div>
            ) : (
              <div className="text-center py-10 text-muted-foreground">
                无法加载服务商详情
              </div>
            )}
            
            <DialogFooter>
              <Button variant="outline" onClick={closeDetail}>
                关闭
              </Button>
              {!selectedProvider?.isOfficial && (
                <Button 
                  onClick={() => selectedProvider && openEditDialog(selectedProvider)}
                  disabled={!selectedProvider}
                >
                  编辑配置
                </Button>
              )}
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
      
      {/* 删除确认对话框 */}
      <Dialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>删除服务商</DialogTitle>
            <DialogDescription>
              您确定要删除此服务商吗？此操作无法撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteConfirmOpen(false)} disabled={isDeleting}>
              取消
            </Button>
            <Button variant="destructive" onClick={confirmDelete} disabled={isDeleting}>
              {isDeleting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  删除中...
                </>
              ) : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      
      {/* 添加/编辑服务商对话框 */}
      <ProviderDialog 
        open={showProviderDialog} 
        onOpenChange={setShowProviderDialog}
        provider={editingProvider}
        onSuccess={loadProviders}
      />
      
      {/* 删除模型确认对话框 */}
      <Dialog open={deleteModelConfirmOpen} onOpenChange={setDeleteModelConfirmOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>删除模型</DialogTitle>
            <DialogDescription>
              您确定要删除此模型吗？此操作无法撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteModelConfirmOpen(false)} disabled={isDeletingModel}>
              取消
            </Button>
            <Button variant="destructive" onClick={confirmDeleteModel} disabled={isDeletingModel}>
              {isDeletingModel ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  删除中...
                </>
              ) : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      
      {/* 添加/编辑模型对话框 */}
      {selectedProvider && (
        <ModelDialog
          open={showModelDialog}
          onOpenChange={setShowModelDialog}
          providerId={selectedProvider.id}
          providerName={selectedProvider.name}
          model={editingModel}
          onSuccess={async () => {
            // 更新详情页数据
            try {
              const response = await getProviderDetail(selectedProvider.id);
              if (response.code === 200) {
                const updatedProvider = response.data;
                setSelectedProvider(updatedProvider);
                
                // 局部更新providers数组中的对应服务商
                setProviders(prev => prev.map(p => {
                  if (p.id === selectedProvider.id) {
                    return {
                      ...p,
                      models: updatedProvider.models || []
                    };
                  }
                  return p;
                }));
              }
            } catch (error) {
              console.error("刷新服务商详情失败:", error);
            }
          }}
        />
      )}
    </div>
  )
} 