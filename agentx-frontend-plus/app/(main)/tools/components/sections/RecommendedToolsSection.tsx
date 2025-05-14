import { MarketTool } from "../../utils/types";
import { Search, Wrench } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { MarketToolCard } from "../cards/MarketToolCard";
import { ToolCardSkeleton } from "../shared/ToolCardSkeleton";
import { EmptyToolsState } from "../shared/EmptyToolsState";

interface RecommendedToolsSectionProps {
  tools: MarketTool[];
  loading: boolean;
  error: string | null;
  onInstallClick: (tool: MarketTool) => void;
}

export function RecommendedToolsSection({
  tools,
  loading,
  error,
  onInstallClick
}: RecommendedToolsSectionProps) {
  return (
    <div className="mb-8 bg-white p-6 rounded-lg shadow-sm border border-gray-100">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h2 className="text-xl font-semibold flex items-center">
            <span className="bg-green-50 p-1.5 rounded-md text-green-500 mr-2">
              <Search className="h-5 w-5" />
            </span>
            推荐工具
          </h2>
          <p className="text-sm text-muted-foreground mt-1">热门工具推荐，提升您的AI助手能力</p>
        </div>
        
      </div>
      
      {/* 工具市场横幅 */}
      <div className="mb-6 rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 p-5 border border-blue-100">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-4">
            <div className="rounded-full bg-white/80 p-3 hidden sm:flex shadow-sm">
              <Wrench className="h-5 w-5 text-blue-500" />
            </div>
            <div>
              <h3 className="font-medium text-lg">探索工具市场</h3>
              <p className="text-sm text-muted-foreground">发现更多提升AI能力的工具，满足您的各种需求</p>
            </div>
          </div>
          <Button asChild className="shadow-sm">
            <Link href="/tools-market">
              <Search className="mr-2 h-4 w-4" />
              浏览工具市场
            </Link>
          </Button>
        </div>
      </div>
      
      {loading ? (
        // 加载状态
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          <ToolCardSkeleton count={3} />
        </div>
      ) : error ? (
        // 错误状态
        <div className="text-center py-10 bg-red-50 rounded-lg border border-red-100">
          <div className="text-red-500 mb-4">{error}</div>
          <Button variant="outline" onClick={() => window.location.reload()}>
            重试
          </Button>
        </div>
      ) : tools.length === 0 ? (
        // 空状态
        <EmptyToolsState 
          icon={Wrench}
          title="暂无推荐工具"
          description="前往工具市场探索更多工具"
          iconClassName="bg-green-50 p-3 rounded-full text-green-500"
          action={{
            label: "浏览工具市场",
            href: "/tools-market",
            icon: Wrench
          }}
        />
      ) : (
        // 工具列表
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-4 gap-4">
          {/* 只显示前10个工具作为推荐 */}
          {tools.slice(0, 10).map((tool) => (
            <MarketToolCard
              key={tool.id}
              tool={tool}
              onInstallClick={onInstallClick}
            />
          ))}
        </div>
      )}
    </div>
  );
} 