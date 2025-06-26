"use client"

import { useState } from "react"
import { Copy, Eye, EyeOff, MoreHorizontal, RotateCcw, Trash2, Power } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"
import { Skeleton } from "@/components/ui/skeleton"
import { ApiKeyResponse } from "@/types/api-key"

interface ApiKeyListProps {
  apiKeys: ApiKeyResponse[]
  loading: boolean
  onToggleStatus: (apiKey: ApiKeyResponse) => Promise<void>
  onResetKey: (apiKey: ApiKeyResponse) => Promise<void>
  onDeleteKey: (apiKey: ApiKeyResponse) => void
  operatingKeyId: string | null
}

export function ApiKeyList({
  apiKeys,
  loading,
  onToggleStatus,
  onResetKey,
  onDeleteKey,
  operatingKeyId
}: ApiKeyListProps) {
  const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set())

  // 复制API密钥到剪贴板
  const handleCopyKey = (apiKey: string) => {
    navigator.clipboard.writeText(apiKey)
    toast({
      title: "已复制到剪贴板",
      description: "API 密钥已复制到剪贴板",
    })
  }

  // 切换密钥可见性
  const toggleKeyVisibility = (keyId: string) => {
    const newVisibleKeys = new Set(visibleKeys)
    if (visibleKeys.has(keyId)) {
      newVisibleKeys.delete(keyId)
    } else {
      newVisibleKeys.add(keyId)
    }
    setVisibleKeys(newVisibleKeys)
  }

  // 格式化API密钥显示
  const formatApiKey = (key: string, isVisible: boolean) => {
    if (isVisible) {
      return key
    }
    // 显示前缀和后缀，中间用点号代替
    if (key.length > 16) {
      return `${key.substring(0, 8)}••••••••${key.substring(key.length - 8)}`
    }
    return "••••••••••••••••"
  }

  // 格式化日期
  const formatDate = (dateString?: string) => {
    if (!dateString) return "-"
    return new Date(dateString).toLocaleString("zh-CN")
  }

  // 获取状态徽章
  const getStatusBadge = (status: boolean) => {
    return status ? (
      <Badge variant="default" className="bg-green-500">
        已启用
      </Badge>
    ) : (
      <Badge variant="secondary">
        已禁用
      </Badge>
    )
  }

  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="flex items-center space-x-4">
            <Skeleton className="h-12 w-12 rounded" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-[250px]" />
              <Skeleton className="h-4 w-[200px]" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="border rounded-md">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>名称</TableHead>
            <TableHead>API密钥</TableHead>
            <TableHead>关联Agent</TableHead>
            <TableHead>状态</TableHead>
            <TableHead>使用次数</TableHead>
            <TableHead>最后使用</TableHead>
            <TableHead>创建时间</TableHead>
            <TableHead className="text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {apiKeys.map((apiKey) => {
            const isVisible = visibleKeys.has(apiKey.id)
            const isOperating = operatingKeyId === apiKey.id

            return (
              <TableRow key={apiKey.id}>
                <TableCell className="font-medium">
                  {apiKey.name}
                </TableCell>
                <TableCell>
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-sm">
                      {formatApiKey(apiKey.apiKey, isVisible)}
                    </span>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => toggleKeyVisibility(apiKey.id)}
                      className="h-8 w-8"
                    >
                      {isVisible ? (
                        <EyeOff className="h-4 w-4" />
                      ) : (
                        <Eye className="h-4 w-4" />
                      )}
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleCopyKey(apiKey.apiKey)}
                      className="h-8 w-8"
                    >
                      <Copy className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
                <TableCell>
                  {apiKey.agentName || apiKey.agentId}
                </TableCell>
                <TableCell>
                  {getStatusBadge(apiKey.status)}
                </TableCell>
                <TableCell>
                  {apiKey.usageCount.toLocaleString()}
                </TableCell>
                <TableCell>
                  {formatDate(apiKey.lastUsedAt)}
                </TableCell>
                <TableCell>
                  {formatDate(apiKey.createdAt)}
                </TableCell>
                <TableCell className="text-right">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        disabled={isOperating}
                      >
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        onClick={() => onToggleStatus(apiKey)}
                        disabled={isOperating}
                      >
                        <Power className="mr-2 h-4 w-4" />
                        {apiKey.status ? "禁用" : "启用"}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => onResetKey(apiKey)}
                        disabled={isOperating}
                      >
                        <RotateCcw className="mr-2 h-4 w-4" />
                        重置密钥
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        onClick={() => onDeleteKey(apiKey)}
                        disabled={isOperating}
                        className="text-red-600"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        删除
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}