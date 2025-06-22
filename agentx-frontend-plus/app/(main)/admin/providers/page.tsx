"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "@/hooks/use-toast";
import { 
  Search, 
  Plus, 
  Edit, 
  Trash2, 
  Eye, 
  Settings, 
  Zap, 
  Server, 
  Bot, 
  Cpu, 
  ArrowLeft,
  Copy,
  EyeOff
} from "lucide-react";
import { 
  AdminProviderService,
  Provider,
  Model,
  ProviderProtocol,
  ModelType,
  CreateProviderRequest,
  UpdateProviderRequest,
  CreateModelRequest,
  UpdateModelRequest,
  getProtocolText,
  getModelTypeText,
  getProtocolConfig
} from "@/lib/admin-provider-service";

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [protocols, setProtocols] = useState<ProviderProtocol[]>([]);
  const [modelTypes, setModelTypes] = useState<ModelType[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [modelSearchQuery, setModelSearchQuery] = useState("");
  // 服务商相关状态
  const [isAddProviderOpen, setIsAddProviderOpen] = useState(false);
  const [isEditProviderOpen, setIsEditProviderOpen] = useState(false);
  const [editingProvider, setEditingProvider] = useState<Provider | null>(null);
  const [newProvider, setNewProvider] = useState<CreateProviderRequest>({
    protocol: ProviderProtocol.OPENAI,
    name: "",
    description: "",
    config: { apiKey: "", baseUrl: "" }
  });
  
  // 模型相关状态
  const [isAddModelOpen, setIsAddModelOpen] = useState(false);
  const [isEditModelOpen, setIsEditModelOpen] = useState(false);
  const [editingModel, setEditingModel] = useState<Model | null>(null);
  const [newModel, setNewModel] = useState<CreateModelRequest>({
    providerId: "",
    modelId: "",
    name: "",
    description: "",
    type: ModelType.CHAT,
    modelEndpoint: "",
    config: {}
  });
  
  // 其他状态
  const [showApiKey, setShowApiKey] = useState(false);

  // 数据加载
  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [providersRes, protocolsRes, modelTypesRes] = await Promise.all([
        AdminProviderService.getProviders(),
        AdminProviderService.getProviderProtocols(),
        AdminProviderService.getModelTypes()
      ]);

      if (providersRes.code === 200) {
        setProviders(providersRes.data);
      } else {
        toast({ title: "错误", description: providersRes.message, variant: "destructive" });
      }

      if (protocolsRes.code === 200) {
        setProtocols(protocolsRes.data);
      }

      if (modelTypesRes.code === 200) {
        setModelTypes(modelTypesRes.data);
      }
    } catch (error) {
      toast({ title: "错误", description: "加载数据失败", variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  const loadModels = async (providerId?: string) => {
    try {
      const modelsRes = await AdminProviderService.getModels(providerId);
      if (modelsRes.code === 200) {
        setModels(modelsRes.data);
      } else {
        toast({ title: "错误", description: modelsRes.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "加载模型失败", variant: "destructive" });
    }
  };

  const handleCreateProvider = async () => {
    if (!newProvider.name || !newProvider.config.apiKey) {
      toast({ title: "错误", description: "请填写必要信息", variant: "destructive" });
      return;
    }

    try {
      const response = await AdminProviderService.createProvider(newProvider);
      if (response.code === 200) {
        toast({ title: "成功", description: "服务商创建成功" });
        loadData();
        setIsAddProviderOpen(false);
        setNewProvider({
          protocol: ProviderProtocol.OPENAI,
          name: "",
          description: "",
          config: { apiKey: "", baseUrl: "" }
        });
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "创建服务商失败", variant: "destructive" });
    }
  };

  const handleDeleteProvider = (providerId: string) => {
    toast({
      title: "确认删除",
      description: "确定要删除此服务商吗？此操作不可撤销。",
      action: (
        <Button
          variant="destructive"
          size="sm"
          onClick={async () => {
            try {
              const response = await AdminProviderService.deleteProvider(providerId);
              if (response.code === 200) {
                toast({ title: "成功", description: "服务商删除成功" });
                loadData();
                if (selectedProvider?.id === providerId) {
                  setSelectedProvider(null);
                }
              } else {
                toast({ title: "错误", description: response.message, variant: "destructive" });
              }
            } catch (error) {
              toast({ title: "错误", description: "删除服务商失败", variant: "destructive" });
            }
          }}
        >
          确认删除
        </Button>
      ),
    });
  };

  const handleToggleProviderStatus = async (providerId: string) => {
    try {
      const response = await AdminProviderService.toggleProviderStatus(providerId);
      if (response.code === 200) {
        toast({ title: "成功", description: "状态切换成功" });
        loadData();
        if (selectedProvider?.id === providerId) {
          const updatedProvider = providers.find(p => p.id === providerId);
          if (updatedProvider) {
            setSelectedProvider({ ...updatedProvider, status: !updatedProvider.status });
          }
        }
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "状态切换失败", variant: "destructive" });
    }
  };

  const handleCreateModel = async () => {
    if (!newModel.providerId || !newModel.modelId || !newModel.name) {
      toast({ title: "错误", description: "请填写必要信息", variant: "destructive" });
      return;
    }

    try {
      const response = await AdminProviderService.createModel(newModel);
      if (response.code === 200) {
        toast({ title: "成功", description: "模型创建成功" });
        loadModels(selectedProvider?.id);
        setIsAddModelOpen(false);
        setNewModel({
          providerId: "",
          modelId: "",
          name: "",
          description: "",
          type: ModelType.CHAT,
          modelEndpoint: "",
          config: {}
        });
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "创建模型失败", variant: "destructive" });
    }
  };

  const handleDeleteModel = (modelId: string) => {
    toast({
      title: "确认删除",
      description: "确定要删除此模型吗？此操作不可撤销。",
      action: (
        <Button
          variant="destructive"
          size="sm"
          onClick={async () => {
            try {
              const response = await AdminProviderService.deleteModel(modelId);
              if (response.code === 200) {
                toast({ title: "成功", description: "模型删除成功" });
                loadModels(selectedProvider?.id);
              } else {
                toast({ title: "错误", description: response.message, variant: "destructive" });
              }
            } catch (error) {
              toast({ title: "错误", description: "删除模型失败", variant: "destructive" });
            }
          }}
        >
          确认删除
        </Button>
      ),
    });
  };

  const handleToggleModelStatus = async (modelId: string) => {
    try {
      const response = await AdminProviderService.toggleModelStatus(modelId);
      if (response.code === 200) {
        toast({ title: "成功", description: "模型状态切换成功" });
        loadModels(selectedProvider?.id);
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "模型状态切换失败", variant: "destructive" });
    }
  };

  const handleEditModel = (model: Model) => {
    setEditingModel(model);
    setIsEditModelOpen(true);
  };

  const handleUpdateModel = async () => {
    if (!editingModel || !editingModel.modelId || !editingModel.name) {
      toast({ title: "错误", description: "请填写必要信息", variant: "destructive" });
      return;
    }

    try {
      const updateRequest: UpdateModelRequest = {
        id: editingModel.id,
        modelId: editingModel.modelId,
        name: editingModel.name,
        description: editingModel.description,
        modelEndpoint: editingModel.modelEndpoint
      };

      const response = await AdminProviderService.updateModel(updateRequest);
      if (response.code === 200) {
        toast({ title: "成功", description: "模型更新成功" });
        loadModels(selectedProvider?.id);
        setIsEditModelOpen(false);
        setEditingModel(null);
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "更新模型失败", variant: "destructive" });
    }
  };

  const handleUpdateProvider = async () => {
    if (!editingProvider || !editingProvider.name || !editingProvider.config.apiKey) {
      toast({ title: "错误", description: "请填写必要信息", variant: "destructive" });
      return;
    }

    try {
      const updateRequest: UpdateProviderRequest = {
        id: editingProvider.id,
        protocol: editingProvider.protocol,
        name: editingProvider.name,
        description: editingProvider.description,
        config: editingProvider.config
      };

      const response = await AdminProviderService.updateProvider(updateRequest);
      if (response.code === 200) {
        toast({ title: "成功", description: "服务商更新成功" });
        loadData();
        setIsEditProviderOpen(false);
        setEditingProvider(null);
        // 如果正在查看该服务商详情，更新详情数据
        if (selectedProvider?.id === editingProvider.id) {
          setSelectedProvider(response.data);
        }
      } else {
        toast({ title: "错误", description: response.message, variant: "destructive" });
      }
    } catch (error) {
      toast({ title: "错误", description: "更新服务商失败", variant: "destructive" });
    }
  };

  useEffect(() => {
    if (selectedProvider) {
      loadModels(selectedProvider.id);
    }
  }, [selectedProvider]);

  useEffect(() => {
    if (isAddModelOpen && selectedProvider) {
      setNewModel(prev => ({ ...prev, providerId: selectedProvider.id }));
    }
  }, [isAddModelOpen, selectedProvider]);

  const getStatusBadge = (status: boolean) => {
    return status ? 
      <Badge variant="default" className="bg-green-100 text-green-800">正常</Badge> :
      <Badge variant="secondary">未激活</Badge>;
  };

  const getProtocolBadge = (protocol: ProviderProtocol) => {
    const protocolMap = {
      [ProviderProtocol.OPENAI]: { label: "OpenAI", color: "bg-blue-100 text-blue-800" },
      [ProviderProtocol.ANTHROPIC]: { label: "Anthropic", color: "bg-purple-100 text-purple-800" },
    };
    const config = protocolMap[protocol] || { label: protocol, color: "bg-gray-100 text-gray-800" };
    return <Badge variant="outline" className={config.color}>{config.label}</Badge>;
  };

  const getModelStatusBadge = (status: boolean) => {
    return status ?
      <Badge variant="default" className="bg-green-100 text-green-800">启用</Badge> :
      <Badge variant="secondary">禁用</Badge>;
  };

  const getModelTypeBadge = (type: ModelType) => {
    const typeMap = {
      [ModelType.CHAT]: { label: "对话模型", color: "bg-blue-100 text-blue-800" },
      [ModelType.EMBEDDING]: { label: "嵌入模型", color: "bg-purple-100 text-purple-800" },
      [ModelType.IMAGE]: { label: "图像模型", color: "bg-pink-100 text-pink-800" },
    };
    const config = typeMap[type] || { label: type, color: "bg-gray-100 text-gray-800" };
    return <Badge variant="outline" className={config.color}>{config.label}</Badge>;
  };

  const filteredProviders = providers.filter(provider =>
    provider.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (provider.description && provider.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const providerModels = models.filter(model => 
    model.providerId === selectedProvider?.id &&
    (model.name.toLowerCase().includes(modelSearchQuery.toLowerCase()) ||
     model.modelId.toLowerCase().includes(modelSearchQuery.toLowerCase()))
  );

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const maskApiKey = (apiKey: string) => {
    if (apiKey.length <= 8) return apiKey;
    return apiKey.substring(0, 8) + "*".repeat(apiKey.length - 8);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">加载中...</div>
      </div>
    );
  }

  // 服务商详情视图
  if (selectedProvider) {
    return (
      <div className="space-y-6">
        {/* 返回按钮和标题 */}
        <div className="flex items-center space-x-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSelectedProvider(null)}
          >
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div className="flex items-center space-x-4">
            <Avatar className="h-12 w-12">
              <AvatarImage src={`/providers/${selectedProvider.name}-logo.png`} />
              <AvatarFallback>
                <Server className="h-6 w-6" />
              </AvatarFallback>
            </Avatar>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{selectedProvider.name}</h1>
              <p className="text-gray-600">{selectedProvider.description || "无描述"}</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 服务商配置信息 */}
          <div className="lg:col-span-1">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Settings className="w-5 h-5" />
                  <span>配置信息</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <Label className="text-sm font-medium text-gray-700">协议</Label>
                  <div className="mt-1">
                    {getProtocolBadge(selectedProvider.protocol)}
                  </div>
                </div>
                
                <div>
                  <Label className="text-sm font-medium text-gray-700">状态</Label>
                  <div className="mt-1">
                    {getStatusBadge(selectedProvider.status)}
                  </div>
                </div>

                <div>
                  <Label className="text-sm font-medium text-gray-700">API地址</Label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Input 
                      value={selectedProvider.config.baseUrl || "默认地址"} 
                      readOnly 
                      className="text-sm"
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => copyToClipboard(selectedProvider.config.baseUrl || "")}
                    >
                      <Copy className="w-4 h-4" />
                    </Button>
                  </div>
                </div>

                <div>
                  <Label className="text-sm font-medium text-gray-700">API Key</Label>
                  <div className="mt-1 flex items-center space-x-2">
                    <Input 
                      type={showApiKey ? "text" : "password"}
                      value={showApiKey ? selectedProvider.config.apiKey : maskApiKey(selectedProvider.config.apiKey)}
                      readOnly 
                      className="text-sm"
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setShowApiKey(!showApiKey)}
                    >
                      {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => copyToClipboard(selectedProvider.config.apiKey)}
                    >
                      <Copy className="w-4 h-4" />
                    </Button>
                  </div>
                </div>

                <div>
                  <Label className="text-sm font-medium text-gray-700">创建时间</Label>
                  <div className="mt-1 text-sm text-gray-600">
                    {new Date(selectedProvider.createdAt).toLocaleString()}
                  </div>
                </div>

                <div className="pt-4 space-y-2">
                  <Button 
                    variant="outline" 
                    className="w-full"
                    onClick={() => handleToggleProviderStatus(selectedProvider.id)}
                  >
                    <Zap className="w-4 h-4 mr-2" />
                    {selectedProvider.status ? "禁用" : "启用"}
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full" 
                    onClick={() => {
                      setEditingProvider(selectedProvider);
                      setIsEditProviderOpen(true);
                    }}
                  >
                    <Edit className="w-4 h-4 mr-2" />
                    编辑配置
                  </Button>
                  <Button 
                    variant="destructive" 
                    className="w-full"
                    onClick={() => handleDeleteProvider(selectedProvider.id)}
                  >
                    <Trash2 className="w-4 h-4 mr-2" />
                    删除
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 模型列表 */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <div className="flex justify-between items-center">
                  <CardTitle className="flex items-center space-x-2">
                    <Cpu className="w-5 h-5" />
                    <span>模型列表 ({providerModels.length})</span>
                  </CardTitle>
                  <Dialog open={isAddModelOpen} onOpenChange={setIsAddModelOpen}>
                    <DialogTrigger asChild>
                      <Button>
                        <Plus className="w-4 h-4 mr-2" />
                        添加模型
                      </Button>
                    </DialogTrigger>
                    <DialogContent className="sm:max-w-[500px]">
                      <DialogHeader>
                        <DialogTitle>为 {selectedProvider.name} 添加模型</DialogTitle>
                      </DialogHeader>
                      <div className="grid gap-4 py-4">
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelId" className="text-right">模型ID</Label>
                          <Input 
                            id="modelId" 
                            placeholder="例如: gpt-4" 
                            className="col-span-3" 
                            value={newModel.modelId}
                            onChange={(e) => setNewModel(prev => ({ ...prev, modelId: e.target.value }))}
                          />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelName" className="text-right">显示名称</Label>
                          <Input 
                            id="modelName" 
                            placeholder="例如: GPT-4" 
                            className="col-span-3" 
                            value={newModel.name}
                            onChange={(e) => setNewModel(prev => ({ ...prev, name: e.target.value }))}
                          />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelType" className="text-right">模型类型</Label>
                          <Select 
                            value={newModel.type} 
                            onValueChange={(value) => setNewModel(prev => ({ ...prev, type: value as ModelType }))}
                          >
                            <SelectTrigger className="col-span-3">
                              <SelectValue placeholder="选择类型" />
                            </SelectTrigger>
                            <SelectContent>
                              {modelTypes.map(type => (
                                <SelectItem key={type} value={type}>
                                  {getModelTypeText(type)}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelEndpoint" className="text-right">部署名称</Label>
                          <Input 
                            id="modelEndpoint" 
                            placeholder="例如: gpt-4-deployment" 
                            className="col-span-3" 
                            value={newModel.modelEndpoint}
                            onChange={(e) => setNewModel(prev => ({ ...prev, modelEndpoint: e.target.value }))}
                          />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelDescription" className="text-right">描述</Label>
                          <Textarea 
                            id="modelDescription" 
                            placeholder="模型描述" 
                            className="col-span-3" 
                            value={newModel.description}
                            onChange={(e) => setNewModel(prev => ({ ...prev, description: e.target.value }))}
                          />
                        </div>
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsAddModelOpen(false)}>取消</Button>
                        <Button onClick={handleCreateModel}>添加</Button>
                      </div>
                    </DialogContent>
                  </Dialog>
                </div>
              </CardHeader>
              <CardContent>
                {/* 模型搜索 */}
                <div className="mb-4">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                    <Input
                      placeholder="搜索模型名称..."
                      value={modelSearchQuery}
                      onChange={(e) => setModelSearchQuery(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>

                {/* 模型表格 */}
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>模型信息</TableHead>
                      <TableHead>类型</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead>创建时间</TableHead>
                      <TableHead>操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {providerModels.map((model) => (
                      <TableRow key={model.id}>
                        <TableCell>
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                              <Bot className="w-4 h-4 text-blue-600" />
                            </div>
                            <div>
                              <div className="font-medium">{model.name}</div>
                              <div className="text-sm text-gray-500">{model.modelId}</div>
                              {model.modelEndpoint && (
                                <div className="text-xs text-blue-600">部署: {model.modelEndpoint}</div>
                              )}
                              {model.description && (
                                <div className="text-xs text-gray-400">{model.description}</div>
                              )}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          {getModelTypeBadge(model.type)}
                        </TableCell>
                        <TableCell>
                          {getModelStatusBadge(model.status)}
                        </TableCell>
                        <TableCell>
                          <div className="text-sm">{new Date(model.createdAt).toLocaleDateString()}</div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              title="编辑"
                              onClick={() => handleEditModel(model)}
                            >
                              <Edit className="w-4 h-4" />
                            </Button>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              title={model.status ? "禁用" : "启用"}
                              onClick={() => handleToggleModelStatus(model.id)}
                            >
                              <Zap className="w-4 h-4" />
                            </Button>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="text-red-600" 
                              title="删除"
                              onClick={() => handleDeleteModel(model.id)}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* 服务商详情页面中的编辑对话框 */}
        <Dialog open={isEditProviderOpen} onOpenChange={setIsEditProviderOpen}>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle>编辑服务商 {editingProvider?.name || ""}</DialogTitle>
            </DialogHeader>
            {editingProvider && (
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editProviderName" className="text-right">名称</Label>
                  <Input 
                    id="editProviderName" 
                    placeholder="例如: openai" 
                    className="col-span-3" 
                    value={editingProvider.name}
                    onChange={(e) => setEditingProvider(prev => prev ? ({ ...prev, name: e.target.value }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editProviderProtocol" className="text-right">协议</Label>
                  <Select 
                    value={editingProvider.protocol} 
                    onValueChange={(value) => setEditingProvider(prev => prev ? ({ ...prev, protocol: value as ProviderProtocol }) : null)}
                  >
                    <SelectTrigger className="col-span-3">
                      <SelectValue placeholder="选择协议" />
                    </SelectTrigger>
                    <SelectContent>
                      {protocols.map(protocol => (
                        <SelectItem key={protocol} value={protocol}>
                          {getProtocolText(protocol)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editApiKey" className="text-right">API Key</Label>
                  <Input 
                    id="editApiKey" 
                    placeholder="sk-..." 
                    className="col-span-3" 
                    value={editingProvider.config.apiKey}
                    onChange={(e) => setEditingProvider(prev => prev ? ({ 
                      ...prev, 
                      config: { ...prev.config, apiKey: e.target.value }
                    }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editBaseUrl" className="text-right">基础URL</Label>
                  <Input 
                    id="editBaseUrl" 
                    placeholder="可选，例如: https://api.openai.com/v1" 
                    className="col-span-3" 
                    value={editingProvider.config.baseUrl || ""}
                    onChange={(e) => setEditingProvider(prev => prev ? ({ 
                      ...prev, 
                      config: { ...prev.config, baseUrl: e.target.value }
                    }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editDescription" className="text-right">描述</Label>
                  <Textarea 
                    id="editDescription" 
                    placeholder="服务商描述" 
                    className="col-span-3" 
                    value={editingProvider.description || ""}
                    onChange={(e) => setEditingProvider(prev => prev ? ({ ...prev, description: e.target.value }) : null)}
                  />
                </div>
              </div>
            )}
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => setIsEditProviderOpen(false)}>取消</Button>
              <Button onClick={handleUpdateProvider}>保存</Button>
            </div>
          </DialogContent>
        </Dialog>

        {/* 模型编辑对话框 */}
        <Dialog open={isEditModelOpen} onOpenChange={setIsEditModelOpen}>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle>编辑模型 {editingModel?.name || ""}</DialogTitle>
            </DialogHeader>
            {editingModel && (
              <div className="grid gap-4 py-4">
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editModelId" className="text-right">模型ID</Label>
                  <Input 
                    id="editModelId" 
                    placeholder="例如: gpt-4" 
                    className="col-span-3" 
                    value={editingModel.modelId}
                    onChange={(e) => setEditingModel(prev => prev ? ({ ...prev, modelId: e.target.value }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editModelName" className="text-right">显示名称</Label>
                  <Input 
                    id="editModelName" 
                    placeholder="例如: GPT-4" 
                    className="col-span-3" 
                    value={editingModel.name}
                    onChange={(e) => setEditingModel(prev => prev ? ({ ...prev, name: e.target.value }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editModelEndpoint" className="text-right">部署名称</Label>
                  <Input 
                    id="editModelEndpoint" 
                    placeholder="例如: gpt-4-deployment" 
                    className="col-span-3" 
                    value={editingModel.modelEndpoint || ""}
                    onChange={(e) => setEditingModel(prev => prev ? ({ ...prev, modelEndpoint: e.target.value }) : null)}
                  />
                </div>
                <div className="grid grid-cols-4 items-center gap-4">
                  <Label htmlFor="editModelDescription" className="text-right">描述</Label>
                  <Textarea 
                    id="editModelDescription" 
                    placeholder="模型描述" 
                    className="col-span-3" 
                    value={editingModel.description}
                    onChange={(e) => setEditingModel(prev => prev ? ({ ...prev, description: e.target.value }) : null)}
                  />
                </div>
              </div>
            )}
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => setIsEditModelOpen(false)}>取消</Button>
              <Button onClick={handleUpdateModel}>保存</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>
    );
  }

  // 服务商卡片列表视图
  return (
    <div className="space-y-6">
      {/* 页面标题和操作 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">服务商管理</h1>
          <p className="text-gray-600 mt-1">管理AI服务提供商和相关模型</p>
        </div>
        <Dialog open={isAddProviderOpen} onOpenChange={setIsAddProviderOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="w-4 h-4 mr-2" />
              新增服务商
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle>添加服务商</DialogTitle>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="providerName" className="text-right">名称</Label>
                <Input 
                  id="providerName" 
                  placeholder="例如: openai" 
                  className="col-span-3" 
                  value={newProvider.name}
                  onChange={(e) => setNewProvider(prev => ({ ...prev, name: e.target.value }))}
                />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="providerProtocol" className="text-right">协议</Label>
                <Select 
                  value={newProvider.protocol} 
                  onValueChange={(value) => setNewProvider(prev => ({ ...prev, protocol: value as ProviderProtocol }))}
                >
                  <SelectTrigger className="col-span-3">
                    <SelectValue placeholder="选择协议" />
                  </SelectTrigger>
                  <SelectContent>
                    {protocols.map(protocol => (
                      <SelectItem key={protocol} value={protocol}>
                        {getProtocolText(protocol)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="apiKey" className="text-right">API Key</Label>
                <Input 
                  id="apiKey" 
                  placeholder="sk-..." 
                  className="col-span-3" 
                  value={newProvider.config.apiKey}
                  onChange={(e) => setNewProvider(prev => ({ 
                    ...prev, 
                    config: { ...prev.config, apiKey: e.target.value }
                  }))}
                />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="baseUrl" className="text-right">基础URL</Label>
                <Input 
                  id="baseUrl" 
                  placeholder="可选，例如: https://api.openai.com/v1" 
                  className="col-span-3" 
                  value={newProvider.config.baseUrl}
                  onChange={(e) => setNewProvider(prev => ({ 
                    ...prev, 
                    config: { ...prev.config, baseUrl: e.target.value }
                  }))}
                />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="description" className="text-right">描述</Label>
                <Textarea 
                  id="description" 
                  placeholder="服务商描述" 
                  className="col-span-3" 
                  value={newProvider.description}
                  onChange={(e) => setNewProvider(prev => ({ ...prev, description: e.target.value }))}
                />
              </div>
            </div>
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => setIsAddProviderOpen(false)}>取消</Button>
              <Button onClick={handleCreateProvider}>添加</Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      {/* 搜索 */}
      <Card>
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              placeholder="搜索服务商名称或描述..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* 服务商卡片网格 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredProviders.map((provider) => (
          <Card 
            key={provider.id}
            className="cursor-pointer hover:shadow-lg transition-shadow duration-200"
            onClick={() => setSelectedProvider(provider)}
          >
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center space-x-3">
                  <Avatar className="h-12 w-12">
                    <AvatarImage src={`/providers/${provider.name}-logo.png`} />
                    <AvatarFallback>
                      <Server className="h-6 w-6" />
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <CardTitle className="text-lg">{provider.name}</CardTitle>
                    <p className="text-sm text-gray-500">{getProtocolText(provider.protocol)}</p>
                  </div>
                </div>
                {getStatusBadge(provider.status)}
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <p className="text-sm text-gray-600 line-clamp-2">
                  {provider.description || "无描述"}
                </p>
                
                <div className="flex items-center justify-between">
                  {getProtocolBadge(provider.protocol)}
                  <div className="text-sm text-gray-500">
                    {provider.models?.length || 0} 个模型
                  </div>
                </div>

                <div className="flex items-center justify-between text-sm text-gray-500">
                  <span>官方服务商</span>
                  <span>创建于: {new Date(provider.createdAt).toLocaleDateString()}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {filteredProviders.length === 0 && (
        <Card>
          <CardContent className="text-center py-12">
            <Server className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">没有找到服务商</h3>
            <p className="text-gray-500">请尝试调整搜索条件或添加新的服务商</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}