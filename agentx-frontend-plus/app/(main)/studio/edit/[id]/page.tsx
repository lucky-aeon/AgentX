"use client"

// 注意: 在未来的 Next.js 版本中，params 将会是一个 Promise 对象
// 届时需要使用 React.use(params) 解包后再访问其属性

import React, { useEffect } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"

import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Skeleton } from "@/components/ui/skeleton"
import { Badge } from "@/components/ui/badge"

// 自定义hooks
import { useAgentEdit } from "@/hooks/use-agent-edit"
import { useAgentVersion } from "@/hooks/use-agent-version"
import { useAgentTools } from "@/hooks/use-agent-tools"
import { useAgentOperations } from "@/hooks/use-agent-operations"

// 组件
import AgentBasicInfoForm from "./components/AgentBasicInfoForm"
import AgentPromptForm from "./components/AgentPromptForm"
import AgentToolsForm from "./components/AgentToolsForm"
import AgentEditHeader from "./components/AgentEditHeader"
import ToolDetailSidebar from "./components/ToolDetailSidebar"
import AgentPreviewChat from "@/components/agent-preview-chat"

// Dialog组件
import AgentDeleteDialog from "./components/AgentDeleteDialog"
import AgentPublishDialog from "./components/AgentPublishDialog"
import AgentVersionHistoryDialog from "./components/AgentVersionHistoryDialog"
import AgentVersionDetailDialog from "./components/AgentVersionDetailDialog"

