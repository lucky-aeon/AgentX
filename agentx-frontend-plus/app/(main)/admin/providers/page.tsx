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

interface Provider {
  id: string;
  name: string;
  displayName: string;
  type: "llm" | "embedding" | "tts" | "stt" | "image";
  status: "active" | "inactive" | "error";
  apiUrl: string;
  apiKey: string;
  modelsCount: number;
  requestsCount: number;
  lastHealthCheck: string;
  createdAt: string;
  updatedAt: string;
  description: string;
}

interface Model {
  id: string;
  name: string;
  displayName: string;
  providerId: string;
  providerName: string;
  type: "text" | "embedding" | "image" | "tts" | "stt";
  status: "active" | "inactive";
  maxTokens: number;
  inputCost: number;
  outputCost: number;
  createdAt: string;
  updatedAt: string;
}

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedProvider, setSelectedProvider] = useState<Provider | null>(null);
  const [modelSearchQuery, setModelSearchQuery] = useState("");
  const [isAddProviderOpen, setIsAddProviderOpen] = useState(false);
  const [isAddModelOpen, setIsAddModelOpen] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);

  // 模拟数据加载
  useEffect(() => {
    const mockProviders: Provider[] = [
      {
        id: "1",
        name: "openai",
        displayName: "OpenAI",
        type: "llm",
        status: "active",
        apiUrl: "https://api.openai.com/v1",
        apiKey: "sk-proj-1234567890abcdef",
        modelsCount: 12,
        requestsCount: 15420,
        lastHealthCheck: "2024-01-21 10:30:00",
        createdAt: "2024-01-01",
        updatedAt: "2024-01-21",
        description: "OpenAI官方API服务，提供GPT系列模型",
      },
      {
        id: "2",
        name: "anthropic",
        displayName: "Anthropic",
        type: "llm",
        status: "active",
        apiUrl: "https://api.anthropic.com/v1",
        apiKey: "sk-ant-api03-1234567890abcdef",
        modelsCount: 5,
        requestsCount: 8924,
        lastHealthCheck: "2024-01-21 10:25:00",
        createdAt: "2024-01-05",
        updatedAt: "2024-01-20",
        description: "Anthropic Claude API服务，提供Claude系列模型",
      },
      {
        id: "3",
        name: "azure-openai",
        displayName: "Azure OpenAI",
        type: "llm",
        status: "active",
        apiUrl: "https://your-resource.openai.azure.com/",
        apiKey: "1234567890abcdef1234567890abcdef",
        modelsCount: 8,
        requestsCount: 3456,
        lastHealthCheck: "2024-01-21 10:20:00",
        createdAt: "2024-01-10",
        updatedAt: "2024-01-19",
        description: "微软Azure平台上的OpenAI服务",
      },
      {
        id: "4",
        name: "google-vertex",
        displayName: "Google Vertex AI",
        type: "llm",
        status: "inactive",
        apiUrl: "https://us-central1-aiplatform.googleapis.com/v1",
        apiKey: "ya29.1234567890abcdef",
        modelsCount: 6,
        requestsCount: 0,
        lastHealthCheck: "2024-01-18 15:30:00",
        createdAt: "2024-01-12",
        updatedAt: "2024-01-18",
        description: "Google Vertex AI服务，提供PaLM等模型",
      },
      {
        id: "5",
        name: "huggingface",
        displayName: "Hugging Face",
        type: "embedding",
        status: "error",
        apiUrl: "https://api-inference.huggingface.co/",
        apiKey: "hf_1234567890abcdef",
        modelsCount: 25,
        requestsCount: 1234,
        lastHealthCheck: "2024-01-21 09:45:00",
        createdAt: "2024-01-15",
        updatedAt: "2024-01-21",
        description: "Hugging Face推理API，提供大量开源模型",
      },
      {
        id: "6",
        name: "elevenlabs",
        displayName: "ElevenLabs",
        type: "tts",
        status: "active",
        apiUrl: "https://api.elevenlabs.io/v1",
        apiKey: "el_1234567890abcdef",
        modelsCount: 15,
        requestsCount: 892,
        lastHealthCheck: "2024-01-21 10:15:00",
        createdAt: "2024-01-08",
        updatedAt: "2024-01-20",
        description: "ElevenLabs语音合成服务，提供高质量TTS",
      },
    ];

    const mockModels: Model[] = [
      {
        id: "1",
        name: "gpt-4",
        displayName: "GPT-4",
        providerId: "1",
        providerName: "OpenAI",
        type: "text",
        status: "active",
        maxTokens: 8192,
        inputCost: 0.03,
        outputCost: 0.06,
        createdAt: "2024-01-01",
        updatedAt: "2024-01-20",
      },
      {
        id: "2",
        name: "gpt-3.5-turbo",
        displayName: "GPT-3.5 Turbo",
        providerId: "1",
        providerName: "OpenAI",
        type: "text",
        status: "active",
        maxTokens: 4096,
        inputCost: 0.001,
        outputCost: 0.002,
        createdAt: "2024-01-01",
        updatedAt: "2024-01-15",
      },
      {
        id: "3",
        name: "claude-3-opus",
        displayName: "Claude 3 Opus",
        providerId: "2",
        providerName: "Anthropic",
        type: "text",
        status: "active",
        maxTokens: 200000,
        inputCost: 0.015,
        outputCost: 0.075,
        createdAt: "2024-01-05",
        updatedAt: "2024-01-18",
      },
      {
        id: "4",
        name: "text-embedding-ada-002",
        displayName: "Text Embedding Ada 002",
        providerId: "1",
        providerName: "OpenAI",
        type: "embedding",
        status: "active",
        maxTokens: 8191,
        inputCost: 0.0001,
        outputCost: 0,
        createdAt: "2024-01-01",
        updatedAt: "2024-01-10",
      },
      {
        id: "5",
        name: "claude-3-sonnet",
        displayName: "Claude 3 Sonnet",
        providerId: "2",
        providerName: "Anthropic",
        type: "text",
        status: "active",
        maxTokens: 200000,
        inputCost: 0.003,
        outputCost: 0.015,
        createdAt: "2024-01-05",
        updatedAt: "2024-01-18",
      },
    ];

    setTimeout(() => {
      setProviders(mockProviders);
      setModels(mockModels);
      setLoading(false);
    }, 1000);
  }, []);

  const getStatusBadge = (status: Provider["status"]) => {
    switch (status) {
      case "active":
        return <Badge variant="default" className="bg-green-100 text-green-800">正常</Badge>;
      case "inactive":
        return <Badge variant="secondary">未激活</Badge>;
      case "error":
        return <Badge variant="destructive">错误</Badge>;
    }
  };

  const getTypeBadge = (type: Provider["type"]) => {
    const typeMap = {
      llm: { label: "大语言模型", color: "bg-blue-100 text-blue-800" },
      embedding: { label: "向量嵌入", color: "bg-purple-100 text-purple-800" },
      tts: { label: "语音合成", color: "bg-green-100 text-green-800" },
      stt: { label: "语音识别", color: "bg-yellow-100 text-yellow-800" },
      image: { label: "图像生成", color: "bg-pink-100 text-pink-800" },
    };
    const config = typeMap[type];
    return <Badge variant="outline" className={config.color}>{config.label}</Badge>;
  };

  const getModelStatusBadge = (status: Model["status"]) => {
    switch (status) {
      case "active":
        return <Badge variant="default" className="bg-green-100 text-green-800">启用</Badge>;
      case "inactive":
        return <Badge variant="secondary">禁用</Badge>;
    }
  };

  const getModelTypeBadge = (type: Model["type"]) => {
    const typeMap = {
      text: { label: "文本生成", color: "bg-blue-100 text-blue-800" },
      embedding: { label: "向量嵌入", color: "bg-purple-100 text-purple-800" },
      image: { label: "图像生成", color: "bg-pink-100 text-pink-800" },
      tts: { label: "语音合成", color: "bg-green-100 text-green-800" },
      stt: { label: "语音识别", color: "bg-yellow-100 text-yellow-800" },
    };
    const config = typeMap[type];
    return <Badge variant="outline" className={config.color}>{config.label}</Badge>;
  };

  const filteredProviders = providers.filter(provider =>
    provider.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    provider.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    provider.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const providerModels = models.filter(model => 
    model.providerId === selectedProvider?.id &&
    (model.name.toLowerCase().includes(modelSearchQuery.toLowerCase()) ||
     model.displayName.toLowerCase().includes(modelSearchQuery.toLowerCase()))
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
              <h1 className="text-2xl font-bold text-gray-900">{selectedProvider.displayName}</h1>
              <p className="text-gray-600">{selectedProvider.description}</p>
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
                  <Label className="text-sm font-medium text-gray-700">类型</Label>
                  <div className="mt-1">
                    {getTypeBadge(selectedProvider.type)}
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
                      value={selectedProvider.apiUrl} 
                      readOnly 
                      className="text-sm"
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => copyToClipboard(selectedProvider.apiUrl)}
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
                      value={showApiKey ? selectedProvider.apiKey : maskApiKey(selectedProvider.apiKey)}
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
                      onClick={() => copyToClipboard(selectedProvider.apiKey)}
                    >
                      <Copy className="w-4 h-4" />
                    </Button>
                  </div>
                </div>

                <div>
                  <Label className="text-sm font-medium text-gray-700">最后检查</Label>
                  <div className="mt-1 text-sm text-gray-600">
                    {selectedProvider.lastHealthCheck}
                  </div>
                </div>

                <div className="pt-4 space-y-2">
                  <Button variant="outline" className="w-full">
                    <Zap className="w-4 h-4 mr-2" />
                    测试连接
                  </Button>
                  <Button variant="outline" className="w-full">
                    <Edit className="w-4 h-4 mr-2" />
                    编辑配置
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
                        <DialogTitle>为 {selectedProvider.displayName} 添加模型</DialogTitle>
                      </DialogHeader>
                      <div className="grid gap-4 py-4">
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelName" className="text-right">模型名称</Label>
                          <Input id="modelName" placeholder="例如: gpt-4" className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelDisplayName" className="text-right">显示名称</Label>
                          <Input id="modelDisplayName" placeholder="例如: GPT-4" className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="modelType" className="text-right">模型类型</Label>
                          <Select>
                            <SelectTrigger className="col-span-3">
                              <SelectValue placeholder="选择类型" />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="text">文本生成</SelectItem>
                              <SelectItem value="embedding">向量嵌入</SelectItem>
                              <SelectItem value="image">图像生成</SelectItem>
                              <SelectItem value="tts">语音合成</SelectItem>
                              <SelectItem value="stt">语音识别</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="maxTokens" className="text-right">最大Token</Label>
                          <Input id="maxTokens" placeholder="8192" className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="inputCost" className="text-right">输入费用</Label>
                          <Input id="inputCost" placeholder="0.03" className="col-span-3" />
                        </div>
                        <div className="grid grid-cols-4 items-center gap-4">
                          <Label htmlFor="outputCost" className="text-right">输出费用</Label>
                          <Input id="outputCost" placeholder="0.06" className="col-span-3" />
                        </div>
                      </div>
                      <div className="flex justify-end space-x-2">
                        <Button variant="outline" onClick={() => setIsAddModelOpen(false)}>取消</Button>
                        <Button onClick={() => setIsAddModelOpen(false)}>添加</Button>
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
                      <TableHead>最大Token</TableHead>
                      <TableHead>费用(千Token)</TableHead>
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
                              <div className="font-medium">{model.displayName}</div>
                              <div className="text-sm text-gray-500">{model.name}</div>
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
                          <div className="text-sm">{model.maxTokens.toLocaleString()}</div>
                        </TableCell>
                        <TableCell>
                          <div className="text-sm">
                            <div>输入: ${model.inputCost}</div>
                            {model.outputCost > 0 && (
                              <div>输出: ${model.outputCost}</div>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center space-x-2">
                            <Button variant="ghost" size="icon" title="编辑">
                              <Edit className="w-4 h-4" />
                            </Button>
                            <Button variant="ghost" size="icon" className="text-red-600" title="删除">
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
                <Input id="providerName" placeholder="例如: openai" className="col-span-3" />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="providerDisplayName" className="text-right">显示名称</Label>
                <Input id="providerDisplayName" placeholder="例如: OpenAI" className="col-span-3" />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="providerType" className="text-right">类型</Label>
                <Select>
                  <SelectTrigger className="col-span-3">
                    <SelectValue placeholder="选择服务商类型" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="llm">大语言模型</SelectItem>
                    <SelectItem value="embedding">向量嵌入</SelectItem>
                    <SelectItem value="tts">语音合成</SelectItem>
                    <SelectItem value="stt">语音识别</SelectItem>
                    <SelectItem value="image">图像生成</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="apiUrl" className="text-right">API地址</Label>
                <Input id="apiUrl" placeholder="https://api.example.com/v1" className="col-span-3" />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="apiKey" className="text-right">API Key</Label>
                <Input id="apiKey" placeholder="sk-..." className="col-span-3" />
              </div>
              <div className="grid grid-cols-4 items-center gap-4">
                <Label htmlFor="description" className="text-right">描述</Label>
                <Textarea id="description" placeholder="服务商描述" className="col-span-3" />
              </div>
            </div>
            <div className="flex justify-end space-x-2">
              <Button variant="outline" onClick={() => setIsAddProviderOpen(false)}>取消</Button>
              <Button onClick={() => setIsAddProviderOpen(false)}>添加</Button>
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
                    <CardTitle className="text-lg">{provider.displayName}</CardTitle>
                    <p className="text-sm text-gray-500">{provider.name}</p>
                  </div>
                </div>
                {getStatusBadge(provider.status)}
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <p className="text-sm text-gray-600 line-clamp-2">
                  {provider.description}
                </p>
                
                <div className="flex items-center justify-between">
                  {getTypeBadge(provider.type)}
                  <div className="text-sm text-gray-500">
                    {provider.modelsCount} 个模型
                  </div>
                </div>

                <div className="flex items-center justify-between text-sm text-gray-500">
                  <span>请求数: {provider.requestsCount.toLocaleString()}</span>
                  <span>最后检查: {provider.lastHealthCheck.split(' ')[1]}</span>
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