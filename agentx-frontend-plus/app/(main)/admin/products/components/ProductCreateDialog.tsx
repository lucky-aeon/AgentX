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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";

import { ProductFormData, CreateProductRequest } from "@/types/product";
import { BillingType, BillingTypeNames, ProductStatus, RuleHandlerKey } from "@/types/billing";
import { Rule } from "@/types/rule";
import { AdminProductServiceWithToast } from "@/lib/admin-product-service";
import { AdminRuleService } from "@/lib/admin-rule-service";
import { BusinessService, BusinessEntity } from "@/lib/business-service";
import { PricingConfigEditor } from "./PricingConfigEditor";

interface ProductCreateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function ProductCreateDialog({ open, onOpenChange, onSuccess }: ProductCreateDialogProps) {
  const [loading, setLoading] = useState(false);
  const [rules, setRules] = useState<Rule[]>([]);
  const [businessOptions, setBusinessOptions] = useState<BusinessEntity[]>([]);
  const [loadingBusinessOptions, setLoadingBusinessOptions] = useState(false);
  const [formData, setFormData] = useState<ProductFormData>({
    name: "",
    type: BillingType.MODEL_USAGE,
    serviceId: "",
    ruleId: "",
    pricingConfig: {},
    status: ProductStatus.ACTIVE
  });

  // 加载规则列表
  useEffect(() => {
    if (open) {
      loadRules();
      loadBusinessOptions(formData.type);
    }
  }, [open]);

  // 监听商品类型变化
  useEffect(() => {
    if (open) {
      loadBusinessOptions(formData.type);
      
      // 如果是固定业务ID的类型，自动设置serviceId
      const fixedServiceId = BusinessService.getFixedServiceId(formData.type);
      if (fixedServiceId) {
        setFormData(prev => ({ ...prev, serviceId: fixedServiceId }));
      } else {
        // 清空serviceId，让用户重新选择
        setFormData(prev => ({ ...prev, serviceId: "" }));
      }
    }
  }, [formData.type, open]);

  const loadRules = async () => {
    try {
      const response = await AdminRuleService.getAllRules();
      if (response.code === 200) {
        setRules(response.data);
      }
    } catch (error) {
      console.error('加载规则列表失败:', error);
    }
  };

  const loadBusinessOptions = async (productType: string) => {
    if (!BusinessService.needsServiceIdSelector(productType)) {
      setBusinessOptions([]);
      return;
    }

    setLoadingBusinessOptions(true);
    try {
      const options = await BusinessService.getBusinessOptions(productType);
      setBusinessOptions(options);
    } catch (error) {
      console.error('加载业务选项失败:', error);
      setBusinessOptions([]);
    } finally {
      setLoadingBusinessOptions(false);
    }
  };

  // 重置表单
  const resetForm = () => {
    setFormData({
      name: "",
      type: BillingType.MODEL_USAGE,
      serviceId: "",
      ruleId: "",
      pricingConfig: {},
      status: ProductStatus.ACTIVE
    });
  };

  // 处理表单提交
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // 表单验证
    if (!formData.name.trim()) {
      toast({
        title: "请输入商品名称",
        variant: "destructive"
      });
      return;
    }

    if (BusinessService.needsServiceIdSelector(formData.type) && !formData.serviceId.trim()) {
      toast({
        title: "请选择关联业务",
        variant: "destructive"
      });
      return;
    }

    if (!formData.ruleId) {
      toast({
        title: "请选择计费规则",
        variant: "destructive"
      });
      return;
    }

    if (!formData.pricingConfig || Object.keys(formData.pricingConfig).length === 0) {
      toast({
        title: "请配置价格信息",
        variant: "destructive"
      });
      return;
    }

    setLoading(true);

