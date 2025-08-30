"use client";

import React, { useState, useEffect } from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Model } from "@/lib/user-settings-service";
import { CreateWidgetRequest, WidgetType } from "@/types/widget";
import { createWidgetWithToast } from "@/lib/agent-widget-service";
import { getDatasets } from "@/lib/rag-dataset-service";
import { RagDataset } from "@/types/rag-dataset";

interface CreateWidgetDialogProps {
  open: boolean;
  onClose: () => void;
  agentId: string;
  models: Model[];
  onSuccess: () => void;
}

export function CreateWidgetDialog({ open, onClose, agentId, models, onSuccess }: CreateWidgetDialogProps) {
  const [formData, setFormData] = useState<CreateWidgetRequest>({
    name: '',
    description: '',
    modelId: '',
    providerId: undefined,
    allowedDomains: [],
    dailyLimit: -1,
    widgetType: 'AGENT',
    knowledgeBaseIds: [],
  });
  const [domainsText, setDomainsText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [knowledgeBases, setKnowledgeBases] = useState<RagDataset[]>([]);
  const [loadingKnowledgeBases, setLoadingKnowledgeBases] = useState(false);

  // 加载知识库列表
  useEffect(() => {
    if (open) {
      loadKnowledgeBases();
    }
  }, [open]);

  const loadKnowledgeBases = async () => {
    setLoadingKnowledgeBases(true);
    try {
      const response = await getDatasets({ page: 1, pageSize: 100 });
      if (response.code === 200) {
        setKnowledgeBases(response.data.records || []);
      }
    } catch (error) {
 
    } finally {
      setLoadingKnowledgeBases(false);
    }
  };

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      return;
    }
    if (!formData.modelId) {
      return;
    }
    // RAG 类型必须选择至少一个知识库
    if (formData.widgetType === 'RAG' && (!formData.knowledgeBaseIds || formData.knowledgeBaseIds.length === 0)) {
      return;
    }

    setSubmitting(true);
    try {
      // 处理域名列表
      const domains = domainsText
        .split('\n')
        .map(d => d.trim())
        .filter(d => d.length > 0);

      const requestData: CreateWidgetRequest = {
        ...formData,
        allowedDomains: domains,
      };

      const response = await createWidgetWithToast(agentId, requestData);
      
      if (response.code === 200) {
        onSuccess();
        handleClose();
      }
    } catch (error) {
 
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    setFormData({
      name: '',
      description: '',
      modelId: '',
      providerId: undefined,
      allowedDomains: [],
      dailyLimit: -1,
      widgetType: 'AGENT',
      knowledgeBaseIds: [],
    });
    setDomainsText('');
    onClose();
  };

  const handleKnowledgeBaseChange = (datasetId: string, checked: boolean) => {
    setFormData(prev => ({
      ...prev,
      knowledgeBaseIds: checked
        ? [...(prev.knowledgeBaseIds || []), datasetId]
        : (prev.knowledgeBaseIds || []).filter(id => id !== datasetId)
    }));
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>创建小组件配置</DialogTitle>
          <DialogDescription>
            为你的Agent创建一个小组件配置，让它可以嵌入到任何网站中使用
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="widgetType">组件类型 *</Label>
            <Select
              value={formData.widgetType}
              onValueChange={(value: WidgetType) => setFormData({ 
                ...formData, 
                widgetType: value, 
                knowledgeBaseIds: value === 'AGENT' ? [] : formData.knowledgeBaseIds 
              })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="AGENT">Agent 模式</SelectItem>
                <SelectItem value="RAG">RAG 问答模式</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              {formData.widgetType === 'AGENT' 
                ? 'Agent 模式：使用完整的 Agent 功能，包括工具调用等'
                : 'RAG 问答模式：专门用于文档问答，响应更快'}
            </p>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="name">配置名称 *</Label>
            <Input
              id="name"
              placeholder={formData.widgetType === 'AGENT' ? "例如：官网客服助手" : "例如：文档问答助手"}
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
          </div>

          <div className="grid gap-2">
            <Label htmlFor="description">配置描述</Label>
            <Textarea
              id="description"
              placeholder="简单描述这个小组件配置的用途"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          {formData.widgetType === 'RAG' && (
            <div className="grid gap-2">
              <Label>选择知识库 *</Label>
              {loadingKnowledgeBases ? (
                <div className="text-sm text-muted-foreground">加载知识库列表中...</div>
              ) : knowledgeBases.length === 0 ? (
                <div className="text-sm text-muted-foreground">暂无可用的知识库</div>
              ) : (
                <div className="max-h-32 overflow-y-auto border rounded-md p-3 space-y-2">
                  {knowledgeBases.map(kb => (
                    <div key={kb.id} className="flex items-center space-x-2">
                      <Checkbox
                        id={`kb-${kb.id}`}
                        checked={formData.knowledgeBaseIds?.includes(kb.id) || false}
                        onCheckedChange={(checked) => handleKnowledgeBaseChange(kb.id, checked as boolean)}
                      />
                      <Label 
                        htmlFor={`kb-${kb.id}`} 
                        className="text-sm font-normal cursor-pointer flex-1"
                      >
                        {kb.name}
                        {kb.description && (
                          <span className="text-muted-foreground ml-1">- {kb.description}</span>
                        )}
                      </Label>
                    </div>
                  ))}
                </div>
              )}
              <p className="text-xs text-muted-foreground">
                RAG 模式需要至少选择一个知识库用于文档检索
              </p>
            </div>
          )}

          <div className="grid gap-2">
            <Label htmlFor="model">选择模型 *</Label>
            <Select
              value={formData.modelId}
              onValueChange={(value) => setFormData({ ...formData, modelId: value })}
            >
              <SelectTrigger>
                <SelectValue placeholder="选择要使用的模型" />
              </SelectTrigger>
              <SelectContent>
                {models.map(model => (
                  <SelectItem key={model.id} value={model.id}>
                    {model.name} - {model.providerName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="dailyLimit">每日调用限制</Label>
            <Select
              value={formData.dailyLimit.toString()}
              onValueChange={(value) => setFormData({ ...formData, dailyLimit: parseInt(value) })}
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="-1">无限制</SelectItem>
                <SelectItem value="100">100次/天</SelectItem>
                <SelectItem value="500">500次/天</SelectItem>
                <SelectItem value="1000">1000次/天</SelectItem>
                <SelectItem value="5000">5000次/天</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="grid gap-2">
            <Label htmlFor="allowedDomains">允许的域名（可选）</Label>
            <Textarea
              id="allowedDomains"
              placeholder="每行一个域名，例如：&#10;example.com&#10;www.example.com&#10;留空表示允许所有域名"
              value={domainsText}
              onChange={(e) => setDomainsText(e.target.value)}
              rows={4}
            />
            <p className="text-xs text-muted-foreground">
              设置后，只有这些域名的网站可以嵌入使用。支持通配符，如 *.example.com
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose}>
            取消
          </Button>
          <Button 
            onClick={handleSubmit}
            disabled={
              !formData.name.trim() || 
              !formData.modelId || 
              (formData.widgetType === 'RAG' && (!formData.knowledgeBaseIds || formData.knowledgeBaseIds.length === 0)) ||
              submitting
            }
          >
            {submitting ? "创建中..." : "创建配置"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}