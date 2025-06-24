"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
import { 
  Settings, 
  Key, 
  Users, 
  Github, 
  Shield, 
  Edit,
  ChevronRight
} from "lucide-react";
import { toast } from "@/hooks/use-toast";
import { 
  getAllAuthSettingsWithToast, 
  toggleAuthSettingWithToast 
} from "@/lib/auth-config-service";
import type { AuthSetting, FeatureType } from "@/lib/types/auth-config";
import { FEATURE_TYPE, AUTH_FEATURE_KEY } from "@/lib/types/auth-config";
import SSOConfigDialog from "./components/SSOConfigDialog";

interface AuthSettingsPageProps {}

export default function AuthSettingsPage({}: AuthSettingsPageProps) {
  const [authSettings, setAuthSettings] = useState<AuthSetting[]>([]);
  const [loading, setLoading] = useState(true);

  // 加载认证设置
  useEffect(() => {
    loadAuthSettings();
  }, []);

  const loadAuthSettings = async () => {
    setLoading(true);
    try {
      const response = await getAllAuthSettingsWithToast();
      if (response.code === 200) {
        setAuthSettings(response.data);
      }
    } catch (error) {
      // 错误已由 withToast 处理
    } finally {
      setLoading(false);
    }
  };

  // 切换开关状态
  const handleToggle = async (setting: AuthSetting) => {
    try {
      const response = await toggleAuthSettingWithToast(setting.id);
      if (response.code === 200) {
        const newEnabled = !setting.enabled;
        setAuthSettings(prev => 
          prev.map(s => 
            s.id === setting.id 
              ? { ...s, enabled: newEnabled }
              : s
          )
        );
        
        // 显示成功提示
        toast({
          title: "设置更新成功",
          description: `${setting.featureName} 已${newEnabled ? '启用' : '禁用'}`,
          variant: "default",
        });
      }
    } catch (error) {
      toast({
        title: "设置更新失败",
        description: "请检查网络连接后重试",
        variant: "destructive",
      });
    }
  };

  // 处理配置更新
  const handleConfigUpdate = (updatedSetting: AuthSetting) => {
    // 重新加载配置列表
    loadAuthSettings();
  };

  // 获取功能图标
  const getFeatureIcon = (featureKey: string) => {
    switch (featureKey) {
      case AUTH_FEATURE_KEY.NORMAL_LOGIN:
        return <Key className="h-5 w-5" />;
      case AUTH_FEATURE_KEY.GITHUB_LOGIN:
        return <Github className="h-5 w-5" />;
      case AUTH_FEATURE_KEY.COMMUNITY_LOGIN:
        return <Shield className="h-5 w-5" />;
      case AUTH_FEATURE_KEY.USER_REGISTER:
        return <Users className="h-5 w-5" />;
      default:
        return <Settings className="h-5 w-5" />;
    }
  };

  // 获取功能类型显示名称
  const getFeatureTypeDisplay = (featureType: string) => {
    return featureType === FEATURE_TYPE.LOGIN ? "登录功能" : "注册功能";
  };

  // 按功能类型分组
  const groupedSettings = authSettings.reduce((acc, setting) => {
    if (!acc[setting.featureType]) {
      acc[setting.featureType] = [];
    }
    acc[setting.featureType].push(setting);
    return acc;
  }, {} as Record<string, AuthSetting[]>);

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <div className="space-y-6">
          <div className="h-8 bg-gray-200 rounded animate-pulse"></div>
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-gray-200 rounded animate-pulse"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">认证设置</h1>
          <p className="text-gray-600 mt-1">管理用户登录和注册方式的配置</p>
        </div>
      </div>

      {/* 设置卡片 */}
      <div className="space-y-6">
        {Object.entries(groupedSettings)
          .sort(([a], [b]) => a === FEATURE_TYPE.LOGIN ? -1 : 1)
          .map(([featureType, settings]) => (
            <Card key={featureType}>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="h-5 w-5" />
                  {getFeatureTypeDisplay(featureType)}
                </CardTitle>
                <CardDescription>
                  {featureType === FEATURE_TYPE.LOGIN 
                    ? "配置用户可用的登录方式" 
                    : "配置用户注册功能"}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {settings
                  .sort((a, b) => a.displayOrder - b.displayOrder)
                  .map((setting, index) => (
                    <div key={setting.id}>
                      {index > 0 && <Separator className="my-4" />}
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="flex-shrink-0">
                            {getFeatureIcon(setting.featureKey)}
                          </div>
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <h3 className="font-medium text-gray-900">
                                {setting.featureName}
                              </h3>
                              <Badge 
                                variant={setting.enabled ? "default" : "secondary"}
                                className={setting.enabled ? "bg-green-100 text-green-800" : ""}
                              >
                                {setting.enabled ? "已启用" : "已禁用"}
                              </Badge>
                            </div>
                            <p className="text-sm text-gray-600 mt-1">
                              {setting.description}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          {/* SSO配置按钮 */}
                          {(setting.featureKey === AUTH_FEATURE_KEY.GITHUB_LOGIN || 
                            setting.featureKey === AUTH_FEATURE_KEY.COMMUNITY_LOGIN) && (
                            <SSOConfigDialog
                              setting={setting}
                              onConfigUpdate={handleConfigUpdate}
                            />
                          )}
                          {/* 开关 */}
                          <Switch
                            checked={setting.enabled}
                            onCheckedChange={() => handleToggle(setting)}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
              </CardContent>
            </Card>
          ))}
      </div>

      {/* 配置说明 */}
      <Card className="bg-blue-50 border-blue-200">
        <CardHeader>
          <CardTitle className="text-blue-900 text-base flex items-center gap-2">
            <Shield className="h-4 w-4" />
            配置说明
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm text-blue-800 space-y-2">
          <div className="flex items-start gap-2">
            <ChevronRight className="h-4 w-4 mt-0.5 flex-shrink-0" />
            <span>普通登录：用户使用邮箱/手机号和密码进行登录</span>
          </div>
          <div className="flex items-start gap-2">
            <ChevronRight className="h-4 w-4 mt-0.5 flex-shrink-0" />
            <span>GitHub登录：用户通过GitHub账号进行OAuth登录</span>
          </div>
          <div className="flex items-start gap-2">
            <ChevronRight className="h-4 w-4 mt-0.5 flex-shrink-0" />
            <span>敲鸭登录：用户通过敲鸭社区账号进行OAuth登录</span>
          </div>
          <div className="flex items-start gap-2">
            <ChevronRight className="h-4 w-4 mt-0.5 flex-shrink-0" />
            <span>用户注册：控制是否允许新用户注册账号</span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}