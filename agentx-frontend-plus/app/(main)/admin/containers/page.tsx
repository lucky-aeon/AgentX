"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Search, Play, Square, Trash2, Plus, RefreshCw, Settings, FileText, Terminal, Monitor, Activity, Network, Server } from "lucide-react";
import Link from "next/link";
import { 
  getContainersWithToast, 
  getContainerStatisticsWithToast,
  startContainerWithToast,
  stopContainerWithToast,
  deleteContainerWithToast,
  getContainerLogsWithToast,
  executeCommandWithToast,
  getSystemInfoWithToast,
  getProcessInfoWithToast,
  getNetworkInfoWithToast,
  getMcpGatewayStatusWithToast,
  Container, 
  ContainerStatistics, 
  PageResponse,
  CONTAINER_STATUSES,
  CONTAINER_TYPES
} from "@/lib/admin-container-service";
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
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import dynamic from 'next/dynamic';

// 动态导入WebTerminal组件，避免SSR问题
const WebTerminal = dynamic(() => import('@/components/WebTerminal'), { 
  ssr: false,
  loading: () => <div className="flex items-center justify-center h-96">加载终端中...</div>
});

export default function ContainersPage() {
  const [containers, setContainers] = useState<Container[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statistics, setStatistics] = useState<ContainerStatistics>({ totalContainers: 0, runningContainers: 0 });
  const [pageData, setPageData] = useState<PageResponse<Container>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; container: Container | null }>({ open: false, container: null });
  const [logsDialog, setLogsDialog] = useState<{ open: boolean; container: Container | null; logs: string }>({ open: false, container: null, logs: '' });
  const [terminalDialog, setTerminalDialog] = useState<{ open: boolean; container: Container | null }>({ open: false, container: null });
  const [infoDialog, setInfoDialog] = useState<{ open: boolean; container: Container | null; title: string; content: string }>({ open: false, container: null, title: '', content: '' });
  const { toast } = useToast();

  // 加载容器数据
  const loadContainers = async (page: number = 1, keyword?: string) => {
    setLoading(true);
    try {
      const response = await getContainersWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      });

      if (response.code === 200) {
        setPageData(response.data);
        setContainers(response.data.records || []);
      }
    } catch (error) {
      // 错误处理已由withToast处理
    } finally {
      setLoading(false);
    }
  };

  // 加载统计信息
  const loadStatistics = async () => {
    try {
      const response = await getContainerStatisticsWithToast();
      if (response.code === 200) {
        setStatistics(response.data);
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  // 初始加载
  useEffect(() => {
    loadContainers();
    loadStatistics();
  }, []);

  // 搜索处理
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadContainers(1, searchQuery);
    }, 500); // 防抖500ms

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const getStatusBadge = (status: any) => {
    const statusMap: { [key: number]: { color: string; text: string } } = {
      1: { color: 'bg-yellow-100 text-yellow-800', text: '创建中' },
      2: { color: 'bg-green-100 text-green-800', text: '运行中' },
      3: { color: 'bg-gray-100 text-gray-800', text: '已停止' },
      4: { color: 'bg-red-100 text-red-800', text: '错误状态' },
      5: { color: 'bg-orange-100 text-orange-800', text: '删除中' },
      6: { color: 'bg-gray-100 text-gray-500', text: '已删除' },
      7: { color: 'bg-purple-100 text-purple-800', text: '已暂停' }
    };
    
    // 如果是字符串状态，转换为对应的数字代码
    let statusCode = status?.code;
    if (typeof status === 'string') {
      const stringToCodeMap: { [key: string]: number } = {
        'CREATING': 1,
        'RUNNING': 2,
        'STOPPED': 3,
        'ERROR': 4,
        'DELETING': 5,
        'DELETED': 6,
        'SUSPENDED': 7
      };
      statusCode = stringToCodeMap[status] || 1; // 默认为创建中
    }
    
    const statusInfo = statusMap[statusCode] || statusMap[1]; // 默认为创建中，而不是错误状态
    return (
      <Badge className={statusInfo.color}>
        {statusInfo.text}
      </Badge>
    );
  };

  const getTypeBadge = (type: any) => {
    const typeMap: { [key: number]: { color: string; text: string } } = {
      1: { color: 'bg-blue-100 text-blue-800', text: '用户容器' },
      2: { color: 'bg-purple-100 text-purple-800', text: '审核容器' }
    };
    
    const typeInfo = typeMap[type?.code] || typeMap[1];
    return (
      <Badge className={typeInfo.color}>
        {typeInfo.text}
      </Badge>
    );
  };

  // 处理分页点击
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadContainers(page, searchQuery);
  };

  // 生成分页页码数组
  const generatePageNumbers = () => {
    const current = pageData.current;
    const total = pageData.pages;
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

  // 容器操作
  const handleStartContainer = async (container: Container) => {
    try {
      const response = await startContainerWithToast(container.id);
      if (response.code === 200) {
        loadContainers(pageData.current, searchQuery);
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  const handleStopContainer = async (container: Container) => {
    try {
      const response = await stopContainerWithToast(container.id);
      if (response.code === 200) {
        loadContainers(pageData.current, searchQuery);
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  const handleDeleteContainer = async () => {
    if (!deleteDialog.container) return;

    try {
      const response = await deleteContainerWithToast(deleteDialog.container.id);
      if (response.code === 200) {
        setDeleteDialog({ open: false, container: null });
        loadContainers(pageData.current, searchQuery);
        loadStatistics();
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  // 查看容器日志
  const handleViewLogs = async (container: Container) => {
    try {
      const response = await getContainerLogsWithToast(container.id, 200);
      if (response.code === 200) {
        setLogsDialog({ 
          open: true, 
          container, 
          logs: response.data || '暂无日志' 
        });
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  // 打开终端
  const handleOpenTerminal = (container: Container) => {
    setTerminalDialog({ open: true, container });
  };

  // 查看系统信息
  const handleViewSystemInfo = async (container: Container) => {
    // 检查容器状态
    const isRunning = container.status?.code === 2 || 
                     container.status?.code === CONTAINER_STATUSES.RUNNING.code ||
                     (typeof container.status === 'string' && container.status === 'RUNNING');
    
    if (!isRunning) {
      toast({
        title: "无法获取系统信息",
        description: "容器未运行，请先启动容器",
        variant: "destructive",
      });
      return;
    }
    
    try {
      const response = await getSystemInfoWithToast(container.id);
      if (response.code === 200) {
        setInfoDialog({
          open: true,
          container,
          title: '系统信息',
          content: response.data || '无法获取系统信息'
        });
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  // 查看MCP网关状态
  const handleViewMcpStatus = async (container: Container) => {
    // 检查容器状态
    const isRunning = container.status?.code === 2 || 
                     container.status?.code === CONTAINER_STATUSES.RUNNING.code ||
                     (typeof container.status === 'string' && container.status === 'RUNNING');
    
    if (!isRunning) {
      toast({
        title: "无法获取MCP网关状态",
        description: "容器未运行，请先启动容器",
        variant: "destructive",
      });
      return;
    }
    
    try {
      const response = await getMcpGatewayStatusWithToast(container.id);
      if (response.code === 200) {
        setInfoDialog({
          open: true,
          container,
          title: 'MCP网关状态',
          content: response.data || '无法获取MCP网关状态'
        });
      }
    } catch (error) {
      // 错误处理已由withToast处理
    }
  };

  const canStartContainer = (container: Container) => {
    return container.status?.code === CONTAINER_STATUSES.STOPPED.code;
  };

  const canStopContainer = (container: Container) => {
    return container.status?.code === CONTAINER_STATUSES.RUNNING.code;
  };

  const canDeleteContainer = (container: Container) => {
    return container.status?.code !== CONTAINER_STATUSES.DELETING.code && 
           container.status?.code !== CONTAINER_STATUSES.DELETED.code;
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
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">容器管理</h1>
          <p className="text-gray-600 mt-1">管理系统中的所有Docker容器</p>
        </div>
        <div className="flex gap-2">
          <Link href="/admin/container-templates">
            <Button variant="outline">
              <Settings className="w-4 h-4 mr-2" />
              模板管理
            </Button>
          </Link>
          <Button>
            <Plus className="w-4 h-4 mr-2" />
            创建容器
          </Button>
        </div>
      </div>

      {/* 统计信息 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">总容器数</p>
                <p className="text-3xl font-bold text-gray-900">{statistics.totalContainers}</p>
              </div>
              <div className="p-3 bg-blue-50 rounded-full">
                <RefreshCw className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">运行中</p>
                <p className="text-3xl font-bold text-green-600">{statistics.runningContainers}</p>
              </div>
              <div className="p-3 bg-green-50 rounded-full">
                <Play className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">停止/错误</p>
                <p className="text-3xl font-bold text-red-600">
                  {statistics.totalContainers - statistics.runningContainers}
                </p>
              </div>
              <div className="p-3 bg-red-50 rounded-full">
                <Square className="w-6 h-6 text-red-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 搜索和操作 */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex justify-between items-center">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="搜索容器名称、用户ID或Docker ID..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
            <Button 
              onClick={() => {
                loadContainers(pageData.current, searchQuery);
                loadStatistics();
              }}
              variant="outline"
            >
              <RefreshCw className="w-4 h-4 mr-2" />
              刷新
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 容器列表 */}
      <Card>
        <CardHeader>
          <CardTitle>容器列表 ({pageData.total})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>容器信息</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>状态</TableHead>
                <TableHead>端口映射</TableHead>
                <TableHead>资源使用率</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead>操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {containers.map((container) => (
                <TableRow key={container.id}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{container.name}</div>
                      <div className="text-sm text-gray-500">{container.image}</div>
                      {container.userId && (
                        <div className="text-xs text-gray-400">用户: {container.userId.substring(0, 8)}...</div>
                      )}
                      {container.errorMessage && (
                        <div className="text-xs text-red-500 mt-1">{container.errorMessage}</div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    {getTypeBadge(container.type)}
                  </TableCell>
                  <TableCell>
                    {getStatusBadge(container.status)}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      <div>内部: {container.internalPort}</div>
                      {container.externalPort && (
                        <div>外部: {container.externalPort}</div>
                      )}
                      {container.ipAddress && (
                        <div className="text-xs text-gray-500">IP: {container.ipAddress}</div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">
                      {container.cpuUsage !== null && container.cpuUsage !== undefined && (
                        <div>CPU: {container.cpuUsage.toFixed(1)}%</div>
                      )}
                      {container.memoryUsage !== null && container.memoryUsage !== undefined && (
                        <div>内存: {container.memoryUsage.toFixed(1)}%</div>
                      )}
                      {(!container.cpuUsage && !container.memoryUsage) && (
                        <div className="text-gray-400">-</div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{formatDate(container.createdAt)}</div>
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {/* 基础操作按钮 */}
                      {canStartContainer(container) && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleStartContainer(container)}
                          title="启动容器"
                        >
                          <Play className="w-3 h-3" />
                        </Button>
                      )}
                      {canStopContainer(container) && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleStopContainer(container)}
                          title="停止容器"
                        >
                          <Square className="w-3 h-3" />
                        </Button>
                      )}
                      
                      {/* 观测功能按钮 - 始终显示，运行状态检查在点击时进行 */}
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleViewLogs(container)}
                        title="查看日志"
                      >
                        <FileText className="w-3 h-3" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleOpenTerminal(container)}
                        title="终端"
                      >
                        <Terminal className="w-3 h-3" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleViewSystemInfo(container)}
                        title="系统信息"
                      >
                        <Monitor className="w-3 h-3" />
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleViewMcpStatus(container)}
                        title="MCP状态"
                      >
                        <Server className="w-3 h-3" />
                      </Button>
                      
                      {canDeleteContainer(container) && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setDeleteDialog({ open: true, container })}
                          title="删除容器"
                        >
                          <Trash2 className="w-3 h-3" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          
          {containers.length === 0 && !loading && (
            <div className="text-center py-8 text-gray-500">
              {searchQuery ? "没有找到匹配的容器" : "暂无容器数据"}
            </div>
          )}
        </CardContent>
      </Card>

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

      {/* 删除确认对话框 */}
      <AlertDialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ open, container: deleteDialog.container })}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除容器</AlertDialogTitle>
            <AlertDialogDescription>
              您确定要删除容器 "{deleteDialog.container?.name}" 吗？此操作不可撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteContainer}>
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* 容器日志对话框 */}
      <Dialog open={logsDialog.open} onOpenChange={(open) => setLogsDialog({ open, container: logsDialog.container, logs: logsDialog.logs })}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle>容器日志 - {logsDialog.container?.name}</DialogTitle>
            <DialogDescription>
              最近200行日志记录
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Textarea
              value={logsDialog.logs}
              readOnly
              className="min-h-[400px] font-mono text-sm bg-black text-green-400 resize-none"
              placeholder="暂无日志..."
            />
            <div className="flex justify-end space-x-2">
              <Button 
                variant="outline"
                onClick={() => handleViewLogs(logsDialog.container!)}
                disabled={!logsDialog.container}
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                刷新日志
              </Button>
              <Button onClick={() => setLogsDialog({ open: false, container: null, logs: '' })}>
                关闭
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* 容器终端对话框 - 使用真正的Web Terminal */}
      <Dialog open={terminalDialog.open} onOpenChange={(open) => setTerminalDialog({ open, container: terminalDialog.container })}>
        <DialogContent className="max-w-6xl max-h-[90vh] p-0">
          <DialogHeader className="sr-only">
            <DialogTitle>容器终端 - {terminalDialog.container?.name}</DialogTitle>
          </DialogHeader>
          <div className="h-[80vh]">
            {terminalDialog.container && (
              <WebTerminal
                containerId={terminalDialog.container.id}
                containerName={terminalDialog.container.name}
                onClose={() => setTerminalDialog({ open: false, container: null })}
              />
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* 信息查看对话框 */}
      <Dialog open={infoDialog.open} onOpenChange={(open) => setInfoDialog({ open, container: infoDialog.container, title: infoDialog.title, content: infoDialog.content })}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle>{infoDialog.title} - {infoDialog.container?.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <Textarea
              value={infoDialog.content}
              readOnly
              className="min-h-[400px] font-mono text-sm bg-slate-50 resize-none"
              placeholder="暂无信息..."
            />
            <div className="flex justify-end">
              <Button onClick={() => setInfoDialog({ open: false, container: null, title: '', content: '' })}>
                关闭
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}