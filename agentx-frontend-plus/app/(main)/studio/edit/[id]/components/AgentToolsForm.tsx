import React, { useEffect, useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { getInstalledTools } from "@/lib/tool-service"; // 导入获取工具的函数
import { getAvailableKnowledgeBasesWithToast } from "@/lib/agent-knowledge-base-service"; // 导入获取知识库的函数
import type { Tool } from "@/types/tool"; // 导入 Tool 类型
import type { AgentTool } from "@/types/agent"; // <-- Import AgentTool
import type { KnowledgeBase } from "@/lib/agent-knowledge-base-service"; // 导入 KnowledgeBase 类型

// 用于全局缓存已加载的工具
let cachedTools: Tool[] | null = null;

// 用于全局缓存已加载的知识库
let cachedKnowledgeBases: KnowledgeBase[] | null = null;

interface AgentToolsFormProps {
  formData: { // 只修改 formData 中 tools 的类型
    tools: AgentTool[]; // <-- Use AgentTool[]
    knowledgeBaseIds: string[];
  };
  selectedType?: "chat" | "agent"; // 可选参数，保持向后兼容
  toggleTool: (tool: Tool) => void; // <--- 修改签名以接受完整的 Tool 对象
  toggleKnowledgeBase: (kbId: string, kbName?: string) => void;
  onToolClick: (tool: Tool) => void;
  onKnowledgeBaseClick: (knowledgeBase: KnowledgeBase) => void;
  updateToolPresetParameters?: (toolId: string, presetParams: Record<string, Record<string, string>>) => void;
}

const AgentToolsForm: React.FC<AgentToolsFormProps> = ({
  formData,
  selectedType,
  toggleTool,
  toggleKnowledgeBase,
  onToolClick,
  onKnowledgeBaseClick,
  updateToolPresetParameters,
}) => {
  const [installedTools, setInstalledTools] = useState<Tool[]>(cachedTools || []);
  const [isLoadingTools, setIsLoadingTools] = useState(cachedTools ? false : true);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>(cachedKnowledgeBases || []);
  const [isLoadingKnowledgeBases, setIsLoadingKnowledgeBases] = useState(cachedKnowledgeBases ? false : true);

  useEffect(() => {
    // 如果已经有缓存数据，直接使用不重新请求
    if (cachedTools) {
      setInstalledTools(cachedTools);
      setIsLoadingTools(false);
      return;
    }

    const fetchTools = async () => {
      try {
        const response = await getInstalledTools({ pageSize: 100 });
        if (response.code === 200 && response.data && Array.isArray(response.data.records)) {
          const tools = response.data.records;
          setInstalledTools(tools);
          // 缓存工具数据
          cachedTools = tools;
        } else {
 
          setInstalledTools([]);
        }
      } catch (error) {
 
        setInstalledTools([]);
      } finally {
        setIsLoadingTools(false);
      }
    };

    fetchTools();

    // 获取知识库列表
    if (cachedKnowledgeBases) {
      setKnowledgeBases(cachedKnowledgeBases);
      setIsLoadingKnowledgeBases(false);
    } else {
      const fetchKnowledgeBases = async () => {
        try {
          const response = await getAvailableKnowledgeBasesWithToast();
          if (response.code === 200 && response.data) {
            const kbs = response.data;
            setKnowledgeBases(kbs);
            cachedKnowledgeBases = kbs; // 缓存数据
          } else {
 
            setKnowledgeBases([]);
          }
        } catch (error) {
 
          setKnowledgeBases([]);
        } finally {
          setIsLoadingKnowledgeBases(false);
        }
      };
      fetchKnowledgeBases();
    }
  }, []);

  // 检查工具是否被选中
  const isToolSelected = (tool: Tool): boolean => {
    // 确保使用 toolId 作为工具的唯一标识符进行匹配
    if (!tool.toolId) {
 
      return false;
    }
    return formData.tools.some(selectedTool => selectedTool.id === tool.toolId);
  };

  return (
    <div className="space-y-6">
      {/* 工具选择 */}
      <div>
        <h2 className="text-lg font-medium mb-2">可用工具</h2>
        <p className="text-sm text-muted-foreground mb-2">
          选择助理可以使用的工具
        </p>
        <div className="min-h-[200px]">
          {isLoadingTools ? (
            <div className="grid grid-cols-2 gap-4 mt-4">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-14 w-full rounded-lg" />
              ))}
            </div>
          ) : installedTools.length > 0 ? (
            <div className="grid grid-cols-2 gap-4 mt-4">
              {installedTools.map((tool) => {
                const isSelected = isToolSelected(tool);
                
                return (
                  <div
                    key={tool.id}
                    className={`border rounded-lg p-4 cursor-pointer transition-all ${
                      isSelected ? "border-blue-500 bg-blue-50" : "hover:border-gray-300"
                    }`}
                    onClick={() => onToolClick(tool)}
                  >
                    <div className="flex items-center justify-between">
                      <h3 className="font-medium">{tool.name}</h3>
                      <Switch 
                        checked={isSelected} 
                        onCheckedChange={() => toggleTool(tool)}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground mt-4">没有找到已安装的工具。</p>
          )}
        </div>
      </div>

      {/* 知识库选择 */}
      <div>
        <h2 className="text-lg font-medium mb-2">知识库</h2>
        <p className="text-sm text-muted-foreground mb-2">选择助理可以访问的知识库</p>
        <div className="min-h-[200px]">
          {isLoadingKnowledgeBases ? (
            <div className="grid grid-cols-2 gap-4 mt-4">
              {[1, 2, 3, 4].map((i) => (
                <Skeleton key={i} className="h-20 w-full rounded-lg" />
              ))}
            </div>
          ) : knowledgeBases.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <p>暂无可用的知识库</p>
              <p className="text-sm mt-1">请先创建或安装知识库</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4 mt-4">
              {knowledgeBases.map((kb) => (
                <div
                  key={kb.id}
                  className={`border rounded-lg p-4 cursor-pointer transition-all ${
                    formData.knowledgeBaseIds.includes(kb.id)
                      ? "border-blue-500 bg-blue-50"
                      : "hover:border-gray-300"
                  }`}
                  onClick={() => onKnowledgeBaseClick(kb)}
                >
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="font-medium">{kb.name}</h3>
                    <Switch 
                      checked={formData.knowledgeBaseIds.includes(kb.id)}
                      onCheckedChange={() => toggleKnowledgeBase(kb.id, kb.name)}
                      onClick={(e) => e.stopPropagation()}
                    />
                  </div>
                  <p className="text-sm text-muted-foreground">{kb.description || "暂无描述"}</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {kb.fileCount} 个文件
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AgentToolsForm; 