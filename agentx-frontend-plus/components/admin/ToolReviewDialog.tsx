"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
} from "@/components/ui/alert-dialog";
import { CheckCircle, XCircle, Clock, Code, Github, ExternalLink } from "lucide-react";
import { Tool, ToolStatus, AdminToolService, getToolStatusText, getToolStatusColor } from "@/lib/admin-tool-service";
import { useToast } from "@/hooks/use-toast";

interface ToolReviewDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tool: Tool | null;
  onReviewComplete?: () => void;
}

export function ToolReviewDialog({ open, onOpenChange, tool, onReviewComplete }: ToolReviewDialogProps) {
  const [rejectReason, setRejectReason] = useState("");
  const [isRejectDialogOpen, setIsRejectDialogOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const { toast } = useToast();

  // 处理审核操作
  const handleReview = async (status: ToolStatus, reason?: string) => {
    if (!tool) return;
    
    try {
      setLoading(true);
      const response = await AdminToolService.updateToolStatus(tool.id, status, reason);
      
      if (response.code === 200) {
        toast({
          title: "操作成功",
          description: status === ToolStatus.APPROVED ? "工具已通过审核" : 
                      status === ToolStatus.FAILED ? "工具已拒绝" : "状态已更新"
        });
        onReviewComplete?.();
        onOpenChange(false);
      } else {
        toast({
          variant: "destructive",
          title: "操作失败",
          description: response.message || "未知错误"
        });
      }
    } catch (error) {
 
      toast({
        variant: "destructive",
        title: "操作失败",
        description: "网络连接异常，请稍后重试"
      });
    } finally {
      setLoading(false);
    }
  };

  // 处理拒绝操作
  const handleReject = () => {
    if (!rejectReason.trim()) {
      toast({
        variant: "destructive",
        title: "请输入拒绝原因"
      });
      return;
    }
    handleReview(ToolStatus.FAILED, rejectReason);
    setIsRejectDialogOpen(false);
    setRejectReason("");
  };

  // 打开拒绝对话框
  const openRejectDialog = () => {
    setRejectReason("");
    setIsRejectDialogOpen(true);
  };

  if (!tool) return null;

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Code className="w-5 h-5" />
              工具审核详情 - {tool.name}
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-6">
            {/* 工具基本信息 */}
            <div className="grid grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-700">工具名称</label>
                  <div className="mt-1 text-sm">{tool.name}</div>
                </div>
                
                <div>
                  <label className="text-sm font-medium text-gray-700">描述</label>
                  <div className="mt-1 text-sm text-gray-600">{tool.description}</div>
                </div>
                
                <div>
                  <label className="text-sm font-medium text-gray-700">创建者</label>
                  <div className="mt-1 text-sm">
                    {tool.userNickname || tool.userEmail || '未知用户'}
                    <span className="text-xs text-gray-400 ml-2">({tool.userId})</span>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">当前状态</label>
                  <div className="mt-1">
                    <Badge className={getToolStatusColor(tool.status)}>
                      {getToolStatusText(tool.status)}
                    </Badge>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="text-sm font-medium text-gray-700">工具类型</label>
                  <div className="mt-1 text-sm">{tool.toolType}</div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">上传方式</label>
                  <div className="mt-1 text-sm">{tool.uploadType}</div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">上传链接</label>
                  <div className="mt-1">
                    <a 
                      href={tool.uploadUrl} 
                      target="_blank" 
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-800 flex items-center gap-1 text-sm"
                    >
                      <Github className="w-4 h-4" />
                      {tool.uploadUrl}
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                </div>

                <div>
                  <label className="text-sm font-medium text-gray-700">是否官方工具</label>
                  <div className="mt-1">
                    <Badge variant={tool.isOffice ? "default" : "outline"}>
                      {tool.isOffice ? "官方" : "第三方"}
                    </Badge>
                  </div>
                </div>
              </div>
            </div>

            {/* 标签 */}
            {tool.labels && tool.labels.length > 0 && (
              <div>
                <label className="text-sm font-medium text-gray-700">标签</label>
                <div className="mt-2 flex flex-wrap gap-2">
                  {tool.labels.map((label, index) => (
                    <Badge key={index} variant="outline" className="text-xs">
                      {label}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* 拒绝原因 */}
            {tool.rejectReason && (
              <div>
                <label className="text-sm font-medium text-gray-700">拒绝原因</label>
                <div className="mt-1 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-800">
                  {tool.rejectReason}
                </div>
              </div>
            )}

            {/* 工具定义列表 */}
            {tool.toolList && tool.toolList.length > 0 && (
              <div>
                <label className="text-sm font-medium text-gray-700">工具功能列表</label>
                <div className="mt-2 space-y-2">
                  {tool.toolList.map((toolDef: any, index: number) => (
                    <div key={index} className="p-3 bg-gray-50 rounded-md">
                      <div className="font-medium text-sm">{toolDef.name}</div>
                      {toolDef.description && (
                        <div className="text-xs text-gray-600 mt-1">{toolDef.description}</div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* 审核操作按钮 */}
            {(tool.status === ToolStatus.WAITING_REVIEW || tool.status === ToolStatus.MANUAL_REVIEW) && (
              <div className="flex justify-end space-x-3 pt-4 border-t">
                <Button
                  variant="outline"
                  className="text-green-600 border-green-600 hover:bg-green-50"
                  onClick={() => handleReview(ToolStatus.APPROVED)}
                  disabled={loading}
                >
                  <CheckCircle className="w-4 h-4 mr-2" />
                  通过审核
                </Button>
                <Button
                  variant="outline"
                  className="text-red-600 border-red-600 hover:bg-red-50"
                  onClick={openRejectDialog}
                  disabled={loading}
                >
                  <XCircle className="w-4 h-4 mr-2" />
                  拒绝审核
                </Button>
              </div>
            )}

            {/* 时间信息 */}
            <div className="grid grid-cols-2 gap-4 pt-4 border-t text-xs text-gray-500">
              <div>创建时间: {new Date(tool.createdAt).toLocaleString()}</div>
              <div>更新时间: {new Date(tool.updatedAt).toLocaleString()}</div>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* 拒绝原因对话框 */}
      <AlertDialog open={isRejectDialogOpen} onOpenChange={setIsRejectDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>拒绝工具审核</AlertDialogTitle>
            <AlertDialogDescription>
              请输入拒绝该工具的详细原因，这将帮助开发者了解需要改进的地方。
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
            }}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleReject} className="bg-red-600 hover:bg-red-700">
              确认拒绝
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}