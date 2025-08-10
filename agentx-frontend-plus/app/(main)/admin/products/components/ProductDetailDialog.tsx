"use client";

import { useState, useEffect } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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

import { Product } from "@/types/product";
import { BillingType, BillingTypeNames, ProductStatus, ProductStatusNames } from "@/types/billing";
import { Rule } from "@/types/rule";
import { AdminRuleService } from "@/lib/admin-rule-service";

interface ProductDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  product: Product;
}

export function ProductDetailDialog({ open, onOpenChange, product }: ProductDetailDialogProps) {
  const [rule, setRule] = useState<Rule | null>(null);
  const [loadingRule, setLoadingRule] = useState(false);

  // 加载关联的规则信息
  useEffect(() => {
    if (open && product.ruleId) {
      loadRule();
    }
  }, [open, product.ruleId]);

  const loadRule = async () => {
    setLoadingRule(true);
    try {
      const response = await AdminRuleService.getRuleById(product.ruleId);
      if (response.code === 200) {
        setRule(response.data);
      }
    } catch (error) {
      console.error('加载规则信息失败:', error);
    } finally {
      setLoadingRule(false);
    }
  };

  // 格式化价格配置显示
  const formatPricingConfig = (config: Record<string, any>) => {
    if (!config || Object.keys(config).length === 0) {
      return "无配置";
    }

    return (
      <div className="space-y-2">
        {Object.entries(config).map(([key, value]) => (
          <div key={key} className="flex justify-between items-center text-sm">
            <span className="text-muted-foreground">{formatConfigKey(key)}:</span>
            <span className="font-mono">{formatConfigValue(value)}</span>
          </div>
        ))}
      </div>
    );
  };

  // 格式化配置键名
  const formatConfigKey = (key: string) => {
    const keyMap: Record<string, string> = {
      input_cost_per_million: '输入Token费用(每百万)',
      output_cost_per_million: '输出Token费用(每百万)',
      cost_per_unit: '单次费用',
      tiers: '价格阶梯'
    };
    return keyMap[key] || key;
  };

  // 格式化配置值
  const formatConfigValue = (value: any): string => {
    if (typeof value === 'number') {
      return value.toFixed(2);
    }
    if (Array.isArray(value)) {
      return `${value.length} 个阶梯`;
    }
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2);
    }
    return String(value);
  };

  // 渲染价格阶梯详情
  const renderTiersDetail = (tiers: any[]) => {
    if (!Array.isArray(tiers) || tiers.length === 0) {
      return "无阶梯配置";
    }

    return (
      <div className="space-y-2">
        {tiers.map((tier, index) => (
          <div key={index} className="p-3 bg-muted/50 rounded-lg">
            <div className="text-sm">
              <div className="flex justify-between">
                <span>数量范围:</span>
                <span className="font-mono">
                  {tier.min_quantity} {tier.max_quantity ? `- ${tier.max_quantity}` : '以上'}
                </span>
              </div>
              <div className="flex justify-between">
                <span>单价:</span>
                <span className="font-mono">{tier.unit_price?.toFixed(2) || '0.00'}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>商品详情</DialogTitle>
          <DialogDescription>
            查看商品的详细配置信息
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* 基本信息 */}
          <Card>
            <CardHeader>
              <CardTitle>基本信息</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">商品名称</div>
                  <div className="font-medium">{product.name}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">状态</div>
                  <Badge variant={product.status === ProductStatus.ACTIVE ? "default" : "secondary"}>
                    {ProductStatusNames[product.status as ProductStatus] || '未知'}
                  </Badge>
                </div>
              </div>

              <Separator />

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">商品类型</div>
                  <Badge variant="outline">
                    {BillingTypeNames[product.type as BillingType] || product.type}
                  </Badge>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">业务ID</div>
                  <div className="font-mono text-sm">{product.serviceId}</div>
                </div>
              </div>

              <Separator />

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <div className="text-sm text-muted-foreground">创建时间</div>
                  <div className="text-sm">{new Date(product.createdAt).toLocaleString('zh-CN')}</div>
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">更新时间</div>
                  <div className="text-sm">{new Date(product.updatedAt).toLocaleString('zh-CN')}</div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* 计费规则 */}
          <Card>
            <CardHeader>
              <CardTitle>计费规则</CardTitle>
            </CardHeader>
            <CardContent>
              {loadingRule ? (
                <div className="text-center py-4 text-muted-foreground">
                  加载规则信息中...
                </div>
              ) : rule ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <div className="text-sm text-muted-foreground">规则名称</div>
                      <div className="font-medium">{rule.name}</div>
                    </div>
                    <div>
                      <div className="text-sm text-muted-foreground">处理器标识</div>
                      <div className="font-mono text-sm">{rule.handlerKey}</div>
                    </div>
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
                </div>
              ) : (
                <div className="text-center py-4 text-muted-foreground">
                  未找到关联的计费规则
                </div>
              )}
            </CardContent>
          </Card>

          {/* 价格配置 */}
          <Card>
            <CardHeader>
              <CardTitle>价格配置</CardTitle>
            </CardHeader>
            <CardContent>
              {product.pricingConfig && Object.keys(product.pricingConfig).length > 0 ? (
                <div className="space-y-4">
                  {/* 特殊处理价格阶梯 */}
                  {product.pricingConfig.tiers ? (
                    <div>
                      <div className="text-sm text-muted-foreground mb-2">价格阶梯</div>
                      {renderTiersDetail(product.pricingConfig.tiers)}
                    </div>
                  ) : (
                    formatPricingConfig(product.pricingConfig)
                  )}
                </div>
              ) : (
                <div className="text-center py-4 text-muted-foreground">
                  暂无价格配置
                </div>
              )}
            </CardContent>
          </Card>

          {/* JSON原始数据 */}
          <Card>
            <CardHeader>
              <CardTitle>原始配置数据</CardTitle>
            </CardHeader>
            <CardContent>
              <pre className="text-xs bg-muted p-3 rounded-lg overflow-auto max-h-40">
                {JSON.stringify(product.pricingConfig, null, 2)}
              </pre>
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