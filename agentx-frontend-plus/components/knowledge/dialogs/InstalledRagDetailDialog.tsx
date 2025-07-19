"use client"

import { Book, User, Calendar, FileText, Power, PowerOff, Trash, AlertTriangle } from "lucide-react"
import { useMemo } from "react"

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
import { Switch } from "@/components/ui/switch"

import type { UserRagDTO } from "@/types/rag-publish"

interface InstalledRagDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  userRag: UserRagDTO | null
  onToggleActive?: (userRag: UserRagDTO, isActive: boolean) => void
  onUninstall?: (userRag: UserRagDTO) => void
  currentUserId?: string | null
}

export function InstalledRagDetailDialog({
  open,
  onOpenChange,
  userRag,
  onToggleActive,
  onUninstall,
  currentUserId
}: InstalledRagDetailDialogProps) {
  // 判断是否为用户自己的知识库
  const isOwner = useMemo(() => {
    return currentUserId && userRag?.creatorId === currentUserId
  }, [currentUserId, userRag?.creatorId])

  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  if (!userRag) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10 text-primary overflow-hidden">
              {userRag.icon ? (
                <img
                  src={userRag.icon}
                  alt={userRag.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Book className="h-6 w-6" />
              )}
            </div>
            <div>
              <div className="font-semibold">{userRag.name}</div>
              <div className="text-sm font-normal text-muted-foreground">
                v{userRag.version}
              </div>
            </div>
          </DialogTitle>
          <DialogDescription>
            {userRag.description || "无描述"}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* 状态信息 */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="text-sm font-medium">状态</div>
              <div className="flex items-center gap-2">
                <Badge variant={userRag.isActive ? "default" : "secondary"}>
                  {userRag.isActive ? "已激活" : "未激活"}
                </Badge>
                {!userRag.isActive && (
                  <span className="text-xs text-muted-foreground">
                    未激活的知识库无法在对话中使用
                  </span>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <div className="text-sm font-medium">安装时间</div>
              <div className="text-sm text-muted-foreground">
                {formatDate(userRag.installedAt)}
              </div>
            </div>
          </div>

          <div className="border-t" />

          {/* 知识库信息 */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <div className="text-sm font-medium">创建者</div>
              <div className="flex items-center gap-2">
                <User className="h-4 w-4" />
                <span className="text-sm text-muted-foreground">
                  {isOwner ? "我创建的" : userRag.creatorNickname || "未知作者"}
                </span>
              </div>
            </div>
            <div className="space-y-2">
              <div className="text-sm font-medium">文件数量</div>
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4" />
                <span className="text-sm text-muted-foreground">
                  {userRag.fileCount || 0} 个文件
                </span>
              </div>
            </div>
          </div>

          {/* 用户自己的知识库提示 */}
          {isOwner && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4">
              <div className="flex items-center gap-2 text-amber-700">
                <AlertTriangle className="h-4 w-4" />
                <span className="font-medium">这是您创建的知识库</span>
              </div>
              <p className="text-sm text-amber-600 mt-1">
                您创建的知识库无法卸载，但可以停用。如需删除，请前往"我创建的知识库"页面。
              </p>
            </div>
          )}

          {/* 状态切换 */}
          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2">
              <Power className="h-4 w-4" />
              <span className="font-medium">启用状态</span>
            </div>
            <div className="flex items-center gap-2">
              <Switch
                checked={userRag.isActive}
                onCheckedChange={(checked) => onToggleActive?.(userRag, checked)}
              />
              <span className="text-sm text-muted-foreground">
                {userRag.isActive ? "已激活" : "未激活"}
              </span>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          {!isOwner && onUninstall && (
            <Button
              variant="destructive"
              onClick={() => {
                onUninstall(userRag)
                onOpenChange(false)
              }}
            >
              <Trash className="mr-2 h-4 w-4" />
              卸载知识库
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}