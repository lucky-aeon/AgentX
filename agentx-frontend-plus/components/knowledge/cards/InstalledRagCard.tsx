"use client"

import { Book, MoreHorizontal, Trash, User, History, RefreshCw } from "lucide-react"
import { useMemo, useState, useEffect } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"

import type { UserRagDTO, RagVersionDTO } from "@/types/rag-publish"
import { getRagVersionHistory, switchRagVersionWithToast } from "@/lib/rag-publish-service"

interface InstalledRagCardProps {
  userRag: UserRagDTO
  onUninstall?: (userRag: UserRagDTO) => void
  onCardClick?: (userRag: UserRagDTO) => void
  onVersionSwitch?: (updatedUserRag: UserRagDTO) => void
  currentUserId?: string | null
}

export function InstalledRagCard({ 
  userRag, 
  onUninstall, 
  onCardClick,
  onVersionSwitch,
  currentUserId 
}: InstalledRagCardProps) {
  // 版本相关状态
  const [availableVersions, setAvailableVersions] = useState<RagVersionDTO[]>([])
  const [isSwitchingVersion, setIsSwitchingVersion] = useState(false)
  const [versionsLoading, setVersionsLoading] = useState(false)
  const [versionsLoaded, setVersionsLoaded] = useState(false)

  // 判断是否为用户自己的知识库
  const isOwner = useMemo(() => {
    return currentUserId && userRag.creatorId === currentUserId
  }, [currentUserId, userRag.creatorId])

  // 获取可用版本列表
  const loadVersions = async () => {
    if (!userRag?.originalRagId || versionsLoaded) {
      return
    }
    
    setVersionsLoading(true)
    try {
      const response = await getRagVersionHistory(userRag.originalRagId)
      if (response.code === 200) {
        setAvailableVersions(response.data)
        setVersionsLoaded(true)
      }
    } catch (error) {
      console.error("获取版本历史失败:", error)
    } finally {
      setVersionsLoading(false)
    }
  }

  // 处理版本切换
  const handleVersionSwitch = async (targetVersionId: string) => {
    if (!userRag?.id || targetVersionId === userRag.ragVersionId) {
      return
    }
    
    setIsSwitchingVersion(true)
    try {
      const response = await switchRagVersionWithToast(userRag.id, targetVersionId)
      if (response.code === 200) {
        onVersionSwitch?.(response.data)
      }
    } catch (error) {
      console.error("版本切换失败:", error)
    } finally {
      setIsSwitchingVersion(false)
    }
  }

  return (
    <Card 
      className="relative overflow-hidden hover:shadow-md transition-all duration-300 border min-h-[180px] border-gray-100"
    >
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-0" onClick={() => onCardClick?.(userRag)} style={{ cursor: 'pointer' }}>
            <div className="flex h-12 w-12 items-center justify-center rounded-md text-primary-foreground overflow-hidden bg-primary/10">
              {userRag.icon ? (
                <img src={userRag.icon} alt={userRag.name} className="h-full w-full object-cover" />
              ) : (
                <Book className="h-6 w-6" />
              )}
            </div>
            <div className="w-[calc(100%-60px)] min-w-0">
              <h3 className="font-semibold line-clamp-1 truncate text-ellipsis overflow-hidden whitespace-nowrap max-w-full">{userRag.name}</h3>
              
              {/* 作者信息 */}
              {userRag.creatorNickname && (
                <p className="text-sm text-muted-foreground mt-1">
                  {isOwner ? "我创建的" : userRag.creatorNickname}
                </p>
              )}
            </div>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-8 w-8">
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">更多选项</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              {/* 版本切换菜单 */}
              <DropdownMenuSub>
                <DropdownMenuSubTrigger 
                  onPointerEnter={() => {
                    // 悬停时自动加载版本列表
                    if (!versionsLoaded && !versionsLoading) {
                      loadVersions()
                    }
                  }}
                >
                  <History className="mr-2 h-4 w-4" />
                  切换版本
                </DropdownMenuSubTrigger>
                <DropdownMenuSubContent>
                  {versionsLoading ? (
                    <DropdownMenuItem disabled>
                      <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
                      加载中...
                    </DropdownMenuItem>
                  ) : availableVersions.length <= 1 ? (
                    <DropdownMenuItem disabled>
                      仅有当前版本
                    </DropdownMenuItem>
                  ) : (
                    availableVersions.map((version) => (
                      <DropdownMenuItem
                        key={version.id}
                        disabled={version.id === userRag.ragVersionId || isSwitchingVersion}
                        onClick={() => handleVersionSwitch(version.id)}
                      >
                        <div className="flex items-center gap-2 w-full">
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
                      </DropdownMenuItem>
                    ))
                  )}
                </DropdownMenuSubContent>
              </DropdownMenuSub>

              <DropdownMenuSeparator />

              {/* 所有知识库都显示卸载选项 */}
              {onUninstall && (
                <DropdownMenuItem 
                  className="text-red-600" 
                  onClick={() => onUninstall(userRag)}
                >
                  <Trash className="mr-2 h-4 w-4" />
                  卸载
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0" onClick={() => onCardClick?.(userRag)} style={{ cursor: 'pointer' }}>
        {userRag.description && (
          <div className="min-h-[40px] mb-3 line-clamp-2 text-sm">
            {userRag.description}
          </div>
        )}
        
        {/* 统计信息 */}
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center">
            <User className="mr-1 h-3 w-3" />
            <span>{userRag.creatorNickname || "未知作者"}</span>
          </div>
          <div className="flex items-center">
            <span>v{userRag.version}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}