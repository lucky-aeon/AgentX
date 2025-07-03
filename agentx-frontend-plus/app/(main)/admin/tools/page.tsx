"use client";

import React, { useState, useEffect, useRef, useMemo, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Search, Eye, RefreshCw, Code, Github, ExternalLink, CheckCircle, XCircle, Plus } from "lucide-react";
import { 
  AdminToolService, 
  Tool, 
  GetToolsParams, 
  PageResponse, 
  ToolStatistics, 
  ToolStatus,
  getToolStatusText,
  getToolStatusColor
} from "@/lib/admin-tool-service";
import { useToast } from "@/hooks/use-toast";
import { ToolReviewDialog } from "@/components/admin/ToolReviewDialog";
import { ToolDetailsDialog } from "@/components/admin/ToolDetailsDialog";
import Link from "next/link";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

export default function ToolsPage() {
  const [tools, setTools] = useState<Tool[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const searchTimeoutRef = useRef<NodeJS.Timeout>();
  const [statusFilter, setStatusFilter] = useState<ToolStatus | undefined>(undefined);
  const [officeFilter, setOfficeFilter] = useState<boolean | undefined>(undefined);
  const [pagination, setPagination] = useState({
    current: 1,
    size: 15,
    total: 0,
    pages: 0
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);
  const [selectedTool, setSelectedTool] = useState<Tool | null>(null);
  const [reviewDialogOpen, setReviewDialogOpen] = useState(false);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [updatingToolIds, setUpdatingToolIds] = useState<Set<string>>(new Set());
  const [statistics, setStatistics] = useState<ToolStatistics>({
    totalTools: 0,
    pendingReviewTools: 0,
    manualReviewTools: 0,
    approvedTools: 0,
    failedTools: 0,
    officialTools: 0
  });
  const { toast } = useToast();

  // 初始化时同步searchInput和searchQuery
  useEffect(() => {
    setSearchInput(searchQuery);
  }, []);

  // 使用useMemo优化搜索参数
  const searchParams = useMemo(() => ({
    keyword: searchQuery || undefined,
    status: statusFilter,
    isOffice: officeFilter,
    page: currentPage,
    pageSize: pageSize
  }), [searchQuery, statusFilter, officeFilter, currentPage, pageSize]);

  // 处理搜索输入的防抖动
  useEffect(() => {
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }
    
    searchTimeoutRef.current = setTimeout(() => {
      setSearchQuery(searchInput);
      setCurrentPage(1);
    }, 500);

    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, [searchInput]);

  // 加载工具数据
  const loadTools = useCallback(async () => {
    try {
      setLoading(true);
      const response = await AdminToolService.getTools(searchParams);
      
      if (response.code === 200 && response.data) {
        setTools(response.data.records);
        setPagination({
          current: response.data.current,
          size: response.data.size,
          total: response.data.total,
          pages: response.data.pages
        });
      } else {
        toast({
          variant: "destructive",
          title: "获取工具列表失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
      console.error('加载工具列表失败:', error);
      toast({
        variant: "destructive",
        title: "获取工具列表失败",
        description: "网络连接异常，请稍后重试"
      });
    } finally {
      setLoading(false);
    }
  }, [searchParams, toast]);

  // 加载统计数据
  const loadStatistics = useCallback(async () => {
    try {
      const response = await AdminToolService.getToolStatistics();
      if (response.code === 200 && response.data) {
        setStatistics(response.data);
      }
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  }, []);

  // 初始加载和依赖更新时重新加载  
  useEffect(() => {
    loadTools();
  }, [loadTools]);

  // 只在组件挂载时加载统计数据
  useEffect(() => {
    loadStatistics();
  }, [loadStatistics]);

  // 使用useCallback优化搜索处理函数
  const handleSearchInputChange = useCallback((value: string) => {
    setSearchInput(value);
  }, []);

  // 使用useCallback优化状态筛选处理函数
  const handleStatusFilter = useCallback((value: string) => {
    const status = value === "all" ? undefined : (value as ToolStatus);
    setStatusFilter(status);
    setCurrentPage(1);
  }, []);

  // 处理官方工具筛选
  const handleOfficeFilter = useCallback((value: string) => {
    const isOffice = value === "all" ? undefined : value === "official";
    setOfficeFilter(isOffice);
    setCurrentPage(1);
  }, []);

  // 打开审核对话框
  const handleReviewTool = (tool: Tool) => {
    setSelectedTool(tool);
    setReviewDialogOpen(true);
  };

  // 打开详情对话框
  const handleViewDetails = (tool: Tool) => {
    setSelectedTool(tool);
    setDetailsDialogOpen(true);
  };

  // 审核完成后刷新列表
  const handleReviewComplete = () => {
    loadTools();
    loadStatistics();
  };

  // 切换工具全局状态（乐观更新）
  const handleToggleGlobalStatus = async (tool: Tool) => {
    // 防止重复操作
    if (updatingToolIds.has(tool.id)) {
      return;
    }

    const newGlobalStatus = !tool.isGlobal;
    const originalTool = { ...tool };
    
    // 1. 添加加载状态
    setUpdatingToolIds(prev => new Set(prev).add(tool.id));
    
    // 2. 立即更新UI（乐观更新）
    setTools(prevTools => prevTools.map(t => 
      t.id === tool.id 
        ? { ...t, isGlobal: newGlobalStatus }
        : t
    ));

    try {
      // 3. 调用API
      const response = await AdminToolService.updateToolGlobalStatus(tool.id, newGlobalStatus);
      
      if (response.code === 200) {
        // 4. 成功时不显示提示（UI已经反映了变化）
        // 用户能直接看到Switch状态和文字的变化，无需额外提示
      } else {
        // 5. API返回错误时回滚UI
        setTools(prevTools => prevTools.map(t => 
          t.id === tool.id ? originalTool : t
        ));
        toast({
          variant: "destructive",
          title: "操作失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
      // 6. 网络错误时回滚UI
      setTools(prevTools => prevTools.map(t => 
        t.id === tool.id ? originalTool : t
      ));
      console.error('切换工具全局状态失败:', error);
      toast({
        variant: "destructive",
        title: "操作失败",
        description: "网络连接异常，请稍后重试"
      });
    } finally {
      // 7. 移除加载状态
      setUpdatingToolIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(tool.id);
        return newSet;
      });
    }
  };


  // 快速审核操作
  const handleQuickReview = async (tool: Tool, status: ToolStatus) => {
    try {
      const response = await AdminToolService.updateToolStatus(tool.id, status);
      if (response.code === 200) {
        toast({
          title: "操作成功",
          description: status === ToolStatus.APPROVED ? "工具已通过审核" : "工具已拒绝"
        });
        loadTools();
        loadStatistics();
      } else {
        toast({
          variant: "destructive",
          title: "操作失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
      console.error('审核操作失败:', error);
      toast({
        variant: "destructive",
        title: "操作失败",
        description: "网络连接异常，请稍后重试"
      });
    }
  };

  // 处理分页点击
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pagination.pages) return;
    setCurrentPage(page);
  };

  // 生成分页页码数组
  const generatePageNumbers = () => {
    const current = pagination.current;
    const total = pagination.pages;
    const pages: (number | string)[] = [];

    if (total <= 7) {
      // 如果总页数少于等于7，显示所有页码
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      // 总是显示第一页
      pages.push(1);

      if (current <= 4) {
        // 当前页在前面
        for (let i = 2; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      } else if (current >= total - 3) {
        // 当前页在后面
        pages.push('...');
        for (let i = total - 4; i <= total; i++) {
          pages.push(i);
        }
      } else {
        // 当前页在中间
        pages.push('...');
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      }
    }

    return pages;
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
          <h1 className="text-2xl font-bold text-gray-900">工具管理</h1>
          <p className="text-gray-600 mt-1">管理平台中的所有开发工具和审核流程</p>
        </div>
        <Button asChild>
          <Link href="/admin/tools/upload">
            <Plus className="w-4 h-4 mr-2" />
            创建官方工具
          </Link>
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-6 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {statistics.totalTools}
            </div>
            <div className="text-sm text-gray-600">总工具数</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-orange-600">
              {statistics.pendingReviewTools}
            </div>
            <div className="text-sm text-gray-600">待审核</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-yellow-600">
              {statistics.manualReviewTools}
            </div>
            <div className="text-sm text-gray-600">人工审核</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {statistics.approvedTools}
            </div>
            <div className="text-sm text-gray-600">已通过</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {statistics.failedTools}
            </div>
            <div className="text-sm text-gray-600">审核失败</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-purple-600">
              {statistics.officialTools}
            </div>
            <div className="text-sm text-gray-600">官方工具</div>
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
                placeholder="搜索工具名称、描述..."
                value={searchInput}
                onChange={(e) => handleSearchInputChange(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select 
              value={statusFilter || "all"} 
              onValueChange={handleStatusFilter}
            >
              <SelectTrigger className="w-40">
                <SelectValue placeholder="状态筛选" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value={ToolStatus.WAITING_REVIEW}>等待审核</SelectItem>
                <SelectItem value={ToolStatus.MANUAL_REVIEW}>人工审核</SelectItem>
                <SelectItem value={ToolStatus.APPROVED}>已通过</SelectItem>
                <SelectItem value={ToolStatus.FAILED}>审核失败</SelectItem>
              </SelectContent>
            </Select>
            <Select 
              value={officeFilter === undefined ? "all" : officeFilter ? "official" : "third-party"} 
              onValueChange={handleOfficeFilter}
            >
              <SelectTrigger className="w-32">
                <SelectValue placeholder="工具类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部类型</SelectItem>
                <SelectItem value="official">官方</SelectItem>
                <SelectItem value="third-party">第三方</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={() => loadTools()} disabled={loading}>
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 工具列表 */}
      <Card>
        <CardHeader>
          <CardTitle>工具列表 ({pagination.total})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>工具信息</TableHead>
                <TableHead>创建者</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>范围</TableHead>
                <TableHead>审核状态</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tools.map((tool) => (
                <TableRow key={tool.id}>
                  <TableCell>
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
                        {tool.icon ? (
                          <img src={tool.icon} alt={tool.name} className="w-8 h-8 rounded" />
                        ) : (
                          <Code className="h-5 w-5 text-gray-400" />
                        )}
                      </div>
                      <div>
                        <div className="font-medium">{tool.name}</div>
                        <div className="text-sm text-gray-500 max-w-xs truncate">
                          {tool.description || "暂无描述"}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Avatar className="h-6 w-6">
                        <AvatarImage src={tool.userAvatarUrl} />
                        <AvatarFallback>
                          {tool.userNickname?.charAt(0) || tool.userEmail?.charAt(0) || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div className="text-sm font-medium">
                        {tool.userNickname || tool.userEmail || '未知用户'}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <Badge variant={tool.isOffice ? "default" : "outline"}>
                        {tool.isOffice ? "官方" : "第三方"}
                      </Badge>
                      <div className="text-xs text-gray-500">{tool.toolType}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Switch
                        checked={tool.isGlobal || false}
                        onCheckedChange={() => handleToggleGlobalStatus(tool)}
                        disabled={updatingToolIds.has(tool.id)}
                        size="sm"
                      />
                      <span className={`text-sm ${updatingToolIds.has(tool.id) ? 'text-gray-400' : 'text-gray-600'}`}>
                        {updatingToolIds.has(tool.id) ? '更新中...' : (tool.isGlobal ? "全局" : "用户")}
                      </span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <Badge className={getToolStatusColor(tool.status)}>
                        {getToolStatusText(tool.status)}
                      </Badge>
                      {tool.rejectReason && tool.status === ToolStatus.FAILED && (
                        <div className="text-xs text-red-600 max-w-xs truncate">
                          原因: {tool.rejectReason}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{new Date(tool.createdAt).toLocaleDateString()}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        title="查看详情"
                        onClick={() => handleViewDetails(tool)}
                      >
                        <Eye className="w-4 h-4" />
                      </Button>
                      {(tool.status === ToolStatus.WAITING_REVIEW || tool.status === ToolStatus.MANUAL_REVIEW) && (
                        <>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="text-green-600 hover:text-green-700"
                            title="快速通过"
                            onClick={() => handleQuickReview(tool, ToolStatus.APPROVED)}
                          >
                            <CheckCircle className="w-4 h-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="text-red-600 hover:text-red-700"
                            title="快速拒绝"
                            onClick={() => handleQuickReview(tool, ToolStatus.FAILED)}
                          >
                            <XCircle className="w-4 h-4" />
                          </Button>
                        </>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* 分页组件 */}
      {pagination.pages > 1 && (
        <div className="flex justify-center">
          <Pagination>
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious 
                  onClick={() => handlePageChange(pagination.current - 1)}
                  className={pagination.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                />
              </PaginationItem>
              
              {generatePageNumbers().map((page, index) => (
                <PaginationItem key={index}>
                  {page === '...' ? (
                    <PaginationEllipsis />
                  ) : (
                    <PaginationLink
                      onClick={() => handlePageChange(page as number)}
                      isActive={page === pagination.current}
                      className="cursor-pointer"
                    >
                      {page}
                    </PaginationLink>
                  )}
                </PaginationItem>
              ))}
              
              <PaginationItem>
                <PaginationNext 
                  onClick={() => handlePageChange(pagination.current + 1)}
                  className={pagination.current >= pagination.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      )}

      {/* 工具详情对话框 */}
      <ToolDetailsDialog
        open={detailsDialogOpen}
        onOpenChange={setDetailsDialogOpen}
        tool={selectedTool}
      />

      {/* 工具审核对话框 */}
      <ToolReviewDialog
        open={reviewDialogOpen}
        onOpenChange={setReviewDialogOpen}
        tool={selectedTool}
        onReviewComplete={handleReviewComplete}
      />
    </div>
  );
}