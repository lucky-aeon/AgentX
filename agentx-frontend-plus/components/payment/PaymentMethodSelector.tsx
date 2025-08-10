"use client";

import { useState, useEffect } from "react";
import { CreditCard, QrCode, Smartphone, Globe, Zap } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "@/hooks/use-toast";

import { PaymentMethodDTO, PaymentTypeDTO } from "@/types/payment";
import { PaymentService } from "@/lib/payment-service";

interface PaymentMethodSelectorProps {
  onSelectionChange?: (platform: string, type: string) => void;
  onConfirm?: (platform: string, type: string) => void;
  disabled?: boolean;
  className?: string;
}

export default function PaymentMethodSelector({
  onSelectionChange,
  onConfirm,
  disabled = false,
  className = ""
}: PaymentMethodSelectorProps) {
  
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedPlatform, setSelectedPlatform] = useState<string>("");
  const [selectedType, setSelectedType] = useState<string>("");
  
  // 加载支付方法列表
  const loadPaymentMethods = async () => {
    setLoading(true);
    try {
      const response = await PaymentService.getAvailablePaymentMethods();
      if (response.code === 200) {
        setPaymentMethods(response.data);
        
        // 自动选择第一个可用的支付方式
        if (response.data.length > 0 && response.data[0].paymentTypes.length > 0) {
          const firstPlatform = response.data[0].platformCode;
          const firstType = response.data[0].paymentTypes[0].typeCode;
          setSelectedPlatform(firstPlatform);
          setSelectedType(firstType);
          onSelectionChange?.(firstPlatform, firstType);
        }
      } else {
        toast({
          title: "获取支付方式失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "网络错误",
        description: "请检查网络连接后重试",
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };
  
  // 获取支付类型图标
  const getPaymentTypeIcon = (typeCode: string) => {
    switch (typeCode) {
      case 'WEB':
        return <Globe className="h-4 w-4" />;
      case 'QR_CODE':
        return <QrCode className="h-4 w-4" />;
      case 'MOBILE':
        return <Smartphone className="h-4 w-4" />;
      case 'H5':
        return <Smartphone className="h-4 w-4" />;
      case 'MINI_PROGRAM':
        return <Zap className="h-4 w-4" />;
      default:
        return <CreditCard className="h-4 w-4" />;
    }
  };
  
  // 获取支付平台颜色
  const getPlatformColor = (platformCode: string) => {
    switch (platformCode) {
      case 'alipay':
        return 'from-blue-50 to-blue-100 border-blue-200';
      case 'wechat':
        return 'from-green-50 to-green-100 border-green-200';
      case 'stripe':
        return 'from-purple-50 to-purple-100 border-purple-200';
      default:
        return 'from-gray-50 to-gray-100 border-gray-200';
    }
  };
  
  // 处理支付方式选择
  const handleSelectionChange = (platform: string, type: string) => {
    setSelectedPlatform(platform);
    setSelectedType(type);
    onSelectionChange?.(platform, type);
  };
  
  // 处理确认选择
  const handleConfirm = () => {
    if (selectedPlatform && selectedType) {
      onConfirm?.(selectedPlatform, selectedType);
    }
  };
  
  useEffect(() => {
    loadPaymentMethods();
  }, []);
  
  if (loading) {
    return (
      <div className={`space-y-4 ${className}`}>
        <div className="space-y-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-64" />
        </div>
        {[1, 2].map((i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-5 w-24" />
              <Skeleton className="h-4 w-48" />
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }
  
  if (paymentMethods.length === 0) {
    return (
      <div className={`text-center py-8 ${className}`}>
        <CreditCard className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
        <p className="text-muted-foreground">暂无可用的支付方式</p>
        <Button 
          variant="outline" 
          onClick={loadPaymentMethods}
          className="mt-4"
        >
          重新加载
        </Button>
      </div>
    );
  }
  
  return (
    <div className={`space-y-4 ${className}`}>
      <div>
        <h3 className="text-lg font-medium">选择支付方式</h3>
        <p className="text-sm text-muted-foreground">
          请选择您偏好的支付平台和支付类型
        </p>
      </div>
      
      <RadioGroup
        value={`${selectedPlatform}:${selectedType}`}
        onValueChange={(value) => {
          const [platform, type] = value.split(':');
          handleSelectionChange(platform, type);
        }}
        className="space-y-3"
      >
        {paymentMethods.map((method) => (
          <Card 
            key={method.platformCode}
            className={`transition-all ${
              method.available 
                ? `bg-gradient-to-r ${getPlatformColor(method.platformCode)} hover:shadow-md` 
                : 'opacity-50'
            }`}
          >
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-base font-medium">
                    {method.platformName}
                  </CardTitle>
                  <CardDescription className="text-sm">
                    {method.description}
                  </CardDescription>
                </div>
                {!method.available && (
                  <Badge variant="secondary">暂不可用</Badge>
                )}
              </div>
            </CardHeader>
            
            {method.available && (
              <CardContent className="pt-0">
                <div className="space-y-2">
                  {method.paymentTypes.map((type) => (
                    <div 
                      key={type.typeCode}
                      className="flex items-center space-x-3"
                    >
                      <RadioGroupItem 
                        value={`${method.platformCode}:${type.typeCode}`}
                        id={`${method.platformCode}-${type.typeCode}`}
                        disabled={disabled}
                      />
                      <Label 
                        htmlFor={`${method.platformCode}-${type.typeCode}`}
                        className="flex items-center space-x-2 cursor-pointer flex-1"
                      >
                        {getPaymentTypeIcon(type.typeCode)}
                        <span className="font-medium">{type.typeName}</span>
                        {type.requireRedirect && (
                          <Badge variant="outline" className="text-xs">
                            跳转
                          </Badge>
                        )}
                      </Label>
                    </div>
                  ))}
                </div>
                
                {method.paymentTypes.length > 0 && (
                  <div className="mt-3 p-2 bg-white/50 rounded text-xs text-muted-foreground">
                    {method.paymentTypes.find(t => t.typeCode === selectedType && selectedPlatform === method.platformCode)?.description}
                  </div>
                )}
              </CardContent>
            )}
          </Card>
        ))}
      </RadioGroup>
      
      {onConfirm && (
        <div className="flex justify-end pt-4">
          <Button 
            onClick={handleConfirm}
            disabled={disabled || !selectedPlatform || !selectedType}
            className="min-w-24"
          >
            确认选择
          </Button>
        </div>
      )}
    </div>
  );
}