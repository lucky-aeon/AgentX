"use client"

import type React from "react"

import { useState, useRef, useEffect } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { X, MessageCircle, Bot, Upload, Trash, FileText, Workflow, Zap, Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Slider } from "@/components/ui/slider"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Card, CardContent } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"

// 在文件顶部添加导入
import { createAgent, createAgentWithToast } from "@/lib/agent-service"
import { getInstalledTools } from "@/lib/tool-service" // 导入获取工具的函数
import { API_CONFIG } from "@/lib/api-config"
import { getUserDefaultModelIdWithToast } from "@/lib/user-settings-service"

// 从 edit 页面导入的组件和类型
import AgentBasicInfoForm from "../edit/[id]/components/AgentBasicInfoForm";
import AgentPromptForm from "../edit/[id]/components/AgentPromptForm";
import AgentToolsForm, { knowledgeBaseOptions } from "../edit/[id]/components/AgentToolsForm"; // knowledgeBaseOptions 仍然从这里导入
import ToolDetailSidebar from "../edit/[id]/components/ToolDetailSidebar";
import type { Tool } from "@/types/tool";
import type { AgentTool } from "@/types/agent"; // <-- Import AgentTool

// 导入预览组件
import AgentPreviewChat from "@/components/agent-preview-chat";

// 应用类型定义
type AgentType = "chat" | "agent"

// 应用类型数据
const agentTypes = [
  {
    id: "chat",
    name: "聊天助理",
    description: "可使用工具和知识库的对话机器人，具有记忆功能",
    icon: MessageCircle,
    color: "bg-blue-100 text-blue-600",
  },
  {
    id: "agent",
    name: "功能性助理",
    description: "专注于使用工具处理复杂任务的智能助理，无记忆功能",
    icon: Bot,
    color: "bg-purple-100 text-purple-600",
  },
]

// 模型选项
const modelOptions = [
  { value: "gpt-4o", label: "GPT-4o" },
  { value: "gpt-4-turbo", label: "GPT-4 Turbo" },
  { value: "gpt-3.5-turbo", label: "GPT-3.5 Turbo" },
  { value: "claude-3-opus", label: "Claude 3 Opus" },
  { value: "claude-3-sonnet", label: "Claude 3 Sonnet" },
  { value: "claude-3-haiku", label: "Claude 3 Haiku" },
  { value: "gemini-pro", label: "Gemini Pro" },
  { value: "llama-3-70b", label: "Llama 3 70B" },
]

// 临时的接口，只包含工具的基本信息 (与 edit 页面一致) -> This will be removed
// interface SelectedToolInfo {
//   id: string;
//   name: string;
//   description: string;
// }

interface AgentFormData {
  name: string
  avatar: string | null
  description: string
  systemPrompt: string
  welcomeMessage: string
  tools: AgentTool[] // <-- Use AgentTool[]
  knowledgeBaseIds: string[]
  toolPresetParams: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  } // 工具预设参数，与CreateAgentRequest保持一致
  enabled: boolean
  // agentType is derived from selectedType, not part of formData here
}

