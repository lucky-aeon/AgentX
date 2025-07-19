"use client"

import { Book, User, FileText, Trash, FolderOpen, Eye, MessageSquare } from "lucide-react"
import { useMemo, useState, useEffect } from "react"

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

import type { UserRagDTO } from "@/types/rag-publish"
import { SimpleFileBrowserDialog } from "./SimpleFileBrowserDialog"
import { InstalledRagChatDialog } from "./InstalledRagChatDialog"
import { getAllDatasetFilesWithToast } from "@/lib/rag-dataset-service"

interface InstalledRagDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  userRag: UserRagDTO | null
  onUninstall?: (userRag: UserRagDTO) => void
  currentUserId?: string | null
}

export function InstalledRagDetailDialog({
  open,
  onOpenChange,
  userRag,
  onUninstall,
  currentUserId
}: InstalledRagDetailDialogProps) {
  // 子对话框状态
  const [fileBrowserOpen, setFileBrowserOpen] = useState(false)
  const [chatDialogOpen, setChatDialogOpen] = useState(false)
  
  // 实时文件数量
  const [realTimeFileCount, setRealTimeFileCount] = useState<number | null>(null)

  // 判断是否为用户自己的知识库
  const isOwner = useMemo(() => {
    return currentUserId && userRag?.creatorId === currentUserId
  }, [currentUserId, userRag?.creatorId])

  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }
  
  // 获取实时文件数量
  useEffect(() => {
    const fetchFileCount = async () => {
      if (!open || !userRag?.originalRagId) {
        return
      }
      
      try {
        const response = await getAllDatasetFilesWithToast(userRag.originalRagId)
        if (response.code === 200) {
          setRealTimeFileCount(response.data.length)
        }
      } catch (error) {
        console.error("获取文件数量失败:", error)
        // 失败时使用原有的fileCount
        setRealTimeFileCount(userRag.fileCount || 0)
      }
    }
    
    fetchFileCount()
  }, [open, userRag?.originalRagId, userRag?.fileCount])

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
          {/* 安装信息 */}
          <div className="grid grid-cols-2 gap-4">
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
                  {realTimeFileCount !== null ? realTimeFileCount : (userRag.fileCount || 0)} 个文件
                </span>
              </div>
            </div>
          </div>


        </div>

        {/* 功能按钮区域 */}
        <div className="border-t pt-4">
          <div className="grid grid-cols-3 gap-3">
            <Button
              variant="outline"
              onClick={() => setFileBrowserOpen(true)}
              className="flex items-center gap-2"
            >
              <FolderOpen className="h-4 w-4" />
              文件浏览
            </Button>
            <Button
              variant="outline"
              onClick={() => setFileBrowserOpen(true)}
              className="flex items-center gap-2"
            >
              <Eye className="h-4 w-4" />
              文档查看
            </Button>
            <Button
              variant="outline"
              onClick={() => setChatDialogOpen(true)}
              className="flex items-center gap-2"
            >
              <MessageSquare className="h-4 w-4" />
              RAG对话
            </Button>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
          {onUninstall && (
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

      {/* 文件浏览对话框 */}
      <SimpleFileBrowserDialog
        open={fileBrowserOpen}
        onOpenChange={setFileBrowserOpen}
        userRag={userRag}
      />

      {/* RAG对话框 */}
      <InstalledRagChatDialog
        open={chatDialogOpen}
        onOpenChange={setChatDialogOpen}
        userRag={userRag}
      />
    </Dialog>
  )
}