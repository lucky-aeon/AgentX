"use client";

import { useState, useEffect } from "react";
import { Plus, Trash } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";

import { RuleHandlerKey } from "@/types/billing";
import { 
  ModelTokenPricingConfig, 
  PerUnitPricingConfig, 
  TieredPricingConfig 
} from "@/types/product";

interface PricingConfigEditorProps {
  handlerKey?: RuleHandlerKey;
  value: Record<string, any>;
  onChange: (config: Record<string, any>) => void;
}

export function PricingConfigEditor({ handlerKey, value, onChange }: PricingConfigEditorProps) {
  // 根据处理器类型渲染不同的配置编辑器
  const renderConfigEditor = () => {
    switch (handlerKey) {
      case RuleHandlerKey.MODEL_TOKEN_STRATEGY:
        return <ModelTokenConfigEditor value={value} onChange={onChange} />;
      
      case RuleHandlerKey.PER_UNIT_STRATEGY:
        return <PerUnitConfigEditor value={value} onChange={onChange} />;
      
      case RuleHandlerKey.TIERED_PRICING_STRATEGY:
        return <TieredPricingConfigEditor value={value} onChange={onChange} />;
      
      default:
        return <GenericConfigEditor value={value} onChange={onChange} />;
    }
  };

  if (!handlerKey) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        请先选择计费规则
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="text-sm text-muted-foreground">
        根据所选计费规则配置相应的价格参数
      </div>
      {renderConfigEditor()}
    </div>
  );
}

// 模型Token计费配置编辑器
function ModelTokenConfigEditor({ 
  value, 
  onChange 
}: { 
  value: Record<string, any>; 
  onChange: (config: Record<string, any>) => void; 
}) {
  const config = value as ModelTokenPricingConfig;

  const updateConfig = (updates: Partial<ModelTokenPricingConfig>) => {
    onChange({ ...config, ...updates });
  };

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label htmlFor="inputCost">输入Token费用 (每百万Token)</Label>
          <Input
            id="inputCost"
            type="number"
            step="0.01"
            min="0"
            value={config.input_cost_per_million || ''}
            onChange={(e) => updateConfig({ input_cost_per_million: parseFloat(e.target.value) || 0 })}
            placeholder="0.00"
          />
        </div>
        
        <div className="space-y-2">
          <Label htmlFor="outputCost">输出Token费用 (每百万Token)</Label>
          <Input
            id="outputCost"
            type="number"
            step="0.01"
            min="0"
            value={config.output_cost_per_million || ''}
            onChange={(e) => updateConfig({ output_cost_per_million: parseFloat(e.target.value) || 0 })}
            placeholder="0.00"
          />
        </div>
      </div>
      
      <div className="text-xs text-muted-foreground">
        示例：输入5.00表示每百万个输入Token收费5.00元
      </div>
    </div>
  );
}

// 按次计费配置编辑器
function PerUnitConfigEditor({ 
  value, 
  onChange 
}: { 
  value: Record<string, any>; 
  onChange: (config: Record<string, any>) => void; 
}) {
  const config = value as PerUnitPricingConfig;

  const updateConfig = (updates: Partial<PerUnitPricingConfig>) => {
    onChange({ ...config, ...updates });
  };

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="unitCost">单次费用</Label>
        <Input
          id="unitCost"
          type="number"
          step="0.01"
          min="0"
          value={config.cost_per_unit || ''}
          onChange={(e) => updateConfig({ cost_per_unit: parseFloat(e.target.value) || 0 })}
          placeholder="0.00"
        />
      </div>
      
      <div className="text-xs text-muted-foreground">
        示例：输入10.00表示每次使用收费10.00元
      </div>
    </div>
  );
}

