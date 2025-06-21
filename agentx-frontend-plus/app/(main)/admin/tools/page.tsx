"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Search, Plus, Edit, Trash2, Eye, CheckCircle, XCircle } from "lucide-react";

interface Tool {
  id: string;
  name: string;
  description: string;
  author: string;
  version: string;
  status: "published" | "pending" | "rejected";
  downloads: number;
  createdAt: string;
  updatedAt: string;
  category: string;
}

export default function ToolsPage() {
  const [tools, setTools] = useState<Tool[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");

  // 模拟数据加载
  useEffect(() => {
    const mockTools: Tool[] = [
      {
        id: "1",
        name: "文本处理工具",
        description: "用于处理和分析文本内容的工具",
        author: "开发者A",
        version: "1.0.0",
        status: "published",
        downloads: 256,
        createdAt: "2024-01-01",
        updatedAt: "2024-01-15",
        category: "文本处理",
      },
      {
        id: "2",
        name: "图像识别API",
        description: "基于AI的图像识别和分析工具",
        author: "开发者B",
        version: "2.1.0",
        status: "published",
        downloads: 142,
        createdAt: "2024-01-05",
        updatedAt: "2024-01-18",
        category: "AI工具",
      },
      {
        id: "3",
        name: "数据库查询工具",
        description: "简化数据库查询操作的工具",
        author: "开发者C",
        version: "1.2.0",
        status: "pending",
        downloads: 0,
        createdAt: "2024-01-10",
        updatedAt: "2024-01-20",
        category: "数据库",
      },
      {
        id: "4",
        name: "天气查询服务",
        description: "获取实时天气信息的工具",
        author: "开发者D",
        version: "1.0.1",
        status: "rejected",
        downloads: 0,
        createdAt: "2024-01-12",
        updatedAt: "2024-01-19",
        category: "API服务",
      },
    ];

    setTimeout(() => {
      setTools(mockTools);
      setLoading(false);
    }, 1000);
  }, []);

  const getStatusBadge = (status: Tool["status"]) => {
    switch (status) {
      case "published":
        return <Badge variant="default" className="bg-green-100 text-green-800">已发布</Badge>;
      case "pending":
        return <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">待审核</Badge>;
      case "rejected":
        return <Badge variant="destructive">已拒绝</Badge>;
    }
  };

  const filteredTools = tools.filter(tool =>
    tool.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    tool.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    tool.author.toLowerCase().includes(searchQuery.toLowerCase())
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
          <h1 className="text-2xl font-bold text-gray-900">工具管理</h1>
          <p className="text-gray-600 mt-1">管理平台中的所有工具和插件</p>
        </div>
        <Button>
          <Plus className="w-4 h-4 mr-2" />
          新增工具
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {tools.filter(t => t.status === "published").length}
            </div>
            <div className="text-sm text-gray-600">已发布</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-yellow-600">
              {tools.filter(t => t.status === "pending").length}
            </div>
            <div className="text-sm text-gray-600">待审核</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {tools.filter(t => t.status === "rejected").length}
            </div>
            <div className="text-sm text-gray-600">已拒绝</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {tools.reduce((sum, t) => sum + t.downloads, 0)}
            </div>
            <div className="text-sm text-gray-600">总下载量</div>
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
                placeholder="搜索工具名称、描述或作者..."
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

      {/* 工具列表 */}
      <Card>
        <CardHeader>
          <CardTitle>工具列表 ({filteredTools.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>工具信息</TableHead>
                <TableHead>作者</TableHead>
                <TableHead>版本</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>下载量</TableHead>
                <TableHead>更新时间</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredTools.map((tool) => (
                <TableRow key={tool.id}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{tool.name}</div>
                      <div className="text-sm text-gray-500 max-w-xs truncate">
                        {tool.description}
                      </div>
                      <div className="text-xs text-gray-400 mt-1">
                        分类: {tool.category}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{tool.author}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm font-mono">{tool.version}</div>
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(tool.status)}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{tool.downloads}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{tool.updatedAt}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button variant="ghost" size="icon" title="查看详情">
                        <Eye className="w-4 h-4" />
                      </Button>
                      <Button variant="ghost" size="icon" title="编辑">
                        <Edit className="w-4 h-4" />
                      </Button>
                      {tool.status === "pending" && (
                        <>
                          <Button variant="ghost" size="icon" className="text-green-600" title="批准">
                            <CheckCircle className="w-4 h-4" />
                          </Button>
                          <Button variant="ghost" size="icon" className="text-red-600" title="拒绝">
                            <XCircle className="w-4 h-4" />
                          </Button>
                        </>
                      )}
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