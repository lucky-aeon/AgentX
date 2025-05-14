import { useState, useEffect } from 'react';
import { UserTool } from '../utils/types';
import { getUserToolsWithToast, deleteToolWithToast, getInstalledToolsWithToast, uninstallToolWithToast } from '@/lib/tool-service';
import { toast } from '@/hooks/use-toast';

export function useUserTools() {
  const [userToolsLoading, setUserToolsLoading] = useState(true);
  const [userTools, setUserTools] = useState<UserTool[]>([]);
  const [ownedTools, setOwnedTools] = useState<UserTool[]>([]); // 用户自己创建的工具
  const [installedTools, setInstalledTools] = useState<UserTool[]>([]); // 用户安装的工具
  const [isDeletingTool, setIsDeletingTool] = useState(false);
  
  // 获取用户工具列表（包括创建的工具和安装的工具）
  useEffect(() => {
    fetchUserTools();
  }, []);
  
  // 获取用户工具数据
  async function fetchUserTools() {
    try {
      setUserToolsLoading(true);
      
      // 获取用户创建的工具
      const createdToolsResponse = await getUserToolsWithToast();
      
      // 获取用户安装的工具
      const installedToolsResponse = await getInstalledToolsWithToast({
        page: 1,
        pageSize: 50
      });
      
      // 处理用户创建的工具
      let apiUserTools: UserTool[] = [];
      if (createdToolsResponse.code === 200) {
        // 将API返回的数据转换为UserTool类型
        const toolsList = Array.isArray(createdToolsResponse.data) ? createdToolsResponse.data : [];
        
        // 标记用户自己创建的工具，同时处理字段的兼容性
        apiUserTools = toolsList.map((tool: any) => {
          // 处理工具元数据
          const processedTool: UserTool = {
            ...tool,
            // 确保基础字段存在
            id: tool.id,
            toolId: tool.toolId || tool.id,
            name: tool.name,
            icon: tool.icon,
            subtitle: tool.subtitle || '',
            description: tool.description || '',
            labels: tool.labels || [],
            status: tool.status,
            createdAt: tool.createdAt,
            updatedAt: tool.updatedAt,
            
            // 处理作者信息 - 优先使用userName，但保持author向后兼容
            author: tool.author || tool.userName || '',
            
            // 确保工具列表正确映射
            tool_list: tool.toolList || tool.tool_list || [],
            toolList: tool.toolList || tool.tool_list || [],
            
            // 添加标识字段
            usageCount: tool.usageCount || 0,
            isOwner: true // API返回的是用户创建的工具，所以isOwner为true
          };
          
          return processedTool;
        });
        
        // 设置用户创建的工具
        setOwnedTools(apiUserTools);
      } else {
        toast({
          title: "获取用户创建的工具失败",
          description: createdToolsResponse.message,
          variant: "destructive",
        });
        setOwnedTools([]);
      }
      
      // 处理用户安装的工具
      let apiInstalledTools: UserTool[] = [];
      if (installedToolsResponse.code === 200) {
        // 将API返回的数据转换为UserTool类型
        const toolsList = Array.isArray(installedToolsResponse.data.records) 
          ? installedToolsResponse.data.records 
          : [];
          
        // 转换用户安装的工具
        apiInstalledTools = toolsList.map((tool: any) => {
          return {
            ...tool,
            id: tool.id,
            toolId: tool.toolId || tool.id,
            name: tool.name,
            icon: tool.icon,
            subtitle: tool.subtitle || '',
            description: tool.description || '',
            labels: tool.labels || [],
            author: tool.userName || tool.author || '',
            tool_list: tool.toolList || tool.tool_list || [],
            toolList: tool.toolList || tool.tool_list || [],
            usageCount: tool.usageCount || 0,
            current_version: tool.version || "0.0.1",
            isOwner: false, // 安装的工具不是用户创建的
            status: tool.status || "active",
            createdAt: tool.createdAt,
            updatedAt: tool.updatedAt
          } as UserTool;
        });
        
        // 设置用户安装的工具
        setInstalledTools(apiInstalledTools);
      } else {
        toast({
          title: "获取用户安装的工具失败",
          description: installedToolsResponse.message,
          variant: "destructive",
        });
        setInstalledTools([]);
      }
      
      // 合并用户创建的工具和安装的工具
      setUserTools([...apiUserTools, ...apiInstalledTools]);
      
    } catch (error) {
      console.error("获取用户工具失败", error);
      toast({
        title: "获取用户工具失败",
        description: "无法加载工具列表",
        variant: "destructive",
      });
      setOwnedTools([]);
      setInstalledTools([]);
      setUserTools([]);
    } finally {
      setUserToolsLoading(false);
    }
  }
  
  // 处理删除工具
  const handleDeleteTool = async (toolToDelete: UserTool) => {
    if (!toolToDelete) return false;
    
    try {
      setIsDeletingTool(true);
      
      let response;
      
      // 根据工具是否为用户所有选择不同的删除API
      if (toolToDelete.isOwner) {
        // 删除用户创建的工具
        response = await deleteToolWithToast(toolToDelete.id);
      } else {
        // 卸载（删除）用户安装的工具
        // 优先使用toolId，其次使用id
        const idToDelete = toolToDelete.toolId || toolToDelete.id;
        console.log(`准备卸载工具: toolId=${idToDelete}`);
        response = await uninstallToolWithToast(idToDelete);
      }
      
      if (response.code !== 200) {
        setIsDeletingTool(false);
        return false;
      }
      
      // 更新工具列表，移除已删除的工具
      setUserTools(prev => prev.filter(tool => tool.id !== toolToDelete.id));
      if (toolToDelete.isOwner) {
        setOwnedTools(prev => prev.filter(tool => tool.id !== toolToDelete.id));
      } else {
        setInstalledTools(prev => prev.filter(tool => tool.id !== toolToDelete.id));
      }
      
      toast({
        title: toolToDelete.isOwner ? "删除成功" : "卸载成功",
        description: `工具 "${toolToDelete.name}" 已${toolToDelete.isOwner ? '删除' : '卸载'}`,
      });
      
      return true;
    } catch (error) {
      console.error(toolToDelete.isOwner ? "删除工具失败" : "卸载工具失败", error);
      toast({
        title: toolToDelete.isOwner ? "删除失败" : "卸载失败",
        description: error instanceof Error ? error.message : "操作失败，请重试",
        variant: "destructive"
      });
      return false;
    } finally {
      setIsDeletingTool(false);
    }
  };

  return {
    userTools,
    ownedTools,
    installedTools,
    userToolsLoading,
    isDeletingTool,
    handleDeleteTool,
    fetchUserTools
  };
} 