export default function EditAgentPage() {
  const params = useParams()
  const agentId = params.id as string
  
  // 使用自定义hooks
  const {
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
    isLoadingVersions,
    setIsLoadingVersions,
    isRollingBack,
    setIsRollingBack,
    isLoadingLatestVersion,
    setIsLoadingLatestVersion,
    fileInputRef,
    formData,
    setFormData,
    updateFormField,
    getAvailableTabs,
  } = useAgentEdit()

  const {
    showDeleteDialog,
    setShowDeleteDialog,
    showPublishDialog,
    setShowPublishDialog,
    showVersionsDialog,
    setShowVersionsDialog,
    versionNumber,
    setVersionNumber,
    changeLog,
    setChangeLog,
    versions,
    selectedVersion,
    setSelectedVersion,
    latestVersion,
    fetchLatestVersion,
    loadVersions,
    viewVersionDetail,
    resetPublishForm,
  } = useAgentVersion()

  const {
    selectedToolForSidebar,
    isToolSidebarOpen,
    setIsToolSidebarOpen,
    handleToolClick,
    updateToolPresetParameters,
  } = useAgentTools()

  const {
    loadAgentDetail,
    handleAvatarUpload,
    removeAvatar,
    handleUpdateAgent,
    handleDeleteAgent,
    handleToggleStatus,
    handlePublishVersion,
    rollbackToVersion,
  } = useAgentOperations()

  // 加载助理详情
  useEffect(() => {
    loadAgentDetail(agentId, setFormData, setSelectedType, setIsLoading)
  }, [agentId])

  // 包装的事件处理函数
  const wrappedHandleAvatarUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    handleAvatarUpload(event, updateFormField)
  }

  const wrappedRemoveAvatar = () => {
    removeAvatar(updateFormField, fileInputRef)
  }

  const wrappedTriggerFileInput = () => {
    fileInputRef.current?.click()
  }

  const wrappedHandleUpdateAgent = () => {
    handleUpdateAgent(agentId, formData, setIsSubmitting)
  }

  const wrappedHandleDeleteAgent = () => {
    handleDeleteAgent(agentId, setIsDeleting, setShowDeleteDialog)
  }

  const wrappedHandleToggleStatus = () => {
    handleToggleStatus(formData, updateFormField)
  }

  const wrappedHandlePublishVersion = () => {
    handlePublishVersion(
      agentId,
      formData,
      versionNumber,
      changeLog,
      setIsPublishing,
      setShowPublishDialog,
      resetPublishForm,
      () => fetchLatestVersion(agentId)
    )
  }

  const wrappedOpenPublishDialog = async () => {
    await fetchLatestVersion(agentId)
    setShowPublishDialog(true)
  }

  const wrappedLoadVersions = () => {
    setShowVersionsDialog(true)
    loadVersions(agentId)
  }

  const wrappedRollbackToVersion = (version: any) => {
    rollbackToVersion(
      version,
      setFormData,
      setSelectedType,
      formData,
      setIsRollingBack,
      setSelectedVersion,
      setShowVersionsDialog
    )
  }

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
            onShowVersionsDialog={wrappedLoadVersions}
            onOpenPublishDialog={wrappedOpenPublishDialog}
            onToggleStatus={wrappedHandleToggleStatus}
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
                triggerFileInput={wrappedTriggerFileInput}
                handleAvatarUpload={wrappedHandleAvatarUpload}
                removeAvatar={wrappedRemoveAvatar}
                fileInputRef={fileInputRef}
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
                selectedType={selectedType}
                toggleTool={(tool) => {
                  // 这里需要从hooks中获取toggleTool方法
                  // 由于hook的限制，我们需要在这里实现简化的逻辑
                  const toolIdentifier = tool.toolId || tool.id
                  const isToolCurrentlyEnabled = formData.tools.some(t => t.id === toolIdentifier)
                  
                  if (isToolCurrentlyEnabled) {
                    updateFormField("tools", formData.tools.filter(t => t.id !== toolIdentifier))
                  } else {
                    const newAgentTool = {
                      id: toolIdentifier,
                      name: tool.name,
                      description: tool.description || undefined,
                    }
                    updateFormField("tools", [...formData.tools, newAgentTool])
                  }
                }}
                toggleKnowledgeBase={(kbId, kbName) => {
                  const knowledgeBaseIds = [...formData.knowledgeBaseIds]
                  if (knowledgeBaseIds.includes(kbId)) {
                    updateFormField("knowledgeBaseIds", knowledgeBaseIds.filter(id => id !== kbId))
                  } else {
                    updateFormField("knowledgeBaseIds", [...knowledgeBaseIds, kbId])
                  }
                }}
                onToolClick={handleToolClick}
                updateToolPresetParameters={(toolId, presetParams) => {
                  updateToolPresetParameters(toolId, presetParams, setFormData)
                }}
              />
            </TabsContent>
          </Tabs>

          {/* 底部按钮 */}
          <div className="flex justify-end pt-6 border-t mt-6">
            <div className="space-x-2">
              <Button variant="outline" asChild>
                <Link href="/studio">取消</Link>
              </Button>
              <Button onClick={wrappedHandleUpdateAgent} disabled={isSubmitting}>
                {isSubmitting ? "保存中..." : "保存更改"}
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
          </div>

          {/* Agent预览 */}
          <AgentPreviewChat
            agentName={formData.name || "预览助理"}
            agentAvatar={formData.avatar}
            systemPrompt={formData.systemPrompt}
            welcomeMessage={formData.welcomeMessage}
            toolIds={formData.tools.map(t => t.id)}
            toolPresetParams={formData.toolPresetParams as unknown as Record<string, Record<string, Record<string, string>>>}
            disabled={!formData.name || !formData.systemPrompt}
            className="h-[500px]"
          />

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

      {/* 对话框组件 */}
      <AgentDeleteDialog
        open={showDeleteDialog}
        onOpenChange={setShowDeleteDialog}
        onConfirm={wrappedHandleDeleteAgent}
        isDeleting={isDeleting}
      />

      <AgentPublishDialog
        open={showPublishDialog}
        onOpenChange={setShowPublishDialog}
        onConfirm={wrappedHandlePublishVersion}
        isPublishing={isPublishing}
        isLoadingLatestVersion={isLoadingLatestVersion}
        latestVersion={latestVersion}
        versionNumber={versionNumber}
        onVersionNumberChange={setVersionNumber}
        changeLog={changeLog}
        onChangeLogChange={setChangeLog}
      />

      <AgentVersionHistoryDialog
        open={showVersionsDialog}
        onOpenChange={setShowVersionsDialog}
        isLoadingVersions={isLoadingVersions}
        versions={versions}
        onViewVersion={viewVersionDetail}
        onRollbackToVersion={wrappedRollbackToVersion}
        isRollingBack={isRollingBack}
      />

      <AgentVersionDetailDialog
        version={selectedVersion}
        onClose={() => setSelectedVersion(null)}
        onRollback={wrappedRollbackToVersion}
        isRollingBack={isRollingBack}
      />

      {/* 工具详情侧边栏 */}
      <ToolDetailSidebar
        tool={selectedToolForSidebar}
        isOpen={isToolSidebarOpen}
        onClose={() => setIsToolSidebarOpen(false)}
        presetParameters={selectedToolForSidebar && selectedToolForSidebar.mcpServerName && formData.toolPresetParams[selectedToolForSidebar.mcpServerName] ? 
          formData.toolPresetParams[selectedToolForSidebar.mcpServerName] : 
          {}}
        onSavePresetParameters={(toolId, presetParams) => {
          updateToolPresetParameters(toolId, presetParams, setFormData)
        }}
      />
    </div>
  )
}