    try {
      const request: CreateProductRequest = {
        name: formData.name.trim(),
        type: formData.type,
        serviceId: formData.serviceId.trim(),
        ruleId: formData.ruleId,
        pricingConfig: formData.pricingConfig,
        status: formData.status
      };

      const response = await AdminProductServiceWithToast.createProduct(request);

      if (response.code === 200) {
        resetForm();
        onSuccess();
      }
    } catch (error) {
      console.error('创建商品失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 获取当前选中规则的处理器类型
  const getSelectedRuleHandlerKey = () => {
    const selectedRule = rules.find(rule => rule.id === formData.ruleId);
    return selectedRule?.handlerKey as RuleHandlerKey;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>创建商品</DialogTitle>
          <DialogDescription>
            创建新的计费商品配置
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 基本信息 */}
          <Card>
            <CardHeader>
              <CardTitle>基本信息</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">商品名称 *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="输入商品名称"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="type">商品类型 *</Label>
                <Select 
                  value={formData.type} 
                  onValueChange={(value) => setFormData({ ...formData, type: value as BillingType })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择商品类型" />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(BillingTypeNames).map(([key, name]) => (
                      <SelectItem key={key} value={key}>
                        {name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {BusinessService.needsServiceIdSelector(formData.type) ? (
                <div className="space-y-2">
                  <Label htmlFor="serviceId">关联业务 *</Label>
                  <Select 
                    value={formData.serviceId} 
                    onValueChange={(value) => setFormData({ ...formData, serviceId: value })}
                    disabled={loadingBusinessOptions}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={loadingBusinessOptions ? "加载中..." : "选择关联业务"} />
                    </SelectTrigger>
                    <SelectContent>
                      {businessOptions.map((option) => (
                        <SelectItem key={option.id} value={option.id}>
                          <div>
                            <div className="font-medium">{option.name}</div>
                            {option.description && (
                              <div className="text-xs text-muted-foreground">{option.description}</div>
                            )}
                          </div>
                        </SelectItem>
                      ))}
                      {businessOptions.length === 0 && !loadingBusinessOptions && (
                        <SelectItem value="empty" disabled>
                          暂无可选业务
                        </SelectItem>
                      )}
                    </SelectContent>
                  </Select>
                  <p className="text-xs text-muted-foreground">
                    {formData.type === 'MODEL_USAGE' && '选择要计费的模型'}
                    {formData.type === 'AGENT_USAGE' && '选择要计费的Agent'}
                    {formData.type === 'API_CALL' && '选择要计费的API类型'}
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  <Label>关联业务</Label>
                  <div className="p-2 bg-muted/50 rounded-md text-sm text-muted-foreground">
                    {formData.type === 'AGENT_CREATION' && '此类型按Agent创建数量计费，无需选择特定业务'}
                    {formData.type === 'STORAGE_USAGE' && '此类型按存储使用量计费，无需选择特定业务'}
                  </div>
                </div>
              )}

              <div className="space-y-2">
                <Label htmlFor="ruleId">计费规则 *</Label>
                <Select 
                  value={formData.ruleId} 
                  onValueChange={(value) => setFormData({ ...formData, ruleId: value })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="选择计费规则" />
                  </SelectTrigger>
                  <SelectContent>
                    {rules.map((rule) => (
                      <SelectItem key={rule.id} value={rule.id}>
                        <div>
                          <div className="font-medium">{rule.name}</div>
                          {rule.description && (
                            <div className="text-xs text-muted-foreground">{rule.description}</div>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="status">状态</Label>
                <Select 
                  value={formData.status.toString()} 
                  onValueChange={(value) => setFormData({ ...formData, status: parseInt(value) as ProductStatus })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="1">激活</SelectItem>
                    <SelectItem value="0">禁用</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </CardContent>
          </Card>

          {/* 价格配置 */}
          <Card>
            <CardHeader>
              <CardTitle>价格配置</CardTitle>
            </CardHeader>
            <CardContent>
              <PricingConfigEditor
                handlerKey={getSelectedRuleHandlerKey()}
                value={formData.pricingConfig}
                onChange={(config) => setFormData({ ...formData, pricingConfig: config })}
              />
            </CardContent>
          </Card>

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
              {loading ? "创建中..." : "创建商品"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}