"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Search, X, Plus, Wrench, Download, Info, User, Check, ChevronRight, ArrowLeft } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardHeader, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { toast } from "@/hooks/use-toast"

import { Tool, ToolStatus } from "@/types/tool"
import { getMarketToolsWithToast, installToolWithToast } from "@/lib/tool-service"
import { InstallToolDialog } from "@/components/tool/install-tool-dialog"

export default function ToolsMarketPage() {
  // 工具市场状态
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [debouncedQuery, setDebouncedQuery] = useState("")
  const [tools, setTools] = useState<Tool[]>([])
  const [selectedTool, setSelectedTool] = useState<Tool | null>(null)
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false)
  const [installingToolId, setInstallingToolId] = useState<string | null>(null)
  
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

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("")
  }

  // 处理安装工具
  const handleInstallTool = async (toolId: string) => {
    try {
      setInstallingToolId(toolId)
      
      // 在实际环境中使用API调用安装
      if (process.env.NODE_ENV !== 'development') {
        const response = await installToolWithToast(toolId)
        
        if (response.code !== 200) {
          // 错误处理由withToast处理
          setInstallingToolId(null)
          return
        }
      } else {
        // 模拟安装过程
        await new Promise(resolve => setTimeout(resolve, 1500))
      }
      
      toast({
        title: "安装成功",
        description: `工具已成功安装`,
      })
    } catch (error) {
      console.error("安装工具失败", error)
    } finally {
      setInstallingToolId(null)
    }
  }

  // 打开安装确认对话框
  const openInstallDialog = (tool: Tool) => {
    setSelectedTool(tool)
    setIsInstallDialogOpen(true)
  }

  return (
    <div className="container py-6">
      {/* 页面头部 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <Button variant="ghost" size="sm" asChild className="mb-2">
            <Link href="/tools">
              <ArrowLeft className="mr-2 h-4 w-4" />
              返回我的工具
            </Link>
          </Button>
          <h1 className="text-3xl font-bold tracking-tight">工具市场</h1>
          <p className="text-muted-foreground">发现和安装新的AI助手扩展能力</p>
        </div>
        
        <Button asChild>
          <Link href="/tools/upload">
            <Plus className="mr-2 h-4 w-4" />
            上传工具
          </Link>
        </Button>
      </div>
      
      {/* 搜索区域 */}
      <div className="mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input 
            type="search" 
            placeholder="搜索工具..." 
            className="pl-10 pr-10" 
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {searchQuery && (
            <Button 
              variant="ghost" 
              size="icon" 
              className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
              onClick={clearSearch}
            >
              <X className="h-4 w-4" />
              <span className="sr-only">清除搜索</span>
            </Button>
          )}
        </div>
      </div>

      
      
      {loading ? (
        // 加载状态
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {Array.from({ length: 8 }).map((_, index) => (
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
        <div className="text-center py-16 border rounded-lg bg-gray-50">
          <Wrench className="h-12 w-12 mx-auto text-gray-400 mb-4" />
          <h3 className="text-lg font-medium mb-2">
            {searchQuery ? "未找到匹配的工具" : "还没有工具"}
          </h3>
          <p className="text-muted-foreground mb-6">
            {searchQuery 
              ? "尝试使用不同的搜索词" 
              : "上传一个新工具或等待更多工具发布"}
          </p>
          <Button asChild>
            <Link href="/tools/upload">
              <Plus className="mr-2 h-4 w-4" />
              上传工具
            </Link>
          </Button>
        </div>
      ) : (
        // 工具列表
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {tools.map((tool) => (
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
                  {tool.labels.slice(0, 3).filter(label => label !== "官方").map((label, i) => (
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
                    disabled={installingToolId === tool.id}
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
      
      {/* 工具安装确认对话框 */}
      <InstallToolDialog 
        open={isInstallDialogOpen}
        onOpenChange={setIsInstallDialogOpen}
        tool={selectedTool}
      />
    </div>
  )
}

// 生成模拟工具数据
function generateMockTools(): Tool[] {
  const tools: Tool[] = []
  
  const mockToolNames = [
    "Surge部署工具", "GitHub助手", "代码生成器", 
    "文档摘要工具", "SQL查询助手", "数据分析工具",
    "图像处理工具", "音频转文本", "API测试助手",
    "数据可视化", "翻译助手", "PDF解析器"
  ]
  
  const mockSubtitles = [
    "快速部署网站到Surge.sh", "管理GitHub仓库和PR", "自动生成多语言代码",
    "自动生成文档摘要", "SQL生成与优化", "数据可视化与分析",
    "图像编辑与优化", "将音频转换为文本", "测试API端点",
    "图表与数据展示", "多语言翻译工具", "提取PDF文本内容"
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