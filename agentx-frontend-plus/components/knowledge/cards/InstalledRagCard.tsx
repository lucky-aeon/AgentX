"use client"

import { Book, MoreHorizontal, Trash, User, AlertTriangle } from "lucide-react"
import { useMemo } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"

import type { UserRagDTO } from "@/types/rag-publish"

interface InstalledRagCardProps {
  userRag: UserRagDTO
  onUninstall?: (userRag: UserRagDTO) => void
  onCardClick?: (userRag: UserRagDTO) => void
  currentUserId?: string | null
}

export function InstalledRagCard({ 
  userRag, 
  onUninstall, 
  onCardClick,
  currentUserId 
}: InstalledRagCardProps) {
  // 判断是否为用户自己的知识库
  const isOwner = useMemo(() => {
    return currentUserId && userRag.creatorId === currentUserId
  }, [currentUserId, userRag.creatorId])

  return (
    <Card 
      className={`relative overflow-hidden hover:shadow-md transition-all duration-300 border min-h-[180px] ${
        !userRag.isActive 
          ? 'border-gray-200 bg-gray-50/30 opacity-75' 
          : 'border-gray-100'
      }`}
    >
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3 flex-1 min-w-0" onClick={() => onCardClick?.(userRag)} style={{ cursor: 'pointer' }}>
            <div className={`flex h-12 w-12 items-center justify-center rounded-md text-primary-foreground overflow-hidden ${
              !userRag.isActive ? 'bg-gray-100' : 'bg-primary/10'
            }`}>
              {userRag.icon ? (
                <img src={userRag.icon} alt={userRag.name} className={`h-full w-full object-cover ${!userRag.isActive ? 'opacity-70' : ''}`} />
              ) : (
                <Book className="h-6 w-6" />
              )}
            </div>
            <div className="w-[calc(100%-60px)] min-w-0">
              <h3 className="font-semibold line-clamp-1 truncate text-ellipsis overflow-hidden whitespace-nowrap max-w-full">{userRag.name}</h3>
              
              {/* 状态标签 */}
              {!userRag.isActive && (
                <div className="mt-1">
                  <Badge variant="outline" className="text-gray-600 bg-gray-50 border-gray-200 text-xs flex items-center gap-1">
                    <AlertTriangle className="h-3 w-3" />
                    未激活
                  </Badge>
                </div>
              )}
              
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
              {/* 只有卸载选项，但用户自己的知识库不能卸载 */}
              {onUninstall && !isOwner && (
                <DropdownMenuItem 
                  className="text-red-600" 
                  onClick={() => onUninstall(userRag)}
                >
                  <Trash className="mr-2 h-4 w-4" />
                  卸载
                </DropdownMenuItem>
              )}
              {/* 用户自己的知识库显示提示 */}
              {isOwner && (
                <DropdownMenuItem disabled>
                  <AlertTriangle className="mr-2 h-4 w-4" />
                  自己的知识库无法卸载
                </DropdownMenuItem>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0" onClick={() => onCardClick?.(userRag)} style={{ cursor: 'pointer' }}>
        <div className="min-h-[40px] mb-3 line-clamp-2 text-sm">
          {userRag.description || "无描述"}
        </div>
        
        {/* 统计信息 */}
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center">
            <User className="mr-1 h-3 w-3" />
            <span>{userRag.creatorNickname || "未知作者"}</span>
          </div>
          <div className="flex items-center">
            <span className="mr-1">v{userRag.version}</span>
            {userRag.fileCount && (
              <span>{userRag.fileCount}个文件</span>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}