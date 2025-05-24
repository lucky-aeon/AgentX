"use client"

// 注意: 在未来的 Next.js 版本中，params 将会是一个 Promise 对象
// 届时需要使用 React.use(params) 解包后再访问其属性

import React from "react"

import { useEffect, useState, useRef } from "react"
import { useRouter, useParams } from "next/navigation"
import Link from "next/link"
import {
  MessageCircle,
  Upload,
  Trash,
  FileText,
  Workflow,
  Zap,
  Search,
  ArrowLeft,
  Power,
  PowerOff,
  History,
  RefreshCw,
} from "lucide-react"

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
import { Skeleton } from "@/components/ui/skeleton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"

import {
  getAgentDetail,
  updateAgent,
  publishAgentVersion,
  deleteAgent,
  toggleAgentStatus,
  getAgentVersions,
  updateAgentWithToast,
  publishAgentVersionWithToast,
  deleteAgentWithToast,
  getAgentLatestVersion,
} from "@/lib/agent-service"
import { getInstalledTools } from "@/lib/tool-service"
import { PublishStatus } from "@/types/agent"
import type { AgentVersion } from "@/types/agent"
import type { Tool } from "@/types/tool"
import type { AgentTool } from "@/types/agent"
import AgentBasicInfoForm from "./components/AgentBasicInfoForm"
import AgentPromptForm from "./components/AgentPromptForm"
import AgentToolsForm, { knowledgeBaseOptions } from "./components/AgentToolsForm"
import AgentEditHeader from "./components/AgentEditHeader"
import ToolDetailSidebar from "./components/ToolDetailSidebar"

// 应用类型定义
type AgentType = "chat" | "agent"

// 临时的接口，只包含工具的基本信息
// interface SelectedToolInfo {
// id: string;
// name: string;
// description: string;
// }

interface AgentFormData {
  name: string
  avatar: string | null
  description: string
  systemPrompt: string
  welcomeMessage: string
  tools: AgentTool[]
  knowledgeBaseIds: string[]
  toolPresetParams: Record<string, Record<string, string>> // 工具预设参数
  enabled: boolean
  agentType: number
}

