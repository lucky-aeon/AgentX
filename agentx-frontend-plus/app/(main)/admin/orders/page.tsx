"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Search, Eye, RefreshCw, Package, DollarSign, TrendingUp, Users } from "lucide-react";
import { getAllOrdersWithToast, getAdminOrderDetailWithToast } from "@/lib/admin-order-service";
import { Order, PageResponse, ORDER_STATUS_NAMES, ORDER_STATUS_VARIANTS, ORDER_TYPE_NAMES, PAYMENT_PLATFORM_NAMES } from "@/types/order";
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
import { Label } from "@/components/ui/label";

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [pageData, setPageData] = useState<PageResponse<Order>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  
  // 详情弹窗
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  const { toast } = useToast();

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

  // 计算统计数据
  const getStatistics = () => {
    const totalAmount = orders.reduce((sum, order) => sum + order.amount, 0);
    const totalUsers = new Set(orders.map(order => order.userId)).size;
    const paidOrders = orders.filter(order => order.status === 1).length;
    
    return {
      totalOrders: pageData.total,
      totalAmount,
      totalUsers,
      paidOrders
    };
  };

  // 加载订单数据
  const loadOrders = async (page: number = 1, keyword?: string) => {
    setLoading(true);
    try {
      const response = await getAllOrdersWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      });

      if (response.code === 200) {
        setPageData(response.data);
        setOrders(response.data.records || []);
      } else {
        toast({
          title: "获取订单列表失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "获取订单列表失败",
        description: "网络错误，请稍后重试",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 查看订单详情
  const handleViewDetail = async (order: Order) => {
    setDetailLoading(true);
    setDetailDialogOpen(true);
    
    try {
      const response = await getAdminOrderDetailWithToast(order.id);
      
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

  // 初始加载
  useEffect(() => {
    loadOrders();
  }, []);

  // 搜索防抖
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadOrders(1, searchQuery);
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const stats = getStatistics();

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">订单管理</h1>
          <p className="text-muted-foreground">管理所有用户的订单信息</p>
        </div>
        <Button onClick={() => loadOrders(pageData.current, searchQuery)} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          刷新
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总订单数</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalOrders}</div>
            <p className="text-xs text-muted-foreground">所有订单</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">已支付订单</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.paidOrders}</div>
            <p className="text-xs text-muted-foreground">成功支付的订单</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">总交易金额</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatAmount(stats.totalAmount)}</div>
            <p className="text-xs text-muted-foreground">累计交易金额</p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">下单用户数</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats.totalUsers}</div>
            <p className="text-xs text-muted-foreground">有订单记录的用户</p>
          </CardContent>
        </Card>
      </div>

      {/* 订单列表 */}
      <Card>
        <CardHeader>
          <CardTitle>订单列表</CardTitle>
          <div className="flex items-center space-x-2">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="搜索订单号、标题或描述..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>订单号</TableHead>
                  <TableHead>用户</TableHead>
                  <TableHead>订单类型</TableHead>
                  <TableHead>标题</TableHead>
                  <TableHead>金额</TableHead>
                  <TableHead>支付方式</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  // 加载骨架屏
                  Array.from({ length: 10 }).map((_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: 9 }).map((_, cellIndex) => (
                        <TableCell key={cellIndex}>
                          <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                ) : orders.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8 text-muted-foreground">
                      暂无订单数据
                    </TableCell>
                  </TableRow>
                ) : (
                  orders.map((order) => (
                    <TableRow key={order.id}>
                      <TableCell className="font-mono text-sm">{order.orderNo}</TableCell>
                      <TableCell>{order.userNickname || order.userId.substring(0, 8) + '...'}</TableCell>
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
                      <TableCell>
                        <Badge variant={ORDER_STATUS_VARIANTS[order.status as keyof typeof ORDER_STATUS_VARIANTS]}>
                          {ORDER_STATUS_NAMES[order.status as keyof typeof ORDER_STATUS_NAMES] || order.statusName}
                        </Badge>
                      </TableCell>
                      <TableCell>{formatDateTime(order.createdAt)}</TableCell>
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
            <div className="flex justify-center mt-4">
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
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>订单详情</DialogTitle>
            <DialogDescription>
              完整的订单信息和用户数据
            </DialogDescription>
          </DialogHeader>
          
          {detailLoading ? (
            <div className="space-y-4">
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 bg-gray-200 rounded animate-pulse"></div>
            </div>
          ) : selectedOrder && (
            <div className="space-y-6">
              {/* 基本信息 */}
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单号</Label>
                  <p className="font-mono text-sm">{selectedOrder.orderNo}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">用户</Label>
                  <p>{selectedOrder.userNickname || selectedOrder.userId}</p>
                  {selectedOrder.userNickname && (
                    <p className="font-mono text-xs text-muted-foreground">ID: {selectedOrder.userId}</p>
                  )}
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
                  <p className="font-medium text-lg">{formatAmount(selectedOrder.amount, selectedOrder.currency)}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">货币类型</Label>
                  <p>{selectedOrder.currency}</p>
                </div>
              </div>
              
              {/* 支付信息 */}
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">支付平台</Label>
                  <p>{selectedOrder.paymentPlatformName || 
                     PAYMENT_PLATFORM_NAMES[selectedOrder.paymentPlatform as keyof typeof PAYMENT_PLATFORM_NAMES] || 
                     selectedOrder.paymentPlatform || "暂无"}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">支付类型</Label>
                  <p>{selectedOrder.paymentTypeName || selectedOrder.paymentType || "暂无"}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">第三方订单ID</Label>
                  <p className="font-mono text-sm">{selectedOrder.providerOrderId || "暂无"}</p>
                </div>
              </div>
              
              {/* 订单内容 */}
              <div>
                <Label className="text-sm font-medium text-muted-foreground">订单标题</Label>
                <p className="mt-1">{selectedOrder.title}</p>
              </div>
              
              {selectedOrder.description && (
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">订单描述</Label>
                  <p className="mt-1 text-sm text-muted-foreground">{selectedOrder.description}</p>
                </div>
              )}
              
              {/* 时间信息 */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">创建时间</Label>
                  <p>{formatDateTime(selectedOrder.createdAt)}</p>
                </div>
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">更新时间</Label>
                  <p>{formatDateTime(selectedOrder.updatedAt)}</p>
                </div>
                {selectedOrder.paidAt && (
                  <div>
                    <Label className="text-sm font-medium text-muted-foreground">支付时间</Label>
                    <p>{formatDateTime(selectedOrder.paidAt)}</p>
                  </div>
                )}
                {selectedOrder.expiredAt && (
                  <div>
                    <Label className="text-sm font-medium text-muted-foreground">过期时间</Label>
                    <p>{formatDateTime(selectedOrder.expiredAt)}</p>
                  </div>
                )}
                {selectedOrder.cancelledAt && (
                  <div>
                    <Label className="text-sm font-medium text-muted-foreground">取消时间</Label>
                    <p>{formatDateTime(selectedOrder.cancelledAt)}</p>
                  </div>
                )}
                {selectedOrder.refundedAt && (
                  <div>
                    <Label className="text-sm font-medium text-muted-foreground">退款时间</Label>
                    <p>{formatDateTime(selectedOrder.refundedAt)}</p>
                  </div>
                )}
              </div>
              
              {/* 退款信息 */}
              {selectedOrder.refundAmount && selectedOrder.refundAmount > 0 && (
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">退款金额</Label>
                  <p className="font-medium text-lg">{formatAmount(selectedOrder.refundAmount, selectedOrder.currency)}</p>
                </div>
              )}
              
              {/* 扩展信息 */}
              {selectedOrder.metadata && Object.keys(selectedOrder.metadata).length > 0 && (
                <div>
                  <Label className="text-sm font-medium text-muted-foreground">扩展信息</Label>
                  <pre className="mt-1 p-2 bg-gray-100 rounded text-xs">
                    {JSON.stringify(selectedOrder.metadata, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}