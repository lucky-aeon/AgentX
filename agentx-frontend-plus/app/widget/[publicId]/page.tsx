"use client";

import React, { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { AlertCircle, MessageCircle } from "lucide-react";
import { WidgetChatInterface } from './components/WidgetChatInterface';
import { getWidgetInfoWithToast } from '@/lib/widget-service';

interface WidgetInfo {
  name: string;
  description?: string;
  agentName: string;
  agentAvatar?: string;
  welcomeMessage?: string;
  enabled: boolean;
  dailyLimit: number;
  dailyCalls: number;
  // 新增：Agent配置信息，用于无会话聊天
  systemPrompt?: string;
  toolIds?: string[];
  knowledgeBaseIds?: string[];
}

export default function WidgetChatPage() {
  const params = useParams();
  const publicId = params.publicId as string;
  
  const [widgetInfo, setWidgetInfo] = useState<WidgetInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 获取小组件信息
  useEffect(() => {
    const fetchWidgetInfo = async () => {
      try {
        setLoading(true);
        setError(null);

        const response = await getWidgetInfoWithToast(publicId);
        
        if (response.code === 200) {
          setWidgetInfo(response.data);
        } else {
          setError(response.message || '获取小组件信息失败');
        }
      } catch (error) {
 
        setError(error instanceof Error ? error.message : '获取小组件信息失败');
      } finally {
        setLoading(false);
      }
    };

    if (publicId) {
      fetchWidgetInfo();
    }
  }, [publicId]);

  // 加载状态
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-4">
        <div className="max-w-4xl mx-auto">
          <Card>
            <CardHeader>
              <div className="flex items-center space-x-3">
                <Skeleton className="h-12 w-12 rounded-full" />
                <div>
                  <Skeleton className="h-6 w-32 mb-2" />
                  <Skeleton className="h-4 w-48" />
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Skeleton className="h-[500px] w-full" />
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  // 错误状态
  if (error || !widgetInfo) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardContent className="pt-6">
            <div className="flex items-center space-x-3 mb-4">
              <AlertCircle className="h-8 w-8 text-destructive" />
              <div>
                <h3 className="text-lg font-semibold">无法访问</h3>
                <p className="text-sm text-muted-foreground">
                  {error || '小组件配置不存在或已被禁用'}
                </p>
              </div>
            </div>
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                请检查链接是否正确，或联系网站管理员。
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 检查配置是否启用
  if (!widgetInfo.enabled) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardContent className="pt-6">
            <div className="flex items-center space-x-3 mb-4">
              <AlertCircle className="h-8 w-8 text-amber-500" />
              <div>
                <h3 className="text-lg font-semibold">暂时不可用</h3>
                <p className="text-sm text-muted-foreground">
                  此聊天服务暂时不可用
                </p>
              </div>
            </div>
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                服务已被暂时禁用，请稍后再试或联系网站管理员。
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 检查每日调用限制
  const hasReachedLimit = widgetInfo.dailyLimit !== -1 && widgetInfo.dailyCalls >= widgetInfo.dailyLimit;

  if (hasReachedLimit) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardContent className="pt-6">
            <div className="flex items-center space-x-3 mb-4">
              <AlertCircle className="h-8 w-8 text-amber-500" />
              <div>
                <h3 className="text-lg font-semibold">今日调用已达上限</h3>
                <p className="text-sm text-muted-foreground">
                  今日调用次数已达到 {widgetInfo.dailyLimit} 次上限
                </p>
              </div>
            </div>
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                请明天再来，或联系网站管理员增加调用额度。
              </AlertDescription>
            </Alert>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-4xl mx-auto">
        <Card>
          <CardHeader>
            <div className="flex items-center space-x-3">
              {widgetInfo.agentAvatar ? (
                <img 
                  src={widgetInfo.agentAvatar} 
                  alt={widgetInfo.agentName}
                  className="h-12 w-12 rounded-full object-cover"
                />
              ) : (
                <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                  <MessageCircle className="h-6 w-6 text-primary" />
                </div>
              )}
              <div>
                <CardTitle className="text-xl">{widgetInfo.name}</CardTitle>
                {widgetInfo.description && (
                  <p className="text-sm text-muted-foreground mt-1">
                    {widgetInfo.description}
                  </p>
                )}
              </div>
            </div>
            
            {/* 调用次数显示 */}
            {widgetInfo.dailyLimit !== -1 && (
              <div className="mt-4 text-xs text-muted-foreground">
                今日调用: {widgetInfo.dailyCalls} / {widgetInfo.dailyLimit}
              </div>
            )}
          </CardHeader>
          
          <CardContent>
            <WidgetChatInterface 
              publicId={publicId}
              agentName={widgetInfo.agentName}
              agentAvatar={widgetInfo.agentAvatar}
              welcomeMessage={widgetInfo.welcomeMessage}
              systemPrompt={widgetInfo.systemPrompt}
              toolIds={widgetInfo.toolIds}
              knowledgeBaseIds={widgetInfo.knowledgeBaseIds}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}