"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Search, Plus, Edit, Trash2, Eye, CheckCircle, XCircle, Bot } from "lucide-react";

interface Agent {
  id: string;
  name: string;
  description: string;
  avatar?: string;
  author: string;
  status: "published" | "pending" | "rejected" | "private";
  conversations: number;
  createdAt: string;
  updatedAt: string;
  category: string;
  isPublic: boolean;
}

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");

  // 模拟数据加载
  useEffect(() => {
    const mockAgents: Agent[] = [
      {
        id: "1",
        name: "编程助手",
        description: "专业的编程代码助手，帮助开发者编写和优化代码",
        avatar: "",
        author: "admin",
        status: "published",
        conversations: 1245,
        createdAt: "2024-01-01",
        updatedAt: "2024-01-15",
        category: "编程开发",
        isPublic: true,
      },
      {
        id: "2",
        name: "文案创作助手",
        description: "AI文案创作专家，帮助用户撰写各类文案和文章",
        avatar: "",
        author: "user123",
        status: "published",
        conversations: 892,
        createdAt: "2024-01-05",
        updatedAt: "2024-01-18",
        category: "内容创作",
        isPublic: true,
      },
      {
        id: "3",
        name: "数据分析师",
        description: "专业的数据分析和可视化助手",
        avatar: "",
        author: "analyst",
        status: "pending",
        conversations: 0,
        createdAt: "2024-01-10",
        updatedAt: "2024-01-20",
        category: "数据分析",
        isPublic: false,
      },
      {
        id: "4",
        name: "学习伙伴",
        description: "个人学习辅导助手",
        avatar: "",
        author: "teacher",
        status: "private",
        conversations: 156,
        createdAt: "2024-01-12",
        updatedAt: "2024-01-19",
        category: "教育学习",
        isPublic: false,
      },
      {
        id: "5",
        name: "翻译专家",
        description: "多语言翻译助手",
        avatar: "",
        author: "translator",
        status: "rejected",
        conversations: 0,
        createdAt: "2024-01-14",
        updatedAt: "2024-01-21",
        category: "语言工具",
        isPublic: false,
      },
    ];

    setTimeout(() => {
      setAgents(mockAgents);
      setLoading(false);
    }, 1000);
  }, []);

  const getStatusBadge = (status: Agent["status"]) => {
    switch (status) {
      case "published":
        return <Badge variant="default" className="bg-green-100 text-green-800">已发布</Badge>;
      case "pending":
        return <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">待审核</Badge>;
      case "rejected":
        return <Badge variant="destructive">已拒绝</Badge>;
      case "private":
        return <Badge variant="outline">私有</Badge>;
    }
  };

  const filteredAgents = agents.filter(agent =>
    agent.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    agent.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    agent.author.toLowerCase().includes(searchQuery.toLowerCase())
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
          <h1 className="text-2xl font-bold text-gray-900">Agent管理</h1>
          <p className="text-gray-600 mt-1">管理平台中的所有AI助手</p>
        </div>
        <Button>
          <Plus className="w-4 h-4 mr-2" />
          新增Agent
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {agents.filter(a => a.status === "published").length}
            </div>
            <div className="text-sm text-gray-600">已发布</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-yellow-600">
              {agents.filter(a => a.status === "pending").length}
            </div>
            <div className="text-sm text-gray-600">待审核</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-gray-600">
              {agents.filter(a => a.status === "private").length}
            </div>
            <div className="text-sm text-gray-600">私有</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {agents.filter(a => a.status === "rejected").length}
            </div>
            <div className="text-sm text-gray-600">已拒绝</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {agents.reduce((sum, a) => sum + a.conversations, 0)}
            </div>
            <div className="text-sm text-gray-600">总对话数</div>
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
                placeholder="搜索Agent名称、描述或作者..."
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

      {/* Agent列表 */}
      <Card>
        <CardHeader>
          <CardTitle>Agent列表 ({filteredAgents.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Agent信息</TableHead>
                <TableHead>作者</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>可见性</TableHead>
                <TableHead>对话数</TableHead>
                <TableHead>更新时间</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredAgents.map((agent) => (
                <TableRow key={agent.id}>
                  <TableCell>
                    <div className="flex items-center space-x-3">
                      <Avatar className="h-10 w-10">
                        <AvatarImage src={agent.avatar} />
                        <AvatarFallback>
                          <Bot className="h-5 w-5" />
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-medium">{agent.name}</div>
                        <div className="text-sm text-gray-500 max-w-xs truncate">
                          {agent.description}
                        </div>
                        <div className="text-xs text-gray-400 mt-1">
                          分类: {agent.category}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{agent.author}</div>
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(agent.status)}
                  </TableCell>
                  <TableCell>
                    <Badge variant={agent.isPublic ? "default" : "secondary"}>
                      {agent.isPublic ? "公开" : "私有"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{agent.conversations}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{agent.updatedAt}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button variant="ghost" size="icon" title="查看详情">
                        <Eye className="w-4 h-4" />
                      </Button>
                      <Button variant="ghost" size="icon" title="编辑">
                        <Edit className="w-4 h-4" />
                      </Button>
                      {agent.status === "pending" && (
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