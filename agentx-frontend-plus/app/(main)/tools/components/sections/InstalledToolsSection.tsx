import { UserTool } from "../../utils/types";
import { Download, Wrench, Info } from "lucide-react";
import { UserToolCard } from "../cards/UserToolCard";
import { ToolCardSkeleton } from "../shared/ToolCardSkeleton";
import { EmptyToolsState } from "../shared/EmptyToolsState";
import { Alert, AlertDescription } from "@/components/ui/alert";

interface InstalledToolsSectionProps {
  installedTools: UserTool[];
  loading: boolean;
  onToolClick: (tool: UserTool) => void;
  onDeleteClick: (tool: UserTool, e: React.MouseEvent) => void;
}

export function InstalledToolsSection({
  installedTools,
  loading,
  onToolClick,
  onDeleteClick
}: InstalledToolsSectionProps) {
  // 检查是否有被删除的工具
  const hasDeletedTools = installedTools.some(tool => tool.deleted);

  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-blue-50 p-1.5 rounded-md text-blue-500 mr-2">
              <Download className="h-5 w-5" />
            </span>
            我安装的工具
          </h2>
          <p className="text-sm text-muted-foreground mt-1">管理您从工具市场安装的工具</p>
        </div>
      </div>
      
      {/* 删除状态提示 */}
      {hasDeletedTools && (
        <Alert className="mb-4 border-amber-200 bg-amber-50">
          <Info className="h-4 w-4 text-amber-600" />
          <AlertDescription className="text-amber-800">
            部分工具显示"来源已删除"，表示该工具的原始来源已被作者删除，但您仍可继续使用已安装的版本。
          </AlertDescription>
        </Alert>
      )}
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          <ToolCardSkeleton count={3} />
        </div>
      ) : installedTools.length === 0 ? (
        <EmptyToolsState
          icon={Download}
          title="还没有安装任何工具"
          description="浏览下方推荐工具或工具市场，安装扩展您AI助手的能力"
          iconClassName="bg-blue-50 p-3 rounded-full text-blue-500"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          {installedTools.map((tool) => (
            <UserToolCard
              key={tool.id}
              tool={tool}
              onCardClick={onToolClick}
              onDeleteClick={onDeleteClick}
            />
          ))}
        </div>
      )}
    </div>
  );
} 