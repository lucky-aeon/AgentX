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
import { getMarketToolDetailWithToast } from "@/lib/tool-service"
import { InstallToolDialog } from "@/components/tool/install-tool-dialog"

// 模拟版本历史数据
const mockVersionHistory = [
  {
    version: "0.0.4",
    date: "3 months ago",
    author: "langgenius",
    notes: "添加了新功能，修复了bug",
    changes: [
      "添加了自定义域名功能",
      "修复了部署失败的问题",
      "优化了登录流程"
    ]
  },
  {
    version: "0.0.3",
    date: "4 months ago",
    author: "langgenius",
    notes: "优化了性能，添加了新的API支持",
    changes: []
  },
  {
    version: "0.0.2",
    date: "4 months ago",
    author: "langgenius",
    notes: "修复了用户反馈的问题",
    changes: []
  },
  {
    version: "0.0.1",
    date: "5 months ago",
    author: "langgenius",
    notes: "首次发布",
    changes: []
  }
];

export default function ToolDetailPage({ params }: { params: { id: string } & Promise<{ id: string }> }) {
  // 使用React.use()解包params对象
  const { id } = React.use(params);
  
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tool, setTool] = useState<Tool | null>(null)
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false)
  const [isVersionHistoryOpen, setIsVersionHistoryOpen] = useState(false)
  const [versionHistory, setVersionHistory] = useState(mockVersionHistory)
  const [selectedVersionToInstall, setSelectedVersionToInstall] = useState<string | null>(null)
  const [isVersionInstallDialogOpen, setIsVersionInstallDialogOpen] = useState(false)
  const [isUserInstalledTool, setIsUserInstalledTool] = useState(false)
  const currentVersion = versionHistory[0]?.version || "0.0.1"
  
  // 获取工具详情
  useEffect(() => {
    async function fetchToolDetail() {
      try {
        setLoading(true)
        setError(null)

        // 在生产环境调用API
        if (process.env.NODE_ENV !== 'development') {
          const response = await getMarketToolDetailWithToast(id)
          
          if (response.code === 200) {
            setTool(response.data)
            // 检查是否为已安装工具
            checkIfToolInstalled(response.data.id)
            // 这里应该从API获取版本历史
            // setVersionHistory(response.data.versionHistory || [])
          } else {
            setError(response.message)
          }
        } else {
          // 开发环境使用模拟数据
          setTimeout(() => {
            // 生成模拟工具数据
            const mockTool: Tool = {
              id: id,
              name: "Surge部署工具",
              icon: null,
              subtitle: "快速部署网站到Surge.sh",
              description: `# Surge部署工具\n\n这是一个强大的部署工具，可以帮助您快速将静态网站部署到Surge.sh。\n\n## 特性\n\n- 一键登录Surge账户\n- 自动部署静态网站\n- 生成唯一域名\n- 支持自定义域名\n\n## 使用方法\n\n安装后，您可以在聊天中通过@Surge部署工具来使用此工具。\n\n## 注意事项\n\n使用前请确保已经安装了Surge.sh的命令行工具。`,
              user_id: "user-1",
              author: "开发者小明",
              labels: ["开发工具", "部署", "网站"],
              tool_type: "mcp",
              upload_type: "github",
              upload_url: "https://github.com/example/surge-deploy-tool",
              install_command: {
                type: "sse",
                url: "https://api.example.com/tools/surge-deploy"
              },
              tool_list: [
                {
                  name: 'surge_login',
                  description: '登录到 Surge.sh 账户',
                  inputSchema: {
                    type: 'object',
                    properties: {
                      email: {
                        type: 'string',
                        description: 'Surge 账户邮箱',
                      },
                      password: {
                        type: 'string',
                        description: 'Surge 账户密码',
                      },
                    },
                    required: ['email', 'password'],
                  },
                },
                {
                  name: 'surge_deploy',
                  description: '部署项目到 Surge.sh（生成随机域名）',
                  inputSchema: {
                    type: 'object',
                    properties: {
                      directory: {
                        type: 'string',
                        description: '要部署的目录路径',
                      },
                    },
                    required: ['directory'],
                  },
                }
              ],
              status: ToolStatus.APPROVED,
              is_office: true,
              installCount: 3568,
              createdAt: new Date(Date.now() - 1000000000).toISOString(),
              updatedAt: new Date(Date.now() - 500000000).toISOString()
            };
            
            setTool(mockTool);
            // 开发环境中，检查URL参数或本地存储来确定是否为已安装工具
            checkIfToolInstalled(mockTool.id);
            setLoading(false);
          }, 1000);
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        setError(errorMessage)
      } finally {
        if (process.env.NODE_ENV !== 'development') {
          setLoading(false)
        }
      }
    }

    fetchToolDetail()
  }, [id])

  // 检查工具是否已安装
  const checkIfToolInstalled = async (toolId: string) => {
    try {
      // 生产环境中应该调用API来检查
      if (process.env.NODE_ENV !== 'development') {
        // 示例：调用API检查工具是否已安装
        // const response = await getUserToolsWithToast()
        // const userTools = response.data
        // setIsUserInstalledTool(userTools.some(tool => tool.id === toolId))
      } else {
        // 开发环境中模拟数据
        // 为了测试，我们假设某些特定ID的工具是已安装的
        const installedToolIds = ["tool-1", "tool-2", "tool-3", "tool-4"];
        setIsUserInstalledTool(installedToolIds.includes(toolId));
        
        // 或者通过URL参数判断是从"我的工具"页面跳转而来
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('from') === 'my-tools') {
          setIsUserInstalledTool(true);
        }
      }
    } catch (error) {
      console.error("检查工具安装状态失败", error);
    }
  };

  // 处理安装特定版本
  const handleInstallVersion = (version: string) => {
    setSelectedVersionToInstall(version)
    setIsVersionInstallDialogOpen(true)
  }

  return (
    <div className="container py-6">
      <Button variant="ghost" size="sm" asChild className="mb-4">
        <Link href="/tools">
          <ArrowLeft className="mr-2 h-4 w-4" />
          {isUserInstalledTool ? "返回我的工具" : "返回工具市场"}
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
              
              {/* 只有未安装工具才显示安装按钮 */}
              {!isUserInstalledTool && (
                <Button onClick={() => setIsInstallDialogOpen(true)}>
                  <Download className="mr-2 h-4 w-4" />
                  安装
                </Button>
              )}
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
      
      {/* 安装对话框 - 只在未安装工具时使用 */}
      {!isUserInstalledTool && (
        <InstallToolDialog 
          open={isInstallDialogOpen}
          onOpenChange={setIsInstallDialogOpen}
          tool={tool}
          onSuccess={() => {
            // 安装成功后更新状态
            setIsUserInstalledTool(true);
          }}
        />
      )}
      
      {/* 版本历史对话框 - 只在未安装工具时使用 */}
      {!isUserInstalledTool && (
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
                        {version.changes.map((change, i) => (
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
      )}
      
      {/* 版本安装对话框 - 只在未安装工具时使用 */}
      {!isUserInstalledTool && (
        <Dialog open={isVersionInstallDialogOpen} onOpenChange={setIsVersionInstallDialogOpen}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>安装指定版本</DialogTitle>
            </DialogHeader>
            
            <div className="space-y-4">
              <p>您确定要安装 {tool?.name} v{selectedVersionToInstall} 吗？</p>
              
              <div className="flex justify-end gap-2">
                <Button 
                  variant="outline" 
                  onClick={() => setIsVersionInstallDialogOpen(false)}
                >
                  取消
                </Button>
                <Button 
                  onClick={() => {
                    // 处理安装指定版本
                    toast({
                      title: "安装成功",
                      description: `${tool?.name} v${selectedVersionToInstall} 已安装成功`,
                    })
                    setIsVersionInstallDialogOpen(false)
                    // 更新状态为已安装
                    setIsUserInstalledTool(true)
                  }}
                >
                  <Download className="mr-2 h-4 w-4" />
                  安装
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
} 