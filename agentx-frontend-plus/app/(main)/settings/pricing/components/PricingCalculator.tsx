"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Calculator, RotateCcw } from "lucide-react";

import { Product, ModelTokenPricingConfig, PerUnitPricingConfig, TieredPricingConfig } from "@/types/product";

interface PricingCalculatorProps {
  products: Product[];
  className?: string;
}

interface CalculationResult {
  cost: number;
  breakdown: string[];
}

export function PricingCalculator({ products, className }: PricingCalculatorProps) {
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [inputTokens, setInputTokens] = useState<string>('10000');
  const [outputTokens, setOutputTokens] = useState<string>('5000');
  const [quantity, setQuantity] = useState<string>('1');
  const [storageAmount, setStorageAmount] = useState<string>('1');
  const [result, setResult] = useState<CalculationResult | null>(null);

  // 计算费用
  const calculateCost = () => {
    if (!selectedProduct) return;

    let cost = 0;
    const breakdown: string[] = [];

    try {
      switch (selectedProduct.type) {
        case 'MODEL_USAGE':
          const modelConfig = selectedProduct.pricingConfig as ModelTokenPricingConfig;
          const inputCost = (parseInt(inputTokens) / 10000) * (modelConfig.input_cost_per_million || 0);
          const outputCost = (parseInt(outputTokens) / 10000) * (modelConfig.output_cost_per_million || 0);
          cost = inputCost + outputCost;
          breakdown.push(`输入 ${parseInt(inputTokens).toLocaleString()} tokens: ¥${inputCost.toFixed(4)}`);
          breakdown.push(`输出 ${parseInt(outputTokens).toLocaleString()} tokens: ¥${outputCost.toFixed(4)}`);
          break;

        case 'AGENT_CREATION':
        case 'AGENT_USAGE':
        case 'API_CALL':
          const unitConfig = selectedProduct.pricingConfig as PerUnitPricingConfig;
          cost = parseInt(quantity) * (unitConfig.cost_per_unit || 0);
          breakdown.push(`${parseInt(quantity)} 次 × ¥${(unitConfig.cost_per_unit || 0).toFixed(4)} = ¥${cost.toFixed(4)}`);
          break;

        case 'STORAGE_USAGE':
          const storageConfig = selectedProduct.pricingConfig as TieredPricingConfig;
          const amount = parseFloat(storageAmount);
          let remaining = amount;
          cost = 0;
          
          if (storageConfig.tiers) {
            for (const tier of storageConfig.tiers) {
              const tierMax = tier.max_quantity || Infinity;
              const tierAmount = Math.min(remaining, tierMax - tier.min_quantity);
              if (tierAmount > 0) {
                const tierCost = tierAmount * tier.unit_price;
                cost += tierCost;
                breakdown.push(`${tier.min_quantity}-${tier.max_quantity || '∞'} GB: ${tierAmount.toFixed(2)} GB × ¥${tier.unit_price.toFixed(4)} = ¥${tierCost.toFixed(4)}`);
                remaining -= tierAmount;
              }
              if (remaining <= 0) break;
            }
          }
          break;

        default:
          cost = 0;
          breakdown.push('未知的商品类型');
      }

      setResult({ cost, breakdown });
    } catch (error) {
      setResult({ cost: 0, breakdown: ['计算错误，请检查输入'] });
    }
  };

  // 重置表单
  const resetForm = () => {
    setInputTokens('10000');
    setOutputTokens('5000');
    setQuantity('1');
    setStorageAmount('1');
    setResult(null);
  };

  // 当选择的商品改变时，重置结果
  useEffect(() => {
    setResult(null);
  }, [selectedProduct]);

  // 渲染输入表单
  const renderInputForm = () => {
    if (!selectedProduct) return null;

    switch (selectedProduct.type) {
      case 'MODEL_USAGE':
        return (
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="inputTokens">输入Tokens</Label>
              <Input
                id="inputTokens"
                type="number"
                value={inputTokens}
                onChange={(e) => setInputTokens(e.target.value)}
                placeholder="10000"
                min="0"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="outputTokens">输出Tokens</Label>
              <Input
                id="outputTokens"
                type="number"
                value={outputTokens}
                onChange={(e) => setOutputTokens(e.target.value)}
                placeholder="5000"
                min="0"
              />
            </div>
          </div>
        );

      case 'AGENT_CREATION':
        return (
          <div className="space-y-2">
            <Label htmlFor="quantity">创建数量</Label>
            <Input
              id="quantity"
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="1"
              min="1"
            />
          </div>
        );

      case 'AGENT_USAGE':
        return (
          <div className="space-y-2">
            <Label htmlFor="quantity">调用次数</Label>
            <Input
              id="quantity"
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="1"
              min="1"
            />
          </div>
        );

      case 'API_CALL':
        return (
          <div className="space-y-2">
            <Label htmlFor="quantity">API调用次数</Label>
            <Input
              id="quantity"
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="1"
              min="1"
            />
          </div>
        );

      case 'STORAGE_USAGE':
        return (
          <div className="space-y-2">
            <Label htmlFor="storageAmount">存储容量 (GB)</Label>
            <Input
              id="storageAmount"
              type="number"
              step="0.1"
              value={storageAmount}
              onChange={(e) => setStorageAmount(e.target.value)}
              placeholder="1"
              min="0"
            />
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Calculator className="h-5 w-5" />
          <CardTitle>价格计算器</CardTitle>
        </div>
        <CardDescription>
          选择商品并输入使用量，计算预估费用
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* 商品选择 */}
        <div className="space-y-2">
          <Label>选择商品</Label>
          <Select 
            value={selectedProduct?.id || ''} 
            onValueChange={(value) => {
              const product = products.find(p => p.id === value);
              setSelectedProduct(product || null);
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="请选择要计算的商品" />
            </SelectTrigger>
            <SelectContent>
              {products.map((product) => (
                <SelectItem key={product.id} value={product.id}>
                  {product.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 输入表单 */}
        {selectedProduct && (
          <div className="space-y-4">
            <div className="space-y-4">
              {renderInputForm()}
            </div>

            {/* 操作按钮 */}
            <div className="flex gap-2">
              <Button onClick={calculateCost} className="flex-1">
                <Calculator className="h-4 w-4 mr-2" />
                计算费用
              </Button>
              <Button onClick={resetForm} variant="outline">
                <RotateCcw className="h-4 w-4 mr-2" />
                重置
              </Button>
            </div>
          </div>
        )}

        {/* 计算结果 */}
        {result && (
          <div className="space-y-3 p-4 bg-muted/50 rounded-lg">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">预估费用</span>
              <span className="text-xl font-bold text-green-600">
                ¥{result.cost.toFixed(4)}
              </span>
            </div>
            
            {result.breakdown.length > 0 && (
              <div className="space-y-1">
                <div className="text-sm font-medium text-muted-foreground">费用明细</div>
                {result.breakdown.map((item, index) => (
                  <div key={index} className="text-xs text-muted-foreground">
                    {item}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* 说明文字 */}
        <div className="text-xs text-muted-foreground space-y-1">
          <p>• 计算结果仅供参考，实际费用以系统计费为准</p>
          <p>• 价格可能会根据服务升级进行调整</p>
          <p>• 使用前请确保账户余额充足</p>
        </div>
      </CardContent>
    </Card>
  );
}