"use client";

import { useEffect, useRef, useCallback } from "react";
import { toast } from "@/hooks/use-toast";

import { OrderStatusResponse, PollingCallbacks, PollingConfig } from "@/types/payment";
import { PaymentService } from "@/lib/payment-service";

interface PaymentStatusPollerProps {
  orderNo?: string;
  enabled?: boolean;
  config?: Partial<PollingConfig>;
  callbacks?: PollingCallbacks;
}

export default function PaymentStatusPoller({
  orderNo,
  enabled = false,
  config = {},
  callbacks = {}
}: PaymentStatusPollerProps) {
  
  const stopPollingRef = useRef<(() => void) | null>(null);
  const lastOrderNoRef = useRef<string>("");
  const callbacksRef = useRef(callbacks);
  const configRef = useRef(config);
  
  // 更新 refs
  callbacksRef.current = callbacks;
  configRef.current = config;
  
  // 默认轮询配置：每3秒查询一次，最多5分钟
  const defaultConfig = {
    maxDuration: 300000, // 5分钟
    interval: 3000 // 每3秒查询一次
  };
  
  // 停止轮询
  const stopPolling = useCallback(() => {
    if (stopPollingRef.current) {
      stopPollingRef.current();
      stopPollingRef.current = null;
    }
  }, []);
  
  // 开始轮询
  const startPolling = useCallback(async () => {
 
    
    if (!orderNo || !enabled) {
 
      return;
    }
    
    // 如果已经在轮询同一个订单，不重复启动
    if (lastOrderNoRef.current === orderNo && stopPollingRef.current) {
 
      return;
    }
    
    // 停止之前的轮询
 
    if (stopPollingRef.current) {
      stopPollingRef.current();
      stopPollingRef.current = null;
    }
    
 
    lastOrderNoRef.current = orderNo;
    
    try {
 
      const finalConfig = { ...defaultConfig, ...configRef.current };
      
      stopPollingRef.current = await PaymentService.pollOrderStatus(
        orderNo,
        {
          onStatusChange: (status: OrderStatusResponse) => {
 
            callbacksRef.current.onStatusChange?.(status);
          },
          
          onSuccess: (orderNo: string) => {
 
            toast({
              title: "支付成功",
              description: "您的充值已完成，余额正在更新...",
              variant: "default"
            });
            
            // 延迟执行成功回调，确保后端数据同步
            setTimeout(() => {
 
              callbacksRef.current.onSuccess?.(orderNo);
              
              // 再次延迟显示更新完成提示
              setTimeout(() => {
                toast({
                  title: "余额已更新",
                  description: "账户余额已成功更新",
                  variant: "default"
                });
              }, 1000);
            }, 2000); // 延迟2秒确保后端数据同步
            
            if (stopPollingRef.current) {
              stopPollingRef.current();
              stopPollingRef.current = null;
            }
          },
          
          onFailed: (reason: string) => {
 
            toast({
              title: "支付失败",
              description: reason,
              variant: "destructive"
            });
            callbacksRef.current.onFailed?.(reason);
            if (stopPollingRef.current) {
              stopPollingRef.current();
              stopPollingRef.current = null;
            }
          },
          
          onExpired: () => {
 
            toast({
              title: "支付超时",
              description: "支付二维码已过期，请重新发起支付",
              variant: "destructive"
            });
            callbacksRef.current.onExpired?.();
            if (stopPollingRef.current) {
              stopPollingRef.current();
              stopPollingRef.current = null;
            }
          },
          
          onError: (error: string) => {
 
            // 不显示toast，避免频繁提示网络错误
            callbacksRef.current.onError?.(error);
          }
        },
        finalConfig
      );
      
 
    } catch (error) {
 
      toast({
        title: "查询支付状态失败",
        description: "网络连接异常，请检查网络后重试",
        variant: "destructive"
      });
    }
  }, [orderNo, enabled]); // 移除所有依赖项，只依赖实际的 props
  
  // 监听orderNo和enabled变化
  useEffect(() => {
    if (enabled && orderNo) {
      startPolling();
    } else {
      if (stopPollingRef.current) {
        stopPollingRef.current();
        stopPollingRef.current = null;
      }
    }
    
    // 清理函数
    return () => {
      if (stopPollingRef.current) {
        stopPollingRef.current();
        stopPollingRef.current = null;
      }
    };
  }, [orderNo, enabled]); // 移除 startPolling, stopPolling 依赖
  
  // 组件卸载时停止轮询
  useEffect(() => {
    return () => {
      if (stopPollingRef.current) {
        stopPollingRef.current();
        stopPollingRef.current = null;
      }
    };
  }, []); // 只在组件卸载时执行
  
  // 这个组件不渲染任何UI，只负责轮询逻辑
  return null;
}