export default function CreateAgentPage() {
  const router = useRouter()
  const [selectedType, setSelectedType] = useState<AgentType>("chat")
  const [activeTab, setActiveTab] = useState("basic")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [selectedToolForSidebar, setSelectedToolForSidebar] = useState<Tool | null>(null);
  const [isToolSidebarOpen, setIsToolSidebarOpen] = useState(false);
  const [installedTools, setInstalledTools] = useState<Tool[]>([])
  const [isLoadingTools, setIsLoadingTools] = useState(false)

  // 表单数据
  const [formData, setFormData] = useState<AgentFormData>({
    name: "",
    avatar: null,
    description: "",
    systemPrompt: "你是一个有用的AI助手。",
    welcomeMessage: "你好！我是你的AI助手，有什么可以帮助你的吗？",
    tools: [],
    knowledgeBaseIds: [],
    toolPresetParams: {}, // 初始化为空对象
    enabled: true,
  })

  // 加载已安装的工具
  useEffect(() => {
    const fetchInstalledTools = async () => {
      setIsLoadingTools(true)
      try {
        const response = await getInstalledTools({ pageSize: 100 });
        if (response.code === 200 && response.data && Array.isArray(response.data.records)) {
          setInstalledTools(response.data.records);
        } else {
          console.error("获取已安装工具失败:", response.message);
        }
      } catch (error) {
        console.error("获取已安装工具错误:", error);
      } finally {
        setIsLoadingTools(false);
      }
    };

    fetchInstalledTools();
  }, []);

  // 更新表单字段
  const updateFormField = (field: string, value: any) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  // 更新工具预设参数
  const updateToolPresetParameters = (toolId: string, presetParams: Record<string, Record<string, string>>) => {
    // 获取当前工具信息
    const selectedTool = installedTools.find((t: Tool) => t.id === toolId || t.toolId === toolId);
    
    if (!selectedTool || !selectedTool.mcpServerName) {
      console.error("无法找到对应的工具或工具缺少 mcpServerName");
      toast({
        title: "无法更新工具参数",
        description: "工具信息不完整",
        variant: "destructive",
      });
      return;
    }

    const mcpServerName = selectedTool.mcpServerName;
    
    setFormData(prev => {
      // 创建新的 toolPresetParams 对象
      const newToolPresetParams = { ...prev.toolPresetParams };
      
      // 确保 mcpServerName 的键存在
      if (!newToolPresetParams[mcpServerName]) {
        newToolPresetParams[mcpServerName] = {};
      }
      
      // 遍历工具的所有功能
      Object.keys(presetParams).forEach(functionName => {
        // 获取该功能的所有参数
        const params = presetParams[functionName];
        
        // 确保函数名的键存在
        if (!newToolPresetParams[mcpServerName][functionName]) {
          newToolPresetParams[mcpServerName][functionName] = {};
        }
        
        // 将参数直接设置为嵌套对象
        Object.entries(params).forEach(([paramName, paramValue]) => {
          newToolPresetParams[mcpServerName][functionName][paramName] = paramValue || '';
        });
      });
      
      return {
        ...prev,
        toolPresetParams: newToolPresetParams
      };
    });
    
    toast({
      title: "参数预设已更新",
      description: `已为工具 ${selectedTool.name} 更新参数预设`,
    });
  };

  // 切换工具
  const toggleTool = (toolToToggle: Tool) => {
    // 使用 toolId（如果存在）或 id 作为工具标识符
    const toolIdentifier = toolToToggle.toolId || toolToToggle.id;
    const isToolCurrentlyEnabled = formData.tools.some(t => t.id === toolIdentifier);
    
    setFormData((prev) => {
      let updatedTools: AgentTool[]; // <-- Use AgentTool[]
      if (isToolCurrentlyEnabled) {
        updatedTools = prev.tools.filter((t) => t.id !== toolIdentifier);
      } else {
        // 从 Tool 对象创建 AgentTool 对象
        const newAgentTool: AgentTool = {
          id: toolIdentifier, 
          name: toolToToggle.name, 
          description: toolToToggle.description || undefined, // Ensure compatibility with AgentTool
        };
        updatedTools = [...prev.tools, newAgentTool];
      }
      return { ...prev, tools: updatedTools };
    });
    
    toast({
      title: `工具已${!isToolCurrentlyEnabled ? "添加" : "移除"}: ${toolToToggle.name}`,
    });
  }

  // 切换知识库
  const toggleKnowledgeBase = (kbId: string, kbName?: string) => {
    setFormData((prev) => {
      const knowledgeBaseIds = [...prev.knowledgeBaseIds]
      if (knowledgeBaseIds.includes(kbId)) {
        return { ...prev, knowledgeBaseIds: knowledgeBaseIds.filter((id) => id !== kbId) }
      } else {
        return { ...prev, knowledgeBaseIds: [...knowledgeBaseIds, kbId] }
      }
    })
    const nameToDisplay = kbName || knowledgeBaseOptions.find((kb) => kb.id === kbId)?.name;
    toast({
      title: `知识库已${!formData.knowledgeBaseIds.includes(kbId) ? "关联" : "取消关联"}: ${nameToDisplay || kbId}`
    })
  }

  // 处理头像上传
  const handleAvatarUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    // 检查文件类型
    if (!file.type.startsWith("image/")) {
      toast({
        title: "文件类型错误",
        description: "请上传图片文件",
        variant: "destructive",
      })
      return
    }

    // 检查文件大小 (限制为2MB)
    if (file.size > 2 * 1024 * 1024) {
      toast({
        title: "文件过大",
        description: "头像图片不能超过2MB",
        variant: "destructive",
      })
      return
    }

    // 创建文件预览URL
    const reader = new FileReader()
    reader.onload = (e) => {
      updateFormField("avatar", e.target?.result as string)
    }
    reader.readAsDataURL(file)
  }

  // 移除头像
  const removeAvatar = () => {
    updateFormField("avatar", null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  // 触发文件选择
  const triggerFileInput = () => {
    fileInputRef.current?.click()
  }

  // 新增：处理工具卡片点击，用于显示侧边栏
  const handleToolClick = (tool: Tool) => {
    // 确保当前工具不是已经选中的工具，避免重复打开侧边栏
    if (selectedToolForSidebar && selectedToolForSidebar.id === tool.id) {
      return;
    }
    
    console.log("Tool clicked:", tool);
    // 先关闭侧边栏，再设置工具，避免同时存在两个侧边栏
    setIsToolSidebarOpen(false);
    
    // 使用setTimeout延迟设置新工具，确保旧侧边栏已经关闭
    setTimeout(() => {
      setSelectedToolForSidebar(tool);
      setIsToolSidebarOpen(true);
    }, 100);
  };

  // 处理创建助理
  const handleCreateAgent = async () => {
    if (!formData.name.trim()) {
      toast({
        title: "请输入名称",
        variant: "destructive",
      });
      return;
    }

    setIsSubmitting(true);

    try {
      // 将工具对象数组转换为工具ID字符串数组
      const toolIds = formData.tools.map(tool => tool.id);
      
      const agentData = {
        name: formData.name,
        avatar: formData.avatar,
        description: formData.description || "",
        agentType: selectedType === "chat" ? "CHAT_ASSISTANT" : "FUNCTIONAL_AGENT" as "CHAT_ASSISTANT" | "FUNCTIONAL_AGENT",
        systemPrompt: selectedType === "chat" ? formData.systemPrompt : "",
        welcomeMessage: selectedType === "chat" ? formData.welcomeMessage : "",
        modelConfig: {
          modelName: "gpt-4o", 
          temperature: 0.7,
          maxTokens: 2000
        },
        toolIds: toolIds, // 使用工具ID数组
        knowledgeBaseIds: selectedType === "chat" ? formData.knowledgeBaseIds : [],
        toolPresetParams: formData.toolPresetParams,
        userId: API_CONFIG.CURRENT_USER_ID,
      };

      const response = await createAgentWithToast(agentData);

      if (response.code === 200) {
        // 创建成功的toast已经由createAgentWithToast处理
        router.push("/studio");
      }
      // 错误也已由createAgentWithToast处理
    } catch (error) {
      console.error("创建失败:", error);
      // createAgentWithToast 通常也会处理 catch 块的 toast，但以防万一
      if (!(error instanceof Error && error.message.includes("toast already shown"))) {
        toast({
          title: "创建失败",
          description: "请稍后再试",
          variant: "destructive",
        });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // 根据选择的类型更新可用的标签页
  const getAvailableTabs = () => {
    if (selectedType === "chat") {
      return [
        { id: "basic", label: "基本信息" },
        { id: "prompt", label: "提示词配置" },
        { id: "tools", label: "工具与知识库" },
      ]
    } else {
      return [
        { id: "basic", label: "基本信息" },
        { id: "tools", label: "工具配置" },
      ]
    }
  }

  // 当类型改变时，确保当前标签页有效
  const handleTypeChange = (type: AgentType) => {
    setSelectedType(type)

    // 如果当前标签页在新类型中不可用，则切换到基本信息标签页
    if (type === "agent" && activeTab === "prompt") {
      setActiveTab("basic")
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-7xl flex max-h-[95vh] overflow-hidden">
        {/* 左侧表单 */}
        <div className="w-3/5 p-8 overflow-auto">
          <div className="flex items-center justify-between mb-6">
            <h1 className="text-2xl font-bold">
              创建新的{selectedType === "chat" ? "聊天助理" : "功能性助理"}
            </h1>
            <Button variant="outline" asChild>
                <Link href="/studio">取消</Link>
            </Button>
          </div>

          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
            <TabsList
              className="grid w-full"
              style={{ gridTemplateColumns: `repeat(${getAvailableTabs().length}, minmax(0, 1fr))` }}
            >
              {getAvailableTabs().map((tab) => (
                <TabsTrigger key={tab.id} value={tab.id}>
                  {tab.label}
                </TabsTrigger>
              ))}
            </TabsList>

            <TabsContent value="basic" className="space-y-6">
              <AgentBasicInfoForm
                formData={formData}
                selectedType={selectedType}
                updateFormField={updateFormField}
                triggerFileInput={triggerFileInput}
                handleAvatarUpload={handleAvatarUpload}
                removeAvatar={removeAvatar}
                fileInputRef={fileInputRef}
                    />
            </TabsContent>

            {selectedType === "chat" && (
              <TabsContent value="prompt" className="space-y-6">
                <AgentPromptForm
                  formData={formData}
                  updateFormField={updateFormField}
                  />
              </TabsContent>
            )}

            <TabsContent value="tools" className="space-y-6">
              <AgentToolsForm
                formData={formData}
                selectedType={selectedType}
                toggleTool={toggleTool}
                toggleKnowledgeBase={toggleKnowledgeBase}
                onToolClick={handleToolClick}
              />
            </TabsContent>
          </Tabs>

          {/* 底部按钮 */}
          <div className="flex justify-end pt-6 border-t mt-6">
              <Button onClick={handleCreateAgent} disabled={isSubmitting}>
              {isSubmitting ? "创建中..." : "确认创建"}
              </Button>
          </div>
        </div>

        {/* 右侧预览 - 根据类型显示不同内容 */}
        <div className="w-2/5 bg-gray-50 p-8 overflow-auto border-l">
          <div className="mb-6">
            <h2 className="text-xl font-semibold">预览</h2>
            <p className="text-muted-foreground">
              {selectedType === "chat" ? "查看聊天助理在对话中的表现" : "查看功能性助理处理复杂任务的界面"}
            </p>
          </div>

          {/* 聊天助手预览 */}
          {selectedType === "chat" && (
            <AgentPreviewChat
              agentName={formData.name || "新建聊天助理"}
              agentAvatar={formData.avatar}
              systemPrompt={formData.systemPrompt}
              welcomeMessage={formData.welcomeMessage}
              toolIds={formData.tools.map(t => t.id)}
              toolPresetParams={formData.toolPresetParams as unknown as Record<string, Record<string, Record<string, string>>>}
              disabled={!formData.name || !formData.systemPrompt}
              className="h-[500px]"
            />
          )}

          {/* Agent预览 */}
          {selectedType === "agent" && (
            <div className="border rounded-lg bg-white shadow-sm overflow-hidden">
              <div className="border-b p-3 flex items-center justify-between bg-gray-50">
                <div className="flex items-center gap-2">
                  <Avatar className="h-8 w-8">
                    <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                    <AvatarFallback className="bg-purple-100 text-purple-600">
                      {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                    </AvatarFallback>
                  </Avatar>
                  <span className="font-medium">{formData.name || "新建功能性助理"}</span>
                </div>
                <Badge variant="outline">默认模型</Badge>
              </div>

              <div className="h-[500px] flex flex-col">
                <div className="flex-1 p-4 overflow-auto space-y-4">
                  {/* Agent任务界面 */}
                  <div className="bg-gray-50 rounded-lg p-4 border">
                    <h3 className="font-medium mb-2">任务描述</h3>
                    <p className="text-sm text-muted-foreground mb-4">请Agent帮我分析以下数据并生成报告。</p>
                    <div className="flex items-center gap-2 mb-4">
                      <Button variant="outline" size="sm" disabled>
                        <FileText className="h-4 w-4 mr-2" />
                        上传文件
                      </Button>
                      <Button variant="outline" size="sm" disabled>
                        <Workflow className="h-4 w-4 mr-2" />
                        选择工作流
                      </Button>
                    </div>
                  </div>

                  {/* 任务执行状态 */}
                  <div className="space-y-4">
                    <div className="bg-white rounded-lg p-4 border">
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-medium">任务执行中</h3>
                        <Badge variant="outline" className="bg-blue-50">
                          进行中
                        </Badge>
                      </div>
                      <div className="space-y-3">
                        <div>
                          <div className="flex justify-between text-sm mb-1">
                            <span>分析数据</span>
                            <span>完成</span>
                          </div>
                          <Progress value={100} className="h-2" />
                        </div>
                        <div>
                          <div className="flex justify-between text-sm mb-1">
                            <span>生成报告</span>
                            <span>60%</span>
                          </div>
                          <Progress value={60} className="h-2" />
                        </div>
                        <div>
                          <div className="flex justify-between text-sm mb-1">
                            <span>格式化输出</span>
                            <span>等待中</span>
                          </div>
                          <Progress value={0} className="h-2" />
                        </div>
                      </div>
                    </div>

                    {/* 工具使用记录 */}
                    <div className="bg-white rounded-lg p-4 border">
                      <h3 className="font-medium mb-2">工具使用记录</h3>
                      <div className="space-y-2">
                        {formData.tools.some((t) => t.id === "file-reader") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <FileText className="h-4 w-4 text-blue-500" />
                            <span>已读取文件：数据分析.xlsx</span>
                          </div>
                        )}
                        {formData.tools.some((t) => t.id === "code-interpreter") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <Zap className="h-4 w-4 text-purple-500" />
                            <span>执行代码：数据处理脚本</span>
                          </div>
                        )}
                        {formData.tools.some((t) => t.id === "web-search") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <Search className="h-4 w-4 text-green-500" />
                            <span>搜索相关信息：市场趋势分析</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                {/* 底部操作栏 */}
                <div className="p-4 border-t">
                  <div className="flex gap-2">
                    <Button variant="outline" className="flex-1" disabled>
                      取消任务
                    </Button>
                    <Button className="flex-1" disabled>
                      查看结果
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* 配置摘要 */}
          <div className="mt-6">
            <h3 className="text-lg font-medium mb-3">配置摘要</h3>
            <Card>
              <CardContent className="p-4 space-y-3">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">类型</span>
                  <span className="text-sm font-medium">{selectedType === "chat" ? "聊天助理" : "功能性助理"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">工具数量</span>
                  <span className="text-sm font-medium">{formData.tools.length}</span>
                </div>
                {selectedType === "chat" && (
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">知识库数量</span>
                    <span className="text-sm font-medium">{formData.knowledgeBaseIds.length}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">状态</span>
                  <Badge variant={formData.enabled ? "outline" : "default"} className="text-xs">
                    {formData.enabled ? "公开" : "私有"}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      <ToolDetailSidebar
        tool={selectedToolForSidebar}
        isOpen={isToolSidebarOpen}
        onClose={() => setIsToolSidebarOpen(false)}
        presetParameters={selectedToolForSidebar && selectedToolForSidebar.mcpServerName && formData.toolPresetParams[selectedToolForSidebar.mcpServerName] ? 
          formData.toolPresetParams[selectedToolForSidebar.mcpServerName] : 
          {}}
        onSavePresetParameters={updateToolPresetParameters}
      />
    </div>
  )
}