export default function EditAgentPage() {
  const router = useRouter()
  const params = useParams()
  const agentId = params.id as string
  
  const [selectedType, setSelectedType] = useState<AgentType>("chat")
  const [activeTab, setActiveTab] = useState("basic")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState(false)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [isRollingBack, setIsRollingBack] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [showPublishDialog, setShowPublishDialog] = useState(false)
  const [showVersionsDialog, setShowVersionsDialog] = useState(false)
  const [versionNumber, setVersionNumber] = useState("")
  const [changeLog, setChangeLog] = useState("")
  const [versions, setVersions] = useState<AgentVersion[]>([])
  const [selectedVersion, setSelectedVersion] = useState<AgentVersion | null>(null)
  const [latestVersion, setLatestVersion] = useState<AgentVersion | null>(null)
  const [isLoadingLatestVersion, setIsLoadingLatestVersion] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [selectedToolForSidebar, setSelectedToolForSidebar] = useState<Tool | null>(null)
  const [isToolSidebarOpen, setIsToolSidebarOpen] = useState(false)
  const [installedTools, setInstalledTools] = useState<Tool[]>([])
  const [isLoadingTools, setIsLoadingTools] = useState(false)

  // 表单数据
  const [formData, setFormData] = useState<AgentFormData>({
    name: "",
    avatar: null,
    description: "",
    systemPrompt: "",
    welcomeMessage: "",
    tools: [],
    knowledgeBaseIds: [],
    toolPresetParams: {}, // 初始化为空对象
    enabled: true,
    agentType: 1,
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

  // 加载助理详情
  useEffect(() => {
    async function fetchAgentDetail() {
      try {
        setIsLoading(true)
        const response = await getAgentDetail(agentId)

        if (response.code === 200 && response.data) {
          const agent = response.data

          // 如果返回的是 toolIds，需要获取完整的工具信息
          let agentTools: AgentTool[] = []
          
          if (agent.tools && agent.tools.length > 0) {
            // 如果直接返回了 tools 对象数组，直接使用
            agentTools = agent.tools.map(t => ({ 
              id: t.id, 
              name: t.name, 
              description: t.description || undefined,
              presetParameters: t.presetParameters || {},
            }))
          } else if (agent.toolIds && agent.toolIds.length > 0) {
            // 如果只返回了 toolIds，需要获取完整的工具信息
            try {
              const toolsResponse = await getInstalledTools({ pageSize: 100 })
              if (toolsResponse.code === 200 && toolsResponse.data && Array.isArray(toolsResponse.data.records)) {
                const installedTools = toolsResponse.data.records
                
                // 根据 toolIds 过滤出已选择的工具
                agentTools = agent.toolIds.map(toolId => {
                  // 查找匹配的工具
                  const matchedTool = installedTools.find((t: Tool) => t.id === toolId || t.toolId === toolId)
                  
                  if (matchedTool) {
                    return {
                      id: toolId,
                      name: matchedTool.name,
                      description: matchedTool.description || undefined,
                      presetParameters: {},
                    }
                  } else {
                    // 如果找不到匹配的工具，创建一个基本的工具对象
                    return {
                      id: toolId,
                      name: `工具 (ID: ${toolId.substring(0, 8)}...)`,
                      description: undefined,
                      presetParameters: {},
                    }
                  }
                })
              }
            } catch (error) {
              console.error("获取已安装工具错误:", error)
            }
          }

          // 设置表单数据
          setFormData({
            name: agent.name,
            avatar: agent.avatar,
            description: agent.description,
            systemPrompt: agent.systemPrompt,
            welcomeMessage: agent.welcomeMessage,
            tools: agentTools,
            knowledgeBaseIds: agent.knowledgeBaseIds || [],
            toolPresetParams: agent.toolPresetParams || {},
            enabled: agent.enabled,
            agentType: agent.agentType,
          })

          // 设置助理类型
          setSelectedType(agent.agentType === 1 ? "chat" : "agent")
        } else {
          toast({
            title: "获取助理详情失败",
            description: response.message,
            variant: "destructive",
          })
          router.push("/studio")
        }
      } catch (error) {
        console.error("获取助理详情错误:", error)
        toast({
          title: "获取助理详情失败",
          description: "请稍后再试",
          variant: "destructive",
        })
        router.push("/studio")
      } finally {
        setIsLoading(false)
      }
    }

    fetchAgentDetail()
  }, [agentId, router])

  // 获取助理最新版本
  const fetchLatestVersion = async () => {
    setIsLoadingLatestVersion(true)
    try {
      const response = await getAgentLatestVersion(agentId)
      
      if (response.code === 200) {
        setLatestVersion(response.data)
        
        // 如果有最新版本，预填写下一个版本号
        if (response.data && response.data.versionNumber) {
          const versionParts = response.data.versionNumber.split('.')
          if (versionParts.length >= 3) {
            // 增加补丁版本号
            const major = parseInt(versionParts[0])
            const minor = parseInt(versionParts[1])
            const patch = parseInt(versionParts[2]) + 1
            setVersionNumber(`${major}.${minor}.${patch}`)
          } else {
            // 无法解析版本号，设置为原版本号 + .1
            setVersionNumber(`${response.data.versionNumber}.1`)
          }
        } else {
          // 没有版本，设置初始版本号
          setVersionNumber("1.0.0")
        }
      } else {
        // 没有版本或获取失败，设置初始版本号
        setVersionNumber("1.0.0")
      }
    } catch (error) {
      console.error("获取最新版本错误:", error)
      // 出错，设置初始版本号
      setVersionNumber("1.0.0")
    } finally {
      setIsLoadingLatestVersion(false)
    }
  }

  // 更新表单字段
  const updateFormField = (field: string, value: any) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  // 切换工具
  const toggleTool = (toolToToggle: Tool) => {
    // 使用 toolId（如果存在）或 id 作为工具标识符
    const toolIdentifier = toolToToggle.toolId || toolToToggle.id;
    const isToolCurrentlyEnabled = formData.tools.some(t => t.id === toolIdentifier);
    
    setFormData((prev) => {
      let updatedTools: AgentTool[];
      if (isToolCurrentlyEnabled) {
        updatedTools = prev.tools.filter((t) => t.id !== toolIdentifier);
      } else {
        const newAgentTool: AgentTool = {
          id: toolIdentifier,
          name: toolToToggle.name,
          description: toolToToggle.description || undefined,
        };
        updatedTools = [...prev.tools, newAgentTool];
      }
      return { ...prev, tools: updatedTools };
    });
    
    toast({
      title: `工具已${!isToolCurrentlyEnabled ? "启用" : "禁用"}: ${toolToToggle.name}`,
    });
  };

  // 切换知识库
  const toggleKnowledgeBase = (kbId: string, kbName?: string) => {
    const isKnowledgeBaseAssociated = !formData.knowledgeBaseIds.includes(kbId)
    setFormData((prev) => {
      const knowledgeBaseIds = [...prev.knowledgeBaseIds]
      if (knowledgeBaseIds.includes(kbId)) {
        return { ...prev, knowledgeBaseIds: knowledgeBaseIds.filter((id) => id !== kbId) }
      } else {
        return { ...prev, knowledgeBaseIds: [...knowledgeBaseIds, kbId] }
      }
    })
    // 使用传入的 kbName，如果未提供则回退到从 knowledgeBaseOptions 查找
    const nameToDisplay = kbName || knowledgeBaseOptions.find((kb) => kb.id === kbId)?.name
    toast({
      title: `知识库已${isKnowledgeBaseAssociated ? "关联" : "取消关联"}: ${nameToDisplay || kbId}`,
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

  // 处理更新助理
  const handleUpdateAgent = async () => {
    if (!formData.name.trim()) {
      toast({
        title: "请输入名称",
        variant: "destructive",
      })
      return
    }

    setIsSubmitting(true)

    try {
      // 将工具对象数组转换为工具ID字符串数组
      const toolIds = formData.tools.map(tool => tool.id);
      
      // 准备API请求参数
      const agentData = {
        id: agentId,
        name: formData.name,
        avatar: formData.avatar,
        description: formData.description || "",
        systemPrompt: selectedType === "chat" ? formData.systemPrompt : "",
        welcomeMessage: selectedType === "chat" ? formData.welcomeMessage : "",
        toolIds: toolIds, // 使用工具ID数组
        knowledgeBaseIds: selectedType === "chat" ? formData.knowledgeBaseIds : [],
        toolPresetParams: formData.toolPresetParams,
        enabled: formData.enabled,
        agentType: formData.agentType,
      }

      // 调用API更新助理
      const response = await updateAgentWithToast(agentId, agentData)

      if (response.code === 200) {
        // toast已通过withToast处理，此处不需要额外的toast
      } else {
        // 错误也已由withToast处理
      }
    } catch (error) {
      console.error("更新失败:", error)
      // 错误已由withToast处理
    } finally {
      setIsSubmitting(false)
    }
  }

  // 处理删除助理
  const handleDeleteAgent = async () => {
    setIsDeleting(true)

    try {
      const response = await deleteAgentWithToast(agentId)

      if (response.code === 200) {
        // toast已通过withToast处理
        router.push("/studio")
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
      console.error("删除失败:", error)
      // 错误已由withToast处理
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }

  // 处理切换助理状态
  const handleToggleStatus = async () => {
    // 不发送网络请求，只更新本地状态
    const newEnabledStatus = !formData.enabled;
    
    updateFormField("enabled", newEnabledStatus);
    
    toast({
      title: newEnabledStatus ? "已启用" : "已禁用",
      description: `助理 "${formData.name}" ${newEnabledStatus ? "已启用" : "已禁用"}`,
    });
  }

  // 处理发布助理版本
  const handlePublishVersion = async () => {
    if (!versionNumber.trim()) {
      toast({
        title: "请输入版本号",
        variant: "destructive",
      })
      return
    }

    setIsPublishing(true)

    try {
      // 将工具对象数组转换为工具ID字符串数组
      const toolIds = formData.tools.map(tool => tool.id);
      
      const response = await publishAgentVersionWithToast(agentId, {
        versionNumber,
        changeLog: changeLog || `发布 ${versionNumber} 版本`,
        systemPrompt: formData.systemPrompt,
        welcomeMessage: formData.welcomeMessage,
        toolIds: toolIds, // 使用工具ID数组
        knowledgeBaseIds: formData.knowledgeBaseIds,
        toolPresetParams: formData.toolPresetParams,
      })

      if (response.code === 200) {
        // toast已通过withToast处理
        setShowPublishDialog(false)
        setVersionNumber("")
        setChangeLog("")
        // 更新最新版本信息
        fetchLatestVersion()
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
      console.error("发布失败:", error)
      // 错误已由withToast处理
    } finally {
      setIsPublishing(false)
    }
  }

  // 打开发布对话框
  const openPublishDialog = async () => {
    // 先加载最新版本
    await fetchLatestVersion()
    setShowPublishDialog(true)
  }

  // 加载助理版本列表
  const loadVersions = async () => {
    setIsLoadingVersions(true)
    setVersions([])

    try {
      const response = await getAgentVersions(agentId)

      if (response.code === 200) {
        setVersions(response.data)
      } else {
        toast({
          title: "获取版本列表失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("获取版本列表失败:", error)
      toast({
        title: "获取版本列表失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    } finally {
      setIsLoadingVersions(false)
    }
  }

  // 查看版本详情
  const viewVersionDetail = async (version: AgentVersion) => {
    setSelectedVersion(version)
  }

  // 回滚到特定版本
  const rollbackToVersion = async (version: AgentVersion) => {
    if (!version) return

    setIsRollingBack(true)

    try {
      setFormData({
        name: version.name,
        avatar: version.avatar,
        description: version.description,
        systemPrompt: version.systemPrompt,
        welcomeMessage: version.welcomeMessage,
        tools: version.tools?.map(t => ({
          id: t.id,
          name: t.name,
          description: t.description || undefined,
          presetParameters: t.presetParameters || {},
        })) || [],
        knowledgeBaseIds: version.knowledgeBaseIds || [],
        toolPresetParams: version.toolPresetParams || {},
        enabled: formData.enabled,
        agentType: version.agentType,
      })
      setSelectedType(version.agentType === 1 ? "chat" : "agent")

      toast({
        title: "回滚成功",
        description: `已回滚到版本 ${version.versionNumber}`,
      })

      // 关闭对话框
      setSelectedVersion(null)
      setShowVersionsDialog(false)
    } catch (error) {
      console.error("回滚失败:", error)
      toast({
        title: "回滚失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    } finally {
      setIsRollingBack(false)
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

  // 获取发布状态文本
  const getPublishStatusText = (status: number) => {
    switch (status) {
      case PublishStatus.REVIEWING:
        return "审核中"
      case PublishStatus.PUBLISHED:
        return "已发布"
      case PublishStatus.REJECTED:
        return "已拒绝"
      case PublishStatus.REMOVED:
        return "已下架"
      default:
        return "未知状态"
    }
  }

  // 处理工具点击事件
  const handleToolClick = (tool: Tool) => {
    // 确保工具信息中包含toolId和version信息
    // 这里不需要额外处理，因为从getInstalledTools()得到的tool对象应该已经包含了这些信息
    // 如果需要可以添加日志进行调试
    console.log("Tool clicked:", tool);
    setSelectedToolForSidebar(tool);
    setIsToolSidebarOpen(true);
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
        
        // 将参数格式化为 "{'param1':'value1','param2':'value2'}" 格式
        const paramsObj: Record<string, string> = {};
        Object.entries(params).forEach(([paramName, paramValue]) => {
          // 未设置的参数值设为空字符串
          paramsObj[paramName] = paramValue || '';
        });
        
        // 转换为需要的字符串格式
        // 注意：使用单引号包裹键和值，外层使用双引号
        const formattedParams = JSON.stringify(paramsObj)
          .replace(/"/g, "'")  // 将双引号替换为单引号
          .replace(/'/g, "'"); // 确保所有引号都是单引号
        
        // 设置参数
        newToolPresetParams[mcpServerName][functionName] = formattedParams;
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

  // 如果正在加载，显示加载状态
  if (isLoading) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto p-4">
        <div className="bg-white rounded-lg shadow-xl w-full max-w-7xl flex max-h-[95vh] overflow-hidden">
          <div className="w-3/5 p-8 overflow-auto">
            <div className="flex items-center justify-between mb-6">
              <Skeleton className="h-8 w-64" />
              <Skeleton className="h-10 w-10 rounded-full" />
            </div>
            <div className="space-y-6">
              <Skeleton className="h-10 w-full" />
              <div className="space-y-4">
                <Skeleton className="h-6 w-32" />
                <div className="grid grid-cols-2 gap-4">
                  <Skeleton className="h-32 w-full" />
                  <Skeleton className="h-32 w-full" />
                </div>
              </div>
              <div className="space-y-4">
                <Skeleton className="h-6 w-32" />
                <div className="flex gap-4 items-center">
                  <Skeleton className="h-20 w-full" />
                  <Skeleton className="h-20 w-32" />
                </div>
              </div>
            </div>
          </div>
          <div className="w-2/5 bg-gray-50 p-8 overflow-auto border-l">
            <Skeleton className="h-8 w-32 mb-2" />
            <Skeleton className="h-4 w-64 mb-6" />
            <Skeleton className="h-[500px] w-full mb-6" />
            <Skeleton className="h-6 w-32 mb-3" />
            <Skeleton className="h-40 w-full" />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto p-4">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-7xl flex max-h-[95vh] overflow-hidden">
        {/* 左侧表单 */}
        <div className="w-3/5 p-8 overflow-auto">
          <AgentEditHeader
            selectedType={selectedType}
            formDataEnabled={formData.enabled}
            onShowVersionsDialog={() => {
              setShowVersionsDialog(true);
              loadVersions();
            }}
            onOpenPublishDialog={openPublishDialog}
            onToggleStatus={handleToggleStatus}
            onShowDeleteDialog={() => setShowDeleteDialog(true)}
          />

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

            {/* 仅聊天助理显示提示词配置 */}
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
                updateToolPresetParameters={updateToolPresetParameters}
              />
            </TabsContent>
          </Tabs>

          {/* 底部按钮 */}
          <div className="flex justify-end pt-6 border-t mt-6">
            <div className="space-x-2">
              <Button variant="outline" asChild>
                <Link href="/studio">取消</Link>
              </Button>
              <Button onClick={handleUpdateAgent} disabled={isSubmitting}>
                {isSubmitting ? "保存中..." : "保存更改"}
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

          {/* 聊天助理预览 */}
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
                  <span className="font-medium">{formData.name || "聊天助理"}</span>
                </div>
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
                        {formData.tools.some(t => t.id === "web-search") && <li>搜索互联网获取最新信息</li>}
                        {formData.tools.some(t => t.id === "file-reader") && <li>分析和解读上传的文件</li>}
                        {formData.tools.some(t => t.id === "code-interpreter") && <li>编写和执行代码</li>}
                        {formData.tools.some(t => t.id === "image-generation") && <li>生成和编辑图像</li>}
                        {formData.tools.some(t => t.id === "calculator") && <li>执行数学计算</li>}
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

          {/* 功能性助理预览 */}
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
                  <span className="font-medium">{formData.name || "功能性助理"}</span>
                </div>
              </div>

              <div className="h-[500px] flex flex-col">
                <div className="flex-1 p-4 overflow-auto space-y-4">
                  {/* 助理任务界面 */}
                  <div className="bg-gray-50 rounded-lg p-4 border">
                    <h3 className="font-medium mb-2">任务描述</h3>
                    <p className="text-sm text-muted-foreground mb-4">请助理帮我分析以下数据并生成报告。</p>
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
                        {formData.tools.some(t => t.id === "file-reader") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <FileText className="h-4 w-4 text-blue-500" />
                            <span>已读取文件：数据分析.xlsx</span>
                          </div>
                        )}
                        {formData.tools.some(t => t.id === "code-interpreter") && (
                          <div className="flex items-center gap-2 text-sm p-2 bg-gray-50 rounded">
                            <Zap className="h-4 w-4 text-purple-500" />
                            <span>执行代码：数据处理脚本</span>
                          </div>
                        )}
                        {formData.tools.some(t => t.id === "web-search") && (
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
                  <Badge variant={formData.enabled ? "default" : "outline"} className="text-xs">
                    {formData.enabled ? "已启用" : "已禁用"}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* 删除确认对话框 */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>您确定要删除这个助理吗？此操作无法撤销。</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteAgent} disabled={isDeleting}>
              {isDeleting ? "删除中..." : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 发布版本对话框 */}
      <Dialog open={showPublishDialog} onOpenChange={setShowPublishDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>发布新版本</DialogTitle>
            <DialogDescription>发布新版本将创建当前配置的快照，用户可以使用此版本。</DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {isLoadingLatestVersion ? (
              <div className="flex items-center justify-center py-2">
                <RefreshCw className="h-4 w-4 animate-spin text-blue-500 mr-2" />
                <span className="text-sm">加载版本信息...</span>
              </div>
            ) : latestVersion ? (
              <div className="flex items-center p-2 bg-blue-50 rounded-md border border-blue-100 mb-2">
                <span className="text-sm text-blue-600">当前最新版本：{latestVersion.versionNumber}</span>
              </div>
            ) : (
              <div className="flex items-center p-2 bg-gray-50 rounded-md border border-gray-200 mb-2">
                <span className="text-sm text-gray-600">当前还没有发布过版本</span>
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="version-number">版本号</Label>
              <Input
                id="version-number"
                placeholder="例如: 1.0.0"
                value={versionNumber}
                onChange={(e) => setVersionNumber(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="change-log">更新日志</Label>
              <Textarea
                id="change-log"
                placeholder="描述此版本的更新内容"
                rows={4}
                value={changeLog}
                onChange={(e) => setChangeLog(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowPublishDialog(false)}>
              取消
            </Button>
            <Button onClick={handlePublishVersion} disabled={isPublishing}>
              {isPublishing ? "发布中..." : "发布版本"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 版本历史对话框 */}
      <Dialog open={showVersionsDialog} onOpenChange={setShowVersionsDialog}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>版本历史</DialogTitle>
            <DialogDescription>查看和管理助理的历史版本</DialogDescription>
          </DialogHeader>
          <div className="flex-1 overflow-auto py-4">
            {isLoadingVersions ? (
              <div className="flex items-center justify-center py-8">
                <RefreshCw className="h-6 w-6 animate-spin text-blue-500" />
                <span className="ml-2">加载版本历史...</span>
              </div>
            ) : versions.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">暂无版本历史</div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>版本号</TableHead>
                    <TableHead>发布时间</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>更新日志</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {versions.map((version) => (
                    <TableRow key={version.id}>
                      <TableCell className="font-medium">{version.versionNumber}</TableCell>
                      <TableCell>{new Date(version.publishedAt).toLocaleString()}</TableCell>
                      <TableCell>
                        <Badge variant={version.publishStatus === PublishStatus.PUBLISHED ? "default" : "outline"}>
                          {getPublishStatusText(version.publishStatus)}
                        </Badge>
                      </TableCell>
                      <TableCell className="max-w-[200px] truncate">{version.changeLog}</TableCell>
                      <TableCell className="text-right">
                        <Button variant="outline" size="sm" className="mr-2" onClick={() => viewVersionDetail(version)}>
                          查看
                        </Button>
                        <Button size="sm" onClick={() => rollbackToVersion(version)} disabled={isRollingBack}>
                          {isRollingBack ? "回滚中..." : "回滚"}
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* 版本详情对话框 */}
      {selectedVersion && (
        <Dialog open={!!selectedVersion} onOpenChange={(open) => !open && setSelectedVersion(null)}>
          <DialogContent className="max-w-3xl max-h-[80vh] overflow-auto">
            <DialogHeader>
              <DialogTitle>版本详情: {selectedVersion.versionNumber}</DialogTitle>
              <DialogDescription>发布于 {new Date(selectedVersion.publishedAt).toLocaleString()}</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="flex items-center gap-4">
                <Avatar className="h-12 w-12">
                  <AvatarImage src={selectedVersion.avatar || ""} alt="Avatar" />
                  <AvatarFallback className="bg-blue-100 text-blue-600">
                    {selectedVersion.name ? selectedVersion.name.charAt(0).toUpperCase() : "🤖"}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <h3 className="font-medium">{selectedVersion.name}</h3>
                  <p className="text-sm text-muted-foreground">{selectedVersion.description}</p>
                </div>
              </div>

              <div className="space-y-2">
                <h3 className="font-medium">更新日志</h3>
                <div className="p-3 bg-gray-50 rounded-md">{selectedVersion.changeLog}</div>
              </div>

              <div className="space-y-2">
                <h3 className="font-medium">配置信息</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">工具数量</span>
                    <span className="text-sm">{selectedVersion.tools.length}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">知识库数量</span>
                    <span className="text-sm">{selectedVersion.knowledgeBaseIds.length}</span>
                  </div>
                </div>
              </div>

              {selectedVersion.agentType === 1 && (
                <>
                  <div className="space-y-2">
                    <h3 className="font-medium">系统提示词</h3>
                    <div className="p-3 bg-gray-50 rounded-md text-sm">
                      {selectedVersion.systemPrompt || "无系统提示词"}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h3 className="font-medium">欢迎消息</h3>
                    <div className="p-3 bg-gray-50 rounded-md text-sm">
                      {selectedVersion.welcomeMessage || "无欢迎消息"}
                    </div>
                  </div>
                </>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setSelectedVersion(null)}>
                关闭
              </Button>
              <Button onClick={() => rollbackToVersion(selectedVersion)} disabled={isRollingBack}>
                {isRollingBack ? "回滚中..." : "回滚到此版本"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      {/* 工具详情侧边栏 */}
      <ToolDetailSidebar
        tool={selectedToolForSidebar}
        isOpen={isToolSidebarOpen}
        onClose={() => setIsToolSidebarOpen(false)}
        presetParameters={selectedToolForSidebar && selectedToolForSidebar.mcpServerName && formData.toolPresetParams[selectedToolForSidebar.mcpServerName] ? 
          Object.entries(formData.toolPresetParams[selectedToolForSidebar.mcpServerName]).reduce((acc, [funcName, paramStr]) => {
            try {
              // 将参数字符串如 "{'email':'xxx@qq.com','password':'123'}" 转换为对象
              const cleanParamStr = paramStr
                .replace(/^['"]/, '') // 移除开头的引号
                .replace(/['"]$/, ''); // 移除结尾的引号
              
              // 尝试解析JSON字符串，注意替换单引号为双引号
              const paramObj = JSON.parse(cleanParamStr.replace(/'/g, '"'));
              acc[funcName] = paramObj;
            } catch (e) {
              console.error(`解析工具参数失败: ${funcName}`, e, paramStr);
              // 尝试使用正则表达式解析
              try {
                const params: Record<string, string> = {};
                // 匹配 'key':'value' 模式
                const regex = /'([^']+)'\s*:\s*'([^']*)'/g;
                let match;
                
                while ((match = regex.exec(paramStr)) !== null) {
                  if (match.length >= 3) {
                    params[match[1]] = match[2];
                  }
                }
                
                acc[funcName] = params;
              } catch (regexError) {
                console.error(`正则解析失败: ${funcName}`, regexError);
                acc[funcName] = {};
              }
            }
            return acc;
          }, {} as Record<string, Record<string, string>>) : 
          {}}
        onSavePresetParameters={updateToolPresetParameters}
      />
    </div>
  )
}

