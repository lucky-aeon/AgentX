"use client"

import type React from "react"

import { useState, useRef } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { X, MessageCircle, Bot, Upload, Trash, FileText, Workflow, Zap, Search } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/components/ui/use-toast"
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
import { API_CONFIG } from "@/lib/api-config"

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

// 工具选项
const toolOptions = [
  { id: "web-search", name: "网页搜索", description: "允许搜索互联网获取信息" },
  { id: "file-reader", name: "文件读取", description: "允许读取和分析上传的文件" },
  { id: "code-interpreter", name: "代码解释器", description: "允许执行代码并返回结果" },
  { id: "image-generation", name: "图像生成", description: "允许生成和编辑图像" },
  { id: "calculator", name: "计算器", description: "允许执行数学计算" },
]

// 知识库选项
const knowledgeBaseOptions = [
  { id: "kb-1", name: "产品文档", description: "包含产品说明、使用指南等" },
  { id: "kb-2", name: "常见问题", description: "常见问题及解答集合" },
  { id: "kb-3", name: "技术文档", description: "技术规范和API文档" },
  { id: "kb-4", name: "营销资料", description: "营销内容和宣传材料" },
]

interface AgentFormData {
  name: string
  avatar: string | null
  description: string
  systemPrompt: string
  welcomeMessage: string
  modelConfig: {
    model: string
    temperature: number
    maxTokens: number
  }
  tools: string[]
  knowledgeBaseIds: string[]
  status: number
}

