import { useState, useRef } from "react"
import type { AgentTool } from "@/types/agent"



interface AgentFormData {
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

}

export function useAgentEdit() {
  const [selectedType, setSelectedType] = useState("chat")
  const [activeTab, setActiveTab] = useState("basic")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [isTogglingStatus, setIsTogglingStatus] = useState(false)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [isRollingBack, setIsRollingBack] = useState(false)
  const [isLoadingLatestVersion, setIsLoadingLatestVersion] = useState(false)
  const [isLoadingTools, setIsLoadingTools] = useState(false)
  
  const fileInputRef = useRef<HTMLInputElement>(null)
  
  // 表单数据
  const [formData, setFormData] = useState<AgentFormData>({
    name: "",
    avatar: null,
    description: "",
    systemPrompt: "",
    welcomeMessage: "",
    tools: [],
    knowledgeBaseIds: [],
    toolPresetParams: {},
    enabled: true,

  })

  // 更新表单字段
  const updateFormField = (field: string, value: any) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }))
  }

  // 根据选择的类型更新可用的标签页
  const getAvailableTabs = () => {
    return [
      { id: "basic", label: "基本信息" },
      { id: "prompt", label: "提示词配置" },
      { id: "tools", label: selectedType === "chat" ? "工具与知识库" : "工具配置" },
    ]
  }

  return {
    // 状态
    selectedType,
    setSelectedType,
    activeTab,
    setActiveTab,
    isSubmitting,
    setIsSubmitting,
    isLoading,
    setIsLoading,
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
    isLoadingTools,
    setIsLoadingTools,
    
    // refs
    fileInputRef,
    
    // 表单数据
    formData,
    setFormData,
    updateFormField,
    
    // 工具函数
    getAvailableTabs,
  }
}

export type { AgentFormData } 