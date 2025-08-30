"use client";

import React, { useState, useEffect } from 'react';
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";
import { MoreHorizontal, Plus, ExternalLink, Code, Copy, Eye, Settings, Trash, Power, PowerOff } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
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
import { toast } from "@/hooks/use-toast";
import { AgentWidget } from "@/types/widget";
import { Model } from "@/lib/user-settings-service";
import { getWidgetsWithToast, toggleWidgetStatusWithToast, deleteWidgetWithToast } from "@/lib/agent-widget-service";
import { getAllModelsWithToast } from "@/lib/user-settings-service";
import { CreateWidgetDialog } from "./CreateWidgetDialog";
import { UpdateWidgetDialog } from "./UpdateWidgetDialog";
import WidgetCodeDialog from "./WidgetCodeDialog";

interface AgentWidgetTabProps {
  agentId: string;
}

export function AgentWidgetTab({ agentId }: AgentWidgetTabProps) {
  const [widgets, setWidgets] = useState<AgentWidget[]>([]);
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(true);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [updateDialogOpen, setUpdateDialogOpen] = useState(false);
  const [widgetCodeDialogOpen, setWidgetCodeDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedWidget, setSelectedWidget] = useState<AgentWidget | null>(null);

  // 加载小组件配置列表
  const loadWidgets = async () => {
    setLoading(true);
    try {
      const response = await getWidgetsWithToast(agentId);
      if (response.code === 200) {
        setWidgets(response.data || []);
      }
    } catch (error) {
 
    } finally {
      setLoading(false);
    }
  };

  // 加载模型列表
  const loadModels = async () => {
    try {
      const response = await getAllModelsWithToast();
      if (response.code === 200) {
        setModels(response.data || []);
      }
    } catch (error) {
 
    }
  };

  useEffect(() => {
    loadWidgets();
    loadModels();
  }, [agentId]);

  // 切换启用状态
  const handleToggleStatus = async (widget: AgentWidget) => {
    try {
      const response = await toggleWidgetStatusWithToast(agentId, widget.id);
      if (response.code === 200) {
        loadWidgets(); // 重新加载数据
      }
    } catch (error) {
 
    }
  };

  // 打开删除确认对话框
  const handleDeleteClick = (widget: AgentWidget) => {
    setSelectedWidget(widget);
    setDeleteDialogOpen(true);
  };

  // 删除小组件配置
  const handleDeleteConfirm = async () => {
    if (!selectedWidget) return;

    try {
      const response = await deleteWidgetWithToast(agentId, selectedWidget.id);
      if (response.code === 200) {
        loadWidgets(); // 重新加载数据
        setDeleteDialogOpen(false);
        setSelectedWidget(null);
      }
    } catch (error) {
 
    }
  };

  // 复制嵌入代码
  const handleCopyWidgetCode = async (widgetCode: string) => {
    try {
      await navigator.clipboard.writeText(widgetCode);
      toast({
        title: "复制成功",
        description: "嵌入代码已复制到剪贴板",
      });
    } catch (error) {
      toast({
        title: "复制失败",
        description: "无法复制到剪贴板",
        variant: "destructive",
      });
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="text-muted-foreground">加载中...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 头部说明 */}
      <div className="flex items-start justify-between">
        <div>
          <h3 className="text-lg font-semibold">网站小组件</h3>
          <p className="text-sm text-muted-foreground">
            创建小组件配置，让你的Agent可以嵌入到任何网站中使用
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          创建小组件配置
        </Button>
      </div>

      {/* 小组件配置列表 */}
      {widgets.length === 0 ? (
        <Card>
          <CardContent className="py-8">
            <div className="text-center">
              <Code className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">还没有小组件配置</h3>
              <p className="text-muted-foreground mb-4">
                创建你的第一个小组件配置，让网站访客可以直接与你的Agent对话
              </p>
              <Button onClick={() => setCreateDialogOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                创建小组件配置
              </Button>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {widgets.map(widget => (
            <WidgetConfigCard 
              key={widget.id}
              widget={widget}
              onToggleStatus={handleToggleStatus}
              onEdit={(widget) => {
                setSelectedWidget(widget);
                setUpdateDialogOpen(true);
              }}
              onViewCode={(widget) => {
                setSelectedWidget(widget);
                setWidgetCodeDialogOpen(true);
              }}
              onCopyCode={handleCopyWidgetCode}
              onDelete={handleDeleteClick}
            />
          ))}
        </div>
      )}

      {/* 对话框 */}
      <CreateWidgetDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        agentId={agentId}
        models={models}
        onSuccess={loadWidgets}
      />

      {selectedWidget && (
        <>
          <UpdateWidgetDialog
            open={updateDialogOpen}
            onClose={() => {
              setUpdateDialogOpen(false);
              setSelectedWidget(null);
            }}
            agentId={agentId}
            widget={selectedWidget}
            models={models}
            onSuccess={loadWidgets}
          />

          <WidgetCodeDialog
            open={widgetCodeDialogOpen}
            onClose={() => {
              setWidgetCodeDialogOpen(false);
              setSelectedWidget(null);
            }}
            widget={selectedWidget}
            onCopy={handleCopyWidgetCode}
          />
        </>
      )}

      {/* 删除确认对话框 */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除小组件配置 "{selectedWidget?.name}" 吗？
              <br />
              此操作不可撤销，删除后所有嵌入在网站中的组件将停止工作。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel 
              onClick={() => {
                setDeleteDialogOpen(false);
                setSelectedWidget(null);
              }}
            >
              取消
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteConfirm}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              确认删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

// 小组件配置卡片组件
interface WidgetConfigCardProps {
  widget: AgentWidget;
  onToggleStatus: (widget: AgentWidget) => void;
  onEdit: (widget: AgentWidget) => void;
  onViewCode: (widget: AgentWidget) => void;
  onCopyCode: (widgetCode: string) => void;
  onDelete: (widget: AgentWidget) => void;
}

function WidgetConfigCard({ widget, onToggleStatus, onEdit, onViewCode, onCopyCode, onDelete }: WidgetConfigCardProps) {
  const widgetUrl = `${window.location.origin}/widget/${widget.publicId}`;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg">{widget.name}</CardTitle>
            {widget.description && (
              <CardDescription>{widget.description}</CardDescription>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Badge variant={widget.enabled ? "default" : "secondary"}>
              {widget.enabled ? "已启用" : "已禁用"}
            </Badge>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => onEdit(widget)}>
                  <Settings className="h-4 w-4 mr-2" />
                  编辑配置
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => onViewCode(widget)}>
                  <Code className="h-4 w-4 mr-2" />
                  查看嵌入代码
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => onCopyCode(widget.widgetCode)}>
                  <Copy className="h-4 w-4 mr-2" />
                  复制嵌入代码
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => window.open(widgetUrl, '_blank')}>
                  <ExternalLink className="h-4 w-4 mr-2" />
                  预览页面
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => onToggleStatus(widget)}>
                  {widget.enabled ? (
                    <>
                      <PowerOff className="h-4 w-4 mr-2" />
                      禁用
                    </>
                  ) : (
                    <>
                      <Power className="h-4 w-4 mr-2" />
                      启用
                    </>
                  )}
                </DropdownMenuItem>
                <DropdownMenuItem 
                  onClick={() => onDelete(widget)}
                  className="text-destructive"
                >
                  <Trash className="h-4 w-4 mr-2" />
                  删除
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <Label className="text-muted-foreground">组件类型</Label>
            <div>
              <Badge variant={widget.widgetType === 'RAG' ? "secondary" : "outline"}>
                {widget.widgetType === 'RAG' ? 'RAG 问答模式' : 'Agent 模式'}
              </Badge>
            </div>
          </div>
          <div>
            <Label className="text-muted-foreground">使用模型</Label>
            <p>{widget.model?.name}</p>
          </div>
          <div>
            <Label className="text-muted-foreground">每日限制</Label>
            <p>{widget.dailyLimit === -1 ? "无限制" : `${widget.dailyLimit} 次`}</p>
          </div>
          {widget.widgetType === 'RAG' && widget.knowledgeBaseIds && widget.knowledgeBaseIds.length > 0 && (
            <div>
              <Label className="text-muted-foreground">知识库数量</Label>
              <p>{widget.knowledgeBaseIds.length} 个</p>
            </div>
          )}
          <div className="col-span-2">
            <Label className="text-muted-foreground">访问链接</Label>
            <p className="font-mono text-xs break-all text-blue-600">
              {widgetUrl}
            </p>
          </div>
          <div className="col-span-2">
            <Label className="text-muted-foreground">允许域名</Label>
            <p>{widget.allowedDomains.length > 0 ? widget.allowedDomains.join(", ") : "所有域名"}</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}