"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ContainerTemplateService, type ContainerTemplate } from "@/lib/container-template-service";
import { Settings, Star, Play, Cpu, MemoryStick, Network, RefreshCw } from "lucide-react";
import { toast } from "@/hooks/use-toast";

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<ContainerTemplate[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchTemplates = async () => {
    setLoading(true);
    try {
      const response = await ContainerTemplateService.getUserEnabledTemplates();
      
      if (response.code === 200) {
        setTemplates(response.data || []);
      } else {
        toast({
          title: "获取模板失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "获取模板失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const getTypeLabel = (type: string) => {
    return type === "mcp-gateway" ? "MCP网关" : type;
  };

  const getTypeBadgeVariant = (type: string) => {
    return "default";
  };

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">容器模板</h1>
          <p className="text-gray-600 mt-2">查看可用的容器模板配置</p>
        </div>
        <Button onClick={fetchTemplates} variant="outline">
          <RefreshCw className="w-4 h-4 mr-2" />
          刷新
        </Button>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, index) => (
            <Card key={index} className="animate-pulse">
              <CardHeader>
                <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="h-3 bg-gray-200 rounded"></div>
                  <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : templates.length === 0 ? (
        <div className="text-center py-12">
          <Settings className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-4 text-lg font-medium text-gray-900">暂无可用模板</h3>
          <p className="mt-2 text-gray-500">系统中还没有启用的容器模板</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {templates.map((template) => (
            <Card key={template.id} className="hover:shadow-md transition-shadow">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg flex items-center gap-2">
                      {template.name}
                      {template.isDefault && (
                        <Badge variant="secondary" className="text-xs">
                          <Star className="w-3 h-3 mr-1" />
                          默认
                        </Badge>
                      )}
                    </CardTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge variant={getTypeBadgeVariant(template.type)}>
                        {getTypeLabel(template.type)}
                      </Badge>
                      <Badge variant="default" className="bg-green-100 text-green-800 hover:bg-green-100">
                        <Play className="w-3 h-3 mr-1" />
                        已启用
                      </Badge>
                    </div>
                  </div>
                </div>
                <CardDescription className="mt-2">
                  {template.description || "暂无描述"}
                </CardDescription>
              </CardHeader>
              
              <CardContent>
                <div className="space-y-4">
                  {/* 镜像信息 */}
                  <div>
                    <div className="text-sm text-gray-600 mb-1">容器镜像</div>
                    <div className="font-mono text-xs bg-gray-100 px-3 py-2 rounded border">
                      {template.fullImageName}
                    </div>
                  </div>
                  
                  {/* 资源配置 */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center">
                      <div className="flex items-center justify-center gap-1 text-blue-600 mb-1">
                        <Cpu className="w-4 h-4" />
                        <span className="text-sm font-medium">CPU</span>
                      </div>
                      <div className="text-lg font-bold">{template.cpuLimit}</div>
                      <div className="text-xs text-gray-500">核心</div>
                    </div>
                    
                    <div className="text-center">
                      <div className="flex items-center justify-center gap-1 text-green-600 mb-1">
                        <MemoryStick className="w-4 h-4" />
                        <span className="text-sm font-medium">内存</span>
                      </div>
                      <div className="text-lg font-bold">{template.memoryLimit}</div>
                      <div className="text-xs text-gray-500">MB</div>
                    </div>
                  </div>
                  
                  {/* 网络配置 */}
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <div className="flex items-center gap-1 text-gray-600 mb-1">
                        <Network className="w-3 h-3" />
                        <span>端口</span>
                      </div>
                      <div className="font-medium">{template.internalPort}</div>
                    </div>
                    <div>
                      <div className="text-gray-600 mb-1">网络模式</div>
                      <div className="font-medium">{template.networkMode}</div>
                    </div>
                  </div>
                  
                  {/* 数据卷 */}
                  {template.volumeMountPath && (
                    <div>
                      <div className="text-sm text-gray-600 mb-1">数据卷</div>
                      <div className="font-mono text-xs bg-gray-50 px-2 py-1 rounded border">
                        {template.volumeMountPath}
                      </div>
                    </div>
                  )}
                  
                  {/* 环境变量 */}
                  {template.environment && Object.keys(template.environment).length > 0 && (
                    <div>
                      <div className="text-sm text-gray-600 mb-1">环境变量</div>
                      <div className="max-h-20 overflow-y-auto">
                        {Object.entries(template.environment).map(([key, value]) => (
                          <div key={key} className="font-mono text-xs bg-gray-50 px-2 py-1 rounded border mb-1">
                            <span className="text-blue-600">{key}</span>=<span className="text-green-600">{value}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                  
                  {/* 创建信息 */}
                  <div className="pt-3 border-t border-gray-100">
                    <div className="flex justify-between items-center text-xs text-gray-500">
                      <span>创建者: {template.createdBy}</span>
                      <span>{new Date(template.createdAt).toLocaleDateString('zh-CN')}</span>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
      
      {/* 提示信息 */}
      {!loading && templates.length > 0 && (
        <div className="mt-8 text-center text-sm text-gray-500">
          <p>这些是系统中可用的容器模板。管理员可以配置和管理这些模板。</p>
        </div>
      )}
    </div>
  );
}