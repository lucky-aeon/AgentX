"use client"

import React, { useEffect } from "react"
import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"

import { useAgentForm, type AgentFormData } from "@/hooks/use-agent-form"
import { useToast } from "@/hooks/use-toast"
import ModelSelector from "@/components/model-selector"
import AgentBasicInfoForm from "@/app/(main)/studio/edit/[id]/components/AgentBasicInfoForm"
import AgentPromptForm from "@/app/(main)/studio/edit/[id]/components/AgentPromptForm"
import AgentToolsForm from "@/app/(main)/studio/edit/[id]/components/AgentToolsForm"
import ToolDetailSidebar from "@/app/(main)/studio/edit/[id]/components/ToolDetailSidebar"
import KnowledgeBaseDetailSidebar from "@/app/(main)/studio/edit/[id]/components/KnowledgeBaseDetailSidebar"
import AgentPreviewChat from "@/components/agent-preview-chat"
import { AgentWidgetTab } from "@/app/(main)/studio/edit/[id]/components/AgentWidgetTab"

interface AgentFormModalProps {
  // 模式控制
  mode: "create" | "edit"
  
  // 编辑模式属性
  agentId?: string
  initialData?: Partial<AgentFormData>
  
  // 标题和描述
  title?: string
  description?: string
  
  // 操作回调
  onSubmit: (formData: AgentFormData) => Promise<void>
  onCancel: () => void
  
  // 编辑模式特有的操作
  onDelete?: () => void
  onPublish?: () => void
  onShowVersions?: () => void
  
  // 加载状态
  isSubmitting?: boolean
  
  // 其他编辑模式组件（如版本历史对话框等）
  children?: React.ReactNode
}

