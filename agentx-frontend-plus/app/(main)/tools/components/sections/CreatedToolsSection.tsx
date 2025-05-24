import { UserTool } from "../../utils/types";
import { Settings, Wrench } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { UserToolCard } from "../cards/UserToolCard";
import { ToolCardSkeleton } from "../shared/ToolCardSkeleton";
import { EmptyToolsState } from "../shared/EmptyToolsState";

interface CreatedToolsSectionProps {
  ownedTools: UserTool[];
  loading: boolean;
  onToolClick: (tool: UserTool) => void;
  onEditClick: (tool: UserTool, e: React.MouseEvent) => void;
  onDeleteClick: (tool: UserTool, e: React.MouseEvent) => void;
  onPublishClick?: (tool: UserTool, e: React.MouseEvent) => void;
}

export function CreatedToolsSection({
  ownedTools,
  loading,
  onToolClick,
  onEditClick,
  onDeleteClick,
  onPublishClick
}: CreatedToolsSectionProps) {
  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-primary/10 p-1.5 rounded-md text-primary mr-2">
              <Settings className="h-5 w-5" />
            </span>
            我创建的工具
          </h2>
          <p className="text-sm text-muted-foreground mt-1">管理和编辑您创建的AI工具</p>
        </div>
      </div>
      
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          <ToolCardSkeleton count={2} />
        </div>
      ) : ownedTools.length === 0 ? (
        <EmptyToolsState
          icon={Wrench}
          title="还没有创建任何工具"
          iconClassName="bg-primary/5 p-3 rounded-full"
          action={{
            label: "上传工具",
            href: "/tools/upload",
            icon: Wrench
          }}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          {ownedTools.map((tool) => (
            <UserToolCard
              key={tool.id}
              tool={tool}
              onCardClick={onToolClick}
              onEditClick={onEditClick}
              onDeleteClick={onDeleteClick}
              onPublishClick={onPublishClick}
            />
          ))}
        </div>
      )}
    </div>
  );
} 