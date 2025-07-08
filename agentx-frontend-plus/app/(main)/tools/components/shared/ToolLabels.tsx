import { Badge } from "@/components/ui/badge";

interface ToolLabelsProps {
  labels: string[];
  limit?: number;
  filterOfficial?: boolean;
  className?: string;
}

export function ToolLabels({ 
  labels, 
  limit = 4,
  filterOfficial = true,
  className = "mb-3 flex flex-wrap gap-2" 
}: ToolLabelsProps) {
  // 过滤官方标签，如果需要的话
  const displayLabels = filterOfficial 
    ? labels.filter(label => label !== "官方")
    : labels;
  
  // 截取指定数量的标签
  const limitedLabels = displayLabels.slice(0, limit);
  
  return (
    <div className={className}>
      {limitedLabels.map((label, i) => (
        <Badge 
          key={i} 
          variant={label === "官方" ? "default" : "outline"}
          className={label === "官方" 
            ? "bg-gradient-to-r from-amber-500 to-orange-500 text-white border-0" 
            : "text-xs bg-gray-50 px-2 py-0.5"}
        >
          {label}
        </Badge>
      ))}
      
      {/* 显示更多标签的提示 */}
      {displayLabels.length > limit && (
        <Badge variant="outline" className="text-xs bg-gray-50 px-2 py-0.5">
          +{displayLabels.length - limit}
        </Badge>
      )}
    </div>
  );
} 