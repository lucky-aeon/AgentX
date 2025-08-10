"use client";

import { useState, useCallback } from "react";
import { ArrowLeft, CreditCard, QrCode, CheckCircle, XCircle } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Badge } from "@/components/ui/badge";
import { toast } from "@/hooks/use-toast";

import { PaymentMethodSelector, QRCodeDisplay, PaymentStatusPoller } from "./index";
import { RechargeRequest } from "@/types/account";
import { PaymentResponse, OrderStatusResponse, RechargeStep, RechargeState, QRCodeStatus } from "@/types/payment";
import { PaymentService } from "@/lib/payment-service";

interface RechargeFlowProps {
  onSuccess?: (orderNo: string, amount: number) => void;
  onCancel?: () => void;
  className?: string;
}

export default function RechargeFlow({
  onSuccess,
  onCancel,
  className = ""
}: RechargeFlowProps) {
  
  const [state, setState] = useState<RechargeState>({
    step: RechargeStep.SELECT_AMOUNT,
    amount: 0
  });
  
  const [customAmount, setCustomAmount] = useState<string>("");
  const [qrStatus, setQrStatus] = useState<QRCodeStatus>('generating');
  
  // 预设金额选项
  const presetAmounts = [10, 50, 100, 200, 500, 1000];
  
  // 更新状态
  const updateState = (updates: Partial<RechargeState>) => {
    setState(prev => ({ ...prev, ...updates }));
  };
  
  // 选择金额
  const handleAmountSelect = (amount: number) => {
    updateState({ 
      amount,
      step: RechargeStep.SELECT_PAYMENT,
      error: undefined 
    });
    setCustomAmount("");
  };
  
  // 自定义金额确认
  const handleCustomAmountConfirm = () => {
    const amount = parseFloat(customAmount);
    if (isNaN(amount) || amount <= 0) {
      toast({
        title: "金额无效",
        description: "请输入有效的充值金额",
        variant: "destructive"
      });
      return;
    }
    
    if (amount < 0.01) {
      toast({
        title: "金额过小",
        description: "最小充值金额为 ¥0.01",
        variant: "destructive"
      });
      return;
    }
    
    if (amount > 100000) {
      toast({
        title: "金额过大",
        description: "单次充值金额不能超过 ¥100,000",
        variant: "destructive"
      });
      return;
    }
    
    handleAmountSelect(amount);
  };
  
  // 支付方式选择确认
  const handlePaymentMethodConfirm = async (platform: string, type: string) => {
    updateState({
      selectedPlatform: platform,
      selectedType: type,
      step: RechargeStep.GENERATING_PAYMENT,
      error: undefined
    });
    
    setQrStatus('generating');
    
    try {
      const request: RechargeRequest = {
        amount: state.amount,
        paymentPlatform: platform,
        paymentType: type,
        remark: `账户充值 ¥${state.amount}`
      };
      
      const response = await PaymentService.createRechargePayment(request);
      
      if (response.code === 200) {
        updateState({
          paymentResponse: response.data,
          step: RechargeStep.WAITING_PAYMENT
        });
        
        // 根据支付类型设置二维码状态
        if (type === 'QR_CODE') {
          setQrStatus('waiting');
        } else {
          // 网页支付直接跳转
          if (response.data.paymentUrl) {
            window.open(response.data.paymentUrl, '_blank');
          }
        }
      } else {
        throw new Error(response.message);
      }
    } catch (error) {
      console.error('创建支付失败:', error);
      updateState({
        error: error instanceof Error ? error.message : '创建支付失败',
        step: RechargeStep.PAYMENT_FAILED
      });
      setQrStatus('failed');
      
      toast({
        title: "创建支付失败",
        description: error instanceof Error ? error.message : '网络错误，请重试',
        variant: "destructive"
      });
    }
  };
  
  // 轮询回调处理
  const handlePollingCallbacks = {
    onStatusChange: (status: OrderStatusResponse) => {
      updateState({ orderStatus: status });
      
      switch (status.status) {
        case 'PAID':
          setQrStatus('success');
          updateState({ step: RechargeStep.PAYMENT_SUCCESS });
          break;
        case 'CANCELLED':
          setQrStatus('failed');
          updateState({ 
            step: RechargeStep.PAYMENT_FAILED,
            error: '支付已取消'
          });
          break;
        case 'EXPIRED':
          setQrStatus('expired');
          updateState({
            step: RechargeStep.PAYMENT_FAILED,
            error: '支付已过期'
          });
          break;
        case 'PENDING':
          // 保持等待状态
          break;
      }
    },
    
    onSuccess: (orderNo: string) => {
      updateState({ step: RechargeStep.PAYMENT_SUCCESS });
      onSuccess?.(orderNo, state.amount);
    },
    
    onFailed: (reason: string) => {
      setQrStatus('failed');
      updateState({
        step: RechargeStep.PAYMENT_FAILED,
        error: reason
      });
    },
    
    onExpired: () => {
      setQrStatus('expired');
      updateState({
        step: RechargeStep.PAYMENT_FAILED,
        error: '支付二维码已过期'
      });
    }
  };
  
  // 返回上一步
  const handleBack = () => {
    switch (state.step) {
      case RechargeStep.SELECT_PAYMENT:
        updateState({ step: RechargeStep.SELECT_AMOUNT });
        break;
      case RechargeStep.GENERATING_PAYMENT:
      case RechargeStep.WAITING_PAYMENT:
        updateState({ step: RechargeStep.SELECT_PAYMENT });
        setQrStatus('generating');
        break;
      case RechargeStep.PAYMENT_SUCCESS:
      case RechargeStep.PAYMENT_FAILED:
        updateState({ step: RechargeStep.SELECT_AMOUNT });
        setQrStatus('generating');
        break;
    }
  };
  
  // 重新发起支付
  const handleRetry = () => {
    if (state.selectedPlatform && state.selectedType) {
      handlePaymentMethodConfirm(state.selectedPlatform, state.selectedType);
    } else {
      updateState({ step: RechargeStep.SELECT_PAYMENT });
    }
  };
  
  // 刷新二维码
  const handleRefreshQRCode = () => {
    handleRetry();
  };
  
  // 取消支付
  const handleCancelPayment = () => {
    updateState({
      step: RechargeStep.SELECT_AMOUNT,
      paymentResponse: undefined,
      orderStatus: undefined,
      error: undefined
    });
    setQrStatus('generating');
    onCancel?.();
  };
  
  return (
    <div className={`w-full max-w-2xl mx-auto ${className}`}>
      
      {/* 步骤指示器 */}
      <div className="flex items-center justify-center mb-6 space-x-4">
        <div className={`flex items-center space-x-2 ${
          [RechargeStep.SELECT_AMOUNT].includes(state.step) 
            ? 'text-blue-600' 
            : 'text-green-600'
        }`}>
          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
            [RechargeStep.SELECT_AMOUNT].includes(state.step)
              ? 'bg-blue-100 text-blue-600'
              : 'bg-green-100 text-green-600'
          }`}>
            {[RechargeStep.SELECT_AMOUNT].includes(state.step) ? '1' : '✓'}
          </div>
          <span className="text-sm font-medium">选择金额</span>
        </div>
        
        <Separator className="w-8" />
        
        <div className={`flex items-center space-x-2 ${
          [RechargeStep.SELECT_PAYMENT].includes(state.step)
            ? 'text-blue-600'
            : [RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT, RechargeStep.PAYMENT_SUCCESS, RechargeStep.PAYMENT_FAILED].includes(state.step)
            ? 'text-green-600'
            : 'text-gray-400'
        }`}>
          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
            [RechargeStep.SELECT_PAYMENT].includes(state.step)
              ? 'bg-blue-100 text-blue-600'
              : [RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT, RechargeStep.PAYMENT_SUCCESS, RechargeStep.PAYMENT_FAILED].includes(state.step)
              ? 'bg-green-100 text-green-600'
              : 'bg-gray-100 text-gray-400'
          }`}>
            {[RechargeStep.SELECT_PAYMENT].includes(state.step) ? '2' : [RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT, RechargeStep.PAYMENT_SUCCESS, RechargeStep.PAYMENT_FAILED].includes(state.step) ? '✓' : '2'}
          </div>
          <span className="text-sm font-medium">选择支付</span>
        </div>
        
        <Separator className="w-8" />
        
        <div className={`flex items-center space-x-2 ${
          [RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT].includes(state.step)
            ? 'text-blue-600'
            : [RechargeStep.PAYMENT_SUCCESS].includes(state.step)
            ? 'text-green-600'
            : 'text-gray-400'
        }`}>
          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
            [RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT].includes(state.step)
              ? 'bg-blue-100 text-blue-600'
              : [RechargeStep.PAYMENT_SUCCESS].includes(state.step)
              ? 'bg-green-100 text-green-600'
              : 'bg-gray-100 text-gray-400'
          }`}>
            {[RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT].includes(state.step) ? '3' : [RechargeStep.PAYMENT_SUCCESS].includes(state.step) ? '✓' : '3'}
          </div>
          <span className="text-sm font-medium">完成支付</span>
        </div>
      </div>
      
      {/* 返回按钮 */}
      {state.step !== RechargeStep.SELECT_AMOUNT && (
        <Button 
          variant="ghost" 
          onClick={handleBack}
          className="mb-4"
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          返回上一步
        </Button>
      )}
      
      {/* 步骤内容 */}
      {state.step === RechargeStep.SELECT_AMOUNT && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CreditCard className="h-5 w-5" />
              选择充值金额
            </CardTitle>
            <CardDescription>
              请选择充值金额，充值后余额可用于平台内所有付费服务
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* 预设金额选择 */}
            <div className="space-y-3">
              <Label>快速选择</Label>
              <div className="grid grid-cols-3 gap-3">
                {presetAmounts.map((amount) => (
                  <Button
                    key={amount}
                    variant="outline"
                    onClick={() => handleAmountSelect(amount)}
                    className="h-12 text-lg font-medium"
                  >
                    ¥{amount}
                  </Button>
                ))}
              </div>
            </div>
            
            <Separator />
            
            {/* 自定义金额 */}
            <div className="space-y-3">
              <Label htmlFor="custom-amount">自定义金额</Label>
              <div className="flex gap-2">
                <div className="relative flex-1">
                  <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
                    ¥
                  </span>
                  <Input
                    id="custom-amount"
                    type="number"
                    placeholder="输入充值金额"
                    value={customAmount}
                    onChange={(e) => setCustomAmount(e.target.value)}
                    className="pl-8"
                    min="0.01"
                    max="100000"
                    step="0.01"
                  />
                </div>
                <Button onClick={handleCustomAmountConfirm}>
                  确认
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                充值金额范围：¥0.01 - ¥100,000
              </p>
            </div>
          </CardContent>
        </Card>
      )}
      
      {state.step === RechargeStep.SELECT_PAYMENT && (
        <Card>
          <CardHeader>
            <CardTitle>选择支付方式</CardTitle>
            <CardDescription>
              充值金额: <Badge variant="outline" className="ml-2">¥{state.amount.toFixed(2)}</Badge>
            </CardDescription>
          </CardHeader>
          <CardContent>
            <PaymentMethodSelector
              onConfirm={handlePaymentMethodConfirm}
            />
          </CardContent>
        </Card>
      )}
      
      {[RechargeStep.GENERATING_PAYMENT, RechargeStep.WAITING_PAYMENT].includes(state.step) && (
        <div className="space-y-4">
          <Card>
            <CardHeader className="text-center">
              <CardTitle>完成支付</CardTitle>
              <CardDescription>
                充值金额: <Badge variant="outline" className="ml-2">¥{state.amount.toFixed(2)}</Badge>
              </CardDescription>
            </CardHeader>
          </Card>
          
          <QRCodeDisplay
            paymentUrl={state.paymentResponse?.paymentUrl}
            amount={state.amount}
            platform={state.selectedPlatform}
            orderNo={state.paymentResponse?.orderNo}
            status={qrStatus}
            expiresAt={state.orderStatus?.expiredAt}
            onRefresh={handleRefreshQRCode}
            onCancel={handleCancelPayment}
          />
          
          {/* 订单状态轮询 */}
          <PaymentStatusPoller
            orderNo={state.paymentResponse?.orderNo}
            enabled={state.step === RechargeStep.WAITING_PAYMENT}
            callbacks={handlePollingCallbacks}
          />
        </div>
      )}
      
      {state.step === RechargeStep.PAYMENT_SUCCESS && (
        <Card>
          <CardContent className="text-center py-8">
            <CheckCircle className="h-16 w-16 text-green-600 mx-auto mb-4" />
            <h3 className="text-xl font-medium mb-2">充值成功</h3>
            <p className="text-muted-foreground mb-4">
              ¥{state.amount.toFixed(2)} 已成功充值到您的账户
            </p>
            {state.paymentResponse?.orderNo && (
              <p className="text-xs text-muted-foreground mb-6">
                订单号: {state.paymentResponse.orderNo}
              </p>
            )}
            <Button onClick={() => updateState({ step: RechargeStep.SELECT_AMOUNT })}>
              继续充值
            </Button>
          </CardContent>
        </Card>
      )}
      
      {state.step === RechargeStep.PAYMENT_FAILED && (
        <Card>
          <CardContent className="text-center py-8">
            <XCircle className="h-16 w-16 text-red-600 mx-auto mb-4" />
            <h3 className="text-xl font-medium mb-2">支付失败</h3>
            <p className="text-muted-foreground mb-4">
              {state.error || '支付未成功，请重试或选择其他支付方式'}
            </p>
            <div className="flex gap-2 justify-center">
              <Button onClick={handleRetry}>
                重新支付
              </Button>
              <Button variant="outline" onClick={() => updateState({ step: RechargeStep.SELECT_AMOUNT })}>
                重新开始
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}