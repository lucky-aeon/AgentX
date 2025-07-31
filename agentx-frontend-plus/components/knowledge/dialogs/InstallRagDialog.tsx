"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Download, User, Calendar, FileText, Database } from "lucide-react"

import type { RagMarketDTO } from "@/types/rag-publish"
import { installRagVersionWithToast } from "@/lib/rag-publish-service"
import { formatFileSize, formatDateTime, getLabelColor } from "@/types/rag-publish"

interface InstallRagDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  ragMarket: RagMarketDTO | null
  onSuccess?: () => void
}

export function InstallRagDialog({ 
  open, 
  onOpenChange, 
  ragMarket, 
  onSuccess 
}: InstallRagDialogProps) {
  const [isInstalling, setIsInstalling] = useState(false)

  // 处理安装
  const handleInstall = async () => {
    if (!ragMarket) return

    setIsInstalling(true)

    try {
      const response = await installRagVersionWithToast({
        ragVersionId: ragMarket.id
      })
      
      if (response.code === 200) {
        onOpenChange(false)
        onSuccess?.()
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsInstalling(false)
    }
  }

  if (!ragMarket) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>安装知识库</DialogTitle>
          <DialogDescription>
            确认安装这个知识库到您的账户
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-4">
          {/* 基本信息 */}
          <div className="flex items-start gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary text-primary-foreground overflow-hidden">
              {ragMarket.icon ? (
                <img
                  src={ragMarket.icon}
                  alt={ragMarket.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Database className="h-6 w-6" />
              )}
            </div>
            <div className="flex-1">
              <h3 className="font-medium">{ragMarket.name}</h3>
              <p className="text-sm text-muted-foreground mt-1">
                {ragMarket.description || "无描述"}
              </p>
            </div>
          </div>

          {/* 作者信息 */}
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Avatar className="h-5 w-5">
              <AvatarImage src={ragMarket.userAvatar} />
              <AvatarFallback className="text-xs">
                {ragMarket.userNickname?.[0] || "U"}
              </AvatarFallback>
            </Avatar>
            <span>{ragMarket.userNickname}</span>
            <span>•</span>
            <Calendar className="h-4 w-4" />
            <span>{formatDateTime(ragMarket.publishedAt)}</span>
          </div>

          {/* 标签 */}
          {ragMarket.labels && ragMarket.labels.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {ragMarket.labels.map((label, index) => (
                <Badge 
                  key={label} 
                  variant="outline" 
                  className={`text-xs ${getLabelColor(index)}`}
                >
                  {label}
                </Badge>
              ))}
            </div>
          )}

          {/* 统计信息 */}
          <div className="grid grid-cols-2 gap-4 p-3 bg-muted/50 rounded-lg">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm">
                <Badge variant="secondary">v{ragMarket.version}</Badge>
                <span className="text-muted-foreground">版本</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <FileText className="h-4 w-4" />
                <span>{ragMarket.fileCount} 个文件</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-muted-foreground">
                  大小: {ragMarket.totalSizeDisplay || formatFileSize(ragMarket.totalSize)}
                </span>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-sm">
                <Download className="h-4 w-4" />
                <span>{ragMarket.installCount} 次安装</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <Database className="h-4 w-4" />
                <span>{ragMarket.documentCount} 个文档</span>
              </div>
              {ragMarket.rating && (
                <div className="flex items-center gap-2 text-sm">
                  <span>⭐ {ragMarket.rating.toFixed(1)}</span>
                  {ragMarket.reviewCount && (
                    <span className="text-muted-foreground">
                      ({ragMarket.reviewCount} 评价)
                    </span>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 安装说明 */}
          <div className="text-sm text-muted-foreground p-3 bg-blue-50 rounded-lg">
            <div className="font-medium mb-1">安装说明:</div>
            <ul className="list-disc list-inside space-y-1">
              <li>安装后可以在"我安装的知识库"中查看</li>
              <li>可以在对话中使用这个知识库</li>
              <li>随时可以卸载已安装的知识库</li>
            </ul>
          </div>
        </div>

        <DialogFooter>
          <Button 
            type="button" 
            variant="outline" 
            onClick={() => onOpenChange(false)}
          >
            取消
          </Button>
          <Button 
            onClick={handleInstall}
            disabled={isInstalling || ragMarket.isInstalled}
          >
            <Download className="mr-2 h-4 w-4" />
            {isInstalling ? "安装中..." : ragMarket.isInstalled ? "已安装" : "安装"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}