"use client"

import { Book, Download, Eye, User, Calendar, FileText, Check } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"

import type { RagMarketDTO } from "@/types/rag-publish"
import { formatFileSize, formatDateTime, getLabelColor } from "@/types/rag-publish"

interface MarketRagCardProps {
  ragMarket: RagMarketDTO
  onInstall?: (ragMarket: RagMarketDTO) => void
  onViewDetails?: (ragMarket: RagMarketDTO) => void
}

export function MarketRagCard({ 
  ragMarket, 
  onInstall, 
  onViewDetails 
}: MarketRagCardProps) {
  return (
    <Card className="group relative overflow-hidden border border-gray-100 hover:shadow-md transition-all duration-300 min-h-[180px]">
      {ragMarket.isInstalled && (
        <div className="absolute top-2 right-2 z-10">
          <Badge variant="default" className="text-xs">
            <Check className="mr-1 h-3 w-3" />
            已安装
          </Badge>
        </div>
      )}
      
      <CardHeader className="pb-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-md bg-gray-100 text-primary-foreground overflow-hidden">
            {ragMarket.icon ? (
              <img
                src={ragMarket.icon}
                alt={ragMarket.name}
                className="h-full w-full object-cover"
              />
            ) : (
              <Book className="h-6 w-6" />
            )}
          </div>
          <div className="w-[calc(100%-60px)]">
            <h3 className="font-semibold line-clamp-1">{ragMarket.name}</h3>
            <p className="text-sm text-muted-foreground line-clamp-1">
              {ragMarket.description || "无描述"}
            </p>
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        {/* 标签 */}
        {ragMarket.labels && ragMarket.labels.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {ragMarket.labels.slice(0, 3).map((label, index) => (
              <Badge 
                key={label} 
                variant="outline" 
                className={`text-xs ${getLabelColor(index)}`}
              >
                {label}
              </Badge>
            ))}
            {ragMarket.labels.length > 3 && (
              <Badge variant="outline" className="text-xs">
                +{ragMarket.labels.length - 3}
              </Badge>
            )}
          </div>
        )}
        
        {/* 统计信息 */}
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center">
            <User className="mr-1 h-3 w-3" />
            <span>{ragMarket.userNickname}</span>
          </div>
          <div className="flex items-center">
            <Download className="mr-1 h-3 w-3" />
            <span>{ragMarket.installCount} 安装</span>
          </div>
        </div>
      </CardContent>
      
      {/* 悬停遮罩层 */}
      <div className="absolute inset-0 flex items-center justify-center bg-white/90 opacity-0 transition-opacity group-hover:opacity-100">
        <div className="flex gap-2">
          {onInstall && (
            <Button 
              size="sm"
              className="shadow-sm"
              onClick={() => onInstall(ragMarket)}
              disabled={ragMarket.isInstalled}
            >
              <Download className="mr-2 h-4 w-4" />
              {ragMarket.isInstalled ? "已安装" : "安装"}
            </Button>
          )}
          {onViewDetails && (
            <Button 
              variant="outline" 
              size="sm"
              className="bg-white"
              onClick={() => onViewDetails(ragMarket)}
            >
              <Eye className="mr-2 h-4 w-4" />
              详情
            </Button>
          )}
        </div>
      </div>
    </Card>
  )
}