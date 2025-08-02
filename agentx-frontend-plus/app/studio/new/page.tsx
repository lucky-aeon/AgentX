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
          modelName: "gpt-4o", // 使用默认模型
          temperature: 0.7,
          maxTokens: 2000
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
        toast({
          title: "创建成功",
          description: `已创建${selectedType === "chat" ? "聊天助理" : "功能性助理"}: ${formData.name}`,
        })

        // 创建成功后跳转
        router.push("/studio")
      } else {
        toast({
          title: "创建失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("创建失败:", error)
      toast({
        title: "创建失败",
        description: "请稍后再试",
        variant: "destructive",
      })
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
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-medium">{tool.name}</h3>
                        <Switch checked={formData.tools.includes(tool.id)} />
                      </div>
                      <p className="text-sm text-muted-foreground">{tool.description}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* 知识库选择 - 仅聊天助手显示 */}
              {selectedType === "chat" && (
                <div>
                  <h2 className="text-lg font-medium mb-2">关联知识库</h2>
                  <p className="text-sm text-muted-foreground mb-2">选择聊天助手可以访问的知识库</p>
                  <div className="grid grid-cols-2 gap-4 mt-4">
                    {knowledgeBaseOptions.map((kb) => (
                      <div
                        key={kb.id}
                        className={`border rounded-lg p-4 cursor-pointer transition-all ${
                          formData.knowledgeBaseIds.includes(kb.id)
                            ? "border-blue-500 bg-blue-50"
                            : "hover:border-gray-300"
                        }`}
                        onClick={() => toggleKnowledgeBase(kb.id)}
                      >
                        <div className="flex items-center justify-between mb-2">
                          <h3 className="font-medium">{kb.name}</h3>
                          <Switch checked={formData.knowledgeBaseIds.includes(kb.id)} />
                        </div>
                        <p className="text-sm text-muted-foreground">{kb.description}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </TabsContent>
          </Tabs>

          {/* 底部按钮 */}
          <div className="flex justify-end pt-6 border-t mt-6">
            <div className="space-x-2">
              <Button variant="outline" asChild>
                <Link href="/studio">取消</Link>
              </Button>
              <Button onClick={handleCreateAgent} disabled={isSubmitting}>
                {isSubmitting ? "创建中..." : "创建"}
              </Button>
            </div>
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
            <div className="border rounded-lg bg-white shadow-sm overflow-hidden">
              <div className="border-b p-3 flex items-center justify-between bg-gray-50">
                <div className="flex items-center gap-2">
                  <Avatar className="h-8 w-8">
                    <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                    <AvatarFallback className="bg-blue-100 text-blue-600">
                      {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                    </AvatarFallback>
                  </Avatar>
                  <span className="font-medium">{formData.name || "新建聊天助理"}</span>
                </div>
                <Badge variant="outline">默认模型</Badge>
              </div>

              <div className="h-[500px] flex flex-col">
                <div className="flex-1 p-4 overflow-auto space-y-4 bg-gray-50">
                  {/* 欢迎消息 */}
                  <div className="flex items-start gap-3">
                    <Avatar className="h-8 w-8 mt-1">
                      <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                      </AvatarFallback>
                    </Avatar>
                    <div className="bg-white rounded-lg p-3 shadow-sm max-w-[80%]">
                      {formData.welcomeMessage || "你好！我是你的AI助手，有什么可以帮助你的吗？"}
                    </div>
                  </div>

                  {/* 用户消息示例 */}
                  <div className="flex items-start gap-3 justify-end">
                    <div className="bg-blue-100 rounded-lg p-3 shadow-sm max-w-[80%] text-blue-900">你能做什么？</div>
                    <Avatar className="h-8 w-8 mt-1">
                      <AvatarImage src="/placeholder.svg?height=32&width=32" alt="User" />
                      <AvatarFallback className="bg-blue-500 text-white">U</AvatarFallback>
                    </Avatar>
                  </div>

                  {/* 助手回复示例 */}
                  <div className="flex items-start gap-3">
                    <Avatar className="h-8 w-8 mt-1">
                      <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                      <AvatarFallback className="bg-blue-100 text-blue-600">
                        {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                      </AvatarFallback>
                    </Avatar>
                    <div className="bg-white rounded-lg p-3 shadow-sm max-w-[80%]">
                      <p>我可以帮助你完成以下任务：</p>
                      <ul className="list-disc pl-5 mt-2 space-y-1">
                        <li>回答问题和提供信息</li>
                        <li>协助写作和内容创作</li>
                        {formData.tools.includes("web-search") && <li>搜索互联网获取最新信息</li>}
                        {formData.tools.includes("file-reader") && <li>分析和解读上传的文件</li>}
                        {formData.tools.includes("code-interpreter") && <li>编写和执行代码</li>}
                        {formData.tools.includes("image-generation") && <li>生成和编辑图像</li>}
                        {formData.tools.includes("calculator") && <li>执行数学计算</li>}
                        {formData.knowledgeBaseIds.length > 0 && <li>基于专业知识库提供准确信息</li>}
                      </ul>
                      <p className="mt-2">有什么具体问题我可以帮你解答吗？</p>
                    </div>
                  </div>
                </div>

                {/* 输入框 */}
                <div className="p-4 border-t">
                  <div className="flex gap-2">
                    <Input placeholder="输入消息..." className="flex-1" disabled />
                    <Button size="icon" disabled>
                      <MessageCircle className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            </div>
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
                        {formData.tools.includes("file-reader") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <FileText className="h-4 w-4 text-blue-500" />
                            <span>已读取文件：数据分析.xlsx</span>
                          </div>
                        )}
                        {formData.tools.includes("code-interpreter") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <Zap className="h-4 w-4 text-purple-500" />
                            <span>执行代码：数据处理脚本</span>
                          </div>
                        )}
                        {formData.tools.includes("web-search") && (
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
                  <Badge variant={formData.status === 0 ? "outline" : "default"} className="text-xs">
                    {formData.status === 0 ? "私有" : "待审核"}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}

