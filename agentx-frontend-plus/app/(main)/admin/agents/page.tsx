"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Search, Plus, Edit, Trash2, Eye, CheckCircle, XCircle, Bot, RefreshCw } from "lucide-react";
import { AdminAgentService, Agent, GetAgentsParams, PageResponse, AgentStatistics } from "@/lib/admin-agent-service";
import { useToast } from "@/hooks/use-toast";
import { AgentVersionsDialog } from "@/components/admin/AgentVersionsDialog";

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<boolean | undefined>(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    size: 15,
    total: 0,
    pages: 0
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);
  const [selectedAgent, setSelectedAgent] = useState<{ id: string; name: string } | null>(null);
  const [versionsDialogOpen, setVersionsDialogOpen] = useState(false);
  const [statistics, setStatistics] = useState<AgentStatistics>({
    totalAgents: 0,
    enabledAgents: 0,
    disabledAgents: 0,
    pendingVersions: 0
  });
  const { toast } = useToast();

  // 加载Agent数据
  const loadAgents = async () => {
    try {
      setLoading(true);
      const params: GetAgentsParams = {
        keyword: searchQuery || undefined,
        enabled: statusFilter,
        page: currentPage,
        pageSize: pageSize
      };
      
      const response = await AdminAgentService.getAgents(params);
      
      if (response.code === 200 && response.data) {
        setAgents(response.data.records);
        setPagination({
          current: response.data.current,
          size: response.data.size,
          total: response.data.total,
          pages: response.data.pages
        });
      } else {
        toast({
          variant: "destructive",
          title: "获取Agent列表失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
      console.error('加载Agent列表失败:', error);
      toast({
        variant: "destructive",
        title: "获取Agent列表失败",
        description: "网络连接异常，请稍后重试"
      });
    } finally {
      setLoading(false);
    }
  };

  // 加载统计数据
  const loadStatistics = async () => {
    try {
      const response = await AdminAgentService.getAgentStatistics();
      if (response.code === 200 && response.data) {
        setStatistics(response.data);
      }
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  };

  // 初始加载和依赖更新时重新加载  
  useEffect(() => {
    loadAgents();
    loadStatistics();
  }, [searchQuery, statusFilter, currentPage, pageSize]);

  const getStatusBadge = (enabled: boolean) => {
    return enabled ? (
      <Badge variant="default" className="bg-green-100 text-green-800">启用</Badge>
    ) : (
      <Badge variant="destructive">禁用</Badge>
    );
  };

  // 获取版本状态统计
  const getVersionStats = (versions: AgentVersion[]) => {
    const stats = {
      pending: 0,    // 待审核
      published: 0,  // 已发布
      rejected: 0,   // 已拒绝
      removed: 0     // 已下架
    };

    versions.forEach(version => {
      switch (version.publishStatus) {
        case 1: stats.pending++; break;
        case 2: stats.published++; break;
        case 3: stats.rejected++; break;
        case 4: stats.removed++; break;
      }
    });

    return stats;
  };

  // 获取最新待审核版本
  const getLatestPendingVersion = (versions: AgentVersion[]) => {
    return versions
      .filter(v => v.publishStatus === 1)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];
  };

  // 获取审核状态Badge
  const getReviewStatusBadge = (versions: AgentVersion[]) => {
    const stats = getVersionStats(versions);
    const latestPending = getLatestPendingVersion(versions);

    if (stats.pending > 0) {
      return (
        <div className="space-y-1">
          <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">
            🕒 {stats.pending}个待审核
          </Badge>
          {latestPending && (
            <div className="text-xs text-gray-500">
              最新: v{latestPending.versionNumber}
            </div>
          )}
        </div>
      );
    } else if (stats.published > 0) {
      return <Badge variant="default" className="bg-green-100 text-green-800">✅ 已发布版本</Badge>;
    } else if (stats.rejected > 0) {
      return <Badge variant="destructive">❌ 有拒绝版本</Badge>;
    } else {
      return <Badge variant="outline">📝 暂无版本</Badge>;
    }
  };

  // 处理搜索输入延迟
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    // 重置到第一页
    setCurrentPage(1);
  };

  // 处理状态筛选
  const handleStatusFilter = (value: string) => {
    const enabled = value === "enabled" ? true : value === "disabled" ? false : undefined;
    setStatusFilter(enabled);
    // 重置到第一页
    setCurrentPage(1);
  };

  // 打开版本管理Dialog
  const handleViewVersions = (agent: Agent) => {
    setSelectedAgent({ id: agent.id, name: agent.name });
    setVersionsDialogOpen(true);
  };

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
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {statistics.totalAgents}
            </div>
            <div className="text-sm text-gray-600">总Agent数</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {statistics.enabledAgents}
            </div>
            <div className="text-sm text-gray-600">启用中</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {statistics.disabledAgents}
            </div>
            <div className="text-sm text-gray-600">已禁用</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-orange-600">
              {statistics.pendingVersions}
            </div>
            <div className="text-sm text-gray-600">待审核</div>
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
                placeholder="搜索Agent名称、描述..."
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={statusFilter === undefined ? "all" : statusFilter ? "enabled" : "disabled"} onValueChange={handleStatusFilter}>
              <SelectTrigger className="w-32">
                <SelectValue placeholder="状态筛选" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value="enabled">启用</SelectItem>
                <SelectItem value="disabled">禁用</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={loadAgents} disabled={loading}>
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Agent列表 */}
      <Card>
        <CardHeader>
          <CardTitle>Agent列表 ({pagination.total})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Agent信息</TableHead>
                <TableHead>创建者</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>版本信息</TableHead>
                <TableHead>审核状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {agents.map((agent) => (
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
                          {agent.description || "暂无描述"}
                        </div>
                        <div className="text-xs text-gray-400 mt-1">
                          ID: {agent.id} | 版本数: {agent.versions?.length || 0}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Avatar className="h-6 w-6">
                        <AvatarImage src={agent.userAvatarUrl} />
                        <AvatarFallback>
                          {agent.userNickname?.charAt(0) || agent.userEmail?.charAt(0) || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="text-sm font-medium">
                          {agent.userNickname || agent.userEmail || '未知用户'}
                        </div>
                        <div className="text-xs text-gray-400 font-mono">
                          {agent.userId}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(agent.enabled)}
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <div className="text-sm">
                        总版本: {agent.versions?.length || 0}
                      </div>
                      {agent.versions && agent.versions.length > 0 && (
                        <div className="text-xs text-gray-500">
                          {(() => {
                            const stats = getVersionStats(agent.versions);
                            const parts = [];
                            if (stats.published > 0) parts.push(`已发布: ${stats.published}`);
                            if (stats.pending > 0) parts.push(`待审核: ${stats.pending}`);
                            if (stats.rejected > 0) parts.push(`已拒绝: ${stats.rejected}`);
                            return parts.join(' | ');
                          })()}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    {agent.versions ? getReviewStatusBadge(agent.versions) : (
                      <Badge variant="outline">📝 暂无版本</Badge>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{new Date(agent.createdAt).toLocaleDateString()}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        title="查看版本"
                        onClick={() => handleViewVersions(agent)}
                      >
                        <Eye className="w-4 h-4" />
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        className={agent.enabled ? "text-red-600" : "text-green-600"} 
                        title={agent.enabled ? "禁用" : "启用"}
                      >
                        {agent.enabled ? <XCircle className="w-4 h-4" /> : <CheckCircle className="w-4 h-4" />}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Agent版本管理Dialog */}
      {selectedAgent && (
        <AgentVersionsDialog
          open={versionsDialogOpen}
          onOpenChange={setVersionsDialogOpen}
          agentId={selectedAgent.id}
          agentName={selectedAgent.name}
        />
      )}
    </div>
  );
}