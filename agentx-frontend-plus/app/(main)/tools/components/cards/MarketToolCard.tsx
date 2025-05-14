import { MarketTool } from "../../utils/types";
import { Card, CardHeader, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Download, Info, User, Wrench } from "lucide-react";
import { ToolLabels } from "../shared/ToolLabels";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";

interface MarketToolCardProps {
  tool: MarketTool;
  onInstallClick: (tool: MarketTool) => void;
}

export function MarketToolCard({ tool, onInstallClick }: MarketToolCardProps) {
  return (
    <Card className="group relative overflow-hidden border border-gray-100 hover:shadow-md transition-all duration-300 min-h-[180px]">
      {tool.is_office && (
        <div className="absolute top-2 right-2 z-10">
          <Badge className="flex items-center gap-1 bg-gradient-to-r from-amber-500 to-orange-500 text-white border-0">
            官方
          </Badge>
        </div>
      )}
      <CardHeader className="pb-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-md bg-gray-100 text-primary-foreground overflow-hidden">
            {tool.icon ? (
              <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
            ) : (
              <Wrench className="h-6 w-6" />
            )}
          </div>
          <div className="w-[calc(100%-60px)]">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold line-clamp-1">{tool.name}</h3>
            </div>
            <p className="text-sm text-muted-foreground line-clamp-1">{tool.subtitle}</p>
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        <ToolLabels 
          labels={tool.labels} 
          filterOfficial={true}
          limit={5}
        />
        
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center">
            <User className="mr-1 h-3 w-3" />
            <span>{tool.author}</span>
          </div>
          <div className="flex items-center">
            <Download className="mr-1 h-3 w-3" />
            <span>{tool.installCount} 安装</span>
          </div>
        </div>
      </CardContent>
      
      <div className="absolute inset-0 flex items-center justify-center bg-white/90 opacity-0 transition-opacity group-hover:opacity-100">
        <div className="flex gap-2">
          <Button 
            size="sm"
            className="shadow-sm"
            onClick={() => onInstallClick(tool)}
          >
            <Download className="mr-2 h-4 w-4" />
            安装
          </Button>
          <Button 
            variant="outline" 
            size="sm"
            className="bg-white"
            asChild
          >
            <Link href={`/tools/${tool.toolId || tool.id}/${tool.current_version || '0.0.1'}`}>
              <Info className="mr-2 h-4 w-4" />
              详情
            </Link>
          </Button>
        </div>
      </div>
    </Card>
  );
} 