"use client";

import { useState, useEffect } from "react";
import { CreditCard, Wallet, RefreshCw } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "@/hooks/use-toast";

import { QRCodeDisplay, PaymentStatusPoller } from "./index";
import { RechargeRequest } from "@/types/account";
import { PaymentResponse, OrderStatusResponse, PaymentMethodDTO, QRCodeStatus } from "@/types/payment";
import { PaymentService } from "@/lib/payment-service";

interface SimpleRechargeFlowProps {
  onSuccess?: (orderNo: string, amount: number) => void;
  className?: string;
}

export default function SimpleRechargeFlow({
  onSuccess,
  className = ""
}: SimpleRechargeFlowProps) {
  
  // 金额相关状态
  const [amount, setAmount] = useState<number>(0);
  const [customAmount, setCustomAmount] = useState<string>("");
  
  // 支付平台相关状态
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodDTO[]>([]);
  const [selectedPlatform, setSelectedPlatform] = useState<string>("");
  const [methodsLoading, setMethodsLoading] = useState(true);
  
  // 支付相关状态
  const [isGenerating, setIsGenerating] = useState(false);
  const [paymentResponse, setPaymentResponse] = useState<PaymentResponse | null>(null);
  const [orderStatus, setOrderStatus] = useState<OrderStatusResponse | null>(null);
  const [qrStatus, setQrStatus] = useState<QRCodeStatus>('generating');
  const [showQRCode, setShowQRCode] = useState(false);
  
  // 预设金额选项
  const presetAmounts = [10, 50, 100, 200, 500, 1000];
  
  // 加载支付方法列表
  const loadPaymentMethods = async () => {
    setMethodsLoading(true);
    try {
      const response = await PaymentService.getAvailablePaymentMethods();
      if (response.code === 200) {
        // 只显示支持QR_CODE的平台
        const qrCodeMethods = response.data.filter(method => 
          method.available && method.paymentTypes.some(type => type.typeCode === 'QR_CODE')
        );
        setPaymentMethods(qrCodeMethods);
        
        // 自动选择第一个可用的支付平台
        if (qrCodeMethods.length > 0) {
          setSelectedPlatform(qrCodeMethods[0].platformCode);
        }
      } else {
        toast({
          title: "获取支付方式失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      console.error('加载支付方法失败:', error);
    } finally {
      setMethodsLoading(false);
    }
  };
  
  // 选择预设金额
  const handlePresetAmountSelect = (presetAmount: number) => {
    setAmount(presetAmount);
    setCustomAmount("");
  };
  
  // 自定义金额输入
  const handleCustomAmountChange = (value: string) => {
    setCustomAmount(value);
    const numValue = parseFloat(value);
    if (!isNaN(numValue) && numValue > 0) {
      setAmount(numValue);
    } else {
      setAmount(0);
    }
  };
  
  // 验证输入
  const validateInput = () => {
    if (amount <= 0) {
      toast({
        title: "请选择充值金额",
        description: "请选择预设金额或输入自定义金额",
        variant: "destructive"
      });
      return false;
    }
    
    if (amount < 0.01) {
      toast({
        title: "金额过小",
        description: "最小充值金额为 ¥0.01",
        variant: "destructive"
      });
      return false;
    }
    
    if (amount > 100000) {
      toast({
        title: "金额过大",
        description: "单次充值金额不能超过 ¥100,000",
        variant: "destructive"
      });
      return false;
    }
    
    if (!selectedPlatform) {
      toast({
        title: "请选择支付平台",
        description: "请选择一个支付平台",
        variant: "destructive"
      });
      return false;
    }
    
    return true;
  };
  
  // 发起充值
  const handleRecharge = async () => {
    if (!validateInput()) return;
    
    setIsGenerating(true);
    setShowQRCode(true);
    setQrStatus('generating');
    
    try {
      const request: RechargeRequest = {
        amount: amount,
        paymentPlatform: selectedPlatform,
        paymentType: 'QR_CODE', // 固定为二维码支付
        remark: `账户充值 ¥${amount.toFixed(2)}`
      };
      
      const response = await PaymentService.createRechargePayment(request);
      
      if (response.code === 200) {
        setPaymentResponse(response.data);
        setQrStatus('waiting');
      } else {
        throw new Error(response.message);
      }
    } catch (error) {
      console.error('创建支付失败:', error);
      setQrStatus('failed');
      toast({
        title: "创建支付失败",
        description: error instanceof Error ? error.message : '网络错误，请重试',
        variant: "destructive"
      });
    } finally {
      setIsGenerating(false);
    }
  };
  
  // 轮询回调处理
  const handlePollingCallbacks = {
    onStatusChange: (status: OrderStatusResponse) => {
      setOrderStatus(status);
      
      switch (status.status) {
        case 'PAID':
          setQrStatus('success');
          break;
        case 'CANCELLED':
          setQrStatus('failed');
          break;
        case 'EXPIRED':
          setQrStatus('expired');
          break;
      }
    },
    
    onSuccess: (orderNo: string) => {
      onSuccess?.(orderNo, amount);
      // 2秒后重置界面
      setTimeout(() => {
        handleReset();
      }, 2000);
    },
    
    onFailed: (reason: string) => {
      setQrStatus('failed');
    },
    
    onExpired: () => {
      setQrStatus('expired');
    }
  };
  
  // 重新生成二维码
  const handleRefreshQRCode = () => {
    handleRecharge();
  };
  
  // 取消支付
  const handleCancelPayment = () => {
    handleReset();
  };
  
  // 重置界面
  const handleReset = () => {
    setShowQRCode(false);
    setPaymentResponse(null);
    setOrderStatus(null);
    setQrStatus('generating');
    setAmount(0);
    setCustomAmount("");
  };
  
  // 获取支付平台名称
  const getPlatformName = (code: string) => {
    const method = paymentMethods.find(m => m.platformCode === code);
    return method?.platformName || code;
  };
  
  // 获取支付平台颜色
  const getPlatformColor = (code: string) => {
    switch (code) {
      case 'alipay':
        return 'from-blue-50 to-blue-100 border-blue-200 text-blue-700';
      case 'wechat':
        return 'from-green-50 to-green-100 border-green-200 text-green-700';
      default:
        return 'from-gray-50 to-gray-100 border-gray-200 text-gray-700';
    }
  };
  
  useEffect(() => {
    loadPaymentMethods();
  }, []);
  
  if (showQRCode) {
    return (
      <div className={`w-full max-w-md mx-auto ${className}`}>
        <QRCodeDisplay
          paymentUrl={paymentResponse?.paymentUrl}
          amount={amount}
          platform={selectedPlatform}
          orderNo={paymentResponse?.orderNo}
          status={qrStatus}
          expiresAt={orderStatus?.expiredAt}
          onRefresh={handleRefreshQRCode}
          onCancel={handleCancelPayment}
        />
        
        {/* 订单状态轮询 */}
        <PaymentStatusPoller
          orderNo={paymentResponse?.orderNo}
          enabled={true}
          callbacks={handlePollingCallbacks}
        />
      </div>
    );
  }
  
  return (
    <div className={`w-full max-w-2xl mx-auto space-y-6 ${className}`}>
      
      {/* 充值金额选择 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Wallet className="h-5 w-5" />
            选择充值金额
          </CardTitle>
          <CardDescription>
            请选择充值金额，余额可用于平台内所有付费服务
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* 预设金额按钮 */}
          <div className="grid grid-cols-3 gap-3">
            {presetAmounts.map((presetAmount) => (
              <Button
                key={presetAmount}
                variant={amount === presetAmount ? "default" : "outline"}
                onClick={() => handlePresetAmountSelect(presetAmount)}
                className="h-12 text-lg font-medium"
              >
                ¥{presetAmount}
              </Button>
            ))}
          </div>
          
          {/* 自定义金额输入 */}
          <div className="space-y-2">
            <Label htmlFor="custom-amount">或输入自定义金额</Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
                ¥
              </span>
              <Input
                id="custom-amount"
                type="number"
                placeholder="输入金额"
                value={customAmount}
                onChange={(e) => handleCustomAmountChange(e.target.value)}
                className="pl-8"
                min="0.01"
                max="100000"
                step="0.01"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              充值范围：¥0.01 - ¥100,000
            </p>
          </div>
          
          {/* 当前选择金额显示 */}
          {amount > 0 && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-center">
              <span className="text-sm text-blue-600">当前充值金额</span>
              <div className="text-2xl font-bold text-blue-700">¥{amount.toFixed(2)}</div>
            </div>
          )}
        </CardContent>
      </Card>
      
      {/* 支付平台选择 */}
      <Card>
        <CardHeader>
          <CardTitle>选择支付平台</CardTitle>
          <CardDescription>
            支持扫码支付，安全便捷
          </CardDescription>
        </CardHeader>
        <CardContent>
          {methodsLoading ? (
            <div className="space-y-3">
              {[1, 2].map((i) => (
                <div key={i} className="flex items-center space-x-3">
                  <Skeleton className="h-4 w-4 rounded-full" />
                  <Skeleton className="h-12 flex-1" />
                </div>
              ))}
            </div>
          ) : paymentMethods.length === 0 ? (
            <div className="text-center py-8">
              <CreditCard className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-4">暂无可用的支付方式</p>
              <Button onClick={loadPaymentMethods} variant="outline" size="sm">
                <RefreshCw className="h-4 w-4 mr-2" />
                重新加载
              </Button>
            </div>
          ) : (
            <RadioGroup
              value={selectedPlatform}
              onValueChange={setSelectedPlatform}
              className="space-y-3"
            >
              {paymentMethods.map((method) => (
                <div key={method.platformCode} className="flex items-center space-x-3">
                  <RadioGroupItem 
                    value={method.platformCode}
                    id={method.platformCode}
                  />
                  <Label 
                    htmlFor={method.platformCode}
                    className="flex-1 cursor-pointer"
                  >
                    <Card className={`transition-all ${
                      selectedPlatform === method.platformCode
                        ? `bg-gradient-to-r ${getPlatformColor(method.platformCode)} shadow-md`
                        : 'hover:shadow-sm'
                    }`}>
                      <CardContent className="p-4">
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="font-medium">{method.platformName}</div>
                            <div className="text-sm text-muted-foreground">
                              {method.description}
                            </div>
                          </div>
                          <Badge variant="outline">扫码支付</Badge>
                        </div>
                      </CardContent>
                    </Card>
                  </Label>
                </div>
              ))}
            </RadioGroup>
          )}
        </CardContent>
      </Card>
      
      {/* 充值按钮 */}
      <Card>
        <CardContent className="p-6">
          <Button 
            onClick={handleRecharge}
            disabled={amount <= 0 || !selectedPlatform || isGenerating || methodsLoading}
            className="w-full h-12 text-lg"
            size="lg"
          >
            {isGenerating ? (
              <>
                <RefreshCw className="h-5 w-5 mr-2 animate-spin" />
                正在生成支付码...
              </>
            ) : (
              <>
                <CreditCard className="h-5 w-5 mr-2" />
                充值 ¥{amount.toFixed(2)} - {selectedPlatform ? getPlatformName(selectedPlatform) : '选择支付平台'}
              </>
            )}
          </Button>
          
          {amount > 0 && selectedPlatform && (
            <p className="text-xs text-center text-muted-foreground mt-2">
              点击按钮后将生成支付二维码，请使用{getPlatformName(selectedPlatform)}扫码支付
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}