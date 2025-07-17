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
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground overflow-hidden">
              {ragMarket.icon ? (
                <img
                  src={ragMarket.icon}
                  alt={ragMarket.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Book className="h-4 w-4" />
              )}
            </div>
            <CardTitle className="text-base">{ragMarket.name}</CardTitle>
          </div>
          {ragMarket.isInstalled && (
            <Badge variant="default" className="text-xs">
              <Check className="mr-1 h-3 w-3" />
              已安装
            </Badge>
          )}
        </div>
        <CardDescription className="text-xs">
          <div className="flex items-center gap-2">
            <Avatar className="h-4 w-4">
              <AvatarImage src={ragMarket.userAvatar} />
              <AvatarFallback className="text-xs">
                {ragMarket.userNickname?.[0] || "U"}
              </AvatarFallback>
            </Avatar>
            <span>{ragMarket.userNickname}</span>
            <span>•</span>
            <span>发布于 {formatDateTime(ragMarket.publishedAt)}</span>
          </div>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-3 line-clamp-2">
          {ragMarket.description || "无描述"}
        </p>
        
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
        <div className="flex items-center gap-2 mb-2">
          <Badge variant="secondary">
            v{ragMarket.version}
          </Badge>
          <Badge variant="outline">
            <FileText className="mr-1 h-3 w-3" />
            {ragMarket.fileCount} 文件
          </Badge>
          <Badge variant="outline">
            <Download className="mr-1 h-3 w-3" />
            {ragMarket.installCount} 安装
          </Badge>
        </div>
        
        {/* 文件大小 */}
        <div className="text-xs text-muted-foreground">
          大小: {ragMarket.totalSizeDisplay || formatFileSize(ragMarket.totalSize)}
        </div>
      </CardContent>
      <CardFooter className="flex justify-between">
        <div className="flex items-center gap-2">
          {ragMarket.rating && (
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              <span>⭐</span>
              <span>{ragMarket.rating.toFixed(1)}</span>
              {ragMarket.reviewCount && (
                <span>({ragMarket.reviewCount})</span>
              )}
            </div>
          )}
        </div>
        <div className="flex gap-2">
          {onViewDetails && (
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => onViewDetails(ragMarket)}
            >
              <Eye className="mr-2 h-4 w-4" />
              详情
            </Button>
          )}
          {onInstall && (
            <Button 
              size="sm"
              onClick={() => onInstall(ragMarket)}
              disabled={ragMarket.isInstalled}
            >
              <Download className="mr-2 h-4 w-4" />
              {ragMarket.isInstalled ? "已安装" : "安装"}
            </Button>
          )}
        </div>
      </CardFooter>
    </Card>
  )
}