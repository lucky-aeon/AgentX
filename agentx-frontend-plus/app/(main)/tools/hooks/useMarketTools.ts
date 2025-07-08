import { useState, useEffect } from 'react';
import { MarketTool } from '../utils/types';
import { getMarketToolsWithToast } from '../utils/tool-service';
import { generateMockMarketTools } from '../utils/mock-data';

interface UseMarketToolsOptions {
  initialQuery?: string;
  limit?: number;
}

export function useMarketTools({ initialQuery = '', limit }: UseMarketToolsOptions = {}) {
  const [tools, setTools] = useState<MarketTool[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState(initialQuery);
  
  // 防抖处理搜索查询
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 500);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // 获取工具市场列表
  useEffect(() => {
    async function fetchTools() {
      try {
        setLoading(true);
        setError(null);

        // 开发环境使用模拟数据
        if (process.env.NODE_ENV === 'development') {
          setTimeout(() => {
            let mockTools = generateMockMarketTools();
            
            // 如果存在搜索关键字，进行简单过滤
            if (debouncedQuery) {
              mockTools = mockTools.filter(tool => 
                tool.name.toLowerCase().includes(debouncedQuery.toLowerCase()) ||
                tool.subtitle.toLowerCase().includes(debouncedQuery.toLowerCase()) ||
                tool.author.toLowerCase().includes(debouncedQuery.toLowerCase()) ||
                tool.labels.some(label => label.toLowerCase().includes(debouncedQuery.toLowerCase()))
              );
            }
            
            // 如果有限制数量
            if (limit && limit > 0) {
              mockTools = mockTools.slice(0, limit);
            }
            
            setTools(mockTools);
            setLoading(false);
          }, 1000);
        } else {
          // 生产环境使用实际API
          const response = await getMarketToolsWithToast({
            name: debouncedQuery
          });

          if (response.code === 200) {
            let toolsList = response.data;
            
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

    fetchTools();
  }, [debouncedQuery, limit]);

  // 清除搜索
  const clearSearch = () => {
    setSearchQuery("");
  };

  return {
    tools,
    loading,
    error,
    searchQuery,
    setSearchQuery,
    clearSearch
  };
} 