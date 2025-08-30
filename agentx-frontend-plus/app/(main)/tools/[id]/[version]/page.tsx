"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { ArrowLeft, Wrench, Download, User, Clock, Settings, Command } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"
import { MessageMarkdown } from '@/components/ui/message-markdown'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Separator } from "@/components/ui/separator"
import React from "react"
import { Card, CardContent } from "@/components/ui/card"

import { Tool, ToolStatus } from "@/types/tool"
import { getMarketToolVersionDetail, getMarketToolVersions, getMarketToolVersionDetailWithToast, getMarketToolVersionsWithToast, getUserToolsWithToast } from "@/lib/tool-service"
import { InstallToolDialog } from "@/components/tool/install-tool-dialog"

export default function ToolDetailPage({ params }: { params: { id: string, version: string } & Promise<{ id: string, version: string }> }) {
  // 使用React.use()解包params对象
  const { id, version } = React.use(params);
  
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tool, setTool] = useState<Tool | null>(null)
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false)
  const [isVersionHistoryOpen, setIsVersionHistoryOpen] = useState(false)
  const [versionHistory, setVersionHistory] = useState<any[]>([])
  const [selectedVersionToInstall, setSelectedVersionToInstall] = useState<string | null>(null)
  const [isVersionInstallDialogOpen, setIsVersionInstallDialogOpen] = useState(false)
  const [isUserInstalledTool, setIsUserInstalledTool] = useState(false)
  const [selectedVersionDetail, setSelectedVersionDetail] = useState<any>(null);
  const [isVersionDetailOpen, setIsVersionDetailOpen] = useState(false);
  const [versionDetailLoading, setVersionDetailLoading] = useState(false);
  
  // 获取工具详情
  useEffect(() => {
    async function fetchToolDetail() {
      try {
        setLoading(true)
        setError(null)

        // 获取工具版本详情，使用不带Toast的API
        const detailResponse = await getMarketToolVersionDetail(id, version)
        
        if (detailResponse.code === 200) {
          // 使用any类型进行安全转换
          const apiData = detailResponse.data as any;
          
          // 转换API返回的数据到前端需要的格式
          const toolData: Tool = {
            id: apiData.id,
            toolId: apiData.toolId || id,
            name: apiData.name,
            icon: apiData.icon,
            subtitle: apiData.subtitle,
            description: apiData.description,
            user_id: apiData.userId || "unknown",
            author: apiData.userName || "未知作者",
            labels: apiData.labels || [],
            tool_type: apiData.toolType || "",
            upload_type: apiData.uploadType || "",
            upload_url: apiData.uploadUrl || "",
            install_command: {
              type: 'sse',
              url: `https://api.example.com/tools/${apiData.toolId || id}`
            },
            tool_list: apiData.toolList || [],
            status: ToolStatus.APPROVED,
            is_office: Boolean(apiData.isOffice || apiData.office),
            installCount: apiData.installCount || 0,
            current_version: apiData.version || version,
            createdAt: apiData.createdAt,
            updatedAt: apiData.updatedAt
          };
          
          setTool(toolData);
          
          // 获取版本历史，使用不带Toast的API
          try {
            const versionsResponse = await getMarketToolVersions(apiData.toolId || id);
            if (versionsResponse.code === 200 && versionsResponse.data.length > 0) {
              // 转换版本历史数据
              const versions = versionsResponse.data.map((v: any) => ({
              version: v.version,
              date: new Date(v.createdAt).toLocaleDateString(),
              author: v.userName || toolData.author,
                notes: v.changeLog || "无更新说明",
              changes: []
            }));
            setVersionHistory(versions);
            }
          } catch (versionError) {
 
            // 版本历史获取失败不影响主要功能，继续使用默认数据
          }
          
          // 暂时设置为未安装状态，让安装按钮始终显示
          setIsUserInstalledTool(false);
        } else {
          setError(detailResponse.message);
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误";
        setError(errorMessage);
      } finally {
        setLoading(false);
      }
    }

    fetchToolDetail();
  }, [id, version]);

  // 检查工具是否已安装 - 暂时不使用此逻辑
  const checkIfToolInstalled = async (toolId: string) => {
    // 暂时直接返回，不进行实际检查
    return false;
  };

  // 处理安装特定版本
  const handleInstallVersion = (version: string) => {
    if (!tool) return;
    
    // 确保使用toolId进行安装
    if (!tool.toolId) {
      toast({
        title: "安装失败",
        description: "工具ID不存在",
        variant: "destructive"
      });
      return;
    }
    
    setSelectedVersionToInstall(version);
    setIsVersionInstallDialogOpen(true);
  }

  // 版本历史对话框
  useEffect(() => {
    // 当对话框打开时获取版本历史
    if (isVersionHistoryOpen && tool?.toolId) {
      async function fetchVersionHistory() {
        try {
          // 确保toolId存在并且是字符串类型
          if (!tool || !tool.toolId) return;
          
          const toolId = String(tool.toolId);
          // 使用不带Toast的API
          const versionsResponse = await getMarketToolVersions(toolId);
          if (versionsResponse.code === 200 && versionsResponse.data.length > 0) {
            // 按照版本号排序，最新版本在前
            const sortedVersions = [...versionsResponse.data].sort((a, b) => {
              return compareVersions(b.version || "0.0.0", a.version || "0.0.0");
            });
            
            // 转换版本历史数据
            const versions = sortedVersions.map((v: any) => ({
              version: v.version || "0.0.0",
              date: v.createdAt ? new Date(v.createdAt).toLocaleDateString() : "未知日期",
              author: v.userName || (tool && tool.author ? tool.author : "未知作者"),
              notes: v.changeLog || "无更新说明",
              name: v.name || "未命名版本",
              id: v.id || "",
              changes: []
            }));
            setVersionHistory(versions);
          }
        } catch (error) {
 
          toast({
            title: "获取版本历史失败",
            description: error instanceof Error ? error.message : "未知错误",
            variant: "destructive"
          });
        }
      }
      
      fetchVersionHistory();
    }
  }, [isVersionHistoryOpen, tool]);

  // 比较版本号的函数
  function compareVersions(v1: string, v2: string) {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);
    
    for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
      const part1 = parts1[i] || 0;
      const part2 = parts2[i] || 0;
      
      if (part1 > part2) return 1;
      if (part1 < part2) return -1;
    }
    
    return 0;
  }

  // 查看版本详情
  const handleViewVersionDetail = async (toolId: string, version: string) => {
    try {
      setVersionDetailLoading(true);
      setSelectedVersionDetail(null);
      
      // 获取版本详情，使用不带Toast的API
      const detailResponse = await getMarketToolVersionDetail(toolId, version);
      
      if (detailResponse.code === 200) {
        // 转换API返回的数据为详情对象
        setSelectedVersionDetail(detailResponse.data);
        setIsVersionDetailOpen(true);
      } else {
        toast({
          title: "获取版本详情失败",
          description: detailResponse.message,
          variant: "destructive"
        });
      }
    } catch (error) {
 
      toast({
        title: "获取版本详情失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive"
      });
    } finally {
      setVersionDetailLoading(false);
    }
  };

  return (
    <div className="container py-6">
      <Button variant="ghost" size="sm" asChild className="mb-4">
        <Link href="/tools">
          <ArrowLeft className="mr-2 h-4 w-4" />
          返回工具市场
        </Link>
      </Button>
      
      {loading ? (
        // 加载状态
        <div className="space-y-4">
          <div className="flex items-center gap-4">
            <Skeleton className="h-16 w-16 rounded-md" />
            <div className="space-y-2">
              <Skeleton className="h-8 w-48" />
              <Skeleton className="h-4 w-96" />
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
            <div className="md:col-span-2">
              <Skeleton className="h-64 w-full" />
            </div>
            <div>
              <Skeleton className="h-64 w-full" />
            </div>
          </div>
        </div>
      ) : error ? (
        // 错误状态
        <div className="text-center py-10">
          <div className="text-red-500 mb-4">{error}</div>
          <Button variant="outline" onClick={() => window.location.reload()}>
            重试
          </Button>
        </div>
      ) : tool ? (
        <div>
          {/* 工具标题和操作按钮 */}
          <div className="mb-8">
            <div className="flex items-start justify-between">
              <div className="flex items-start gap-4">
                <div className="flex h-16 w-16 items-center justify-center rounded-md bg-primary/10 text-primary-foreground overflow-hidden">
                  {tool.icon ? (
                    <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
                  ) : (
                    <Wrench className="h-7 w-7" />
                  )}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h1 className="text-3xl font-bold tracking-tight">{tool.name}</h1>
                    {tool.is_office && <Badge>官方</Badge>}
                  </div>
                  <p className="text-muted-foreground mt-1">{tool.subtitle}</p>
                  
                  <div className="flex flex-wrap gap-1 mt-3">
                    {tool.labels.filter(label => label !== "官方").map((label, i) => (
                      <Badge key={i} variant="outline">
                        {label}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>
              
              {/* 始终显示安装按钮 */}
              <Button onClick={() => setIsInstallDialogOpen(true)}>
                <Download className="mr-2 h-4 w-4" />
                安装
              </Button>
            </div>
            
            <div className="flex items-center gap-6 mt-4 text-sm text-muted-foreground">
              <div className="flex items-center">
                <User className="mr-1 h-4 w-4" />
                <span>作者: {tool.author}</span>
              </div>
              <div className="flex items-center">
                <Download className="mr-1 h-4 w-4" />
                <span>{tool.installCount} 安装</span>
              </div>
              <div className="flex items-center">
                <Clock className="mr-1 h-4 w-4" />
                <Button variant="link" size="sm" className="h-auto p-0 text-muted-foreground" onClick={() => setIsVersionHistoryOpen(true)}>
                  查看历史版本
                </Button>
              </div>
            </div>
          </div>
          
          {/* 主要内容区域 */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            {/* 左侧 - 工具介绍 */}
            <div className="md:col-span-2 space-y-8">
              <div>
                <h2 className="text-xl font-semibold mb-4">工具介绍</h2>
                <div className="prose dark:prose-invert max-w-none">
                  <MessageMarkdown showCopyButton={true}
                    content={tool.description}
                    
                  />
                </div>
              </div>
            </div>
            
            {/* 右侧 - 工具列表 */}
            <div>
              <Card className="sticky top-4">
                <CardContent className="p-6">
                  <h2 className="text-xl font-semibold mb-4">工具列表</h2>
                  <div className="space-y-4">
                    {tool.tool_list.map((item, i) => (
                      <div key={i} className="border rounded-md p-4">
                        <h3 className="font-semibold text-base">{item.name}</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          {item.description}
                        </p>
                        <div className="mt-3 space-y-2">
                          <h4 className="text-sm font-medium">参数:</h4>
                          <div className="space-y-2">
                            {item.parameters && item.parameters.properties ? (
                              <div className="px-2 py-2">
                                <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                                <div className="space-y-2">
                                  {Object.entries(item.parameters.properties)
                                    .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                                    .map(([key, value]) => {
                                      const cleanKey = key.replace(/^\{/, '');
                                      const description = typeof value === 'object' && value && 'description' in value 
                                        ? (value as any).description 
                                        : null;
                                      return (
                                        <div key={key} className="flex items-center gap-2">
                                          <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{cleanKey}</code>
                                          {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                            <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                          )}
                                          {description && (
                                            <span className="text-xs text-muted-foreground ml-2">{description}</span>
                                          )}
                                        </div>
                                      );
                                    })}
                                </div>
                              </div>
                            ) : null}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      ) : null}
      
      {/* 安装对话框 - 始终可用 */}
      <InstallToolDialog 
        open={isInstallDialogOpen}
        onOpenChange={setIsInstallDialogOpen}
        tool={tool}
        version={tool?.current_version}
        onSuccess={() => {
          toast({
            title: "安装成功",
            description: `${tool?.name} 工具已成功安装`
          });
        }}
      />
          
      {/* 版本历史对话框 - 始终可用 */}
          <Dialog open={isVersionHistoryOpen} onOpenChange={setIsVersionHistoryOpen}>
        <DialogContent className="max-w-2xl">
              <DialogHeader>
                <DialogTitle>版本历史</DialogTitle>
              </DialogHeader>
              
          <div className="mt-4 border rounded-lg overflow-hidden">
            {versionHistory.length === 0 ? (
              <div className="p-6 text-center text-muted-foreground">
                加载中...
              </div>
            ) : (
              <div className="divide-y max-h-96 overflow-y-auto">
                {versionHistory.map((version, index) => (
                  <div key={index} className="p-4">
                    <div className="flex items-center justify-between">
                        <div>
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-lg">v{version.version}</span>
                          {index === 0 && <Badge className="ml-1 text-xs py-0 px-2">最新</Badge>}
                        </div>
                        <div className="text-sm mt-1">{version.name}</div>
                      </div>
                      <div className="flex gap-2">
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={() => {
                            if (tool?.toolId) {
                              handleViewVersionDetail(tool.toolId, version.version);
                            }
                          }}
                        >
                          <Settings className="mr-2 h-3.5 w-3.5" />
                          查看详情
                        </Button>
                          <Button 
                          variant="outline" 
                            size="sm" 
                          onClick={() => {
                            handleInstallVersion(version.version)
                          }}
                          >
                          <Download className="mr-2 h-3.5 w-3.5" />
                            安装此版本
                          </Button>
                      </div>
                      </div>
                      
                    <div className="mt-4 bg-muted/50 p-3 rounded">
                      <h4 className="font-medium mb-2 text-sm">更新说明:</h4>
                      <p className="text-sm text-muted-foreground">{version.notes}</p>
                    </div>
                    
                    <div className="flex justify-between items-center text-xs text-muted-foreground mt-4 pt-2">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        <span>{version.date}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <User className="h-3 w-3" />
                        <span>{version.author}</span>
                      </div>
                    </div>
                  </div>
                ))}
                  </div>
                )}
              </div>
            </DialogContent>
          </Dialog>
          
      {/* 版本安装对话框 - 始终可用 */}
          {selectedVersionToInstall && (
            <InstallToolDialog
              open={isVersionInstallDialogOpen}
              onOpenChange={setIsVersionInstallDialogOpen}
              tool={tool}
              version={selectedVersionToInstall}
          onSuccess={() => {
            toast({
              title: "安装成功",
              description: `${tool?.name} (v${selectedVersionToInstall}) 已成功安装`
            });
          }}
            />
          )}
      
      {/* 版本详情对话框 */}
      <Dialog open={isVersionDetailOpen} onOpenChange={setIsVersionDetailOpen}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedVersionDetail ? (
                <div className="flex items-center gap-2">
                  <span>版本详情: {selectedVersionDetail.name}</span>
                  <Badge>v{selectedVersionDetail.version}</Badge>
                </div>
              ) : (
                "版本详情"
              )}
            </DialogTitle>
          </DialogHeader>
          
          {versionDetailLoading ? (
            <div className="py-8 flex justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
              <span className="ml-3">加载中...</span>
            </div>
          ) : selectedVersionDetail ? (
            <div className="space-y-6 py-4">
              {/* 基本信息 */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="flex items-center gap-2">
                  <User className="h-4 w-4 text-muted-foreground" />
                  <span>作者: {selectedVersionDetail.userName || "未知"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-muted-foreground" />
                  <span>发布时间: {selectedVersionDetail.createdAt ? new Date(selectedVersionDetail.createdAt).toLocaleString() : "未知"}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Download className="h-4 w-4 text-muted-foreground" />
                  <span>安装次数: {selectedVersionDetail.installCount || 0}</span>
                </div>
              </div>
              
              <Separator />
              
              {/* 详细描述 */}
              <div>
                <h3 className="text-lg font-medium mb-2">详细描述</h3>
                <div className="prose dark:prose-invert max-w-none">
                  <MessageMarkdown showCopyButton={true}
                    content={selectedVersionDetail.description || "暂无描述"}
                    
                  />
                </div>
              </div>
              
              <Separator />
              
              {/* 更新日志 */}
              <div>
                <h3 className="text-lg font-medium mb-2">更新日志</h3>
                <div className="bg-muted/50 p-4 rounded">
                  <p>{selectedVersionDetail.changeLog || "暂无更新日志"}</p>
                </div>
              </div>
              
              <Separator />
              
              {/* 工具列表 */}
              <div>
                <h3 className="text-lg font-medium mb-4">工具列表</h3>
                <div className="space-y-4">
                  {selectedVersionDetail.toolList && selectedVersionDetail.toolList.length > 0 ? (
                    selectedVersionDetail.toolList.map((item: any, i: number) => (
                      <div key={i} className="border rounded-md p-4">
                        <h3 className="font-semibold text-base">{item.name}</h3>
                        <p className="text-sm text-muted-foreground mt-1">
                          {item.description}
                        </p>
                        <div className="mt-3 space-y-2">
                          <h4 className="text-sm font-medium">参数:</h4>
                          <div className="space-y-2">
                            {item.parameters && item.parameters.properties ? (
                              <div className="px-2 py-2">
                                <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                                <div className="space-y-2">
                                  {Object.entries(item.parameters.properties)
                                    .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                                    .map(([key, value]) => {
                                      const cleanKey = key.replace(/^\{/, '');
                                      const description = typeof value === 'object' && value && 'description' in value 
                                        ? (value as any).description 
                                        : null;
                                      return (
                                        <div key={key} className="flex items-center gap-2">
                                          <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{cleanKey}</code>
                                          {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                            <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                          )}
                                          {description && (
                                            <span className="text-xs text-muted-foreground ml-2">{description}</span>
                                          )}
                                        </div>
                                      );
                                    })}
                                </div>
        </div>
      ) : null}
                          </div>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="text-center text-muted-foreground py-4">
                      暂无工具列表
                    </div>
                  )}
                </div>
              </div>
              
              {/* 安装按钮 */}
              <div className="flex justify-end mt-6">
                <Button 
                  onClick={() => {
                    handleInstallVersion(selectedVersionDetail.version);
                    setIsVersionDetailOpen(false);
                  }}
                >
                  <Download className="mr-2 h-4 w-4" />
                  安装此版本
                </Button>
              </div>
            </div>
          ) : (
            <div className="py-8 text-center text-muted-foreground">
              无法加载版本详情
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
} 