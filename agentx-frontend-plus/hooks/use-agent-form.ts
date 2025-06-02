import { useState, useRef, useEffect } from "react"
import { toast } from "@/hooks/use-toast"
import { getInstalledTools } from "@/lib/tool-service"
import type { Tool } from "@/types/tool"
import type { AgentTool } from "@/types/agent"

export interface AgentFormData {
  name: string
  avatar: string | null
  description: string
  systemPrompt: string
  welcomeMessage: string
  tools: AgentTool[]
  knowledgeBaseIds: string[]
  toolPresetParams: {
    [serverName: string]: {
      [functionName: string]: {
        [paramName: string]: string
      }
    }
  }
  enabled: boolean
  agentType: number
  multiModal: boolean
}

interface UseAgentFormProps {
  initialData?: Partial<AgentFormData>
  isEditMode?: boolean
}

export function useAgentForm({ initialData, isEditMode = false }: UseAgentFormProps = {}) {
  const [activeTab, setActiveTab] = useState("basic")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isLoadingTools, setIsLoadingTools] = useState(false)
  
  // 编辑模式特有状态
  const [isDeleting, setIsDeleting] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState(false)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [isRollingBack, setIsRollingBack] = useState(false)
  const [isLoadingLatestVersion, setIsLoadingLatestVersion] = useState(false)
  
  // 工具相关状态
  const [selectedToolForSidebar, setSelectedToolForSidebar] = useState<Tool | null>(null)
  const [isToolSidebarOpen, setIsToolSidebarOpen] = useState(false)
  const [installedTools, setInstalledTools] = useState<Tool[]>([])
  
  const fileInputRef = useRef<HTMLInputElement>(null)
  
  // 表单数据 - 去掉类型选择，统一使用agent类型
  const [formData, setFormData] = useState<AgentFormData>({
    name: "",
    avatar: null,
    description: "",
    systemPrompt: "你是一个有用的AI助手。",
    welcomeMessage: "你好！我是你的AI助手，有什么可以帮助你的吗？",
    tools: [],
    knowledgeBaseIds: [],
    toolPresetParams: {},
    enabled: true,
    agentType: 1, // 统一使用agent类型
    multiModal: false, // 默认关闭多模态
    ...initialData,
  })

  // 当initialData变化时，更新formData
  useEffect(() => {
    if (initialData) {
      setFormData(prev => ({
        ...prev,
        ...initialData,
      }))
    }
  }, [initialData])

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

  // 切换工具
  const toggleTool = (toolToToggle: Tool) => {
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
    
    // 动态导入 knowledgeBaseOptions 避免循环依赖
    const nameToDisplay = kbName || `知识库 ${kbId}`;
    toast({
      title: `知识库已${isKnowledgeBaseAssociated ? "关联" : "取消关联"}: ${nameToDisplay}`,
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

  // 处理工具点击事件
  const handleToolClick = (tool: Tool) => {
    if (selectedToolForSidebar && selectedToolForSidebar.id === tool.id) {
      return;
    }
    
    console.log("Tool clicked:", tool);
    setIsToolSidebarOpen(false);
    
    setTimeout(() => {
      setSelectedToolForSidebar(tool);
      setIsToolSidebarOpen(true);
    }, 100);
  };

  // 更新工具预设参数
  const updateToolPresetParameters = (toolId: string, presetParams: Record<string, Record<string, string>>) => {
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
      const newToolPresetParams = { ...prev.toolPresetParams };
      
      if (!newToolPresetParams[mcpServerName]) {
        newToolPresetParams[mcpServerName] = {};
      }
      
      Object.keys(presetParams).forEach(functionName => {
        const params = presetParams[functionName];
        
        if (!newToolPresetParams[mcpServerName][functionName]) {
          newToolPresetParams[mcpServerName][functionName] = {};
        }
        
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

  // 获取可用的标签页 - 统一显示所有标签页
  const getAvailableTabs = () => {
    return [
      { id: "basic", label: "基本信息" },
      { id: "prompt", label: "提示词配置" },
      { id: "tools", label: "工具与知识库" },
    ]
  }

  return {
    // 基础状态
    activeTab,
    setActiveTab,
    isSubmitting,
    setIsSubmitting,
    isLoading,
    setIsLoading,
    isLoadingTools,
    setIsLoadingTools,
    
    // 编辑模式状态
    isDeleting,
    setIsDeleting,
    isPublishing,
    setIsPublishing,
    isTogglingStatus,
    setIsTogglingStatus,
    isLoadingVersions,
    setIsLoadingVersions,
    isRollingBack,
    setIsRollingBack,
    isLoadingLatestVersion,
    setIsLoadingLatestVersion,
    
    // 工具相关状态
    selectedToolForSidebar,
    setSelectedToolForSidebar,
    isToolSidebarOpen,
    setIsToolSidebarOpen,
    installedTools,
    
    // refs
    fileInputRef,
    
    // 表单数据
    formData,
    setFormData,
    updateFormField,
    
    // 表单操作函数
    toggleTool,
    toggleKnowledgeBase,
    handleAvatarUpload,
    removeAvatar,
    triggerFileInput,
    handleToolClick,
    updateToolPresetParameters,
    
    // 工具函数
    getAvailableTabs,
  }
} 