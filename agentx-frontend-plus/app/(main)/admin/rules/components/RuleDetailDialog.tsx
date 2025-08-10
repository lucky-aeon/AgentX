"use client";

import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";

import { Rule } from "@/types/rule";
import { RuleHandlerKey, RuleHandlerKeyNames } from "@/types/billing";

interface RuleDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rule: Rule;
}

export function RuleDetailDialog({ open, onOpenChange, rule }: RuleDetailDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>规则详情</DialogTitle>
          <DialogDescription>
            查看规则的详细信息
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* 基本信息 */}
          <Card>
            <CardHeader>
              <CardTitle>基本信息</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <div className="text-sm text-muted-foreground">规则名称</div>
                <div className="font-medium">{rule.name}</div>
              </div>

              <Separator />

              <div>
                <div className="text-sm text-muted-foreground">处理器标识</div>
                <Badge variant="outline" className="font-mono">
                  {RuleHandlerKeyNames[rule.handlerKey as RuleHandlerKey] || rule.handlerKey}
                </Badge>
              </div>

              {rule.description && (
                <>
                  <Separator />
                  <div>
                    <div className="text-sm text-muted-foreground">规则描述</div>
                    <div className="text-sm mt-1">{rule.description}</div>
                  </div>
                </>
              )}

              <Separator />

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">创建时间</div>
                  <div className="text-sm">{new Date(rule.createdAt).toLocaleString('zh-CN')}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">更新时间</div>
                  <div className="text-sm">{new Date(rule.updatedAt).toLocaleString('zh-CN')}</div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 策略说明 */}
          <Card>
            <CardHeader>
              <CardTitle>策略说明</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-sm text-muted-foreground">
                {getStrategyDescription(rule.handlerKey as RuleHandlerKey)}
              </div>
            </CardContent>
          </Card>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// 获取策略说明
function getStrategyDescription(handlerKey: RuleHandlerKey): string {
  switch (handlerKey) {
    case RuleHandlerKey.MODEL_TOKEN_STRATEGY:
      return "基于模型输入输出Token数量进行计费，支持分别设置输入和输出Token的单价。适用于AI模型调用场景。";
    
    case RuleHandlerKey.PER_UNIT_STRATEGY:
      return "按使用次数进行固定计费，每次使用收取固定费用。适用于API调用、功能使用等场景。";
      
    case RuleHandlerKey.TIERED_PRICING_STRATEGY:
      return "基于使用量的分层定价策略，不同使用量区间采用不同单价。使用量越大，单价越低，适合大量使用的场景。";
      
    default:
      return "该处理器类型暂无详细说明。";
  }
}