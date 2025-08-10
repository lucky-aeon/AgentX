"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { TrendingUp, Calculator } from "lucide-react";

import { Product, formatPricing, toProductDisplayInfo } from "@/types/product";

interface ProductCardProps {
  product: Product;
  modelName?: string;
}

export function ProductCard({ product, modelName }: ProductCardProps) {
  const displayInfo = toProductDisplayInfo(product);
  const pricingConfig = formatPricing(product.pricingConfig, product.type);

  // 获取显示名称，如果是模型类型且有模型名称，则显示模型名称
  const getDisplayName = () => {
    if (product.type === 'MODEL_USAGE' && modelName) {
      return modelName;
    }
    return displayInfo.name;
  };

  // 获取显示描述，如果是模型类型且有模型名称，则修改描述
  const getDisplayDescription = () => {
    if (product.type === 'MODEL_USAGE' && modelName) {
      return `${modelName} 模型的使用计费`;
    }
    return displayInfo.description;
  };

  return (
    <Card className="h-full hover:shadow-lg transition-shadow">
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="text-2xl">{displayInfo.icon}</div>
          <div className="flex-1">
            <CardTitle className="text-lg">{getDisplayName()}</CardTitle>
            <CardDescription className="text-sm">
              {getDisplayDescription()}
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="space-y-4">
        {/* 价格显示 */}
        <div className="p-3 bg-gradient-to-r from-green-50 to-blue-50 rounded-lg border">
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="h-4 w-4 text-green-600" />
            <span className="text-sm font-medium">当前价格</span>
          </div>
          <p className="text-lg font-semibold text-green-700">
            {displayInfo.pricingDisplay}
          </p>
        </div>

        {/* 使用示例 */}
        {pricingConfig.examples.length > 0 && (
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <Calculator className="h-4 w-4 text-blue-600" />
              <span className="text-sm font-medium">费用示例</span>
            </div>
            <div className="space-y-2">
              {pricingConfig.examples.slice(0, 2).map((example, index) => (
                <div 
                  key={index} 
                  className="flex justify-between items-center text-sm p-3 bg-blue-50 rounded-md hover:bg-blue-100 transition-colors"
                >
                  <div>
                    <div className="font-medium">{example.usage}</div>
                    <div className="text-xs text-muted-foreground">{example.description}</div>
                  </div>
                  <div className="font-semibold text-blue-700">{example.cost}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}