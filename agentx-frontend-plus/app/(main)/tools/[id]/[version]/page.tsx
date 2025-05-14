"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { ArrowLeft, Wrench, Download, User, Clock, Settings, Command } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"
import ReactMarkdown from "react-markdown"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Separator } from "@/components/ui/separator"
import React from "react"
import { Card, CardContent } from "@/components/ui/card"

import { Tool, ToolStatus } from "@/types/tool"
import { getMarketToolVersionDetailWithToast, getMarketToolVersionsWithToast, getUserToolsWithToast } from "@/lib/tool-service"
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
  
  // 获取工具详情
  useEffect(() => {
    async function fetchToolDetail() {
      try {
        setLoading(true)
        setError(null)

        // 获取工具版本详情
        const detailResponse = await getMarketToolVersionDetailWithToast(id, version)
        
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
          
          // 获取版本历史
          try {
            const versionsResponse = await getMarketToolVersionsWithToast(apiData.toolId || id);
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
            console.error("获取版本历史失败", versionError);
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
            </div>
          </div>
          
          {/* 主要内容区域 */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
            {/* 左侧 - 工具介绍 */}
            <div className="md:col-span-2 space-y-8">
              <div>
                <h2 className="text-xl font-semibold mb-4">工具介绍</h2>
                <div className="prose dark:prose-invert max-w-none">
                  <ReactMarkdown>{tool.description}</ReactMarkdown>
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
                              Object.entries(item.parameters.properties)
                                .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                                .map(([key, value]) => {
                                  // 处理特殊键名，移除可能的前缀如 "{"
                                  const cleanKey = key.replace(/^\{/, '');
                                  // 确保value是对象并且有description属性
                                  const description = typeof value === 'object' && value ? value.description : null;
                                  
                                  if (description === null) return null;
                                  
                                  return (
                                    <div key={key} className="bg-secondary/30 p-2 rounded text-sm">
                                      <span className="font-mono text-primary">{cleanKey}</span>
                                      <span className="mx-1">-</span>
                                      <span>{description}</span>
                                      {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                        <Badge variant="outline" className="ml-2 text-xs">必填</Badge>
                                      )}
                                    </div>
                                  );
                                })
                                .filter(Boolean)
                            ) : (
                              // 处理旧的inputSchema数据结构
                              item.inputSchema && item.inputSchema.properties ? (
                                Object.entries(item.inputSchema.properties).map(([key, value]) => (
                                  <div key={key} className="bg-secondary/30 p-2 rounded text-sm">
                                    <span className="font-mono text-primary">{key}</span>
                                    <span className="mx-1">-</span>
                                    <span>{(value as any).description}</span>
                                    {item.inputSchema && item.inputSchema.required?.includes(key) && (
                                      <Badge variant="outline" className="ml-2 text-xs">必填</Badge>
                                    )}
                                  </div>
                                ))
                              ) : (
                                <div className="text-sm text-muted-foreground">无参数</div>
                              )
                            )}
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
            <div className="divide-y max-h-96 overflow-y-auto">
              {versionHistory.map((version, index) => (
                <div key={index} className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-lg">v{version.version}</span>
                      {index === 0 && <Badge className="ml-1 text-xs py-0 px-2">最新</Badge>}
                    </div>
                    <Button 
                      variant="outline" 
                      size="sm"
                      onClick={() => {
                        handleInstallVersion(version.version)
                        setIsVersionHistoryOpen(false)
                      }}
                    >
                      <Download className="mr-2 h-3.5 w-3.5" />
                      安装
                    </Button>
                  </div>
                  
                  <p className="mt-2 mb-3 text-sm">{version.notes}</p>
                  
                  {version.changes.length > 0 && (
                    <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground ml-1">
                      {version.changes.map((change: string, i: number) => (
                        <li key={i}>{change}</li>
                      ))}
                    </ul>
                  )}
                  
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
    </div>
  )
} 