"use client";

import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Database, FileText, Users, Clock, Tag } from "lucide-react";
import { 
  RagVersionDTO, 
  RagContentPreviewDTO,
  RagPublishStatus,
  getPublishStatusText,
  getPublishStatusColor,
  formatFileSize,
  AdminRagService,
  reviewRagVersionWithToast
} from "@/lib/admin-rag-service";
import { useToast } from "@/hooks/use-toast";

interface RagReviewDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rag: RagVersionDTO | null;
  onReviewComplete: () => void;
}

export function RagReviewDialog({ 
  open, 
  onOpenChange, 
  rag, 
  onReviewComplete 
}: RagReviewDialogProps) {
  const [status, setStatus] = useState<number>(RagPublishStatus.PUBLISHED);
  const [rejectReason, setRejectReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [contentPreview, setContentPreview] = useState<RagContentPreviewDTO | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const { toast } = useToast();

  // 重置表单
  useEffect(() => {
    if (open && rag) {
      setStatus(RagPublishStatus.PUBLISHED);
      setRejectReason("");
      loadContentPreview();
    }
  }, [open, rag]);

  // 加载内容预览
  const loadContentPreview = async () => {
    if (!rag) return;
    
    try {
      setPreviewLoading(true);
      const response = await AdminRagService.getRagContentPreview(rag.id);
      if (response.code === 200) {
        setContentPreview(response.data);
      }
    } catch (error) {
 
    } finally {
      setPreviewLoading(false);
    }
  };

  // 提交审核
  const handleReview = async () => {
    if (!rag) return;
    
    if (status === RagPublishStatus.REJECTED && !rejectReason.trim()) {
      toast({
        title: "请填写拒绝原因",
        description: "拒绝时必须填写拒绝原因",
        variant: "destructive",
      });
      return;
    }

    try {
      setSubmitting(true);
      const response = await reviewRagVersionWithToast(rag.id, {
        status,
        rejectReason: status === RagPublishStatus.REJECTED ? rejectReason : undefined
      });
      
      if (response.code === 200) {
        onOpenChange(false);
        onReviewComplete();
      }
    } catch (error) {
 
    } finally {
      setSubmitting(false);
    }
  };

  if (!rag) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <Database className="w-5 h-5" />
            <span>RAG审核 - {rag.name}</span>
          </DialogTitle>
          <DialogDescription>
            审核RAG版本的内容和发布状态
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="basic" className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="basic">基本信息</TabsTrigger>
            <TabsTrigger value="files">文件列表</TabsTrigger>
            <TabsTrigger value="content">内容预览</TabsTrigger>
            <TabsTrigger value="review">审核操作</TabsTrigger>
          </TabsList>

          <TabsContent value="basic" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">基本信息</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <Label className="text-sm font-medium text-gray-500">RAG名称</Label>
                    <p className="text-sm">{rag.name}</p>
                  </div>
                  <div>
                    <Label className="text-sm font-medium text-gray-500">版本号</Label>
                    <p className="text-sm">v{rag.version}</p>
                  </div>
                  <div>
                    <Label className="text-sm font-medium text-gray-500">创建者</Label>
                    <div className="flex items-center space-x-2">
                      <Avatar className="h-6 w-6">
                        <AvatarFallback>
                          {rag.userNickname?.charAt(0) || 'U'}
                        </AvatarFallback>
                      </Avatar>
                      <span className="text-sm">{rag.userNickname || '未知用户'}</span>
                    </div>
                  </div>
                  <div>
                    <Label className="text-sm font-medium text-gray-500">当前状态</Label>
                    <Badge className={getPublishStatusColor(rag.publishStatus)}>
                      {getPublishStatusText(rag.publishStatus)}
                    </Badge>
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-sm font-medium text-gray-500">描述</Label>
                  <p className="text-sm text-gray-700 mt-1">
                    {rag.description || "暂无描述"}
                  </p>
                </div>

                {rag.changeLog && (
                  <div>
                    <Label className="text-sm font-medium text-gray-500">更新日志</Label>
                    <p className="text-sm text-gray-700 mt-1 whitespace-pre-wrap">
                      {rag.changeLog}
                    </p>
                  </div>
                )}

                {rag.labels && rag.labels.length > 0 && (
                  <div>
                    <Label className="text-sm font-medium text-gray-500">标签</Label>
                    <div className="flex flex-wrap gap-2 mt-1">
                      {rag.labels.map((label, index) => (
                        <Badge key={index} variant="outline" className="text-xs">
                          <Tag className="w-3 h-3 mr-1" />
                          {label}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                <Separator />

                <div className="grid grid-cols-3 gap-4">
                  <div className="text-center">
                    <div className="text-2xl font-bold text-blue-600">{rag.fileCount}</div>
                    <div className="text-sm text-gray-500">文件数量</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-green-600">{rag.documentCount}</div>
                    <div className="text-sm text-gray-500">文档单元</div>
                  </div>
                  <div className="text-center">
                    <div className="text-2xl font-bold text-purple-600">{formatFileSize(rag.totalSize)}</div>
                    <div className="text-sm text-gray-500">总大小</div>
                  </div>
                </div>

                <Separator />

                <div>
                  <Label className="text-sm font-medium text-gray-500">创建时间</Label>
                  <div className="flex items-center space-x-2 mt-1">
                    <Clock className="w-4 h-4 text-gray-400" />
                    <span className="text-sm">{new Date(rag.createdAt).toLocaleString('zh-CN')}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="files" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">文件列表</CardTitle>
              </CardHeader>
              <CardContent>
                {previewLoading ? (
                  <div className="flex items-center justify-center h-32">
                    <div className="text-sm text-gray-500">加载中...</div>
                  </div>
                ) : contentPreview?.files ? (
                  <div className="space-y-3">
                    {contentPreview.files.map((file, index) => (
                      <div key={index} className="flex items-center justify-between p-3 border rounded-lg">
                        <div className="flex items-center space-x-3">
                          <FileText className="w-5 h-5 text-gray-400" />
                          <div>
                            <div className="font-medium">{file.fileName}</div>
                            <div className="text-sm text-gray-500">
                              {file.fileType} • {formatFileSize(file.fileSize)}
                            </div>
                          </div>
                        </div>
                        <div className="flex space-x-2">
                          <Badge variant="outline" className="text-xs">
                            {file.fileType}
                          </Badge>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    暂无文件信息
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="content" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">内容预览</CardTitle>
              </CardHeader>
              <CardContent>
                {previewLoading ? (
                  <div className="flex items-center justify-center h-32">
                    <div className="text-sm text-gray-500">加载中...</div>
                  </div>
                ) : contentPreview?.sampleDocuments ? (
                  <div className="space-y-4">
                    <div className="text-sm text-gray-600">
                      文档内容预览（共 {contentPreview.sampleDocuments.length} 个文档单元）：
                    </div>
                    <ScrollArea className="h-[400px]">
                      <div className="space-y-4">
                        {(() => {
                          // 按文件分组文档单元
                          const groupedDocs = contentPreview.sampleDocuments.reduce((acc, doc) => {
                            const fileName = doc.fileName || '未知文件';
                            if (!acc[fileName]) {
                              acc[fileName] = [];
                            }
                            acc[fileName].push(doc);
                            return acc;
                          }, {} as Record<string, typeof contentPreview.sampleDocuments>);

                          return Object.entries(groupedDocs).map(([fileName, docs]) => {
                            // 对每个文件内的文档按页码排序
                            const sortedDocs = docs.sort((a, b) => {
                              // 优先按页码排序
                              if (a.page && b.page) {
                                return a.page - b.page;
                              }
                              // 如果页码相同或缺失，保持原顺序
                              return 0;
                            });

                            return (
                              <div key={fileName} className="border rounded-lg p-4 space-y-3">
                                <div className="flex items-center justify-between border-b pb-2">
                                  <div className="text-sm font-medium text-gray-700 flex items-center space-x-2">
                                    <FileText className="w-4 h-4" />
                                    <span>{fileName}</span>
                                  </div>
                                  <div className="text-xs text-gray-500">
                                    {sortedDocs.length} 个文档单元
                                  </div>
                                </div>
                                <div className="space-y-2">
                                  {sortedDocs.map((doc, index) => {
                                    // 如果页码存在且大于0，显示页码，否则显示顺序号
                                    const hasValidPage = doc.page !== null && doc.page !== undefined && doc.page > 0;
                                    const pageDisplay = hasValidPage ? `第 ${doc.page} 页` : `文档片段 ${index + 1}`;
                                    
                                    return (
                                      <div key={doc.id || index} className="bg-gray-50 rounded p-3">
                                        <div className="text-xs text-gray-500 mb-1">
                                          {pageDisplay}
                                        </div>
                                        <div className="text-sm text-gray-700 max-h-24 overflow-y-auto">
                                          {doc.content}
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>
                            );
                          });
                        })()}
                      </div>
                    </ScrollArea>
                  </div>
                ) : (
                  <div className="text-center py-8 text-gray-500">
                    暂无内容预览
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="review" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">审核操作</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <Label htmlFor="status">审核状态</Label>
                  <Select 
                    value={status.toString()} 
                    onValueChange={(value) => setStatus(parseInt(value))}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={RagPublishStatus.PUBLISHED.toString()}>
                        通过 - 发布到市场
                      </SelectItem>
                      <SelectItem value={RagPublishStatus.REJECTED.toString()}>
                        拒绝 - 退回修改
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {status === RagPublishStatus.REJECTED && (
                  <div>
                    <Label htmlFor="reason">拒绝原因</Label>
                    <Textarea
                      id="reason"
                      placeholder="请详细说明拒绝的原因，帮助用户改进..."
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      className="min-h-[100px]"
                    />
                  </div>
                )}

                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-medium mb-2">审核建议</h4>
                  <ul className="text-sm text-gray-600 space-y-1">
                    <li>• 检查内容是否符合社区规范</li>
                    <li>• 确认文件和文档内容完整性</li>
                    <li>• 评估RAG的实用性和质量</li>
                    <li>• 如有疑问，可以先拒绝并给出改进建议</li>
                  </ul>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={handleReview} disabled={submitting}>
            {submitting ? "处理中..." : "提交审核"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}