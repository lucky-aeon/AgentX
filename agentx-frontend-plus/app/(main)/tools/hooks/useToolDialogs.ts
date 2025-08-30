import { useState } from 'react';
import { MarketTool, UserTool } from '../utils/types';
import { installToolWithToast } from '../utils/tool-service';

interface UseToolDialogsResult {
  // 市场工具详情对话框
  isDetailOpen: boolean;
  selectedTool: MarketTool | null;
  openToolDetail: (tool: MarketTool) => void;
  closeToolDetail: () => void;
  
  // 安装确认对话框
  isInstallDialogOpen: boolean;
  installingToolId: string | null;
  openInstallDialog: (tool: MarketTool) => void;
  closeInstallDialog: () => void;
  handleInstallTool: () => Promise<boolean>;
  
  // 用户工具详情对话框
  isUserToolDetailOpen: boolean;
  selectedUserTool: UserTool | null;
  openUserToolDetail: (tool: UserTool) => void;
  closeUserToolDetail: () => void;
  
  // 删除确认对话框
  isDeleteDialogOpen: boolean;
  toolToDelete: UserTool | null;
  isDeletingTool: boolean;
  openDeleteConfirm: (tool: UserTool, e?: React.MouseEvent) => void;
  closeDeleteDialog: () => void;
}

export function useToolDialogs(
  onInstallSuccess?: () => void,
  onDeleteSuccess?: (tool: UserTool) => Promise<boolean>
): UseToolDialogsResult {
  // 市场工具详情对话框
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedTool, setSelectedTool] = useState<MarketTool | null>(null);
  
  // 安装确认对话框
  const [isInstallDialogOpen, setIsInstallDialogOpen] = useState(false);
  const [installingToolId, setInstallingToolId] = useState<string | null>(null);
  
  // 用户工具详情对话框
  const [isUserToolDetailOpen, setIsUserToolDetailOpen] = useState(false);
  const [selectedUserTool, setSelectedUserTool] = useState<UserTool | null>(null);
  
  // 删除确认对话框
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [toolToDelete, setToolToDelete] = useState<UserTool | null>(null);
  const [isDeletingTool, setIsDeletingTool] = useState(false);

  // 打开工具详情
  const openToolDetail = (tool: MarketTool) => {
    setSelectedTool(tool);
    setIsDetailOpen(true);
  };

  // 关闭工具详情
  const closeToolDetail = () => {
    setIsDetailOpen(false);
  };
  
  // 打开安装确认对话框
  const openInstallDialog = (tool: MarketTool) => {
    setSelectedTool(tool);
    setIsInstallDialogOpen(true);
  };
  
  // 关闭安装确认对话框
  const closeInstallDialog = () => {
    setIsInstallDialogOpen(false);
  };
  
  // 处理安装工具
  const handleInstallTool = async () => {
    if (!selectedTool) return false;
    
    try {
      setInstallingToolId(selectedTool.id);
      
      // 优先使用toolId，确保API调用正确
      const actualToolId = selectedTool.toolId || selectedTool.id;
      const version = selectedTool.current_version || "0.0.1";
      
      const response = await installToolWithToast(actualToolId, version);
      
      if (response.code !== 200) {
        setInstallingToolId(null);
        setIsInstallDialogOpen(false);
        return false;
      }
      
      // 安装成功后，关闭对话框并调用成功回调
      setIsInstallDialogOpen(false);
      setIsDetailOpen(false);
      
      if (onInstallSuccess) {
        onInstallSuccess();
      }
      
      return true;
    } catch (error) {
 
      return false;
    } finally {
      setInstallingToolId(null);
    }
  };
  
  // 打开用户工具详情
  const openUserToolDetail = (tool: UserTool) => {
    setSelectedUserTool(tool);
    setIsUserToolDetailOpen(true);
  };
  
  // 关闭用户工具详情
  const closeUserToolDetail = () => {
    setIsUserToolDetailOpen(false);
  };
  
  // 打开删除确认对话框
  const openDeleteConfirm = (tool: UserTool, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation(); // 防止触发卡片点击事件
    }
    setToolToDelete(tool);
    setIsDeleteDialogOpen(true);
  };
  
  // 关闭删除确认对话框
  const closeDeleteDialog = () => {
    setIsDeleteDialogOpen(false);
  };

  return {
    // 市场工具详情对话框
    isDetailOpen,
    selectedTool,
    openToolDetail,
    closeToolDetail,
    
    // 安装确认对话框
    isInstallDialogOpen,
    installingToolId,
    openInstallDialog,
    closeInstallDialog,
    handleInstallTool,
    
    // 用户工具详情对话框
    isUserToolDetailOpen,
    selectedUserTool,
    openUserToolDetail,
    closeUserToolDetail,
    
    // 删除确认对话框
    isDeleteDialogOpen,
    toolToDelete,
    isDeletingTool,
    openDeleteConfirm,
    closeDeleteDialog
  };
} 