export default function CreateAgentPage() {
  const router = useRouter()
  const [selectedType, setSelectedType] = useState<AgentType>("chat")
  const [activeTab, setActiveTab] = useState("basic")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 表单数据
  const [formData, setFormData] = useState<AgentFormData>({
    name: "",
    avatar: null,
    description: "",
    systemPrompt: "你是一个有用的AI助手。",
    welcomeMessage: "你好！我是你的AI助手，有什么可以帮助你的吗？",
    modelConfig: {
      model: "gpt-4o",
      temperature: 0.7,
      maxTokens: 2000,
    },
    tools: [],
    knowledgeBaseIds: [],
    status: 0, // 默认为私有
  })

  // 更新表单字段
  const updateFormField = (field: string, value: any) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  // 更新模型配置
  const updateModelConfig = (field: string, value: any) => {
    setFormData((prev) => ({
      ...prev,
      modelConfig: {
        ...prev.modelConfig,
        [field]: value,
      },
    }))
  }

  // 切换工具
  const toggleTool = (toolId: string) => {
    setFormData((prev) => {
      const tools = [...prev.tools]
      if (tools.includes(toolId)) {
        return { ...prev, tools: tools.filter((id) => id !== toolId) }
      } else {
        return { ...prev, tools: [...tools, toolId] }
      }
    })
  }

  // 切换知识库
  const toggleKnowledgeBase = (kbId: string) => {
    setFormData((prev) => {
      const knowledgeBaseIds = [...prev.knowledgeBaseIds]
      if (knowledgeBaseIds.includes(kbId)) {
        return { ...prev, knowledgeBaseIds: knowledgeBaseIds.filter((id) => id !== kbId) }
      } else {
        return { ...prev, knowledgeBaseIds: [...knowledgeBaseIds, kbId] }
      }
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

  // 处理创建助理
  const handleCreateAgent = async () => {
    if (!formData.name.trim()) {
      toast({
        title: "请输入名称",
        variant: "destructive",
      })
      return
    }

    setIsSubmitting(true)

    try {
      // 准备API请求参数
      const agentData = {
        name: formData.name,
        avatar: formData.avatar,
        description: formData.description || "",
        agentType: selectedType === "chat" ? "CHAT_ASSISTANT" : "FUNCTIONAL_AGENT" as "CHAT_ASSISTANT" | "FUNCTIONAL_AGENT",
        systemPrompt: selectedType === "chat" ? formData.systemPrompt : "",
        welcomeMessage: selectedType === "chat" ? formData.welcomeMessage : "",
        modelConfig: {
          modelName: formData.modelConfig.model,
          temperature: formData.modelConfig.temperature,
          maxTokens: formData.modelConfig.maxTokens,
        },
        tools: formData.tools.map((toolId) => {
          const tool = toolOptions.find((t) => t.id === toolId)
          return {
            id: toolId,
            name: tool?.name || toolId,
            description: tool?.description || "",
          }
        }),
        knowledgeBaseIds: selectedType === "chat" ? formData.knowledgeBaseIds : [],
        userId: API_CONFIG.CURRENT_USER_ID,
      }

      // 调用API创建助理
      const response = await createAgentWithToast(agentData)

      if (response.code === 200) {
        // toast已由withToast处理
        router.push("/studio")
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
      console.error("创建失败:", error)
      // 错误已由withToast处理
    } finally {
      setIsSubmitting(false)
    }
  }

  // 根据选择的类型更新可用的标签页
  const getAvailableTabs = () => {
    if (selectedType === "chat") {
      return [
        { id: "basic", label: "基本信息" },
        { id: "prompt", label: "提示词配置" },
        { id: "model", label: "模型配置" },
        { id: "tools", label: "工具与知识库" },
      ]
    } else {
      return [
        { id: "basic", label: "基本信息" },
        { id: "model", label: "模型配置" },
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
            <h1 className="text-2xl font-bold">创建{selectedType === "chat" ? "聊天助理" : "功能性助理"}</h1>
            <Button variant="ghost" size="icon" asChild>
              <Link href="/studio">
                <X className="h-5 w-5" />
                <span className="sr-only">关闭</span>
              </Link>
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
              {/* Agent类型选择 */}
              <div>
                <h2 className="text-lg font-medium mb-4">选择类型</h2>
                <div className="grid grid-cols-2 gap-4">
                  {agentTypes.map((type) => (
                    <div
                      key={type.id}
                      className={`border rounded-lg p-4 cursor-pointer transition-all ${
                        selectedType === type.id ? "border-blue-500 bg-blue-50" : "hover:border-gray-300"
                      }`}
                      onClick={() => handleTypeChange(type.id as AgentType)}
                    >
                      <div className={`${type.color} w-10 h-10 rounded-lg flex items-center justify-center mb-3`}>
                        <type.icon className="h-5 w-5" />
                      </div>
                      <h3 className="font-medium mb-1">{type.name}</h3>
                      <p className="text-sm text-muted-foreground">{type.description}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* 名称和头像 */}
              <div>
                <h2 className="text-lg font-medium mb-4">名称 & 头像</h2>
                <div className="flex gap-4 items-center">
                  <div className="flex-1">
                    <Label htmlFor="agent-name" className="mb-2 block">
                      名称
                    </Label>
                    <Input
                      id="agent-name"
                      placeholder={`给你的${selectedType === "chat" ? "聊天助理" : "功能性助理"}起个名字`}
                      value={formData.name}
                      onChange={(e) => updateFormField("name", e.target.value)}
                      className="mb-2"
                    />
                  </div>
                  <div>
                    <Label className="mb-2 block">头像</Label>
                    <div className="flex items-center gap-2">
                      <Avatar className="h-12 w-12">
                        <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                        <AvatarFallback className="bg-blue-100 text-blue-600">
                          {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                        </AvatarFallback>
                      </Avatar>
                      <div className="flex flex-col gap-1">
                        <Button variant="outline" size="sm" onClick={triggerFileInput}>
                          <Upload className="h-4 w-4 mr-2" />
                          上传
                        </Button>
                        {formData.avatar && (
                          <Button variant="outline" size="sm" onClick={removeAvatar}>
                            <Trash className="h-4 w-4 mr-2" />
                            移除
                          </Button>
                        )}
                      </div>
                      <input
                        type="file"
                        ref={fileInputRef}
                        className="hidden"
                        accept="image/*"
                        onChange={handleAvatarUpload}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* 描述 */}
              <div>
                <h2 className="text-lg font-medium mb-2">描述</h2>
                <Textarea
                  placeholder={`输入${selectedType === "chat" ? "聊天助理" : "功能性助理"}的描述`}
                  value={formData.description}
                  onChange={(e) => updateFormField("description", e.target.value)}
                  rows={4}
                />
              </div>

              {/* 状态设置 */}
              <div>
                <h2 className="text-lg font-medium mb-4">可见性设置</h2>
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <Label htmlFor="status-private" className="font-medium">
                        私有
                      </Label>
                      <p className="text-sm text-muted-foreground">仅创建者可见</p>
                    </div>
                    <Switch
                      id="status-private"
                      checked={formData.status === 0}
                      onCheckedChange={() => updateFormField("status", 0)}
                    />
                  </div>
                  <div className="flex items-center justify-between">
                    <div>
                      <Label htmlFor="status-public" className="font-medium">
                        公开
                      </Label>
                      <p className="text-sm text-muted-foreground">提交审核后公开展示</p>
                    </div>
                    <Switch
                      id="status-public"
                      checked={formData.status === 1}
                      onCheckedChange={() => updateFormField("status", 1)}
                    />
                  </div>
                </div>
              </div>
            </TabsContent>

            {/* 仅聊天助手显示提示词配置 */}
            {selectedType === "chat" && (
              <TabsContent value="prompt" className="space-y-6">
                {/* 系统提示词 */}
                <div>
                  <h2 className="text-lg font-medium mb-2">系统提示词</h2>
                  <p className="text-sm text-muted-foreground mb-2">定义聊天助手的角色、能力和行为限制</p>
                  <Textarea
                    placeholder="输入系统提示词"
                    value={formData.systemPrompt}
                    onChange={(e) => updateFormField("systemPrompt", e.target.value)}
                    rows={8}
                  />
                </div>

                {/* 欢迎消息 */}
                <div>
                  <h2 className="text-lg font-medium mb-2">欢迎消息</h2>
                  <p className="text-sm text-muted-foreground mb-2">用户首次与聊天助手交互时显示的消息</p>
                  <Textarea
                    placeholder="输入欢迎消息"
                    value={formData.welcomeMessage}
                    onChange={(e) => updateFormField("welcomeMessage", e.target.value)}
                    rows={4}
                  />
                </div>
              </TabsContent>
            )}

            <TabsContent value="model" className="space-y-6">
              {/* 模型选择 */}
              <div>
                <h2 className="text-lg font-medium mb-2">选择模型</h2>
                <p className="text-sm text-muted-foreground mb-2">
                  选择{selectedType === "chat" ? "聊天助理" : "功能性助理"}使用的大语言模型
                </p>
                <Select value={formData.modelConfig.model} onValueChange={(value) => updateModelConfig("model", value)}>
                  <SelectTrigger className="w-full">
                    <SelectValue placeholder="选择模型" />
                  </SelectTrigger>
                  <SelectContent>
                    {modelOptions.map((model) => (
                      <SelectItem key={model.value} value={model.value}>
                        {model.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 温度设置 */}
              <div>
                <h2 className="text-lg font-medium mb-2">温度</h2>
                <p className="text-sm text-muted-foreground mb-2">
                  控制输出的随机性：较低的值使输出更确定，较高的值使输出更多样化
                </p>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm">精确</span>
                    <span className="text-sm font-medium">{formData.modelConfig.temperature.toFixed(1)}</span>
                    <span className="text-sm">创意</span>
                  </div>
                  <Slider
                    value={[formData.modelConfig.temperature]}
                    min={0}
                    max={1}
                    step={0.1}
                    onValueChange={(value) => updateModelConfig("temperature", value[0])}
                  />
                </div>
              </div>

              {/* 最大Token */}
              <div>
                <h2 className="text-lg font-medium mb-2">最大输出Token</h2>
                <p className="text-sm text-muted-foreground mb-2">限制模型单次回复的最大长度</p>
                <div className="space-y-2">
                  <div className="flex justify-between">
                    <span className="text-sm">简短</span>
                    <span className="text-sm font-medium">{formData.modelConfig.maxTokens}</span>
                    <span className="text-sm">详细</span>
                  </div>
                  <Slider
                    value={[formData.modelConfig.maxTokens]}
                    min={500}
                    max={4000}
                    step={100}
                    onValueChange={(value) => updateModelConfig("maxTokens", value[0])}
                  />
                </div>
              </div>
            </TabsContent>

            <TabsContent value="tools" className="space-y-6">
              {/* 工具选择 */}
              <div>
                <h2 className="text-lg font-medium mb-2">可用工具</h2>
                <p className="text-sm text-muted-foreground mb-2">
                  选择{selectedType === "chat" ? "聊天助理" : "功能性助理"}可以使用的工具
                </p>
                <div className="grid grid-cols-2 gap-4 mt-4">
                  {toolOptions.map((tool) => (
                    <div
                      key={tool.id}
                      className={`border rounded-lg p-4 cursor-pointer transition-all ${
                        formData.tools.includes(tool.id) ? "border-blue-500 bg-blue-50" : "hover:border-gray-300"
                      }`}
                      onClick={() => toggleTool(tool.id)}
                    >
                      <h3 className="font-medium mb-1">{tool.name}</h3>
                      <p className="text-sm text-muted-foreground">{tool.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            </TabsContent>
          </Tabs>
        </div>

        {/* 右侧表单 */}
        <div className="w-2/5 p-8 overflow-auto">
          {/* 表单部分 */}
        </div>
      </div>
    </div>
  )
}