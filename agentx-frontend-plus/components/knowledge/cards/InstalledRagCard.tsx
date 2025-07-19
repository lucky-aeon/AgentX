"use client"

import { Book, MoreHorizontal, Trash, User } from "lucide-react"
import { useMemo } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

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