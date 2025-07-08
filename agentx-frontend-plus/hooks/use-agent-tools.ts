import { useState, useEffect } from "react"
import type { Tool } from "@/types/tool"
import type { AgentTool } from "@/types/agent"
import { getInstalledTools } from "@/lib/tool-service"
import { toast } from "@/hooks/use-toast"

export function useAgentTools() {
  const [selectedToolForSidebar, setSelectedToolForSidebar] = useState<Tool | null>(null)
  const [isToolSidebarOpen, setIsToolSidebarOpen] = useState(false)
  const [installedTools, setInstalledTools] = useState<Tool[]>([])
  const [isLoadingTools, setIsLoadingTools] = useState(false)

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

  // 切换工具
  const toggleTool = (
    toolToToggle: Tool, 
    currentTools: AgentTool[], 
    setFormData: (updater: (prev: any) => any) => void
  ) => {
    // 使用 toolId（如果存在）或 id 作为工具标识符
    const toolIdentifier = toolToToggle.toolId || toolToToggle.id;
    const isToolCurrentlyEnabled = currentTools.some(t => t.id === toolIdentifier);
    
    setFormData((prev: any) => {
      let updatedTools: AgentTool[];
      if (isToolCurrentlyEnabled) {
        updatedTools = prev.tools.filter((t: AgentTool) => t.id !== toolIdentifier);
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
  const toggleKnowledgeBase = (
    kbId: string, 
    currentKnowledgeBaseIds: string[], 
    setFormData: (updater: (prev: any) => any) => void,
    kbName?: string
  ) => {
    const isKnowledgeBaseAssociated = !currentKnowledgeBaseIds.includes(kbId)
    setFormData((prev: any) => {
      const knowledgeBaseIds = [...prev.knowledgeBaseIds]
      if (knowledgeBaseIds.includes(kbId)) {
        return { ...prev, knowledgeBaseIds: knowledgeBaseIds.filter((id: string) => id !== kbId) }
      } else {
        return { ...prev, knowledgeBaseIds: [...knowledgeBaseIds, kbId] }
      }
    })
    
    toast({
      title: `知识库已${isKnowledgeBaseAssociated ? "关联" : "取消关联"}: ${kbName || kbId}`,
    })
  }

  // 处理工具点击事件
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
  }

  // 更新工具预设参数
  const updateToolPresetParameters = (
    toolId: string, 
    presetParams: Record<string, Record<string, string>>,
    setFormData: (updater: (prev: any) => any) => void
  ) => {
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
    
    setFormData((prev: any) => {
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
        
        // 将参数添加到嵌套结构中
        if (!newToolPresetParams[mcpServerName][functionName]) {
          newToolPresetParams[mcpServerName][functionName] = {};
        }
        
        // 添加每个参数
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

  return {
    // 工具侧边栏状态
    selectedToolForSidebar,
    setSelectedToolForSidebar,
    isToolSidebarOpen,
    setIsToolSidebarOpen,
    
    // 工具数据
    installedTools,
    isLoadingTools,
    
    // 方法
    toggleTool,
    toggleKnowledgeBase,
    handleToolClick,
    updateToolPresetParameters,
  }
} 