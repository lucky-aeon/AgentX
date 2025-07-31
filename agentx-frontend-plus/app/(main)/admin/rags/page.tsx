"use client";

import React, { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { 
  Search, 
  Eye, 
  RefreshCw, 
  CheckCircle, 
  XCircle, 
  Database,
  FileText,
  Trash2
} from "lucide-react";
import { 
  AdminRagService,
  RagVersionDTO,
  RagStatisticsDTO,
  PageResponse,
  GetRagVersionsParams,
  RagPublishStatus,
  getPublishStatusText,
  getPublishStatusColor,
  formatFileSize,
  getAllRagVersionsWithToast,
  getRagStatisticsWithToast,
  reviewRagVersionWithToast,
  batchReviewRagVersionsWithToast,
  removeRagVersionWithToast
} from "@/lib/admin-rag-service";
import { useToast } from "@/hooks/use-toast";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { RagReviewDialog } from "@/components/admin/rag/RagReviewDialog";
import { RagDetailsDialog } from "@/components/admin/rag/RagDetailsDialog";

export default function RagManagementPage() {
  const [rags, setRags] = useState<RagVersionDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [statusFilter, setStatusFilter] = useState<number | undefined>(undefined);
  const [selectedRags, setSelectedRags] = useState<string[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    size: 15,
    total: 0,
    pages: 0
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(15);
  const [statistics, setStatistics] = useState<RagStatisticsDTO>({
    totalRags: 0,
    pendingReview: 0,
    approved: 0,
    rejected: 0,
    removed: 0,
    totalInstalls: 0
  });
  const [selectedRag, setSelectedRag] = useState<RagVersionDTO | null>(null);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [reviewDialogOpen, setReviewDialogOpen] = useState(false);
  const [batchReviewDialogOpen, setBatchReviewDialogOpen] = useState(false);
  const [batchReviewStatus, setBatchReviewStatus] = useState<number>(RagPublishStatus.PUBLISHED);
  const [batchRejectReason, setBatchRejectReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { toast } = useToast();

  // 使用useCallback优化搜索参数
  const searchParams = React.useMemo(() => ({
    keyword: searchQuery || undefined,
    status: statusFilter,
    page: currentPage,
    pageSize: pageSize
  }), [searchQuery, statusFilter, currentPage, pageSize]);

  // 处理搜索输入的防抖动
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setSearchQuery(searchInput);
      setCurrentPage(1);
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchInput]);

  // 加载RAG数据
  const loadRags = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAllRagVersionsWithToast(searchParams);
      
      if (response.code === 200 && response.data) {
        setRags(response.data.records || []);
        setPagination({
          current: response.data.current,
          size: response.data.size,
          total: response.data.total,
          pages: response.data.pages
        });
      }
    } catch (error) {
      console.error('加载RAG列表失败:', error);
    } finally {
      setLoading(false);
    }
  }, [searchParams]);

  // 加载统计数据
  const loadStatistics = useCallback(async () => {
    try {
      const response = await getRagStatisticsWithToast();
      if (response.code === 200 && response.data) {
        setStatistics(response.data);
      }
    } catch (error) {
      console.error('加载统计数据失败:', error);
    }
  }, []);

  // 初始加载和依赖更新时重新加载
  useEffect(() => {
    loadRags();
  }, [loadRags]);

  // 只在组件挂载时加载统计数据
  useEffect(() => {
    loadStatistics();
  }, [loadStatistics]);

  // 处理搜索输入
  const handleSearchInputChange = useCallback((value: string) => {
    setSearchInput(value);
  }, []);

  // 处理状态筛选
  const handleStatusFilter = useCallback((value: string) => {
    const status = value === "all" ? undefined : parseInt(value);
    setStatusFilter(status);
    setCurrentPage(1);
  }, []);

  // 处理RAG选择
  const handleRagSelection = (ragId: string, checked: boolean) => {
    if (checked) {
      setSelectedRags([...selectedRags, ragId]);
    } else {
      setSelectedRags(selectedRags.filter(id => id !== ragId));
    }
  };

  // 全选/取消全选
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedRags(rags.map(rag => rag.id));
    } else {
      setSelectedRags([]);
    }
  };

  // 快速审核操作
  const handleQuickReview = async (rag: RagVersionDTO, status: RagPublishStatus) => {
    try {
      // 如果是拒绝状态，需要先打开详细审核对话框
      if (status === RagPublishStatus.REJECTED) {
        handleReviewRag(rag);
        return;
      }
      
      const response = await reviewRagVersionWithToast(rag.id, { status });
      if (response.code === 200) {
        loadRags();
        loadStatistics();
      }
    } catch (error) {
      console.error('快速审核失败:', error);
    }
  };

  // 查看详情
  const handleViewDetails = (rag: RagVersionDTO) => {
    setSelectedRag(rag);
    setDetailsDialogOpen(true);
  };

  // 打开审核对话框
  const handleReviewRag = (rag: RagVersionDTO) => {
    setSelectedRag(rag);
    setReviewDialogOpen(true);
  };

  // 批量审核
  const handleBatchReview = async () => {
    if (selectedRags.length === 0) {
      toast({
        title: "请选择RAG版本",
        description: "请先选择要审核的RAG版本",
        variant: "destructive",
      });
      return;
    }

    if (batchReviewStatus === RagPublishStatus.REJECTED && !batchRejectReason.trim()) {
      toast({
        title: "请填写拒绝原因",
        description: "拒绝时必须填写拒绝原因",
        variant: "destructive",
      });
      return;
    }

    try {
      setSubmitting(true);
      const response = await batchReviewRagVersionsWithToast({
        versionIds: selectedRags,
        status: batchReviewStatus,
        rejectReason: batchReviewStatus === RagPublishStatus.REJECTED ? batchRejectReason : undefined
      });
      
      if (response.code === 200) {
        setBatchReviewDialogOpen(false);
        setSelectedRags([]);
        setBatchRejectReason("");
        loadRags();
        loadStatistics();
      }
    } catch (error) {
      console.error('批量审核失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  // 下架RAG
  const handleRemoveRag = async (rag: RagVersionDTO) => {
    try {
      const response = await removeRagVersionWithToast(rag.id);
      if (response.code === 200) {
        loadRags();
        loadStatistics();
      }
    } catch (error) {
      console.error('下架失败:', error);
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
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      if (current <= 4) {
        for (let i = 2; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      } else if (current >= total - 3) {
        pages.push('...');
        for (let i = total - 4; i <= total; i++) {
          pages.push(i);
        }
      } else {
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
      {/* 页面标题 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">RAG管理</h1>
        <p className="text-gray-600 mt-1">管理平台中的所有RAG数据集和审核流程</p>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-blue-600">
              {statistics.totalRags}
            </div>
            <div className="text-sm text-gray-600">总RAG数</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-orange-600">
              {statistics.pendingReview}
            </div>
            <div className="text-sm text-gray-600">待审核</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-green-600">
              {statistics.approved}
            </div>
            <div className="text-sm text-gray-600">已发布</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-red-600">
              {statistics.rejected}
            </div>
            <div className="text-sm text-gray-600">已拒绝</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <div className="text-2xl font-bold text-gray-600">
              {statistics.removed}
            </div>
            <div className="text-sm text-gray-600">已下架</div>
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
                placeholder="搜索RAG名称、描述..."
                value={searchInput}
                onChange={(e) => handleSearchInputChange(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select 
              value={statusFilter?.toString() || "all"} 
              onValueChange={handleStatusFilter}
            >
              <SelectTrigger className="w-40">
                <SelectValue placeholder="状态筛选" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部状态</SelectItem>
                <SelectItem value={RagPublishStatus.REVIEWING.toString()}>审核中</SelectItem>
                <SelectItem value={RagPublishStatus.PUBLISHED.toString()}>已发布</SelectItem>
                <SelectItem value={RagPublishStatus.REJECTED.toString()}>已拒绝</SelectItem>
                <SelectItem value={RagPublishStatus.REMOVED.toString()}>已下架</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={() => loadRags()} disabled={loading}>
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              刷新
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 批量操作 */}
      {selectedRags.length > 0 && (
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-600">
                已选择 {selectedRags.length} 个RAG版本
              </div>
              <div className="flex space-x-2">
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setBatchReviewDialogOpen(true)}
                >
                  批量审核
                </Button>
                <Button 
                  variant="outline" 
                  size="sm"
                  onClick={() => setSelectedRags([])}
                >
                  取消选择
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* RAG列表 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>RAG列表 ({pagination.total})</span>
            <div className="flex items-center space-x-2">
              <Checkbox
                checked={selectedRags.length === rags.length && rags.length > 0}
                onCheckedChange={handleSelectAll}
              />
              <span className="text-sm text-gray-600">全选</span>
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>选择</TableHead>
                <TableHead>RAG信息</TableHead>
                <TableHead>创建者</TableHead>
                <TableHead>版本信息</TableHead>
                <TableHead>发布状态</TableHead>
                <TableHead>时间信息</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rags.map((rag) => (
                <TableRow key={rag.id}>
                  <TableCell>
                    <Checkbox
                      checked={selectedRags.includes(rag.id)}
                      onCheckedChange={(checked) => handleRagSelection(rag.id, checked as boolean)}
                    />
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
                        {rag.icon ? (
                          <img src={rag.icon} alt={rag.name} className="w-8 h-8 rounded" />
                        ) : (
                          <Database className="h-5 w-5 text-gray-400" />
                        )}
                      </div>
                      <div>
                        <div className="font-medium">{rag.name}</div>
                        <div className="text-sm text-gray-500 max-w-xs truncate">
                          {rag.description || "暂无描述"}
                        </div>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Avatar className="h-6 w-6">
                        <AvatarFallback>
                          {rag.userNickname?.charAt(0) || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <div className="text-sm font-medium">
                        {rag.userNickname || '未知用户'}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <div className="text-sm font-medium">v{rag.version}</div>
                      <div className="text-xs text-gray-500">
                        {rag.fileCount} 文件 • {rag.documentCount} 文档
                      </div>
                      <div className="text-xs text-gray-500">
                        {formatFileSize(rag.totalSize)}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="space-y-1">
                      <Badge className={getPublishStatusColor(rag.publishStatus)}>
                        {getPublishStatusText(rag.publishStatus)}
                      </Badge>
                      {rag.rejectReason && rag.publishStatus === RagPublishStatus.REJECTED && (
                        <div className="text-xs text-red-600 max-w-xs truncate">
                          原因: {rag.rejectReason}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm space-y-1">
                      <div>创建: {new Date(rag.createdAt).toLocaleDateString()}</div>
                      {rag.reviewTime && (
                        <div className="text-gray-500">
                          审核: {new Date(rag.reviewTime).toLocaleDateString()}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center space-x-2">
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        title="查看详情"
                        onClick={() => handleViewDetails(rag)}
                      >
                        <Eye className="w-4 h-4" />
                      </Button>
                      {rag.publishStatus === RagPublishStatus.REVIEWING && (
                        <>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="text-green-600 hover:text-green-700"
                            title="快速通过"
                            onClick={() => handleQuickReview(rag, RagPublishStatus.PUBLISHED)}
                          >
                            <CheckCircle className="w-4 h-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="text-red-600 hover:text-red-700"
                            title="拒绝审核（需填写原因）"
                            onClick={() => handleQuickReview(rag, RagPublishStatus.REJECTED)}
                          >
                            <XCircle className="w-4 h-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="text-blue-600 hover:text-blue-700"
                            title="详细审核"
                            onClick={() => handleReviewRag(rag)}
                          >
                            <Eye className="w-4 h-4" />
                          </Button>
                        </>
                      )}
                      {rag.publishStatus === RagPublishStatus.PUBLISHED && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          className="text-red-600 hover:text-red-700"
                          title="下架"
                          onClick={() => handleRemoveRag(rag)}
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          
          {rags.length === 0 && !loading && (
            <div className="text-center py-8 text-gray-500">
              {searchQuery ? "没有找到匹配的RAG版本" : "暂无RAG版本数据"}
            </div>
          )}
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

      {/* 批量审核对话框 */}
      <Dialog open={batchReviewDialogOpen} onOpenChange={setBatchReviewDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>批量审核RAG版本</DialogTitle>
            <DialogDescription>
              选择审核状态并填写相关信息
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="status">审核状态</Label>
              <Select 
                value={batchReviewStatus.toString()} 
                onValueChange={(value) => setBatchReviewStatus(parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={RagPublishStatus.PUBLISHED.toString()}>通过</SelectItem>
                  <SelectItem value={RagPublishStatus.REJECTED.toString()}>拒绝</SelectItem>
                  <SelectItem value={RagPublishStatus.REMOVED.toString()}>下架</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {batchReviewStatus === RagPublishStatus.REJECTED && (
              <div>
                <Label htmlFor="reason">拒绝原因</Label>
                <Textarea
                  id="reason"
                  placeholder="请输入拒绝原因"
                  value={batchRejectReason}
                  onChange={(e) => setBatchRejectReason(e.target.value)}
                />
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setBatchReviewDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleBatchReview} disabled={submitting}>
              {submitting ? "处理中..." : "确认审核"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* RAG详情对话框 */}
      <RagDetailsDialog
        open={detailsDialogOpen}
        onOpenChange={setDetailsDialogOpen}
        rag={selectedRag}
      />

      {/* RAG审核对话框 */}
      <RagReviewDialog
        open={reviewDialogOpen}
        onOpenChange={setReviewDialogOpen}
        rag={selectedRag}
        onReviewComplete={() => {
          loadRags();
          loadStatistics();
        }}
      />
    </div>
  );
}