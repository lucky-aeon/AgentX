"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Search, X, Plus, Wrench, Download, Info, User, Check, ChevronRight, Command, MoreVertical, Trash } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from "@/components/ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"
import ReactMarkdown from "react-markdown"
import { AspectRatio } from "@/components/ui/aspect-ratio"
import { Separator } from "@/components/ui/separator"
import { Label } from "@/components/ui/label"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

import { Tool, ToolStatus } from "@/types/tool"
import { getMarketToolsWithToast, installToolWithToast, getUserToolsWithToast, deleteUserToolWithToast } from "@/lib/tool-service"
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from "@/components/ui/alert-dialog"
import { InstallToolDialog } from "@/components/tool/install-tool-dialog"

// 扩展Tool类型以包含usageCount属性
interface UserTool extends Tool {
  usageCount?: number;
}

export default function ToolsPage() {
  // 工具市场状态
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [tools, setTools] = useState<Tool[]>([])
  const [selectedTool, setSelectedTool] = useState<Tool | null>(null)
  const [isDetailOpen, setIsDetailOpen] = useState(false)
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false)
  const [installingToolId, setInstallingToolId] = useState<string | null>(null)
  const [selectedToolVersion, setSelectedToolVersion] = useState<any | null>(null)

  // 用户工具状态
  const [userToolsLoading, setUserToolsLoading] = useState(true)
  const [userTools, setUserTools] = useState<UserTool[]>([])
  const [selectedUserTool, setSelectedUserTool] = useState<UserTool | null>(null)
  const [isUserToolDetailOpen, setIsUserToolDetailOpen] = useState(false)
  const [activeTab, setActiveTab] = useState<"market" | "my">("market")
  
  // 删除工具相关状态
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [toolToDelete, setToolToDelete] = useState<UserTool | null>(null)
  const [isDeletingTool, setIsDeletingTool] = useState(false)
  
  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, 500)

    return () => clearTimeout(timer)
  }, [searchQuery])

  // 获取工具市场列表
  useEffect(() => {
    async function fetchTools() {
      try {
        setLoading(true)
        setError(null)

        const response = await getMarketToolsWithToast({
          name: debouncedQuery
        })

        if (response.code === 200) {
          setTools(response.data)
        } else {
          setError(response.message)
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        setError(errorMessage)
      } finally {
        setLoading(false)
      }
    }

    // 模拟数据
    if (process.env.NODE_ENV === 'development') {
      setTimeout(() => {
        setTools(generateMockTools())
        setLoading(false)
      }, 1000)
    } else {
      fetchTools()
    }
  }, [debouncedQuery])

  // 获取用户已安装工具
  useEffect(() => {
    async function fetchUserTools() {
      try {
        setUserToolsLoading(true)
        
        // 实际环境调用API
        if (process.env.NODE_ENV !== 'development') {
          const response = await getUserToolsWithToast()
          if (response.code === 200) {
            // 处理API响应
            const responseData = response.data as any;
            if (Array.isArray(responseData)) {
              setUserTools(responseData as UserTool[])
            } else if (responseData.userTools) {
              setUserTools(responseData.userTools as UserTool[])
            }
          }
        } else {
          // 模拟数据
          setTimeout(() => {
            setUserTools(generateMockUserTools())
            setUserToolsLoading(false)
          }, 800)
        }
      } catch (error) {
        console.error("获取用户工具失败", error)
        toast({
          title: "获取用户工具失败",
          description: "无法加载已安装工具",
          variant: "destructive",
        })
      } finally {
        setUserToolsLoading(false)
      }
    }

    fetchUserTools()
  }, [])

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  // 处理安装工具
  const handleInstallTool = async () => {
    if (!selectedTool) return
    
    try {
      setInstallingToolId(selectedTool.id)
      
      // 在实际环境中使用API调用安装
      if (process.env.NODE_ENV !== 'development') {
        const response = await installToolWithToast(selectedTool.id)
        
        if (response.code !== 200) {
          // 错误处理由withToast处理
          setInstallingToolId(null)
          setIsInstallDialogOpen(false)
          return
        }
      } else {
        // 模拟安装过程
        await new Promise(resolve => setTimeout(resolve, 1500))
      }
      
      toast({
        title: "安装成功",
        description: `${selectedTool.name} 已成功安装`,
      })
      
      // 安装成功后刷新用户工具列表
      const newUserTool: UserTool = {
        ...selectedTool,
        usageCount: 0
      }
      setUserTools(prev => [newUserTool, ...prev])
      
      setIsInstallDialogOpen(false)
      setIsDetailOpen(false)
    } catch (error) {
      console.error("安装工具失败", error)
    } finally {
      setInstallingToolId(null)
    }
  }

  // 打开工具详情
  const openToolDetail = (tool: Tool) => {
    setSelectedTool(tool)
    setIsDetailOpen(true)
  }

  // 打开安装确认对话框
  const openInstallDialog = (tool: Tool) => {
    setSelectedTool(tool)
    setIsInstallDialogOpen(true)
  }

  const handleInstallSuccess = () => {
    setIsDetailOpen(false)
  }

  // 打开用户工具详情
  const openUserToolDetail = (tool: UserTool) => {
    setSelectedUserTool(tool)
    setIsUserToolDetailOpen(true)
  }

  // 打开删除确认对话框
  const openDeleteDialog = (e: React.MouseEvent, tool: UserTool) => {
    e.stopPropagation() // 防止触发卡片点击事件
    setToolToDelete(tool)
    setIsDeleteDialogOpen(true)
  }

  // 处理删除工具
  const handleDeleteTool = async () => {
    if (!toolToDelete) return
    
    try {
      setIsDeletingTool(true)
      
      // 在实际环境中使用API调用删除
      if (process.env.NODE_ENV !== 'development') {
        const response = await deleteUserToolWithToast(toolToDelete.id)
        
        if (response.code !== 200) {
          // 错误处理由withToast处理
          setIsDeletingTool(false)
          setIsDeleteDialogOpen(false)
          return
        }
      } else {
        // 模拟删除过程
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
      
      // 更新工具列表，移除已删除的工具
      setUserTools(prev => prev.filter(tool => tool.id !== toolToDelete.id))
      
      toast({
        title: "工具已删除",
        description: `${toolToDelete.name} 已成功从您的工具列表移除`,
      })
      
      setIsDeleteDialogOpen(false)
    } catch (error) {
      console.error("删除工具失败", error)
    } finally {
      setIsDeletingTool(false)
    }
  }

  return (
    <div className="container py-6">
      {/* 页面头部 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">工具中心</h1>
          <p className="text-muted-foreground">探索和管理AI助手的扩展能力</p>
        </div>
        
        <Button asChild>
          <Link href="/tools/upload">
            <Plus className="mr-2 h-4 w-4" />
            上传工具
          </Link>
        </Button>
      </div>
      
      {/* 用户已安装工具部分 */}
      <div className="mb-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold">我的工具</h2>
        </div>
        
        {userToolsLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {Array.from({ length: 3 }).map((_, index) => (
              <Card key={index} className="overflow-hidden">
                <CardHeader className="pb-2">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-10 w-10 rounded-md" />
                    <div>
                      <Skeleton className="h-5 w-32 mb-1" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-4 w-full mb-2" />
                  <div className="mb-2 flex flex-wrap gap-1">
                    <Skeleton className="h-5 w-16 rounded-full" />
                    <Skeleton className="h-5 w-20 rounded-full" />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : userTools.length === 0 ? (
          <div className="text-center py-10 border rounded-lg bg-gray-50">
            <Wrench className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-medium mb-2">
              还没有安装任何工具
            </h3>
            <p className="text-muted-foreground mb-2">
              在下方工具市场中安装有用的AI助手工具
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {userTools.map((tool) => (
              <Card 
                key={tool.id} 
                className="relative overflow-hidden hover:border-primary transition-colors"
              >
                <CardHeader className="pb-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2" onClick={() => openUserToolDetail(tool)} style={{ cursor: 'pointer' }}>
                      <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary-foreground overflow-hidden">
                        {tool.icon ? (
                          <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
                        ) : (
                          <Wrench className="h-5 w-5" />
                        )}
                      </div>
                      <div>
                        <h3 className="font-semibold">{tool.name}</h3>
                        <p className="text-sm text-muted-foreground">{tool.author}</p>
                      </div>
                    </div>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreVertical className="h-4 w-4" />
                          <span className="sr-only">更多选项</span>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem 
                          className="text-destructive focus:text-destructive" 
                          onClick={(e) => openDeleteDialog(e, tool)}
                        >
                          <Trash className="mr-2 h-4 w-4" />
                          <span>删除工具</span>
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </CardHeader>
                
                <CardContent onClick={() => openUserToolDetail(tool)} style={{ cursor: 'pointer' }}>
                  <p className="text-sm mb-2 line-clamp-2">{tool.subtitle}</p>
                  <div className="flex flex-wrap gap-1">
                    {tool.labels.slice(0, 3).map((label, i) => (
                      <Badge key={i} variant="outline" className="text-xs">
                        {label}
                      </Badge>
                    ))}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
      
      {/* 工具市场分割线 */}
      <div className="relative mb-6">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t"></div>
        </div>
        <div className="relative flex justify-between">
          <div className="bg-background pr-4">
            <h2 className="text-xl font-semibold">推荐工具</h2>
          </div>
        </div>
      </div>
      
      {/* 工具市场横幅 */}
      <div className="mb-6 rounded-lg border border-dashed p-4 bg-muted/5">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-4">
            <div className="rounded-full bg-primary/10 p-3 hidden sm:flex">
              <Wrench className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h3 className="font-medium text-lg">探索工具市场</h3>
              <p className="text-sm text-muted-foreground">发现更多提升AI能力的工具，满足您的各种需求</p>
            </div>
          </div>
          <Button asChild>
            <Link href="/tools-market">
              <Search className="mr-2 h-4 w-4" />
              浏览工具市场
            </Link>
          </Button>
        </div>
      </div>
      
      {loading ? (
        // 加载状态
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 3 }).map((_, index) => (
            <Card key={index} className="overflow-hidden">
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-10 w-10 rounded-md" />
                    <div>
                      <Skeleton className="h-5 w-32 mb-1" />
                      <Skeleton className="h-4 w-24" />
                    </div>
                  </div>
                  <Skeleton className="h-6 w-12 rounded-full" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="mb-2 flex flex-wrap gap-1">
                  <Skeleton className="h-5 w-16 rounded-full" />
                  <Skeleton className="h-5 w-20 rounded-full" />
                  <Skeleton className="h-5 w-14 rounded-full" />
                </div>
                <div className="flex items-center justify-between">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-20" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : error ? (
        // 错误状态
        <div className="text-center py-10">
          <div className="text-red-500 mb-4">{error}</div>
          <Button variant="outline" onClick={() => window.location.reload()}>
            重试
          </Button>
        </div>
      ) : tools.length === 0 ? (
        // 空状态
        <div className="text-center py-10 border rounded-lg bg-gray-50">
          <Wrench className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">
            暂无推荐工具
          </h3>
          <p className="text-muted-foreground mb-6">
            前往工具市场探索更多工具
          </p>
          <Button asChild variant="outline">
            <Link href="/tools-market">
              <Wrench className="mr-2 h-4 w-4" />
              浏览工具市场
            </Link>
          </Button>
        </div>
      ) : (
        // 工具列表
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {/* 只显示前5个工具作为推荐 */}
          {tools.slice(0, 6).map((tool) => (
            <Card key={tool.id} className="group relative overflow-hidden">
              {tool.is_office && (
                <div className="absolute top-2 right-2 z-10">
                  <Badge className="flex items-center gap-1">
                    官方
                  </Badge>
                </div>
              )}
              <CardHeader className="pb-2">
                <div className="flex items-center gap-2">
                  <div className="flex h-10 w-10 items-center justify-center rounded-md bg-primary/10 text-primary-foreground overflow-hidden">
                    {tool.icon ? (
                      <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
                    ) : (
                      <Wrench className="h-5 w-5" />
                    )}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-semibold">{tool.name}</h3>
                    </div>
                    <p className="text-sm text-muted-foreground">{tool.subtitle}</p>
                  </div>
                </div>
              </CardHeader>
              
              <CardContent>
                <div className="mb-2 flex flex-wrap gap-1">
                  {tool.labels.slice(0, 5).filter(label => label !== "官方").map((label, i) => (
                    <Badge key={i} variant="outline" className="text-xs">
                      {label}
                    </Badge>
                  ))}
                </div>
                
                <div className="flex items-center justify-between text-sm text-muted-foreground">
                  <div className="flex items-center">
                    <User className="mr-1 h-3 w-3" />
                    <span>{tool.author}</span>
                  </div>
                  <div className="flex items-center">
                    <Download className="mr-1 h-3 w-3" />
                    <span>{tool.installCount} 安装</span>
                  </div>
                </div>
              </CardContent>
              
              <div className="absolute inset-0 flex items-center justify-center bg-background/80 opacity-0 transition-opacity group-hover:opacity-100">
                <div className="flex gap-2">
                  <Button 
                    size="sm"
                    onClick={() => openInstallDialog(tool)}
                  >
                    <Download className="mr-2 h-4 w-4" />
                    安装
                  </Button>
                  <Button 
                    variant="outline" 
                    size="sm"
                    asChild
                  >
                    <Link href={`/tools/${tool.id}`}>
                      <Info className="mr-2 h-4 w-4" />
                      详情
                    </Link>
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
      
      {/* 工具详情对话框 */}
      <Dialog open={isDetailOpen} onOpenChange={setIsDetailOpen}>
        <DialogContent className="max-w-3xl">
          {selectedTool && (
            <>
              <DialogHeader>
                <div className="flex items-center gap-3 mb-2">
                  <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10">
                    {selectedTool.icon ? (
                      <img src={selectedTool.icon} alt={selectedTool.name} className="h-full w-full object-cover" />
                    ) : (
                      <Wrench className="h-6 w-6" />
                    )}
                  </div>
                  <div>
                    <DialogTitle className="text-xl">{selectedTool.name}</DialogTitle>
                    <p className="text-muted-foreground">{selectedTool.subtitle}</p>
                  </div>
                </div>
              </DialogHeader>
              
              <div className="flex flex-wrap gap-1 mb-4">
                {selectedTool.labels.map((label, i) => (
                  <Badge key={i} variant={label === "官方" ? "default" : "outline"}>
                    {label}
                  </Badge>
                ))}
              </div>
              
              <div className="flex justify-between items-center mb-4 text-sm text-muted-foreground">
                <div className="flex items-center">
                  <User className="mr-1 h-4 w-4" />
                  <span>作者: {selectedTool.author}</span>
                </div>
                <div className="flex items-center">
                  <Download className="mr-1 h-4 w-4" />
                  <span>{selectedTool.installCount} 安装</span>
                </div>
              </div>
              
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsDetailOpen(false)}>取消</Button>
                <Button onClick={() => openInstallDialog(selectedTool)}>
                  <Download className="mr-2 h-4 w-4" />
                  安装
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
      
      {/* 工具安装确认对话框 */}
      <InstallToolDialog 
        open={isInstallDialogOpen}
        onOpenChange={setIsInstallDialogOpen}
        tool={selectedTool}
        onSuccess={handleInstallSuccess}
      />
      
      {/* 用户工具详情对话框 */}
      <Dialog open={isUserToolDetailOpen} onOpenChange={setIsUserToolDetailOpen}>
        <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
          {selectedUserTool && (
            <>
              <DialogHeader>
                <div className="flex items-center gap-3 mb-2">
                  <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10">
                    {selectedUserTool.icon ? (
                      <img src={selectedUserTool.icon} alt={selectedUserTool.name} className="h-full w-full object-cover" />
                    ) : (
                      <Wrench className="h-6 w-6" />
                    )}
                  </div>
                  <div>
                    <DialogTitle className="text-xl">{selectedUserTool.name}</DialogTitle>
                    <div className="flex items-center gap-2 mt-1">
                      <p className="text-muted-foreground">{selectedUserTool.subtitle}</p>
                      <Badge variant="outline" className="text-xs">v{selectedUserTool.current_version || "1.0.0"}</Badge>
                    </div>
                  </div>
                </div>
              </DialogHeader>
              
              <div className="flex flex-wrap gap-1 mb-4">
                {selectedUserTool.labels.map((label, i) => (
                  <Badge key={i} variant={label === "官方" ? "default" : "outline"}>
                    {label}
                  </Badge>
                ))}
              </div>
              
              <div className="mb-4 text-sm text-muted-foreground">
                <div className="flex items-center gap-2">
                  <User className="mr-1 h-4 w-4" />
                  <span>作者: {selectedUserTool.author}</span>
                </div>
              </div>
              
              <div className="mt-6 space-y-6">
                <div>
                  <h3 className="text-lg font-medium mb-3">工具介绍</h3>
                  <div className="prose dark:prose-invert max-w-none">
                    <ReactMarkdown>{selectedUserTool.description}</ReactMarkdown>
                  </div>
                </div>
                
                <div>
                  <h3 className="text-lg font-medium mb-3">工具列表</h3>
                  <div className="rounded-md border overflow-hidden">
                    {selectedUserTool.tool_list && selectedUserTool.tool_list.map((item, i) => (
                      <div key={i} className={`${i !== 0 ? "border-t" : ""}`}>
                        {/* 工具头部信息 */}
                        <div className="px-4 py-3 bg-muted/5 flex items-center gap-3">
                          <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/10">
                            <Command className="h-3 w-3" />
                          </div>
                          <div className="font-medium">{item.name}</div>
                        </div>
                        
                        {/* 工具描述 */}
                        <div className="px-4 py-2 text-sm text-muted-foreground">
                          {item.description}
                        </div>
                        
                        {/* 参数列表 */}
                        {item.parameters && Object.keys(item.parameters.properties).length > 0 ? (
                          <div className="px-4 py-3 bg-muted/5">
                            <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                            <div className="grid grid-cols-12 gap-2">
                              {Object.entries(item.parameters.properties)
                                .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                                .map(([key, value]) => {
                                  // 处理特殊键名，移除可能的前缀如 "{"
                                  const cleanKey = key.replace(/^\{/, '');
                                  // 确保value是对象并且有description属性
                                  const description = typeof value === 'object' && value ? value.description : null;
                                  
                                  if (description === null) return null;
                                  
                                  return (
                                    <div key={key} className="col-span-12 sm:col-span-6 xl:col-span-4">
                                      <div className="flex items-center gap-2">
                                        <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{cleanKey}</code>
                                        {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                          <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                        )}
                                      </div>
                                      <div className="text-xs text-muted-foreground mt-1">{description}</div>
                                    </div>
                                  );
                                })
                                .filter(Boolean)
                              }
                            </div>
                          </div>
                        ) : item.inputSchema && Object.keys(item.inputSchema.properties).length > 0 ? (
                          <div className="px-4 py-3 bg-muted/5">
                            <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                            <div className="grid grid-cols-12 gap-2">
                              {Object.entries(item.inputSchema.properties).map(([key, value]) => (
                                <div key={key} className="col-span-12 sm:col-span-6 xl:col-span-4">
                                  <div className="flex items-center gap-2">
                                    <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{key}</code>
                                    {item.inputSchema && item.inputSchema.required?.includes(key) && (
                                      <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                    )}
                                  </div>
                                  <div className="text-xs text-muted-foreground mt-1">{(value as any).description}</div>
                                </div>
                              ))}
                            </div>
                          </div>
                        ) : null}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
              
              <DialogFooter className="mt-6">
                <Button variant="outline" onClick={() => setIsUserToolDetailOpen(false)}>关闭</Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* 删除工具确认对话框 */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除工具</AlertDialogTitle>
            <AlertDialogDescription>
              您确定要删除 "{toolToDelete?.name}" 吗？此操作无法撤销，删除后您将需要重新安装才能使用此工具。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction 
              className="bg-destructive hover:bg-destructive/90"
              onClick={handleDeleteTool}
              disabled={isDeletingTool}
            >
              {isDeletingTool ? "删除中..." : "删除"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

// 模拟用户已安装工具数据
function generateMockUserTools(): UserTool[] {
  return [
    {
      id: "tool-1",
      name: "数据库查询",
      icon: null,
      subtitle: "数据库查询工具 (预授权).",
      description: "强大的数据库查询工具，支持多种数据库类型。\n\n## 功能亮点\n\n- 支持MySQL、PostgreSQL、SQLite等多种数据库\n- 自动补全SQL语句\n- 查询结果可视化\n- 支持导出为CSV、JSON等格式\n\n## 使用方法\n\n只需在对话中输入您的SQL查询需求，我会自动为您生成最佳查询语句。",
      user_id: "user-1",
      author: "junjiem",
      labels: ["数据", "查询", "utilities"],
      tool_type: "mcp",
      upload_type: "github",
      upload_url: "https://github.com/example/db-query",
      install_command: { type: "sse", url: "https://api.example.com/tools/1" },
      is_office: true,
      installCount: 23928,
      usageCount: 47,
      current_version: "2.1.0",
      status: ToolStatus.APPROVED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      tool_list: [
        {
          name: "查询数据",
          description: "执行SQL查询并返回结果",
          inputSchema: {
            type: "object",
            properties: {
              sql: {
                type: "string",
                description: "要执行的SQL查询语句",
              },
              database: {
                type: "string",
                description: "要连接的数据库名称",
              },
              limit: {
                type: "number",
                description: "限制返回的结果数量",
              }
            },
            required: ["sql", "database"]
          }
        },
        {
          name: "导出数据",
          description: "将查询结果导出为CSV或JSON格式",
          inputSchema: {
            type: "object",
            properties: {
              format: {
                type: "string",
                description: "导出格式，支持csv或json",
              },
              query_id: {
                type: "string",
                description: "要导出的查询ID",
              }
            },
            required: ["format", "query_id"]
          }
        },
        {
          name: "连接测试",
          description: "测试数据库连接是否可用",
          inputSchema: {
            type: "object",
            properties: {
              host: {
                type: "string",
                description: "数据库主机地址",
              },
              port: {
                type: "number",
                description: "数据库端口",
              },
              username: {
                type: "string",
                description: "用户名",
              },
              password: {
                type: "string",
                description: "密码",
              },
              database: {
                type: "string",
                description: "数据库名称",
              }
            },
            required: ["host", "username", "password", "database"]
          }
        }
      ]
    },
    {
      id: "tool-2",
      name: "DuckDuckGo",
      icon: "/mock-icons/duckduckgo.png",
      subtitle: "一个注重隐私的搜索引擎。",
      description: "DuckDuckGo是一个专注于保护用户隐私的搜索引擎。\n\n## 隐私特性\n\n- 不跟踪用户搜索历史\n- 不存储个人信息\n- 不针对用户进行个性化广告投放\n\n## 搜索功能\n\n- 支持普通网页搜索\n- 支持图片搜索\n- 支持视频搜索\n- 支持新闻搜索\n\n使用此工具，您可以在保护隐私的同时获取搜索结果。",
      user_id: "user-2",
      author: "langgenius",
      labels: ["搜索"],
      tool_type: "mcp",
      upload_type: "github",
      upload_url: "https://github.com/example/duckduckgo",
      install_command: { type: "sse", url: "https://api.example.com/tools/2" },
      is_office: false,
      installCount: 15782,
      usageCount: 23,
      current_version: "1.5.3",
      status: ToolStatus.APPROVED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      tool_list: [
        {
          name: "网页搜索",
          description: "搜索网页内容",
          inputSchema: {
            type: "object",
            properties: {
              query: {
                type: "string",
                description: "搜索关键词",
              },
              region: {
                type: "string",
                description: "搜索区域，如us, uk, cn等",
              },
              safe_search: {
                type: "boolean",
                description: "是否启用安全搜索",
              }
            },
            required: ["query"]
          }
        },
        {
          name: "图片搜索",
          description: "搜索图片内容",
          inputSchema: {
            type: "object",
            properties: {
              query: {
                type: "string",
                description: "搜索关键词",
              },
              size: {
                type: "string",
                description: "图片尺寸筛选，如small, medium, large",
              },
              color: {
                type: "string",
                description: "图片颜色筛选",
              }
            },
            required: ["query"]
          }
        }
      ]
    },
    {
      id: "tool-3",
      name: "WolframAlpha",
      icon: "/mock-icons/wolframalpha.png",
      subtitle: "WolframAlpha 是一个强大的计算知识引擎。",
      description: "WolframAlpha是一个计算知识引擎，提供数学、科学等领域的答案。\n\n## 核心功能\n\n- 数学计算与分析\n- 科学数据查询\n- 复杂问题求解\n- 图表绘制与可视化\n\n## 应用领域\n\n- 学术研究\n- 教育辅助\n- 数据分析\n- 科学计算",
      user_id: "user-2",
      author: "langgenius",
      labels: ["数学", "科学", "教育", "productivity"],
      tool_type: "mcp",
      upload_type: "github",
      upload_url: "https://github.com/example/wolframalpha",
      install_command: { type: "sse", url: "https://api.example.com/tools/3" },
      is_office: false,
      installCount: 12543,
      usageCount: 15,
      current_version: "3.0.1",
      status: ToolStatus.APPROVED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      tool_list: [
        {
          name: "数学计算",
          description: "计算数学表达式",
          inputSchema: {
            type: "object",
            properties: {
              expression: {
                type: "string",
                description: "数学表达式",
              },
              format: {
                type: "string",
                description: "结果格式化类型",
              }
            },
            required: ["expression"]
          }
        },
        {
          name: "方程求解",
          description: "求解方程式",
          inputSchema: {
            type: "object",
            properties: {
              equation: {
                type: "string",
                description: "要求解的方程",
              },
              variable: {
                type: "string",
                description: "求解变量",
              }
            },
            required: ["equation", "variable"]
          }
        },
        {
          name: "图表绘制",
          description: "绘制函数图表",
          inputSchema: {
            type: "object",
            properties: {
              function: {
                type: "string",
                description: "要绘制的函数",
              },
              xrange: {
                type: "string",
                description: "x轴范围，如'-10,10'",
              },
              yrange: {
                type: "string",
                description: "y轴范围，如'-5,5'",
              }
            },
            required: ["function"]
          }
        },
        {
          name: "单位转换",
          description: "在不同单位之间进行转换",
          inputSchema: {
            type: "object",
            properties: {
              value: {
                type: "number",
                description: "要转换的数值",
              },
              from_unit: {
                type: "string",
                description: "原始单位",
              },
              to_unit: {
                type: "string",
                description: "目标单位",
              }
            },
            required: ["value", "from_unit", "to_unit"]
          }
        }
      ]
    },
    {
      id: "tool-4",
      name: "storage",
      icon: null,
      subtitle: "a key-value storage tool allow you save and get data across different apps.",
      description: "一个键值存储工具，允许您跨不同应用程序保存和获取数据。\n\n## 使用场景\n\n- 会话状态保存\n- 用户偏好设置存储\n- 临时数据缓存\n- 应用程序间数据共享\n\n## 技术特点\n\n- 高性能读写\n- 数据持久化\n- 安全加密\n- 支持多种数据类型",
      user_id: "user-3",
      author: "hjlarry",
      labels: ["存储", "数据"],
      tool_type: "mcp",
      upload_type: "github",
      upload_url: "https://github.com/example/storage",
      install_command: { type: "sse", url: "https://api.example.com/tools/4" },
      is_office: false,
      installCount: 8721,
      usageCount: 62,
      current_version: "0.9.5",
      status: ToolStatus.APPROVED,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      tool_list: [
        {
          name: "存储数据",
          description: "存储键值对数据",
          inputSchema: {
            type: "object",
            properties: {
              key: {
                type: "string",
                description: "数据键名",
              },
              value: {
                type: "string",
                description: "要存储的数据值",
              },
              ttl: {
                type: "number",
                description: "数据生存时间(秒)，0表示永久",
              }
            },
            required: ["key", "value"]
          }
        },
        {
          name: "获取数据",
          description: "根据键名获取数据",
          inputSchema: {
            type: "object",
            properties: {
              key: {
                type: "string",
                description: "要获取的数据键名",
              },
              default_value: {
                type: "string",
                description: "数据不存在时的默认返回值",
              }
            },
            required: ["key"]
          }
        },
        {
          name: "删除数据",
          description: "删除指定键的数据",
          inputSchema: {
            type: "object",
            properties: {
              key: {
                type: "string",
                description: "要删除的数据键名",
              }
            },
            required: ["key"]
          }
        }
      ]
    }
  ];
}

// 生成模拟工具数据
function generateMockTools(): Tool[] {
  const tools: Tool[] = []
  
  const mockToolNames = [
    "Surge部署工具", "GitHub助手", "代码生成器", 
    "文档摘要工具", "SQL查询助手", "数据分析工具",
    "图像处理工具", "音频转文本", "API测试助手"
  ]
  
  const mockSubtitles = [
    "快速部署网站到Surge.sh", "管理GitHub仓库和PR", "自动生成多语言代码",
    "自动生成文档摘要", "SQL生成与优化", "数据可视化与分析",
    "图像编辑与优化", "将音频转换为文本", "测试API端点"
  ]
  
  const mockAuthors = ["用户1", "John Doe", "Jane Smith", "Dev Tools", "AI Helper"]
  
  const mockIcons = [null, "/icons/tool1.png", "/icons/tool2.png"]
  
  const mockLabels = ["数据分析", "代码生成", "文档处理", "图像处理", "音频处理", "网络爬虫", "开发工具", "办公工具"]
  
  for (let i = 0; i < mockToolNames.length; i++) {
    const randomLabels = []
    const labelPool = [...mockLabels]
    const labelCount = Math.min(Math.floor(Math.random() * 5) + 1, 5) // 最多5个标签
    
    for (let j = 0; j < labelCount; j++) {
      if (labelPool.length === 0) break
      const randomIndex = Math.floor(Math.random() * labelPool.length)
      randomLabels.push(labelPool[randomIndex])
      labelPool.splice(randomIndex, 1)
    }
    
    // 随机决定是否为官方工具
    const isOffice = Math.random() > 0.7
    if (isOffice) randomLabels.push("官方")
    
    const toolList = []
    const toolCount = Math.floor(Math.random() * 5) + 1 // 1-5个工具
    
    for (let j = 0; j < toolCount; j++) {
      toolList.push({
        name: `功能${j + 1}`,
        description: `这是工具的第${j + 1}个功能，用于执行特定任务。`,
        inputSchema: {
          type: "object",
          properties: {
            param1: {
              type: "string",
              description: "参数1描述",
            }
          },
          required: ["param1"]
        }
      })
    }
    
    tools.push({
      id: `tool-${i + 5}`, // 避免与用户工具ID冲突
      name: mockToolNames[i],
      icon: mockIcons[i % mockIcons.length],
      subtitle: mockSubtitles[i],
      description: `# ${mockToolNames[i]}\n\n这是一个强大的工具，可以帮助您完成各种任务。\n\n## 特性\n\n- 特性1\n- 特性2\n- 特性3\n\n## 使用方法\n\n安装后，您可以在聊天中通过@${mockToolNames[i]}来使用此工具。`,
      user_id: `user-${i % 5 + 1}`,
      author: mockAuthors[i % mockAuthors.length],
      labels: randomLabels,
      tool_type: "mcp",
      upload_type: "github",
      upload_url: `https://github.com/example/tool-${i + 1}`,
      install_command: {
        type: "sse",
        url: `https://api.example.com/tools/${i + 1}`
      },
      tool_list: toolList,
      status: ToolStatus.APPROVED,
      is_office: isOffice,
      installCount: Math.floor(Math.random() * 5000),
      createdAt: new Date(Date.now() - Math.random() * 10000000000).toISOString(),
      updatedAt: new Date(Date.now() - Math.random() * 1000000000).toISOString()
    })
  }
  
  return tools
}

