"use client";

import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { 
  Database, 
  FileText, 
  Users, 
  Clock, 
  Tag, 
  BarChart3
} from "lucide-react";
import { 
  RagVersionDTO, 
  RagContentPreviewDTO,
  getPublishStatusText,
  getPublishStatusColor,
  formatFileSize,
  AdminRagService
} from "@/lib/admin-rag-service";
import { useToast } from "@/hooks/use-toast";

interface RagDetailsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rag: RagVersionDTO | null;
}

export function RagDetailsDialog({ 
  open, 
  onOpenChange, 
  rag 
}: RagDetailsDialogProps) {
  const [contentPreview, setContentPreview] = useState<RagContentPreviewDTO | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const { toast } = useToast();

  // 加载内容预览
  useEffect(() => {
    if (open && rag) {
      loadContentPreview();
    }
  }, [open, rag]);

  const loadContentPreview = async () => {
    if (!rag) return;
    
    try {
      setPreviewLoading(true);
      const response = await AdminRagService.getRagContentPreview(rag.id);
      if (response.code === 200) {
        setContentPreview(response.data);
      }
    } catch (error) {
      console.error('加载内容预览失败:', error);
    } finally {
      setPreviewLoading(false);
    }
  };

  if (!rag) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-6xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center space-x-2">
            <Database className="w-5 h-5" />
            <span>RAG详情 - {rag.name}</span>
          </DialogTitle>
          <DialogDescription>
            查看RAG版本的详细信息和内容
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="basic" className="w-full">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="basic">基本信息</TabsTrigger>
            <TabsTrigger value="files">文件列表</TabsTrigger>
            <TabsTrigger value="documents">文档内容</TabsTrigger>
            <TabsTrigger value="stats">统计信息</TabsTrigger>
          </TabsList>

          <TabsContent value="basic" className="space-y-4">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* 基本信息卡片 */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center space-x-2">
                    <Database className="w-5 h-5" />
                    <span>基本信息</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
                      {rag.icon ? (
                        <img src={rag.icon} alt={rag.name} className="w-10 h-10 rounded" />
                      ) : (
                        <Database className="h-6 w-6 text-gray-400" />
                      )}
                    </div>
                    <div>
                      <h3 className="font-semibold text-lg">{rag.name}</h3>
                      <p className="text-sm text-gray-500">版本 {rag.version}</p>
                    </div>
                  </div>

                  <div>
                    <Label className="text-sm font-medium text-gray-500">状态</Label>
                    <div className="mt-1">
                      <Badge className={getPublishStatusColor(rag.publishStatus)}>
                        {getPublishStatusText(rag.publishStatus)}
                      </Badge>
                    </div>
                  </div>

                  <div>
                    <Label className="text-sm font-medium text-gray-500">描述</Label>
                    <p className="text-sm text-gray-700 mt-1">
                      {rag.description || "暂无描述"}
                    </p>
                  </div>

                  {rag.changeLog && (
                    <div>
                      <Label className="text-sm font-medium text-gray-500">更新日志</Label>
                      <div className="mt-1 text-sm text-gray-700 bg-gray-50 p-3 rounded-lg max-h-32 overflow-y-auto">
                        <pre className="whitespace-pre-wrap font-sans">{rag.changeLog}</pre>
                      </div>
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
                </CardContent>
              </Card>

              {/* 创建者信息卡片 */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center space-x-2">
                    <Users className="w-5 h-5" />
                    <span>创建者信息</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center space-x-3">
                    <Avatar className="h-10 w-10">
                      <AvatarFallback>
                        {rag.userNickname?.charAt(0) || 'U'}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="font-medium">{rag.userNickname || '未知用户'}</div>
                      <div className="text-sm text-gray-500">创建者</div>
                    </div>
                  </div>

                  <Separator />

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label className="text-sm font-medium text-gray-500">创建时间</Label>
                      <div className="flex items-center space-x-2 mt-1">
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span className="text-sm">{new Date(rag.createdAt).toLocaleString('zh-CN')}</span>
                      </div>
                    </div>
                    <div>
                      <Label className="text-sm font-medium text-gray-500">最后更新</Label>
                      <div className="flex items-center space-x-2 mt-1">
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span className="text-sm">{new Date(rag.updatedAt).toLocaleString('zh-CN')}</span>
                      </div>
                    </div>
                  </div>

                  {rag.reviewTime && (
                    <div>
                      <Label className="text-sm font-medium text-gray-500">审核时间</Label>
                      <div className="flex items-center space-x-2 mt-1">
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span className="text-sm">{new Date(rag.reviewTime).toLocaleString('zh-CN')}</span>
                      </div>
                    </div>
                  )}

                  {rag.publishedAt && (
                    <div>
                      <Label className="text-sm font-medium text-gray-500">发布时间</Label>
                      <div className="flex items-center space-x-2 mt-1">
                        <Clock className="w-4 h-4 text-gray-400" />
                        <span className="text-sm">{new Date(rag.publishedAt).toLocaleString('zh-CN')}</span>
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

            {/* 统计信息 */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center space-x-2">
                  <BarChart3 className="w-5 h-5" />
                  <span>统计信息</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
              </CardContent>
            </Card>

            {/* 拒绝原因 */}
            {rag.rejectReason && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg text-red-600">拒绝原因</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="bg-red-50 p-4 rounded-lg">
                    <p className="text-sm text-red-700">{rag.rejectReason}</p>
                  </div>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="files" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center space-x-2">
                  <FileText className="w-5 h-5" />
                  <span>文件列表 ({contentPreview?.files?.length || 0})</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {previewLoading ? (
                  <div className="flex items-center justify-center h-32">
                    <div className="text-sm text-gray-500">加载中...</div>
                  </div>
                ) : contentPreview?.files ? (
                  <div className="space-y-3">
                    {contentPreview.files.map((file, index) => (
                      <div key={index} className="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50">
                        <div className="flex items-center space-x-3">
                          <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                            <FileText className="w-5 h-5 text-blue-600" />
                          </div>
                          <div>
                            <div className="font-medium">{file.fileName}</div>
                            <div className="text-sm text-gray-500">
                              {file.fileType} • {formatFileSize(file.fileSize)}
                            </div>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
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

          <TabsContent value="documents" className="space-y-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center space-x-2">
                  <FileText className="w-5 h-5" />
                  <span>文档内容预览</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {previewLoading ? (
                  <div className="flex items-center justify-center h-32">
                    <div className="text-sm text-gray-500">加载中...</div>
                  </div>
                ) : contentPreview?.sampleDocuments ? (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div className="text-sm text-gray-600">
                        文档内容预览（共 {contentPreview.sampleDocuments.length} 个文档单元）
                      </div>
                    </div>
                    <ScrollArea className="h-[600px]">
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
                                <div className="flex items-center justify-between border-b pb-3">
                                  <div className="flex items-center space-x-2">
                                    <FileText className="w-4 h-4 text-gray-400" />
                                    <span className="text-sm font-medium text-gray-700">
                                      {fileName}
                                    </span>
                                  </div>
                                  <div className="text-xs text-gray-500">
                                    {sortedDocs.length} 个文档单元
                                  </div>
                                </div>
                                <div className="space-y-3">
                                  {sortedDocs.map((doc, index) => {
                                    // 如果页码存在且大于0，显示页码，否则显示顺序号
                                    const hasValidPage = doc.page !== null && doc.page !== undefined && doc.page > 0;
                                    const pageDisplay = hasValidPage ? `第 ${doc.page} 页` : `文档片段 ${index + 1}`;
                                    
                                    return (
                                      <div key={doc.id || index} className="bg-gray-50 rounded-lg p-3">
                                        <div className="flex items-center justify-between mb-2">
                                          <Badge variant="outline" className="text-xs">
                                            {pageDisplay}
                                          </Badge>
                                          <div className="text-xs text-gray-500">
                                            {index + 1} / {sortedDocs.length}
                                          </div>
                                        </div>
                                        <div className="text-sm text-gray-700 max-h-40 overflow-y-auto">
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
                    暂无文档内容
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="stats" className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center space-x-2">
                    <BarChart3 className="w-5 h-5" />
                    <span>内容统计</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">文件数量</span>
                      <span className="font-medium">{rag.fileCount}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">文档单元</span>
                      <span className="font-medium">{rag.documentCount}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">总大小</span>
                      <span className="font-medium">{formatFileSize(rag.totalSize)}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">平均文档大小</span>
                      <span className="font-medium">
                        {rag.documentCount > 0 ? formatFileSize(rag.totalSize / rag.documentCount) : "0 B"}
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle className="text-lg flex items-center space-x-2">
                    <Users className="w-5 h-5" />
                    <span>使用统计</span>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">版本号</span>
                      <span className="font-medium">v{rag.version}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-sm text-gray-600">原始RAG</span>
                      <span className="font-medium text-blue-600">
                        {rag.originalRagName || "未知"}
                      </span>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

        </Tabs>
      </DialogContent>
    </Dialog>
  );
}