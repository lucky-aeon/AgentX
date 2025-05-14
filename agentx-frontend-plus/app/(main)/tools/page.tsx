"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"

// 自定义Hooks
import { useMarketTools } from "./hooks/useMarketTools"
import { useUserTools } from "./hooks/useUserTools"
import { useToolDialogs } from "./hooks/useToolDialogs"

// 页面部分组件
import { CreatedToolsSection } from "./components/sections/CreatedToolsSection"
import { InstalledToolsSection } from "./components/sections/InstalledToolsSection"
import { RecommendedToolsSection } from "./components/sections/RecommendedToolsSection"

// 对话框组件
import { UserToolDetailDialog } from "./components/dialogs/UserToolDetailDialog"
import { InstallToolDialog } from "./components/dialogs/InstallToolDialog"
import { DeleteToolDialog } from "./components/dialogs/DeleteToolDialog"

export default function ToolsPage() {
  // 获取市场工具数据
  const {
    tools,
    loading: marketToolsLoading,
    error: marketToolsError
  } = useMarketTools({ limit: 10 });
  
  // 获取用户工具数据
  const {
    ownedTools,
    installedTools,
    userToolsLoading,
    isDeletingTool,
    handleDeleteTool
  } = useUserTools();
  
  // 对话框状态管理
  const {
    // 市场工具详情
    isDetailOpen,
    selectedTool,
    openToolDetail,
    closeToolDetail,
    
    // 安装确认
    isInstallDialogOpen,
    installingToolId,
    openInstallDialog,
    closeInstallDialog,
    handleInstallTool,
    
    // 用户工具详情
    isUserToolDetailOpen,
    selectedUserTool,
    openUserToolDetail,
    closeUserToolDetail,
    
    // 删除确认
    isDeleteDialogOpen,
    toolToDelete,
    openDeleteConfirm,
    closeDeleteDialog
  } = useToolDialogs();

  // 处理编辑工具
  const handleEditTool = (tool: any, event?: React.MouseEvent) => {
    if (event) {
      event.stopPropagation();
    }
    // 直接跳转到编辑工具页面
    window.location.href = `/tools/edit/${tool.id}`;
  };
  
  // 处理删除工具确认
  const handleConfirmDelete = async (): Promise<boolean> => {
    if (!toolToDelete) return false;
    
    const success = await handleDeleteTool(toolToDelete);
    
    if (success) {
      closeDeleteDialog();
    }
    
    return success || false;
  };

  return (
    <div className="py-6 min-h-screen bg-gray-50">
      <div className="container max-w-7xl mx-auto px-2">
        {/* 页面头部 */}
        <div className="flex items-center justify-between mb-8 bg-white p-6 rounded-lg shadow-sm">
          <div>
            <h1 className="text-3xl font-bold tracking-tight bg-gradient-to-r from-primary to-blue-600 bg-clip-text text-transparent">工具中心</h1>
            <p className="text-muted-foreground mt-1">探索和管理AI助手的扩展能力</p>
          </div>
          
          <Button asChild className="shadow-sm">
            <Link href="/tools/upload">
              <Plus className="mr-2 h-4 w-4" />
              上传工具
            </Link>
          </Button>
        </div>
        
        {/* 用户创建的工具部分 */}
        <CreatedToolsSection
          ownedTools={ownedTools}
          loading={userToolsLoading}
          onToolClick={openUserToolDetail}
          onEditClick={handleEditTool}
          onDeleteClick={openDeleteConfirm}
        />
        
        {/* 用户安装的工具部分 */}
        <InstalledToolsSection
          installedTools={installedTools}
          loading={userToolsLoading}
          onToolClick={openUserToolDetail}
          onDeleteClick={openDeleteConfirm}
        />
        
        {/* 工具市场推荐部分 */}
        <RecommendedToolsSection
          tools={tools}
          loading={marketToolsLoading}
          error={marketToolsError}
          onInstallClick={openInstallDialog}
        />
        
        {/* 用户工具详情对话框 */}
        <UserToolDetailDialog
          open={isUserToolDetailOpen}
          onOpenChange={closeUserToolDetail}
          tool={selectedUserTool}
        />
        
        {/* 工具安装确认对话框 */}
        <InstallToolDialog 
          open={isInstallDialogOpen}
          onOpenChange={closeInstallDialog}
          tool={selectedTool}
          isInstalling={!!installingToolId}
          onConfirm={handleInstallTool}
        />

        {/* 删除工具确认对话框 */}
        <DeleteToolDialog
          open={isDeleteDialogOpen}
          onOpenChange={closeDeleteDialog}
          tool={toolToDelete}
          isDeleting={isDeletingTool}
          onConfirm={handleConfirmDelete}
        />
      </div>
    </div>
  )
}