// 分层定价配置编辑器
function TieredPricingConfigEditor({ 
  value, 
  onChange 
}: { 
  value: Record<string, any>; 
  onChange: (config: Record<string, any>) => void; 
}) {
  const config = value as TieredPricingConfig;
  const tiers = config.tiers || [];

  const updateTiers = (newTiers: TieredPricingConfig['tiers']) => {
    onChange({ ...config, tiers: newTiers });
  };

  const addTier = () => {
    const newTier = {
      min_quantity: tiers.length > 0 ? (tiers[tiers.length - 1].max_quantity || 0) + 1 : 1,
      max_quantity: undefined,
      unit_price: 0
    };
    updateTiers([...tiers, newTier]);
  };

  const removeTier = (index: number) => {
    const newTiers = tiers.filter((_, i) => i !== index);
    updateTiers(newTiers);
  };

  const updateTier = (index: number, updates: Partial<TieredPricingConfig['tiers'][0]>) => {
    const newTiers = tiers.map((tier, i) => 
      i === index ? { ...tier, ...updates } : tier
    );
    updateTiers(newTiers);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <Label>价格阶梯</Label>
        <Button type="button" variant="outline" size="sm" onClick={addTier}>
          <Plus className="mr-2 h-4 w-4" />
          添加阶梯
        </Button>
      </div>

      {tiers.length === 0 ? (
        <div className="text-center py-8 text-muted-foreground">
          点击"添加阶梯"创建价格分层
        </div>
      ) : (
        <div className="space-y-3">
          {tiers.map((tier, index) => (
            <Card key={index}>
              <CardContent className="p-4">
                <div className="flex items-center gap-4">
                  <div className="flex-1 grid grid-cols-3 gap-2">
                    <div>
                      <Label className="text-xs">最小数量</Label>
                      <Input
                        type="number"
                        min="1"
                        value={tier.min_quantity}
                        onChange={(e) => updateTier(index, { min_quantity: parseInt(e.target.value) || 1 })}
                      />
                    </div>
                    <div>
                      <Label className="text-xs">最大数量</Label>
                      <Input
                        type="number"
                        min={tier.min_quantity}
                        value={tier.max_quantity || ''}
                        onChange={(e) => updateTier(index, { 
                          max_quantity: e.target.value ? parseInt(e.target.value) : undefined 
                        })}
                        placeholder="无上限"
                      />
                    </div>
                    <div>
                      <Label className="text-xs">单价</Label>
                      <Input
                        type="number"
                        step="0.01"
                        min="0"
                        value={tier.unit_price}
                        onChange={(e) => updateTier(index, { unit_price: parseFloat(e.target.value) || 0 })}
                      />
                    </div>
                  </div>
                  <Button 
                    type="button" 
                    variant="outline" 
                    size="sm" 
                    onClick={() => removeTier(index)}
                    className="text-red-600 hover:text-red-700"
                  >
                    <Trash className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
      
      <div className="text-xs text-muted-foreground">
        示例：1-100个单价1.00元，101-1000个单价0.80元，1000个以上单价0.60元
      </div>
    </div>
  );
}

// 通用配置编辑器（JSON格式）
function GenericConfigEditor({ 
  value, 
  onChange 
}: { 
  value: Record<string, any>; 
  onChange: (config: Record<string, any>) => void; 
}) {
  const [jsonValue, setJsonValue] = useState('');

  useEffect(() => {
    setJsonValue(JSON.stringify(value, null, 2));
  }, [value]);

  const handleJsonChange = (newValue: string) => {
    setJsonValue(newValue);
    try {
      const parsed = JSON.parse(newValue);
      onChange(parsed);
    } catch (error) {
      // 忽略JSON解析错误，等待用户完成输入
    }
  };

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="jsonConfig">JSON配置</Label>
        <textarea
          id="jsonConfig"
          className="w-full min-h-[200px] p-3 text-sm font-mono border rounded-md"
          value={jsonValue}
          onChange={(e) => handleJsonChange(e.target.value)}
          placeholder="输入JSON格式的价格配置"
        />
      </div>
      
      <div className="text-xs text-muted-foreground">
        请输入有效的JSON格式配置。具体字段含义请参考计费规则说明。
      </div>
    </div>
  );
}