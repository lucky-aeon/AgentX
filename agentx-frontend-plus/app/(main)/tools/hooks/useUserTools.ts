import { useState, useEffect } from 'react';
import { UserTool } from '../utils/types';
import { getUserToolsWithToast, deleteToolWithToast, deleteUserToolWithToast } from '../utils/tool-service';
import { generateMockUserTools } from '../utils/mock-data';
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
      
      // 获取用户工具数据(专注于获取用户创建的工具)
      const response = await getUserToolsWithToast();
      
      if (response.code === 200) {
        // 将API返回的数据转换为UserTool类型
        const toolsList = Array.isArray(response.data) ? response.data : [];
        
        // 标记用户自己创建的工具，同时处理字段的兼容性
        const apiUserTools = toolsList.map((tool: any) => {
          // 处理工具元数据
          const processedTool: UserTool = {
            ...tool,
            // 确保基础字段存在
            id: tool.id,
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
        
        // 设置用户创建的工具(使用API数据)
        setOwnedTools(apiUserTools);
        
        // 使用模拟数据作为安装的工具
        const mockInstalled = generateMockUserTools().filter(tool => !tool.isOwner);
        setInstalledTools(mockInstalled);
        
        // 合并用户创建的工具(API数据)和安装的工具(模拟数据)
        setUserTools([...apiUserTools, ...mockInstalled]);
      } else {
        toast({
          title: "获取用户工具失败",
          description: response.message,
          variant: "destructive",
        });
        
        // API调用失败时，不显示用户创建的工具
        setOwnedTools([]);
        
        // 安装的工具使用模拟数据
        const mockInstalled = generateMockUserTools().filter(tool => !tool.isOwner);
        setInstalledTools(mockInstalled);
        setUserTools(mockInstalled);
      }
    } catch (error) {
      console.error("获取用户工具失败", error);
      toast({
        title: "获取用户工具失败",
        description: "无法加载已创建的工具",
        variant: "destructive",
      });
      
      // 发生错误时，不显示用户创建的工具
      setOwnedTools([]);
      
      // 安装的工具使用模拟数据
      const mockInstalled = generateMockUserTools().filter(tool => !tool.isOwner);
      setInstalledTools(mockInstalled);
      setUserTools(mockInstalled);
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
        response = await deleteUserToolWithToast(toolToDelete.id);
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
      
      return true;
    } catch (error) {
      console.error("删除工具失败", error);
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