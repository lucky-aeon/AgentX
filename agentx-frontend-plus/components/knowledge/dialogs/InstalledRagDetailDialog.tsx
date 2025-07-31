"use client"

import { Book, User, FileText, Trash, FolderOpen, Eye, MessageSquare, RefreshCw, History } from "lucide-react"
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

import type { UserRagDTO, RagVersionDTO } from "@/types/rag-publish"
import { SimpleFileBrowserDialog } from "./SimpleFileBrowserDialog"
import { InstalledRagChatDialog } from "./InstalledRagChatDialog"
import { getInstalledRagFilesWithToast } from "@/lib/rag-publish-service"
import { getRagVersionHistory, switchRagVersionWithToast } from "@/lib/rag-publish-service"

interface InstalledRagDetailDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  userRag: UserRagDTO | null
  onUninstall?: (userRag: UserRagDTO) => void
  onVersionSwitch?: (updatedUserRag: UserRagDTO) => void
  currentUserId?: string | null
}

export function InstalledRagDetailDialog({
  open,
  onOpenChange,
  userRag,
  onUninstall,
  onVersionSwitch,
  currentUserId
}: InstalledRagDetailDialogProps) {
  // 子对话框状态
  const [fileBrowserOpen, setFileBrowserOpen] = useState(false)
  const [chatDialogOpen, setChatDialogOpen] = useState(false)
  
  // 实时文件数量
  const [realTimeFileCount, setRealTimeFileCount] = useState<number | null>(null)
  
  // 版本相关状态
  const [availableVersions, setAvailableVersions] = useState<RagVersionDTO[]>([])
  const [selectedVersionId, setSelectedVersionId] = useState<string>("")
  const [isSwitchingVersion, setIsSwitchingVersion] = useState(false)
  const [versionsLoading, setVersionsLoading] = useState(false)

  // 判断是否为用户自己的知识库
  const isOwner = useMemo(() => {
    return currentUserId && userRag?.creatorId === currentUserId
  }, [currentUserId, userRag?.creatorId])

  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }
  
  // 获取实时文件数量 - 当对话框打开或版本切换时更新
  useEffect(() => {
    const fetchFileCount = async () => {
      if (!open || !userRag?.id) {
        setRealTimeFileCount(null)
        return
      }
      
      try {
        const response = await getInstalledRagFilesWithToast(userRag.id)
        if (response.code === 200) {
          setRealTimeFileCount(response.data.length)
        } else {
          // API失败时，使用后端返回的统计数据
          setRealTimeFileCount(userRag.fileCount || 0)
        }
      } catch (error) {
        console.error("获取文件数量失败:", error)
        // 失败时使用后端返回的统计数据
        setRealTimeFileCount(userRag.fileCount || 0)
      }
    }
    
    fetchFileCount()
  }, [open, userRag?.id, userRag?.ragVersionId]) // 添加ragVersionId依赖，版本切换时重新获取

  // 获取可用版本列表
  useEffect(() => {
    const fetchVersions = async () => {
      if (!open || !userRag?.originalRagId) {
        return
      }
      
      setVersionsLoading(true)
      try {
        const response = await getRagVersionHistory(userRag.originalRagId)
        if (response.code === 200) {
          setAvailableVersions(response.data)
          setSelectedVersionId(userRag.ragVersionId || "")
        }
      } catch (error) {
        console.error("获取版本历史失败:", error)
      } finally {
        setVersionsLoading(false)
      }
    }
    
    fetchVersions()
  }, [open, userRag?.originalRagId, userRag?.ragVersionId])

  // 处理版本切换
  const handleVersionSwitch = async (targetVersionId: string) => {
    if (!userRag?.id || targetVersionId === userRag.ragVersionId) {
      return
    }
    
    setIsSwitchingVersion(true)
    try {
      const response = await switchRagVersionWithToast(userRag.id, targetVersionId)
      if (response.code === 200) {
        // 切换成功，通知父组件更新状态
        setSelectedVersionId(targetVersionId)
        onVersionSwitch?.(response.data)
        
        // useEffect会因为userRag更新而自动重新获取最新的文件数量
        
        // 版本切换成功后不关闭对话框，让用户继续查看切换后的信息
      }
    } catch (error) {
      console.error("版本切换失败:", error)
    } finally {
      setIsSwitchingVersion(false)
    }
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

          {/* 版本切换 */}
          {availableVersions.length > 1 && (
            <div className="space-y-2">
              <div className="text-sm font-medium flex items-center gap-2">
                <History className="h-4 w-4" />
                版本切换
              </div>
              <div className="flex items-center gap-2">
                <Select
                  value={selectedVersionId}
                  onValueChange={handleVersionSwitch}
                  disabled={isSwitchingVersion || versionsLoading}
                >
                  <SelectTrigger className="flex-1">
                    <SelectValue>
                      {versionsLoading ? (
                        "加载版本中..."
                      ) : (
                        availableVersions.find(v => v.id === selectedVersionId)?.version 
                          ? `v${availableVersions.find(v => v.id === selectedVersionId)?.version}`
                          : "选择版本"
                      )}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    {availableVersions.map((version) => (
                      <SelectItem key={version.id} value={version.id}>
                        <div className="flex items-center gap-2">
                          <span>v{version.version}</span>
                          {version.version === "0.0.1" && (
                            <Badge variant="secondary" className="text-xs">
                              私有
                            </Badge>
                          )}
                          {version.id === userRag.ragVersionId && (
                            <Badge variant="outline" className="text-xs">
                              当前
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {isSwitchingVersion && (
                  <RefreshCw className="h-4 w-4 animate-spin text-muted-foreground" />
                )}
              </div>
              <div className="text-xs text-muted-foreground">
                {availableVersions.length} 个可用版本
              </div>
            </div>
          )}

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