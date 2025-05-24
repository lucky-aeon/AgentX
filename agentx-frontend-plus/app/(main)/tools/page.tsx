"use client"

import { useState, useEffect } from "react"
import Link from "next/link"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { toast } from "@/hooks/use-toast"

// 自定义Hooks
import { useMarketTools } from "./hooks/useMarketTools"
import { useUserTools } from "./hooks/useUserTools"
import { useToolDialogs } from "./hooks/useToolDialogs"
import { useRecommendTools } from "./hooks/useRecommendTools"

// 页面部分组件
import { CreatedToolsSection } from "./components/sections/CreatedToolsSection"
import { InstalledToolsSection } from "./components/sections/InstalledToolsSection"
import { RecommendedToolsSection } from "./components/sections/RecommendedToolsSection"

// 对话框组件
import { UserToolDetailDialog } from "./components/dialogs/UserToolDetailDialog"
import { InstallToolDialog } from "./components/dialogs/InstallToolDialog"
import { DeleteToolDialog } from "./components/dialogs/DeleteToolDialog"
import { InstallToolDialog as GlobalInstallToolDialog } from "@/components/tool/install-tool-dialog"
import { PublishToolDialog } from "./components/dialogs/PublishToolDialog"
import { UserTool } from "./utils/types"
import { Tool as GlobalToolType, ToolItem } from "@/types/tool"
import { MarketTool, ToolFunction } from "./utils/types"

export default function ToolsPage() {
  // 获取推荐工具数据
  const {
    tools,
    loading: marketToolsLoading,
    error: marketToolsError
  } = useRecommendTools(10);
  
  // 获取用户工具数据
  const {
    ownedTools,
    installedTools,
    userToolsLoading,
    isDeletingTool,
    handleDeleteTool,
    fetchUserTools
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

  // 2. 添加状态 isPublishDialogOpen 和 toolToPublish
  const [isPublishDialogOpen, setIsPublishDialogOpen] = useState(false);
  const [toolToPublish, setToolToPublish] = useState<UserTool | null>(null);

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

  // 3. 创建 handleOpenPublishDialog 函数
  const handleOpenPublishDialog = (tool: UserTool, event?: React.MouseEvent) => {
    if (event) {
      event.stopPropagation();
    }
    setToolToPublish(tool);
    setIsPublishDialogOpen(true);
  };

  // Helper function to adapt MarketTool to GlobalToolType
  const adaptMarketToolToGlobalTool = (marketTool: MarketTool | null): GlobalToolType | null => {
    if (!marketTool) return null;

    // Assuming ToolFunction and ToolItem are structurally compatible
    const adaptedToolList: ToolItem[] = (marketTool.tool_list || []).map(tf => tf as ToolItem);

    return {
      ...marketTool,
      tool_list: adaptedToolList, // Ensure tool_list is present and correctly typed
      // Ensure all required fields for GlobalToolType are present
      // Add default or mapped values for any fields that differ significantly
      // For example, if GlobalToolType has fields not in MarketTool:
      // some_required_field_in_GlobalToolType: marketTool.some_equivalent_field || defaultValue,
      user_id: marketTool.user_id || "", // Ensure user_id is a string
      tool_type: marketTool.tool_type || "",
      upload_type: marketTool.upload_type || "",
      upload_url: marketTool.upload_url || "",
      status: marketTool.status as any, // Assuming ToolStatus enums are compatible enough or map them
      is_office: marketTool.is_office || false,
      install_command: marketTool.install_command || { type: 'sse', url: '' }, // Provide a default if necessary
    };
  };

  // 处理工具安装成功
  const handleToolInstallSuccess = () => {
    // 刷新用户工具列表，确保新安装的工具会显示在"我安装的工具"中
    fetchUserTools();
    // 可以添加一个安装成功的提示
    toast({
      title: "安装成功",
      description: "工具已成功安装，您可以在我安装的工具中查看。"
    });
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
          onPublishClick={handleOpenPublishDialog}
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
          onDelete={handleDeleteTool}
        />
        
        {/* 工具安装确认对话框 */}
        <GlobalInstallToolDialog 
          open={isInstallDialogOpen}
          onOpenChange={closeInstallDialog}
          tool={adaptMarketToolToGlobalTool(selectedTool)}
          version={selectedTool?.current_version}
          onSuccess={handleToolInstallSuccess}
        />

        {/* 删除工具确认对话框 */}
        <DeleteToolDialog
          open={isDeleteDialogOpen}
          onOpenChange={closeDeleteDialog}
          tool={toolToDelete}
          isDeleting={isDeletingTool}
          onConfirm={handleConfirmDelete}
        />

        {/* 6. 渲染 PublishToolDialog */}
        {toolToPublish && (
          <PublishToolDialog
            open={isPublishDialogOpen}
            onOpenChange={setIsPublishDialogOpen}
            tool={toolToPublish}
            onPublishSuccess={() => {
              // 可选：刷新列表或显示提示
              setIsPublishDialogOpen(false); // 关闭对话框
              // 刷新用户工具列表等操作可以在这里触发
            }}
          />
        )}
      </div>
    </div>
  )
}

