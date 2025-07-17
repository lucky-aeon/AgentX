"use client"

import { Book, MoreHorizontal, Trash, Eye, Power, PowerOff } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"
import { Switch } from "@/components/ui/switch"

import type { UserRagDTO } from "@/types/rag-publish"

interface InstalledRagCardProps {
  userRag: UserRagDTO
  onUninstall?: (userRag: UserRagDTO) => void
  onToggleActive?: (userRag: UserRagDTO, isActive: boolean) => void
  onViewDetails?: (userRag: UserRagDTO) => void
}

export function InstalledRagCard({ 
  userRag, 
  onUninstall, 
  onToggleActive, 
  onViewDetails 
}: InstalledRagCardProps) {
  // 格式化时间
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN')
  }

  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground overflow-hidden">
              {userRag.icon ? (
                <img
                  src={userRag.icon}
                  alt={userRag.name}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Book className="h-4 w-4" />
              )}
            </div>
            <CardTitle className="text-base">{userRag.name}</CardTitle>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">打开菜单</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>操作</DropdownMenuLabel>
              <DropdownMenuSeparator />
              {onViewDetails && (
                <DropdownMenuItem onClick={() => onViewDetails(userRag)}>
                  <Eye className="mr-2 h-4 w-4" />
                  查看详情
                </DropdownMenuItem>
              )}
              {onToggleActive && (
                <DropdownMenuItem onClick={() => onToggleActive(userRag, !userRag.isActive)}>
                  {userRag.isActive ? (
                    <>
                      <PowerOff className="mr-2 h-4 w-4" />
                      停用
                    </>
                  ) : (
                    <>
                      <Power className="mr-2 h-4 w-4" />
                      激活
                    </>
                  )}
                </DropdownMenuItem>
              )}
              {onUninstall && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem 
                    className="text-red-600" 
                    onClick={() => onUninstall(userRag)}
                  >
                    <Trash className="mr-2 h-4 w-4" />
                    卸载
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
        <CardDescription className="text-xs">
          安装于 {formatDate(userRag.installedAt)}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-3">
          {userRag.description || "无描述"}
        </p>
        <div className="flex items-center gap-2 mb-2">
          <Badge variant="secondary">
            v{userRag.version}
          </Badge>
          {userRag.fileCount && (
            <Badge variant="outline">
              {userRag.fileCount} 个文件
            </Badge>
          )}
          <Badge variant={userRag.isActive ? "default" : "secondary"}>
            {userRag.isActive ? "已激活" : "未激活"}
          </Badge>
        </div>
        {userRag.creatorNickname && (
          <div className="text-xs text-muted-foreground">
            作者: {userRag.creatorNickname}
          </div>
        )}
      </CardContent>
      <CardFooter className="flex justify-between">
        <div className="flex items-center gap-2">
          <Switch 
            checked={userRag.isActive}
            onCheckedChange={(checked) => onToggleActive?.(userRag, checked)}
          />
          <span className="text-sm">
            {userRag.isActive ? "已激活" : "未激活"}
          </span>
        </div>
        <div className="flex gap-2">
          {onViewDetails && (
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => onViewDetails(userRag)}
            >
              <Eye className="mr-2 h-4 w-4" />
              详情
            </Button>
          )}
          {onUninstall && (
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => onUninstall(userRag)}
            >
              <Trash className="mr-2 h-4 w-4" />
              卸载
            </Button>
          )}
        </div>
      </CardFooter>
    </Card>
  )
}