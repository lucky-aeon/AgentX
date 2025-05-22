import { useState, useEffect } from 'react';
import { MarketTool } from '../utils/types';
import { getRecommendToolsWithToast } from '@/lib/tool-service';
import { generateMockMarketTools } from '../utils/mock-data';

export function useRecommendTools(limit?: number) {
  const [tools, setTools] = useState<MarketTool[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  // 获取推荐工具列表
  useEffect(() => {
    async function fetchRecommendTools() {
      try {
        setLoading(true);
        setError(null);

        // 开发环境使用模拟数据
        if (process.env.NODE_ENV === 'development' && false) { // 设置为false以便始终使用真实API
          setTimeout(() => {
            let mockTools = generateMockMarketTools().slice(0, 6); // 取前6个工具作为推荐
            
            // 如果有限制数量
            if (limit && limit > 0) {
              mockTools = mockTools.slice(0, limit);
            }
            
            setTools(mockTools);
            setLoading(false);
          }, 1000);
        } else {
          // 使用实际API
          const response = await getRecommendToolsWithToast();

          if (response.code === 200) {
            let toolsList = response.data || [];
            
            // 转换格式，确保符合MarketTool接口规范
            toolsList = toolsList.map((item: any) => ({
              id: item.id,
              toolId: item.toolId, // 确保使用正确的toolId
              name: item.name,
              icon: item.icon,
              subtitle: item.subtitle,
              description: item.description,
              user_id: item.userId || "",
              author: item.userName || "未知作者",
              labels: item.labels || [],
              tool_type: item.toolType || "",
              upload_type: item.uploadType || "",
              upload_url: item.uploadUrl || "",
              install_command: {
                type: 'sse',
                url: `https://api.example.com/tools/${item.toolId || item.id}`
              },
              is_office: item.office || false,
              installCount: item.installCount || 0,
              status: item.status || "APPROVED",
              current_version: item.version || "0.0.1", // 确保版本正确
              createdAt: item.createdAt,
              updatedAt: item.updatedAt,
              tool_list: item.toolList || []
            }));
            
            // 如果有限制数量
            if (limit && limit > 0) {
              toolsList = toolsList.slice(0, limit);
            }
            
            setTools(toolsList);
          } else {
            setError(response.message);
          }
          setLoading(false);
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误";
        setError(errorMessage);
        setLoading(false);
      }
    }

    fetchRecommendTools();
  }, [limit]);

  return {
    tools,
    loading,
    error
  };
} 