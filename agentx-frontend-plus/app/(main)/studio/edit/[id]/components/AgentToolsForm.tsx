import React, { useEffect, useState } from "react";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { getInstalledTools } from "@/lib/tool-service"; // 导入获取工具的函数
import type { Tool } from "@/types/tool"; // 导入 Tool 类型
import type { AgentTool } from "@/types/agent"; // <-- Import AgentTool
import ToolDetailSidebar from "./ToolDetailSidebar"; // 导入工具详情侧边栏

// 用于全局缓存已加载的工具
let cachedTools: Tool[] | null = null;

// 知识库选项 (暂时保留，如果后续不需要可以移除)
export const knowledgeBaseOptions = [
  { id: "kb-1", name: "产品文档", description: "包含产品说明、使用指南等" },
  { id: "kb-2", name: "常见问题", description: "常见问题及解答集合" },
  { id: "kb-3", name: "技术文档", description: "技术规范和API文档" },
  { id: "kb-4", name: "营销资料", description: "营销内容和宣传材料" },
];

interface AgentToolsFormProps {
  formData: { // 只修改 formData 中 tools 的类型
    tools: AgentTool[]; // <-- Use AgentTool[]
    knowledgeBaseIds: string[];
  };
  selectedType: "chat" | "agent";
  toggleTool: (tool: Tool) => void; // <--- 修改签名以接受完整的 Tool 对象
  toggleKnowledgeBase: (kbId: string, kbName?: string) => void;
  onToolClick: (tool: Tool) => void;
  updateToolPresetParameters?: (toolId: string, presetParams: Record<string, Record<string, string>>) => void;
}

const AgentToolsForm: React.FC<AgentToolsFormProps> = ({
  formData,
  selectedType,
  toggleTool,
  toggleKnowledgeBase,
  onToolClick,
  updateToolPresetParameters,
}) => {
  const [installedTools, setInstalledTools] = useState<Tool[]>(cachedTools || []);
  const [isLoadingTools, setIsLoadingTools] = useState(cachedTools ? false : true);
  const [selectedTool, setSelectedTool] = useState<Tool | null>(null);

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
          console.error("获取已安装工具失败:", response.message);
          setInstalledTools([]);
        }
      } catch (error) {
        console.error("获取已安装工具错误:", error);
        setInstalledTools([]);
      } finally {
        setIsLoadingTools(false);
      }
    };

    fetchTools();
  }, []);

  // 处理工具点击事件
  const handleToolClick = (tool: Tool) => {
    setSelectedTool(tool);
  };

  // 处理保存预设参数
  const handleSavePresetParameters = (toolId: string, presetParams: Record<string, Record<string, string>>) => {
    if (updateToolPresetParameters) {
      updateToolPresetParameters(toolId, presetParams);
    }
    setSelectedTool(null);
  };

  // 获取工具的预设参数
  const getToolPresetParameters = (toolId: string): Record<string, Record<string, string>> => {
    const selectedTool = formData.tools.find(tool => tool.id === toolId);
    return selectedTool?.presetParameters || {};
  };

  return (
    <div className="space-y-6">
      {/* 工具选择 */}
      <div>
        <h2 className="text-lg font-medium mb-2">可用工具</h2>
        <p className="text-sm text-muted-foreground mb-2">
          选择{selectedType === "chat" ? "聊天助理" : "功能性助理"}可以使用的工具
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
                const isSelected = formData.tools.some(selectedTool => selectedTool.id === tool.id);
                return (
                  <div
                    key={tool.id}
                    className={`border rounded-lg p-4 cursor-pointer transition-all ${
                      isSelected ? "border-blue-500 bg-blue-50" : "hover:border-gray-300"
                    }`}
                    onClick={() => handleToolClick(tool)}
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

      {/* 知识库选择 - 仅聊天助理显示 */}
      {selectedType === "chat" && (
        <div>
          <h2 className="text-lg font-medium mb-2">知识库</h2>
          <p className="text-sm text-muted-foreground mb-2">选择聊天助理可以访问的知识库</p>
          <div className="grid grid-cols-2 gap-4 mt-4">
            {knowledgeBaseOptions.map((kb) => (
              <div
                key={kb.id}
                className={`border rounded-lg p-4 cursor-pointer transition-all ${
                  formData.knowledgeBaseIds.includes(kb.id)
                    ? "border-blue-500 bg-blue-50"
                    : "hover:border-gray-300"
                }`}
                onClick={() => toggleKnowledgeBase(kb.id, kb.name)} // 传递名称用于 toast
              >
                <div className="flex items-center justify-between mb-2">
                  <h3 className="font-medium">{kb.name}</h3>
                  <Switch checked={formData.knowledgeBaseIds.includes(kb.id)} />
                </div>
                <p className="text-sm text-muted-foreground">{kb.description}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 工具详情侧边栏 */}
      {selectedTool && (
        <ToolDetailSidebar
          tool={selectedTool}
          isOpen={!!selectedTool}
          onClose={() => setSelectedTool(null)}
          presetParameters={getToolPresetParameters(selectedTool.id)}
          onSavePresetParameters={handleSavePresetParameters}
        />
      )}
    </div>
  );
};

export default AgentToolsForm; 