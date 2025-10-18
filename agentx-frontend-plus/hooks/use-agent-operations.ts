import { useRouter } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import {
  getAgentDetail,
  updateAgentWithToast,
  publishAgentVersionWithToast,
  deleteAgentWithToast,
} from "@/lib/agent-service"
import { getInstalledTools } from "@/lib/tool-service"
import type { Tool } from "@/types/tool"
import type { AgentTool, AgentVersion } from "@/types/agent"
import type { AgentFormData } from "./use-agent-edit"

export function useAgentOperations() {
  const router = useRouter()

  // 加载助理详情
  const loadAgentDetail = async (
    agentId: string, 
    setFormData: (data: AgentFormData) => void,
    setSelectedType: (type: "chat" | "agent") => void,
    setIsLoading: (loading: boolean) => void
  ) => {
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
          linkedAgentIds: agent.linkedAgentIds || [],
        })

        // 设置助理类型为默认值
        setSelectedType("chat")
      } else {
        toast({
          title: "获取助理详情失败",
          description: response.message,
          variant: "destructive",
        })
        router.push("/studio")
      }
    } catch (error) {
 
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

  // 处理头像上传
  const handleAvatarUpload = (
    event: React.ChangeEvent<HTMLInputElement>,
    updateFormField: (field: string, value: any) => void
  ) => {
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
  const removeAvatar = (
    updateFormField: (field: string, value: any) => void,
    fileInputRef: React.RefObject<HTMLInputElement | null>
  ) => {
    updateFormField("avatar", null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ""
    }
  }

  // 处理更新助理
  const handleUpdateAgent = async (
    agentId: string,
    formData: AgentFormData,
    setIsSubmitting: (submitting: boolean) => void
  ) => {
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
      const toolIds = formData.tools.map(tool => tool.id)
      
      // 准备API请求参数
      const agentData = {
        id: agentId,
        name: formData.name,
        avatar: formData.avatar,
        description: formData.description || "",
        systemPrompt: formData.systemPrompt,
        welcomeMessage: formData.welcomeMessage,
        toolIds: toolIds,
        knowledgeBaseIds: formData.knowledgeBaseIds,
        toolPresetParams: formData.toolPresetParams,
        enabled: formData.enabled,
        linkedAgentIds: formData.linkedAgentIds,
      }

      // 调用API更新助理
      await updateAgentWithToast(agentId, agentData)
    } catch (error) {
 
    } finally {
      setIsSubmitting(false)
    }
  }

  // 处理删除助理
  const handleDeleteAgent = async (
    agentId: string,
    setIsDeleting: (deleting: boolean) => void,
    setShowDeleteDialog: (show: boolean) => void
  ) => {
    setIsDeleting(true)

    try {
      const response = await deleteAgentWithToast(agentId)

      if (response.code === 200) {
        router.push("/studio")
      }
    } catch (error) {
 
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }

  // 处理切换助理状态
  const handleToggleStatus = (
    formData: AgentFormData,
    updateFormField: (field: string, value: any) => void
  ) => {
    // 不发送网络请求，只更新本地状态
    const newEnabledStatus = !formData.enabled
    
    updateFormField("enabled", newEnabledStatus)
    
    toast({
      title: newEnabledStatus ? "已启用" : "已禁用",
      description: `助理 "${formData.name}" ${newEnabledStatus ? "已启用" : "已禁用"}`,
    })
  }

  // 处理发布助理版本
  const handlePublishVersion = async (
    agentId: string,
    formData: AgentFormData,
    versionNumber: string,
    changeLog: string,
    setIsPublishing: (publishing: boolean) => void,
    setShowPublishDialog: (show: boolean) => void,
    resetPublishForm: () => void,
    fetchLatestVersion: () => void
  ) => {
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
      const toolIds = formData.tools.map(tool => tool.id)
      
      const response = await publishAgentVersionWithToast(agentId, {
        versionNumber,
        changeLog: changeLog || `发布 ${versionNumber} 版本`,
        systemPrompt: formData.systemPrompt,
        welcomeMessage: formData.welcomeMessage,
        toolIds: toolIds,
        knowledgeBaseIds: formData.knowledgeBaseIds,
        toolPresetParams: formData.toolPresetParams,
        linkedAgentIds: formData.linkedAgentIds,
      })

      if (response.code === 200) {
        setShowPublishDialog(false)
        resetPublishForm()
        // 更新最新版本信息
        fetchLatestVersion()
      }
    } catch (error) {
 
    } finally {
      setIsPublishing(false)
    }
  }

  // 回滚到特定版本
  const rollbackToVersion = async (
    version: AgentVersion,
    setFormData: (data: AgentFormData) => void,
    setSelectedType: (type: "chat" | "agent") => void,
    formData: AgentFormData,
    setIsRollingBack: (rolling: boolean) => void,
    setSelectedVersion: (version: AgentVersion | null) => void,
    setShowVersionsDialog: (show: boolean) => void
  ) => {
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
        linkedAgentIds: version.linkedAgentIds || formData.linkedAgentIds,
      })
      setSelectedType("chat")

      toast({
        title: "回滚成功",
        description: `已回滚到版本 ${version.versionNumber}`,
      })

      // 关闭对话框
      setSelectedVersion(null)
      setShowVersionsDialog(false)
    } catch (error) {
 
      toast({
        title: "回滚失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    } finally {
      setIsRollingBack(false)
    }
  }

  return {
    loadAgentDetail,
    handleAvatarUpload,
    removeAvatar,
    handleUpdateAgent,
    handleDeleteAgent,
    handleToggleStatus,
    handlePublishVersion,
    rollbackToVersion,
  }
} 