export default function AgentFormModal({
  mode,
  agentId,
  initialData,
  title,
  description,
  onSubmit,
  onCancel,
  onDelete,
  onPublish,
  onShowVersions,
  isSubmitting: externalIsSubmitting = false,
  children,
}: AgentFormModalProps) {
  const {
    // 基础状态
    activeTab,
    setActiveTab,
    isSubmitting: internalIsSubmitting,
    
    // 工具相关状态
    selectedToolForSidebar,
    isToolSidebarOpen,
    setIsToolSidebarOpen,
    installedTools, // 已安装工具列表，用于过滤有效工具ID
    
    // 知识库相关状态
    selectedKnowledgeBaseForSidebar,
    isKnowledgeBaseSidebarOpen,
    setIsKnowledgeBaseSidebarOpen,
    
    // 表单数据
    formData,
    updateFormField,
    
    // 表单操作函数
    toggleTool,
    toggleKnowledgeBase,
    handleToolClick,
    handleKnowledgeBaseClick,
    updateToolPresetParameters,
    
    // 工具函数
    getAvailableTabs,
  } = useAgentForm({ 
    initialData, 
    isEditMode: mode === "edit" 
  })

  const { toast } = useToast()

  const isSubmitting = externalIsSubmitting || internalIsSubmitting
  
  // 编辑模式下，如果没有initialData，说明还在加载
  const isLoading = mode === "edit" && !initialData

  // 获取有效的工具ID列表（只包含在已安装工具列表中存在的工具）
  const getValidToolIds = (): string[] => {
    const formToolIds = formData.tools.map(t => t.id);
    const installedToolIds = installedTools.map(tool => tool.toolId).filter(Boolean);
    
    // 过滤出在已安装工具列表中存在的工具ID
    const validToolIds = formToolIds.filter(formToolId => installedToolIds.includes(formToolId));
    
    // 打印调试信息
 
 
 
    
    // 检查是否有无效的工具ID
    const invalidToolIds = formToolIds.filter(formToolId => !installedToolIds.includes(formToolId));
    if (invalidToolIds.length > 0) {
 
    }
    
    return validToolIds;
  }

  // 监控预览传递的工具ID变化
  useEffect(() => {
    const validToolIds = getValidToolIds();
    if (validToolIds.length > 0) {
 
    }
  }, [formData.tools, installedTools])

  // 处理提交
  const handleSubmit = async () => {
    // 前端校验
    if (!formData.name.trim()) {
      toast({
        title: "请输入助理名称",
        description: "助理名称是必填项",
        variant: "destructive",
      })
      return
    }

    if (!formData.description?.trim()) {
      toast({
        title: "请输入助理描述",
        description: "助理描述是必填项",
        variant: "destructive",
      })
      return
    }
    
    await onSubmit(formData)
  }

  // 如果正在加载（编辑模式），显示加载状态
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
          {/* 头部 */}
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold">
                {title || (mode === "create" ? "创建新的助理" : "编辑助理")}
              </h1>
              {description && (
                <p className="text-muted-foreground mt-1">{description}</p>
              )}
            </div>
            
            <div className="flex items-center gap-2">
              {/* 编辑模式的额外操作 */}
              {mode === "edit" && (
                <>
                  {onShowVersions && (
                    <Button variant="outline" size="sm" onClick={onShowVersions}>
                      版本历史
                    </Button>
                  )}
                  {onPublish && (
                    <Button variant="outline" size="sm" onClick={onPublish}>
                      发布版本
                    </Button>
                  )}

                  {onDelete && (
                    <Button variant="destructive" size="sm" onClick={onDelete}>
                      删除
                    </Button>
                  )}
                </>
              )}
              
              <Button variant="outline" onClick={onCancel}>
                取消
              </Button>
            </div>
          </div>

          {/* 表单标签页 */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
            <TabsList className={`grid w-full ${mode === "edit" ? "grid-cols-4" : "grid-cols-3"}`}>
              {getAvailableTabs().map((tab) => (
                <TabsTrigger key={tab.id} value={tab.id}>
                  {tab.label}
                </TabsTrigger>
              ))}
            </TabsList>

            <TabsContent value="basic" className="space-y-6">
              <AgentBasicInfoForm
                formData={formData}
                selectedType="agent"
                updateFormField={updateFormField}
              />
            </TabsContent>

            <TabsContent value="prompt" className="space-y-6">
              <AgentPromptForm
                formData={formData}
                updateFormField={updateFormField}
              />
            </TabsContent>

            <TabsContent value="tools" className="space-y-6">
              <AgentToolsForm
                formData={formData}
                selectedType="agent"
                toggleTool={toggleTool}
                toggleKnowledgeBase={toggleKnowledgeBase}
                onToolClick={handleToolClick}
                onKnowledgeBaseClick={handleKnowledgeBaseClick}
                updateToolPresetParameters={updateToolPresetParameters}
              />
            </TabsContent>

            {/* 小组件标签页 - 仅编辑模式 */}
            {mode === "edit" && agentId && (
              <TabsContent value="widget" className="space-y-6">
                <AgentWidgetTab agentId={agentId} />
              </TabsContent>
            )}
          </Tabs>

          {/* 底部按钮 */}
          <div className="flex justify-end pt-6 border-t mt-6">
            <div className="space-x-2">
              <Button variant="outline" onClick={onCancel}>
                取消
              </Button>
              <Button onClick={handleSubmit} disabled={isSubmitting}>
                {isSubmitting 
                  ? (mode === "create" ? "创建中..." : "保存中...") 
                  : (mode === "create" ? "确认创建" : "保存更改")
                }
              </Button>
            </div>
          </div>
        </div>

        {/* 右侧预览 */}
        <div className="w-2/5 bg-gray-50 p-8 overflow-auto border-l">
          <div className="mb-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold">预览</h2>
                <p className="text-muted-foreground">
                  与你的Agent进行实时对话，预览实际效果
                </p>
              </div>
            </div>
            
            {/* 默认模型选择 */}
            <ModelSelector mode="preview" className="mt-4" />
          </div>

          {/* Agent预览 */}
          <AgentPreviewChat
            agentName={formData.name || (mode === "create" ? "新建助理" : "预览助理")}
            agentAvatar={formData.avatar}
            systemPrompt={formData.systemPrompt || "你是一个智能助手，可以帮助用户解答问题和完成任务。"}
            welcomeMessage={formData.welcomeMessage}
            toolIds={getValidToolIds()} 
            toolPresetParams={formData.toolPresetParams as unknown as Record<string, Record<string, Record<string, string>>>}
            multiModal={formData.multiModal}
            knowledgeBaseIds={formData.knowledgeBaseIds}
            disabled={false}
            className="h-[500px]"
          />

          {/* 配置摘要 */}
          <div className="mt-6">
            <h3 className="text-lg font-medium mb-3">配置摘要</h3>
            <Card>
              <CardContent className="p-4 space-y-3">
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">类型</span>
                  <span className="text-sm font-medium">智能助理</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">工具数量</span>
                  <span className="text-sm font-medium">{formData.tools.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">知识库数量</span>
                  <span className="text-sm font-medium">{formData.knowledgeBaseIds.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-muted-foreground">状态</span>
                  <Badge variant={formData.enabled ? "outline" : "default"} className="text-xs">
                    {formData.enabled ? "启用" : "禁用"}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* 工具详情侧边栏 */}
      <ToolDetailSidebar
        tool={selectedToolForSidebar}
        isOpen={isToolSidebarOpen}
        onClose={() => setIsToolSidebarOpen(false)}
        presetParameters={selectedToolForSidebar && selectedToolForSidebar.mcpServerName && formData.toolPresetParams[selectedToolForSidebar.mcpServerName] ? 
          formData.toolPresetParams[selectedToolForSidebar.mcpServerName] : 
          {}}
        onSavePresetParameters={updateToolPresetParameters}
      />

      {/* 知识库详情侧边栏 */}
      <KnowledgeBaseDetailSidebar
        knowledgeBase={selectedKnowledgeBaseForSidebar}
        isOpen={isKnowledgeBaseSidebarOpen}
        onClose={() => setIsKnowledgeBaseSidebarOpen(false)}
      />

      {/* 编辑模式的额外组件（如版本历史对话框等） */}
      {children}
    </div>
  )
} 