"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Search, Plus, Edit, Trash2, Eye, Settings, Zap, Server } from "lucide-react";

interface Provider {
  id: string;
  name: string;
  displayName: string;
  type: "llm" | "embedding" | "tts" | "stt" | "image";
  status: "active" | "inactive" | "error";
  apiUrl: string;
  modelsCount: number;
  requestsCount: number;
  lastHealthCheck: string;
  createdAt: string;
  updatedAt: string;
  description: string;
}

export default function ProvidersPage() {
  const [providers, setProviders] = useState<Provider[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");

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
        modelsCount: 12,
        requestsCount: 15420,
        lastHealthCheck: "2024-01-21 10:30:00",
        createdAt: "2024-01-01",
        updatedAt: "2024-01-21",
        description: "OpenAI官方API服务",
      },
      {
        id: "2",
        name: "anthropic",
        displayName: "Anthropic",
        type: "llm",
        status: "active",
        apiUrl: "https://api.anthropic.com/v1",
        modelsCount: 5,
        requestsCount: 8924,
        lastHealthCheck: "2024-01-21 10:25:00",
        createdAt: "2024-01-05",
        updatedAt: "2024-01-20",
        description: "Anthropic Claude API服务",
      },
      {
        id: "3",
        name: "azure-openai",
        displayName: "Azure OpenAI",
        type: "llm",
        status: "active",
        apiUrl: "https://your-resource.openai.azure.com/",
        modelsCount: 8,
        requestsCount: 3456,
        lastHealthCheck: "2024-01-21 10:20:00",
        createdAt: "2024-01-10",
        updatedAt: "2024-01-19",
        description: "Azure OpenAI服务",
      },
      {
        id: "4",
        name: "google-vertex",
        displayName: "Google Vertex AI",
        type: "llm",
        status: "inactive",
        apiUrl: "https://us-central1-aiplatform.googleapis.com/v1",
        modelsCount: 6,
        requestsCount: 0,
        lastHealthCheck: "2024-01-18 15:30:00",
        createdAt: "2024-01-12",
        updatedAt: "2024-01-18",
        description: "Google Vertex AI服务",
      },
      {
        id: "5",
        name: "huggingface",
        displayName: "Hugging Face",
        type: "embedding",
        status: "error",
        apiUrl: "https://api-inference.huggingface.co/",
        modelsCount: 25,
        requestsCount: 1234,
        lastHealthCheck: "2024-01-21 09:45:00",
        createdAt: "2024-01-15",
        updatedAt: "2024-01-21",
        description: "Hugging Face推理API",
      },
      {
        id: "6",
        name: "elevenlabs",
        displayName: "ElevenLabs",
        type: "tts",
        status: "active",
        apiUrl: "https://api.elevenlabs.io/v1",
        modelsCount: 15,
        requestsCount: 892,
        lastHealthCheck: "2024-01-21 10:15:00",
        createdAt: "2024-01-08",
        updatedAt: "2024-01-20",
        description: "ElevenLabs语音合成服务",
      },
    ];

    setTimeout(() => {
      setProviders(mockProviders);
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

  const filteredProviders = providers.filter(provider =>
    provider.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    provider.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    provider.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">加载中...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题和操作 */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">服务商管理</h1>
          <p className="text-gray-600 mt-1">管理AI服务提供商和API配置</p>
        </div>
        <Button>
          <Plus className="w-4 h-4 mr-2" />
          新增服务商
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {providers.filter(p => p.status === "active").length}
            </div>
            <div className="text-sm text-gray-600">正常运行</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-gray-600">
              {providers.filter(p => p.status === "inactive").length}
            </div>
            <div className="text-sm text-gray-600">未激活</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {providers.filter(p => p.status === "error").length}
            </div>
            <div className="text-sm text-gray-600">错误状态</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {providers.reduce((sum, p) => sum + p.requestsCount, 0)}
            </div>
            <div className="text-sm text-gray-600">总请求数</div>
          </CardContent>
        </Card>
      </div>

      {/* 搜索和过滤 */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center space-x-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="搜索服务商名称或描述..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
            <Button variant="outline">
              筛选
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 服务商列表 */}
      <Card>
        <CardHeader>
          <CardTitle>服务商列表 ({filteredProviders.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>服务商信息</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>模型数量</TableHead>
                <TableHead>请求数</TableHead>
                <TableHead>最后检查</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredProviders.map((provider) => (
                <TableRow key={provider.id}>
                  <TableCell>
                    <div className="flex items-center space-x-3">
                      <Avatar className="h-10 w-10">
                        <AvatarImage src={`/providers/${provider.name}-logo.png`} />
                        <AvatarFallback>
                          <Server className="h-5 w-5" />
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{provider.displayName}</div>
                        <div className="text-sm text-gray-500">
                          {provider.name}
                        </div>
                        <div className="text-xs text-gray-400 mt-1 max-w-xs truncate">
                          {provider.description}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    {getTypeBadge(provider.type)}
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(provider.status)}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{provider.modelsCount}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{provider.requestsCount.toLocaleString()}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{provider.lastHealthCheck}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button variant="ghost" size="icon" title="查看详情">
                        <Eye className="w-4 h-4" />
                      </Button>
                      <Button variant="ghost" size="icon" title="配置">
                        <Settings className="w-4 h-4" />
                      </Button>
                      <Button variant="ghost" size="icon" title="测试连接">
                        <Zap className="w-4 h-4" />
                      </Button>
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
  );
}