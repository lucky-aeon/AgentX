import { UserTool } from "../../utils/types";
import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ToolLabels } from "../shared/ToolLabels";
import { MoreVertical, PencilIcon, Settings, Trash, Wrench } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useMemo } from "react";

interface UserToolCardProps {
  tool: UserTool;
  onCardClick: (tool: UserTool) => void;
  onEditClick?: (tool: UserTool, e: React.MouseEvent) => void;
  onDeleteClick: (tool: UserTool, e: React.MouseEvent) => void;
}

export function UserToolCard({ 
  tool, 
  onCardClick, 
  onEditClick,
  onDeleteClick 
}: UserToolCardProps) {
  // 获取作者信息，优先使用userName，其次使用author
  const authorName = useMemo(() => {
    return tool.userName || tool.author || '';
  }, [tool]);

  return (
    <Card 
      className="relative overflow-hidden hover:shadow-md transition-all duration-300 border border-gray-100 min-h-[180px]"
    >
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-0" onClick={() => onCardClick(tool)} style={{ cursor: 'pointer' }}>
            <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10 text-primary-foreground overflow-hidden">
              {tool.icon ? (
                <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
              ) : (
                <Wrench className="h-6 w-6" />
              )}
            </div>
            <div className="w-[calc(100%-60px)] min-w-0">
              <h3 className="font-semibold line-clamp-1 truncate text-ellipsis overflow-hidden whitespace-nowrap max-w-full">{tool.name}</h3>
              {authorName && (
                <p className="text-sm text-muted-foreground">{authorName}</p>
              )}
            </div>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreVertical className="h-4 w-4" />
                <span className="sr-only">更多选项</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {tool.isOwner && onEditClick && (
                <DropdownMenuItem onClick={(e) => onEditClick(tool, e as unknown as React.MouseEvent)}>
                  <PencilIcon className="mr-2 h-4 w-4" />
                  编辑工具
                </DropdownMenuItem>
              )}
              <DropdownMenuItem onClick={(e) => onDeleteClick(tool, e as unknown as React.MouseEvent)}>
                <Trash className="mr-2 h-4 w-4" />
                {tool.isOwner ? "删除工具" : "卸载工具"}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0" onClick={() => onCardClick(tool)} style={{ cursor: 'pointer' }}>
        <div className="min-h-[40px] mb-3 line-clamp-2 text-sm">
          {tool.subtitle}
        </div>
        
        <ToolLabels labels={tool.labels} />
      </CardContent>
    </Card>
  );
} 