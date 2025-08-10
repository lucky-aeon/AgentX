"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toast } from "@/hooks/use-toast";

import { Rule, RuleFormData, UpdateRuleRequest } from "@/types/rule";
import { RuleHandlerKey, RuleHandlerKeyNames } from "@/types/billing";
import { AdminRuleServiceWithToast } from "@/lib/admin-rule-service";

interface RuleEditDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rule: Rule;
  onSuccess: () => void;
}

export function RuleEditDialog({ open, onOpenChange, rule, onSuccess }: RuleEditDialogProps) {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<RuleFormData>({
    name: "",
    handlerKey: RuleHandlerKey.MODEL_TOKEN_STRATEGY,
    description: ""
  });

  // 初始化表单数据
  useEffect(() => {
    if (open && rule) {
      setFormData({
        name: rule.name,
        handlerKey: rule.handlerKey as RuleHandlerKey,
        description: rule.description || ""
      });
    }
  }, [open, rule]);

  // 处理表单提交
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name.trim()) {
      toast({
        title: "请输入规则名称",
        variant: "destructive"
      });
      return;
    }

    setLoading(true);

    try {
      const request: UpdateRuleRequest = {
        name: formData.name.trim(),
        handlerKey: formData.handlerKey,
        description: formData.description.trim() || undefined
      };

      const response = await AdminRuleServiceWithToast.updateRule(rule.id, request);

      if (response.code === 200) {
        onSuccess();
      }
    } catch (error) {
      console.error('更新规则失败:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>编辑规则</DialogTitle>
          <DialogDescription>
            修改规则的配置信息
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">规则名称 *</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              placeholder="输入规则名称"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="handlerKey">处理器标识 *</Label>
            <Select 
              value={formData.handlerKey} 
              onValueChange={(value) => setFormData({ ...formData, handlerKey: value as RuleHandlerKey })}
            >
              <SelectTrigger>
                <SelectValue placeholder="选择处理器类型" />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(RuleHandlerKeyNames).map(([key, name]) => (
                  <SelectItem key={key} value={key}>
                    {name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">规则描述</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="输入规则描述（可选）"
              rows={3}
            />
          </div>

          <DialogFooter>
            <Button 
              type="button" 
              variant="outline" 
              onClick={() => onOpenChange(false)}
              disabled={loading}
            >
              取消
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "保存中..." : "保存修改"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}