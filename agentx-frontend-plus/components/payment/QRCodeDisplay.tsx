"use client";

import { useState, useEffect, useRef } from "react";
import { RefreshCw, CheckCircle, XCircle, Clock, AlertTriangle } from "lucide-react";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

import { QRCodeStatus } from "@/types/payment";

interface QRCodeDisplayProps {
  paymentUrl?: string;
  amount?: number;
  platform?: string;
  orderNo?: string;
  status?: QRCodeStatus;
  expiresAt?: string;
  onRefresh?: () => void;
  onCancel?: () => void;
  className?: string;
}

export default function QRCodeDisplay({
  paymentUrl,
  amount,
  platform,
  orderNo,
  status = 'generating',
  expiresAt,
  onRefresh,
  onCancel,
  className = ""
}: QRCodeDisplayProps) {
  
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [timeRemaining, setTimeRemaining] = useState<number>(0);
  const [qrCodeGenerated, setQrCodeGenerated] = useState(false);
  
  // 生成二维码
  const generateQRCode = async (url: string) => {
    if (!canvasRef.current || !url) return;
    
    try {
      // 动态导入qrcode库
      const QRCode = (await import('qrcode')).default;
      
      await QRCode.toCanvas(canvasRef.current, url, {
        width: 200,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF'
        }
      });
      
      setQrCodeGenerated(true);
    } catch (error) {
 
      setQrCodeGenerated(false);
    }
  };
  
  // 计算剩余时间
  const calculateTimeRemaining = () => {
    if (!expiresAt) return 0;
    
    const expireTime = new Date(expiresAt).getTime();
    const currentTime = Date.now();
    const remaining = Math.max(0, expireTime - currentTime);
    
    return Math.floor(remaining / 1000); // 转换为秒
  };
  
  // 格式化时间显示
  const formatTime = (seconds: number) => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };
  
  // 获取状态显示信息
  const getStatusInfo = () => {
    switch (status) {
      case 'generating':
        return {
          icon: <RefreshCw className="h-5 w-5 animate-spin" />,
          title: '生成支付码中...',
          description: '正在为您生成专属支付二维码',
          color: 'text-blue-600'
        };
      case 'waiting':
        return {
          icon: <Clock className="h-5 w-5" />,
          title: '等待支付',
          description: '请使用手机扫描二维码完成支付',
          color: 'text-green-600'
        };
      case 'scanned':
        return {
          icon: <CheckCircle className="h-5 w-5" />,
          title: '已扫描',
          description: '检测到扫码，请在手机上完成支付',
          color: 'text-blue-600'
        };
      case 'success':
        return {
          icon: <CheckCircle className="h-5 w-5" />,
          title: '支付成功',
          description: '支付已完成，正在处理中...',
          color: 'text-green-600'
        };
      case 'failed':
        return {
          icon: <XCircle className="h-5 w-5" />,
          title: '支付失败',
          description: '支付未成功，请重试或选择其他支付方式',
          color: 'text-red-600'
        };
      case 'expired':
        return {
          icon: <AlertTriangle className="h-5 w-5" />,
          title: '二维码已过期',
          description: '请刷新二维码或重新发起支付',
          color: 'text-orange-600'
        };
      default:
        return {
          icon: <Clock className="h-5 w-5" />,
          title: '准备中',
          description: '正在准备支付信息',
          color: 'text-gray-600'
        };
    }
  };
  
  // 获取进度值
  const getProgressValue = () => {
    if (!expiresAt) return 0;
    
    const totalTime = 300; // 假设总时间为5分钟
    const remaining = timeRemaining;
    return Math.max(0, (remaining / totalTime) * 100);
  };
  
  // 生成二维码
  useEffect(() => {
    if (paymentUrl && status === 'waiting') {
      generateQRCode(paymentUrl);
    }
  }, [paymentUrl, status]);
  
  // 倒计时
  useEffect(() => {
    if (expiresAt) {
      const updateTimer = () => {
        const remaining = calculateTimeRemaining();
        setTimeRemaining(remaining);
        
        if (remaining <= 0 && status !== 'expired') {
          // 时间到了，但状态还没有更新为过期，可以通知父组件
        }
      };
      
      updateTimer();
      const timer = setInterval(updateTimer, 1000);
      
      return () => clearInterval(timer);
    }
  }, [expiresAt, status]);
  
  const statusInfo = getStatusInfo();
  
  return (
    <Card className={`w-full max-w-md mx-auto ${className}`}>
      <CardHeader className="text-center">
        <div className={`mx-auto mb-2 ${statusInfo.color}`}>
          {statusInfo.icon}
        </div>
        <CardTitle className="text-lg">{statusInfo.title}</CardTitle>
        <CardDescription>{statusInfo.description}</CardDescription>
        
        {amount && (
          <div className="mt-2">
            <span className="text-2xl font-bold">¥{amount.toFixed(2)}</span>
          </div>
        )}
        
        {platform && (
          <Badge variant="outline" className="w-fit mx-auto mt-2">
            {platform === 'alipay' ? '支付宝' : platform === 'wechat' ? '微信支付' : platform}
          </Badge>
        )}
      </CardHeader>
      
      <CardContent className="text-center space-y-4">
        {/* 二维码显示区域 */}
        <div className="flex justify-center">
          {status === 'generating' ? (
            <div className="w-[200px] h-[200px] bg-gray-100 rounded-lg flex items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-gray-400" />
            </div>
          ) : status === 'waiting' || status === 'scanned' ? (
            <div className="relative">
              <canvas 
                ref={canvasRef}
                className={`rounded-lg ${!qrCodeGenerated ? 'hidden' : ''}`}
              />
              {!qrCodeGenerated && (
                <div className="w-[200px] h-[200px] bg-gray-100 rounded-lg flex items-center justify-center">
                  <Skeleton className="w-full h-full" />
                </div>
              )}
              
              {/* 过期遮罩 */}
              {status === 'expired' && (
                <div className="absolute inset-0 bg-black/50 rounded-lg flex items-center justify-center">
                  <div className="text-white text-center">
                    <AlertTriangle className="h-8 w-8 mx-auto mb-2" />
                    <div className="text-sm">已过期</div>
                  </div>
                </div>
              )}
            </div>
          ) : status === 'success' ? (
            <div className="w-[200px] h-[200px] bg-green-50 rounded-lg flex items-center justify-center">
              <div className="text-center text-green-600">
                <CheckCircle className="h-16 w-16 mx-auto mb-2" />
                <div className="text-lg font-medium">支付成功</div>
              </div>
            </div>
          ) : status === 'failed' ? (
            <div className="w-[200px] h-[200px] bg-red-50 rounded-lg flex items-center justify-center">
              <div className="text-center text-red-600">
                <XCircle className="h-16 w-16 mx-auto mb-2" />
                <div className="text-lg font-medium">支付失败</div>
              </div>
            </div>
          ) : (
            <div className="w-[200px] h-[200px] bg-gray-100 rounded-lg flex items-center justify-center">
              <div className="text-gray-400 text-center">
                <Clock className="h-8 w-8 mx-auto mb-2" />
                <div className="text-sm">准备中</div>
              </div>
            </div>
          )}
        </div>
        
        {/* 倒计时和进度条 */}
        {timeRemaining > 0 && status === 'waiting' && (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">剩余时间</span>
              <span className="font-medium">{formatTime(timeRemaining)}</span>
            </div>
            <Progress value={getProgressValue()} className="h-2" />
          </div>
        )}
        
        {/* 订单信息 */}
        {orderNo && (
          <div className="text-xs text-muted-foreground">
            订单号: {orderNo}
          </div>
        )}
        
        {/* 操作按钮 */}
        <div className="flex gap-2 justify-center">
          {(status === 'expired' || status === 'failed') && onRefresh && (
            <Button onClick={onRefresh} size="sm">
              <RefreshCw className="h-4 w-4 mr-2" />
              重新生成
            </Button>
          )}
          
          {onCancel && ['generating', 'waiting', 'scanned'].includes(status) && (
            <Button onClick={onCancel} variant="outline" size="sm">
              取消支付
            </Button>
          )}
        </div>
        
        {/* 支付提示 */}
        {status === 'waiting' && (
          <div className="text-xs text-muted-foreground space-y-1">
            <p>• 请使用{platform === 'alipay' ? '支付宝' : '微信'}扫一扫功能</p>
            <p>• 扫码后请在手机上完成支付</p>
            <p>• 支付完成后页面将自动跳转</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}