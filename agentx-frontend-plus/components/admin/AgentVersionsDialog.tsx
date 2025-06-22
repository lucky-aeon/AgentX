"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { CheckCircle, XCircle, Clock, Download } from "lucide-react";
import { httpClient } from "@/lib/http-client";
import { useToast } from "@/hooks/use-toast";
import { Textarea } from "@/components/ui/textarea";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";

// Agent版本信息接口
interface AgentVersion {
  id: string;
  agentId: string;
  versionNumber: string;
  publishStatus: number; // 1-审核中, 2-已发布, 3-已拒绝, 4-已下架
  publishedAt: string;
  createdAt: string;
  updatedAt: string;
  rejectReason?: string;
}

interface AgentVersionsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agentId: string;
  agentName: string;
}

export function AgentVersionsDialog({ open, onOpenChange, agentId, agentName }: AgentVersionsDialogProps) {
  const [versions, setVersions] = useState<AgentVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [isRejectDialogOpen, setIsRejectDialogOpen] = useState(false);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);
  const { toast } = useToast();

  // 获取版本状态标签
  const getStatusBadge = (status: number) => {
    switch (status) {
      case 1:
        return <Badge variant="secondary" className="bg-yellow-100 text-yellow-800"><Clock className="w-3 h-3 mr-1" />审核中</Badge>;
      case 2:
        return <Badge variant="default" className="bg-green-100 text-green-800"><CheckCircle className="w-3 h-3 mr-1" />已发布</Badge>;
      case 3:
        return <Badge variant="destructive"><XCircle className="w-3 h-3 mr-1" />已拒绝</Badge>;
      case 4:
        return <Badge variant="outline"><Download className="w-3 h-3 mr-1" />已下架</Badge>;
      default:
        return <Badge variant="outline">未知状态</Badge>;
    }
  };

  // 获取版本列表
  const loadVersions = async () => {
    if (!agentId) return;
    
    try {
      setLoading(true);
      const response = await httpClient.get(`/admin/agents/versions?agentId=${agentId}`);
      if (response.code === 200 && response.data) {
        setVersions(response.data);
      }
    } catch (error) {
      console.error('获取Agent版本失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 审核版本
  const handleReviewVersion = async (versionId: string, status: number, reason?: string) => {
    try {
      const url = `/admin/agents/versions/${versionId}/status?status=${status}${reason ? `&reason=${encodeURIComponent(reason)}` : ''}`;
      const response = await httpClient.post(url);
      if (response.code === 200) {
        toast({
          title: "操作成功",
          description: status === 2 ? "版本已通过审核" : status === 3 ? "版本已拒绝" : "版本已下架"
        });
        // 重新加载版本列表
        loadVersions();
      } else {
        toast({
          variant: "destructive",
          title: "操作失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
      console.error('审核版本失败:', error);
      toast({
        variant: "destructive",
        title: "操作失败",
        description: "网络连接异常，请稍后重试"
      });
    }
  };

  // 处理拒绝操作
  const handleReject = () => {
    if (!selectedVersionId) return;
    if (!rejectReason.trim()) {
      toast({
        variant: "destructive",
        title: "请输入拒绝原因"
      });
      return;
    }
    handleReviewVersion(selectedVersionId, 3, rejectReason);
    setIsRejectDialogOpen(false);
    setRejectReason("");
    setSelectedVersionId(null);
  };

  // 打开拒绝对话框
  const openRejectDialog = (versionId: string) => {
    setSelectedVersionId(versionId);
    setRejectReason("");
    setIsRejectDialogOpen(true);
  };

  useEffect(() => {
    if (open && agentId) {
      loadVersions();
    }
  }, [open, agentId]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Agent版本审核管理 - {agentName}</DialogTitle>
          <div className="text-sm text-gray-600 mt-2">
            管理用户提交的Agent版本，审核通过后用户的Agent版本将对其他用户可见
          </div>
        </DialogHeader>
        
        <div className="space-y-4">
          {/* 审核统计 */}
          {!loading && versions.length > 0 && (
            <div className="grid grid-cols-4 gap-4 p-4 bg-gray-50 rounded-lg">
              <div className="text-center">
                <div className="text-lg font-bold text-orange-600">
                  {versions.filter(v => v.publishStatus === 1).length}
                </div>
                <div className="text-xs text-gray-600">待审核</div>
              </div>
              <div className="text-center">
                <div className="text-lg font-bold text-green-600">
                  {versions.filter(v => v.publishStatus === 2).length}
                </div>
                <div className="text-xs text-gray-600">已发布</div>
              </div>
              <div className="text-center">
                <div className="text-lg font-bold text-red-600">
                  {versions.filter(v => v.publishStatus === 3).length}
                </div>
                <div className="text-xs text-gray-600">已拒绝</div>
              </div>
              <div className="text-center">
                <div className="text-lg font-bold text-gray-600">
                  {versions.filter(v => v.publishStatus === 4).length}
                </div>
                <div className="text-xs text-gray-600">已下架</div>
              </div>
            </div>
          )}
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="text-sm text-gray-500">加载中...</div>
            </div>
          ) : versions.length === 0 ? (
            <div className="flex justify-center py-8">
              <div className="text-sm text-gray-500">暂无版本记录</div>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>版本号</TableHead>
                  <TableHead>审核状态</TableHead>
                  <TableHead>提交时间</TableHead>
                  <TableHead>拒绝原因</TableHead>
                  <TableHead className="w-40">审核操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {versions.map((version) => (
                  <TableRow key={version.id}>
                    <TableCell>
                      <div className="font-medium">v{version.versionNumber}</div>
                    </TableCell>
                    <TableCell>
                      {getStatusBadge(version.publishStatus)}
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        {new Date(version.createdAt).toLocaleString()}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="text-sm text-red-600 max-w-xs truncate">
                        {version.rejectReason || '-'}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center space-x-2">
                        {version.publishStatus === 1 && (
                          <>
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-green-600 border-green-600 hover:bg-green-50"
                              onClick={() => handleReviewVersion(version.id, 2)}
                            >
                              <CheckCircle className="w-3 h-3 mr-1" />
                              通过
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              className="text-red-600 border-red-600 hover:bg-red-50"
                              onClick={() => openRejectDialog(version.id)}
                            >
                              <XCircle className="w-3 h-3 mr-1" />
                              拒绝
                            </Button>
                          </>
                        )}
                        {version.publishStatus === 2 && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-gray-600"
                            onClick={() => handleReviewVersion(version.id, 4)}
                          >
                            <Download className="w-3 h-3 mr-1" />
                            下架
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </DialogContent>
      
      {/* 拒绝原因对话框 */}
      <AlertDialog open={isRejectDialogOpen} onOpenChange={setIsRejectDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>拒绝版本审核</AlertDialogTitle>
            <AlertDialogDescription>
              请输入拒绝该版本的详细原因，这将帮助用户了解需要改进的地方。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="py-4">
            <Textarea
              placeholder="请详细说明拒绝原因..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="min-h-[100px]"
            />
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => {
              setIsRejectDialogOpen(false);
              setRejectReason("");
              setSelectedVersionId(null);
            }}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleReject} className="bg-red-600 hover:bg-red-700">
              确认拒绝
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Dialog>
  );
}