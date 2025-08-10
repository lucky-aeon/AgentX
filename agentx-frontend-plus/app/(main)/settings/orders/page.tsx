"use client";

import { useState, useEffect } from "react";
import { Package, Calendar, DollarSign, RefreshCw, Search, Eye, FileText, ExternalLink } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { 
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationEllipsis
} from "@/components/ui/pagination";
import { toast } from "@/hooks/use-toast";

import { Order, GetUserOrdersParams, PageResponse, ORDER_STATUS_NAMES, ORDER_STATUS_VARIANTS, ORDER_TYPE_NAMES, PAYMENT_PLATFORM_NAMES } from "@/types/order";
import { getUserOrdersWithToast, getOrderDetailWithToast } from "@/lib/order-service";

export default function OrdersPage() {
  
  // 订单相关状态
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(false);
  const [pageData, setPageData] = useState<PageResponse<Order>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  
  // 搜索条件
  const [searchQuery, setSearchQuery] = useState("");
  
  // 详情弹窗
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  // 格式化时间
  const formatDateTime = (dateTime?: string) => {
    if (!dateTime) return "暂无";
    return new Date(dateTime).toLocaleString('zh-CN');
  };

  // 格式化金额
  const formatAmount = (amount: number, currency: string = 'CNY') => {
    if (currency === 'CNY') {
      return `¥${amount.toFixed(2)}`;
    }
    return `${currency} ${amount.toFixed(2)}`;
  };

  // 加载订单列表
  const loadOrders = async (page: number = 1, keyword?: string) => {
    setLoading(true);
    try {
      const params: GetUserOrdersParams = {
        page,
        pageSize: 15
      };

      const response = await getUserOrdersWithToast(params);

      if (response.code === 200) {
        setPageData(response.data);
        
        // 如果有搜索关键词，在前端过滤
        let filteredOrders = response.data.records || [];
        if (keyword && keyword.trim()) {
          const searchTerm = keyword.trim().toLowerCase();
          filteredOrders = filteredOrders.filter(order => 
            order.orderNo.toLowerCase().includes(searchTerm) ||
            order.title.toLowerCase().includes(searchTerm) ||
            (order.description && order.description.toLowerCase().includes(searchTerm))
          );
        }
        
        setOrders(filteredOrders);
      } else {
        toast({
          title: "获取订单列表失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "获取订单列表失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  // 搜索订单
  const handleSearch = () => {
    loadOrders(1, searchQuery);
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadOrders(page, searchQuery);
  };

  // 生成页码数组
  const generatePageNumbers = () => {
    const currentPage = pageData.current;
    const totalPages = pageData.pages;
    const pageNumbers = [];

    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) {
        pageNumbers.push(i);
      }
    } else {
      if (currentPage <= 4) {
        for (let i = 1; i <= 5; i++) {
          pageNumbers.push(i);
        }
        pageNumbers.push('...');
        pageNumbers.push(totalPages);
      } else if (currentPage >= totalPages - 3) {
        pageNumbers.push(1);
        pageNumbers.push('...');
        for (let i = totalPages - 4; i <= totalPages; i++) {
          pageNumbers.push(i);
        }
      } else {
        pageNumbers.push(1);
        pageNumbers.push('...');
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pageNumbers.push(i);
        }
        pageNumbers.push('...');
        pageNumbers.push(totalPages);
      }
    }
    return pageNumbers;
  };

  // 查看订单详情
  const handleViewDetail = async (order: Order) => {
    setDetailLoading(true);
    setDetailDialogOpen(true);
    
    try {
      const response = await getOrderDetailWithToast(order.id);
      
      if (response.code === 200) {
        setSelectedOrder(response.data);
      } else {
        toast({
          title: "获取订单详情失败",
          description: response.message,
          variant: "destructive"
        });
        setDetailDialogOpen(false);
      }
    } catch (error) {
      toast({
        title: "获取订单详情失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
      setDetailDialogOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  // 搜索防抖
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      if (searchQuery) {
        handleSearch();
      } else {
        loadOrders(1);
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  // 初始加载
  useEffect(() => {
    loadOrders();
  }, []);

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">我的订单</h1>
          <p className="text-muted-foreground">查看您的订单历史记录</p>
        </div>
        <Button onClick={() => loadOrders(pageData.current, searchQuery)} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          刷新
        </Button>
      </div>

      {/* 订单统计卡片 */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总订单数</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{pageData.total}</div>
            <p className="text-xs text-muted-foreground">已支付订单</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总金额</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatAmount(orders.reduce((sum, order) => sum + order.amount, 0))}
            </div>
            <p className="text-xs text-muted-foreground">累计消费金额</p>
          </CardContent>
        </Card>
      </div>

      {/* 搜索和筛选 */}
      <Card>
        <CardHeader>
          <CardTitle>订单列表</CardTitle>
          <CardDescription>您的所有已支付订单记录</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* 搜索栏 */}
          <div className="flex gap-4">
            <div className="flex-1">
              <Label htmlFor="search" className="sr-only">搜索订单</Label>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="搜索订单号、标题或描述..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
          </div>

          {/* 订单表格 */}
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>订单号</TableHead>
                  <TableHead>订单类型</TableHead>
                  <TableHead>标题</TableHead>
                  <TableHead>金额</TableHead>
                  <TableHead>支付方式</TableHead>
                  <TableHead>支付时间</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  // 加载骨架屏
                  Array.from({ length: 5 }).map((_, index) => (
                    <TableRow key={index}>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                      <TableCell><div className="h-4 bg-gray-200 rounded animate-pulse"></div></TableCell>
                    </TableRow>
                  ))
                ) : orders.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                      暂无订单记录
                    </TableCell>
                  </TableRow>
                ) : (
                  orders.map((order) => (
                    <TableRow key={order.id}>
                      <TableCell className="font-mono text-sm">{order.orderNo}</TableCell>
                      <TableCell>
                        <Badge variant="outline">
                          {ORDER_TYPE_NAMES[order.orderType as keyof typeof ORDER_TYPE_NAMES] || order.orderType}
                        </Badge>
                      </TableCell>
                      <TableCell className="max-w-48 truncate" title={order.title}>
                        {order.title}
                      </TableCell>
                      <TableCell className="font-medium">
                        {formatAmount(order.amount, order.currency)}
                      </TableCell>
                      <TableCell>
                        {order.paymentPlatformName || 
                         PAYMENT_PLATFORM_NAMES[order.paymentPlatform as keyof typeof PAYMENT_PLATFORM_NAMES] || 
                         order.paymentPlatform || "暂无"}
                      </TableCell>
                      <TableCell>{formatDateTime(order.paidAt)}</TableCell>
                      <TableCell>
                        <Badge variant={ORDER_STATUS_VARIANTS[order.status as keyof typeof ORDER_STATUS_VARIANTS]}>
                          {ORDER_STATUS_NAMES[order.status as keyof typeof ORDER_STATUS_NAMES] || order.statusName}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewDetail(order)}
                        >
                          <Eye className="h-4 w-4 mr-1" />
                          查看
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* 分页组件 */}
          {pageData.pages > 1 && (
            <div className="flex justify-center">
              <Pagination>
                <PaginationContent>
                  <PaginationItem>
                    <PaginationPrevious 
                      onClick={() => handlePageChange(pageData.current - 1)}
                      className={pageData.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                    />
                  </PaginationItem>
                  
                  {generatePageNumbers().map((page, index) => (
                    <PaginationItem key={index}>
                      {page === '...' ? (
                        <PaginationEllipsis />
                      ) : (
                        <PaginationLink
                          onClick={() => handlePageChange(page as number)}
                          isActive={page === pageData.current}
                          className="cursor-pointer"
                        >
                          {page}
                        </PaginationLink>
                      )}
                    </PaginationItem>
                  ))}
                  
                  <PaginationItem>
                    <PaginationNext 
                      onClick={() => handlePageChange(pageData.current + 1)}
                      className={pageData.current >= pageData.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                    />
                  </PaginationItem>
                </PaginationContent>
              </Pagination>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 订单详情弹窗 */}
      <Dialog open={detailDialogOpen} onOpenChange={setDetailDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>订单详情</DialogTitle>
            <DialogDescription>
              订单的完整信息
            </DialogDescription>
          </DialogHeader>
          
          {detailLoading ? (
            <div className="space-y-4">
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
            </div>
          ) : selectedOrder && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单号</Label>
                  <p className="font-mono text-sm">{selectedOrder.orderNo}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单状态</Label>
                  <div className="mt-1">
                    <Badge variant={ORDER_STATUS_VARIANTS[selectedOrder.status as keyof typeof ORDER_STATUS_VARIANTS]}>
                      {ORDER_STATUS_NAMES[selectedOrder.status as keyof typeof ORDER_STATUS_NAMES] || selectedOrder.statusName}
                    </Badge>
                  </div>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单类型</Label>
                  <p>{ORDER_TYPE_NAMES[selectedOrder.orderType as keyof typeof ORDER_TYPE_NAMES] || selectedOrder.orderType}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单金额</Label>
                  <p className="font-medium">{formatAmount(selectedOrder.amount, selectedOrder.currency)}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">支付方式</Label>
                  <p>{selectedOrder.paymentPlatformName || 
                     PAYMENT_PLATFORM_NAMES[selectedOrder.paymentPlatform as keyof typeof PAYMENT_PLATFORM_NAMES] || 
                     selectedOrder.paymentPlatform || "暂无"}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">支付时间</Label>
                  <p>{formatDateTime(selectedOrder.paidAt)}</p>
                </div>
              </div>
              
              <div>
                <Label className="text-sm font-medium text-muted-foreground">订单标题</Label>
                <p>{selectedOrder.title}</p>
              </div>
              
              {selectedOrder.description && (
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单描述</Label>
                  <p className="text-sm">{selectedOrder.description}</p>
                </div>
              )}
              
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">创建时间</Label>
                  <p>{formatDateTime(selectedOrder.createdAt)}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">更新时间</Label>
                  <p>{formatDateTime(selectedOrder.updatedAt)}</p>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}