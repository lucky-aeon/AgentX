"use client";

import { useState, useEffect } from "react";
import { CreditCard, Download, AlertTriangle, Wallet, TrendingUp, RefreshCw, Search, Calendar, Eye, RotateCcw, FileText, ExternalLink, Tag, CheckCircle } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
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

import { Account, RechargeRequest } from "@/types/account";
import { SimpleRechargeFlow } from "@/components/payment";
import { UsageRecord, QueryUsageRecordRequest } from "@/types/usage-record";
import { PageResponse } from "@/types/billing";
import { AccountService, AccountServiceWithToast } from "@/lib/account-service";
import { UsageRecordService, UsageRecordServiceWithToast } from "@/lib/usage-record-service";
import { useAccount } from "@/contexts/account-context";

export default function BillingPage() {
  // 使用全局账户状态
  const { 
    account, 
    loading: accountLoading, 
    refreshAccount, 
    isLowBalance, 
    formatAmount 
  } = useAccount();
  
  const [totalCost, setTotalCost] = useState<number>(0);
  const [balanceUpdated, setBalanceUpdated] = useState(false);

  // 用量记录相关状态
  const [records, setRecords] = useState<UsageRecord[]>([]);
  const [recordsLoading, setRecordsLoading] = useState(false);
  const [pageData, setPageData] = useState<PageResponse<UsageRecord>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  
  // 筛选条件
  const [productId, setProductId] = useState("");
  const [requestId, setRequestId] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  
  // 详情弹窗
  const [selectedRecord, setSelectedRecord] = useState<UsageRecord | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);

  // 加载总消费数据
  const loadTotalCost = async () => {
    try {
      const totalCostResponse = await UsageRecordService.getCurrentUserTotalCost();
      if (totalCostResponse.code === 200) {
        setTotalCost(totalCostResponse.data);
      }
    } catch (error) {
 
    }
  };

  // 充值成功回调
  const handleRechargeSuccess = (orderNo: string, amount: number) => {
    // 立即显示更新动画
    setBalanceUpdated(true);
    
    toast({
      title: "充值成功",
      description: `¥${amount.toFixed(2)} 已成功充值到您的账户`,
      variant: "default"
    });
    
    // 延迟刷新数据，让动画先显示
    setTimeout(() => {
      // 刷新全局账户数据
      refreshAccount();
      // 同时刷新总消费数据
      loadTotalCost();
      
      // 3秒后隐藏更新动画
      setTimeout(() => {
        setBalanceUpdated(false);
      }, 3000);
    }, 500);
  };


  // 格式化时间
  const formatDateTime = (dateTime?: string) => {
    if (!dateTime) return "暂无";
    return new Date(dateTime).toLocaleString('zh-CN');
  };

  // 加载用量记录
  const loadUsageRecords = async (page: number = 1, params?: Partial<QueryUsageRecordRequest>) => {
    setRecordsLoading(true);
    try {
      const request: QueryUsageRecordRequest = {
        page,
        pageSize: 15,
        productId: params?.productId?.trim() || undefined,
        startTime: params?.startTime || undefined,
        endTime: params?.endTime || undefined
      };

      const response = await UsageRecordService.queryUsageRecords(request);
      
      if (response.code === 200) {
        setPageData(response.data);
        setRecords(response.data.records || []);
      } else {
        toast({
          title: "获取用量记录失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "获取用量记录失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    } finally {
      setRecordsLoading(false);
    }
  };

  // 搜索用量记录
  const handleSearchRecords = () => {
    loadUsageRecords(1, {
      productId,
      startTime,
      endTime
    });
  };

  // 重置搜索条件
  const handleResetSearch = () => {
    setProductId("");
    setStartTime("");
    setEndTime("");
    loadUsageRecords(1);
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadUsageRecords(page, {
      productId: productId || undefined,
      startTime: startTime || undefined,
      endTime: endTime || undefined
    });
  };

  // 查看记录详情
  const handleViewRecordDetail = async (record: UsageRecord) => {
    try {
      const response = await UsageRecordService.getUsageRecordById(record.id);
      if (response.code === 200) {
        setSelectedRecord(response.data);
        setDetailDialogOpen(true);
      } else {
        toast({
          title: "获取记录详情失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "获取记录详情失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    }
  };

  // 导出记录
  const handleExportRecords = async () => {
    try {
      await UsageRecordServiceWithToast.exportUsageRecords({
        productId: productId || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined
      });
    } catch (error) {
      toast({
        title: "导出失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    }
  };

  // 获取服务名称（优先使用后端提供的业务信息）
  const getServiceName = (record: UsageRecord): string => {
    // 优先使用后端提供的服务名称
    if (record.serviceName) {
      return record.serviceName;
    }
    
    // 兜底：根据productId模式识别服务类型
    if (record.productId.includes('model')) {
      return "模型调用";
    } else if (record.productId.includes('agent-creation')) {
      return "Agent创建";
    } else if (record.productId.includes('agent')) {
      return "Agent使用";
    } else if (record.productId.includes('api')) {
      return "API调用";
    } else if (record.productId.includes('storage')) {
      return "存储服务";
    } else {
      return "其他服务";
    }
  };

  // 格式化用量详情
  const formatUsageDetails = (quantityData: Record<string, any>, productId: string): string => {
    if (!quantityData) return "无数据";
    
    // 模型调用数据
    if (quantityData.input !== undefined && quantityData.output !== undefined) {
      return `输入 ${quantityData.input.toLocaleString()} tokens，输出 ${quantityData.output.toLocaleString()} tokens`;
    }
    
    // 按次计费数据
    if (quantityData.calls !== undefined) {
      return `调用 ${quantityData.calls} 次`;
    }
    
    // 创建类数据
    if (quantityData.quantity !== undefined) {
      return `数量 ${quantityData.quantity}`;
    }
    
    // 其他类型，取第一个有意义的字段
    const entries = Object.entries(quantityData);
    if (entries.length > 0) {
      const [key, value] = entries[0];
      return `${key}: ${value}`;
    }
    
    return "无数据";
  };

  // 生成页码
  const generatePageNumbers = () => {
    const pages = [];
    const totalPages = pageData.pages;
    const current = pageData.current;

    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      } else if (current >= totalPages - 3) {
        pages.push(1);
        pages.push('...');
        for (let i = totalPages - 4; i <= totalPages; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push('...');
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      }
    }
    return pages;
  };


  useEffect(() => {
    loadTotalCost(); // 加载总消费数据
    loadUsageRecords(); // 加载用量记录
  }, []);

  return (
    <div className="container py-6">
      <div className="mb-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">账单与用量</h1>
            <p className="text-muted-foreground">管理您的账户和查看使用情况</p>
          </div>
          <Button 
            onClick={() => {
              refreshAccount();
              loadTotalCost();
            }} 
            disabled={accountLoading} 
            variant="outline" 
            size="sm"
          >
            <RefreshCw className={`mr-2 h-4 w-4 ${accountLoading ? 'animate-spin' : ''}`} />
            刷新
          </Button>
        </div>
      </div>

      <Tabs defaultValue="overview" className="space-y-6">
        <TabsList>
          <TabsTrigger value="overview">账户概览</TabsTrigger>
          <TabsTrigger value="records">用量记录</TabsTrigger>
          <TabsTrigger value="recharge">充值管理</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <div className="grid gap-6">
            {/* 余额不足警告 */}
            {isLowBalance && (
              <Card className="border-orange-200 bg-orange-50">
                <CardContent className="flex items-center gap-3 pt-6">
                  <AlertTriangle className="h-5 w-5 text-orange-600" />
                  <div>
                    <p className="font-medium text-orange-800">余额不足提醒</p>
                    <p className="text-sm text-orange-600">您的账户余额较低，建议及时充值以免影响正常使用</p>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 账户余额卡片 */}
            <Card className={`transition-all duration-500 ${balanceUpdated ? 'ring-2 ring-green-500 shadow-lg' : ''}`}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Wallet className="h-5 w-5" />
                  账户余额
                  {balanceUpdated && (
                    <CheckCircle className="h-5 w-5 text-green-500 animate-pulse" />
                  )}
                </CardTitle>
                <CardDescription>
                  当前账户资金状况
                  {balanceUpdated && (
                    <span className="text-green-600 font-medium ml-2">余额已更新</span>
                  )}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {accountLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="text-sm text-muted-foreground">加载中...</div>
                  </div>
                ) : account ? (
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">当前余额</p>
                      <p className={`text-2xl font-bold transition-all duration-500 ${balanceUpdated ? 'scale-110 text-green-600' : ''}`}>
                        {formatAmount(account.balance)}
                      </p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">信用额度</p>
                      <p className="text-2xl font-bold text-blue-600">{formatAmount(account.credit)}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">可用余额</p>
                      <p className={`text-2xl font-bold text-green-600 transition-all duration-500 ${balanceUpdated ? 'scale-110' : ''}`}>
                        {formatAmount(account.availableBalance)}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-8 text-muted-foreground">
                    获取账户信息失败
                  </div>
                )}
              </CardContent>
            </Card>

            {/* 消费统计卡片 */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="h-5 w-5" />
                  消费统计
                </CardTitle>
                <CardDescription>账户使用情况统计</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {accountLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="text-sm text-muted-foreground">加载中...</div>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">历史总消费</p>
                      <p className="text-2xl font-bold">{formatAmount(account?.totalConsumed || 0)}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">累计用量费用</p>
                      <p className="text-2xl font-bold">{formatAmount(totalCost)}</p>
                    </div>
                    <div className="space-y-2">
                      <p className="text-sm font-medium text-muted-foreground">最后交易时间</p>
                      <p className="text-sm">{formatDateTime(account?.lastTransactionAt)}</p>
                    </div>
                  </div>
                )}
              </CardContent>
             
            </Card>

            {/* 账户信息 */}
            {account && (
              <Card>
                <CardHeader>
                  <CardTitle>账户信息</CardTitle>
                  <CardDescription>基本账户信息</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">账户ID</span>
                    <span className="text-sm font-mono">{account.id}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">创建时间</span>
                    <span className="text-sm">{formatDateTime(account.createdAt)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-muted-foreground">更新时间</span>
                    <span className="text-sm">{formatDateTime(account.updatedAt)}</span>
                  </div>
                </CardContent>
              </Card>
            )}

            {/* 价格说明快速链接 */}
            <Card className="bg-gradient-to-r from-blue-50 to-indigo-50 border-blue-200">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Tag className="h-5 w-5 text-blue-600" />
                  价格说明
                </CardTitle>
                <CardDescription>了解各项服务的详细价格和计费方式</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-sm text-muted-foreground">
                  <p>• 模型调用按Token数量计费</p>
                  <p>• Agent创建和使用按次数计费</p>
                  <p>• API调用和存储服务按用量计费</p>
                  <p>• 支持价格计算器和商品比较功能</p>
                </div>
              </CardContent>
              <CardFooter>
                <Link href="/settings/pricing" className="w-full">
                  <Button className="w-full" variant="outline">
                    <ExternalLink className="mr-2 h-4 w-4" />
                    查看详细价格说明
                  </Button>
                </Link>
              </CardFooter>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="records">
          <div className="space-y-4">
            {/* 筛选条件 */}
            <Card>
              <CardHeader>
                <CardTitle>筛选条件</CardTitle>
                <CardDescription>
                  根据时间范围等条件筛选用量记录
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="productId">服务类型</Label>
                    <Input
                      id="productId"
                      placeholder="输入服务类型关键词"
                      value={productId}
                      onChange={(e) => setProductId(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="startTime">开始时间</Label>
                    <Input
                      id="startTime"
                      type="datetime-local"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="endTime">结束时间</Label>
                    <Input
                      id="endTime"
                      type="datetime-local"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                    />
                  </div>
                  <div className="flex items-end space-x-2">
                    <Button onClick={handleSearchRecords}>
                      <Search className="mr-2 h-4 w-4" />
                      搜索
                    </Button>
                    <Button onClick={handleResetSearch} variant="outline">
                      <RotateCcw className="mr-2 h-4 w-4" />
                      重置
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 用量记录表格 */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle>用量记录</CardTitle>
                    <CardDescription>
                      共 {pageData.total} 条记录，当前第 {pageData.current} 页
                    </CardDescription>
                  </div>
                  <Button onClick={handleExportRecords} variant="outline" size="sm">
                    <Download className="mr-2 h-4 w-4" />
                    导出
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                {recordsLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="text-sm text-muted-foreground">加载中...</div>
                  </div>
                ) : records.length === 0 ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="text-sm text-muted-foreground">暂无用量记录</div>
                  </div>
                ) : (
                  <>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>时间</TableHead>
                          <TableHead>服务名称</TableHead>
                          <TableHead>使用详情</TableHead>
                          <TableHead>费用</TableHead>
                          <TableHead className="text-right">操作</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {records.map((record) => (
                          <TableRow key={record.id}>
                            <TableCell>
                              <div className="text-sm">
                                {formatDateTime(record.billedAt)}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="space-y-1">
                                <Badge variant="outline">
                                  {getServiceName(record)}
                                </Badge>
                                {record.relatedEntityName && (
                                  <div className="text-xs text-muted-foreground">
                                    {record.relatedEntityName}
                                  </div>
                                )}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="space-y-1">
                                <div className="text-sm">
                                  {formatUsageDetails(record.quantityData, record.productId)}
                                </div>
                                {record.pricingRule && (
                                  <div className="text-xs text-muted-foreground">
                                    {record.pricingRule}
                                  </div>
                                )}
                              </div>
                            </TableCell>
                            <TableCell>
                              <span className="font-medium">
                                {formatAmount(record.cost)}
                              </span>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleViewRecordDetail(record)}
                              >
                                <Eye className="h-4 w-4" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>

                    {/* 分页 */}
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
                  </>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="recharge">
          <div className="space-y-6">
            <SimpleRechargeFlow 
              onSuccess={handleRechargeSuccess}
            />
            
            {/* 充值说明 */}
            <Card>
              <CardHeader>
                <CardTitle>充值说明</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <p>• 充值金额将直接添加到您的账户余额中</p>
                <p>• 最低充值金额为 ¥0.01，最高单次充值 ¥100,000</p>
                <p>• 目前支持支付宝扫码支付</p>
                <p>• 充值后余额可用于平台内所有付费服务</p>
                <p>• 支付过程中请不要关闭页面，等待支付完成</p>
                <p>• 如遇到问题或需要退款，请联系客服</p>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

      </Tabs>

      {/* 用量记录详情对话框 */}
      <Dialog open={detailDialogOpen} onOpenChange={setDetailDialogOpen}>
        <DialogContent className="max-w-xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>用量详情</DialogTitle>
            <DialogDescription>
              详细的使用记录和费用信息
            </DialogDescription>
          </DialogHeader>
          {selectedRecord && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 gap-4 text-sm">
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="font-medium text-muted-foreground">服务类型</span>
                  <Badge variant="outline">{getServiceName(selectedRecord)}</Badge>
                </div>
                {selectedRecord.serviceDescription && (
                  <div className="flex justify-between items-center py-2 border-b">
                    <span className="font-medium text-muted-foreground">服务描述</span>
                    <span className="text-sm">{selectedRecord.serviceDescription}</span>
                  </div>
                )}
                {selectedRecord.relatedEntityName && (
                  <div className="flex justify-between items-center py-2 border-b">
                    <span className="font-medium text-muted-foreground">关联实体</span>
                    <span className="text-sm">{selectedRecord.relatedEntityName}</span>
                  </div>
                )}
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="font-medium text-muted-foreground">使用详情</span>
                  <span>{formatUsageDetails(selectedRecord.quantityData, selectedRecord.productId)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="font-medium text-muted-foreground">费用</span>
                  <span className="font-semibold text-lg">{formatAmount(selectedRecord.cost)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="font-medium text-muted-foreground">计费时间</span>
                  <span>{formatDateTime(selectedRecord.billedAt)}</span>
                </div>
                <div className="flex justify-between items-center py-2">
                  <span className="font-medium text-muted-foreground">创建时间</span>
                  <span>{formatDateTime(selectedRecord.createdAt)}</span>
                </div>
              </div>
              
              {/* 计费说明 */}
              <div>
                <p className="font-medium text-muted-foreground mb-2">计费说明</p>
                <div className="bg-muted p-3 rounded-md text-sm space-y-2">
                  {selectedRecord.pricingRule ? (
                    <p>• {selectedRecord.pricingRule}</p>
                  ) : (
                    <>
                      {selectedRecord.productId.includes('model') && (
                        <p>• 模型调用按输入和输出token分别计费</p>
                      )}
                      {selectedRecord.productId.includes('agent-creation') && (
                        <p>• Agent创建按固定单价计费</p>
                      )}
                      {selectedRecord.productId.includes('agent') && !selectedRecord.productId.includes('creation') && (
                        <p>• Agent使用按调用次数计费</p>
                      )}
                    </>
                  )}
                  <p>• 费用已从您的账户余额中扣除</p>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}

