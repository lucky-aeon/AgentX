"use client"

import { AlertTriangle } from "lucide-react"
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
import { ApiKeyResponse } from "@/types/api-key"

interface DeleteApiKeyDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  apiKey: ApiKeyResponse | null
  onConfirm: () => Promise<void>
  loading: boolean
}

export function DeleteApiKeyDialog({
  open,
  onOpenChange,
  apiKey,
  onConfirm,
  loading
}: DeleteApiKeyDialogProps) {
  if (!apiKey) return null

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("zh-CN")
  }

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

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-red-500" />
            删除 API 密钥
          </DialogTitle>
          <DialogDescription>
            您确定要删除这个 API 密钥吗？此操作无法撤销。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          <div className="bg-muted p-4 rounded-md space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">密钥名称：</span>
              <span className="text-sm">{apiKey.name}</span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">关联 Agent：</span>
              <span className="text-sm">{apiKey.agentName || apiKey.agentId}</span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">状态：</span>
              {getStatusBadge(apiKey.status)}
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">使用次数：</span>
              <span className="text-sm">{apiKey.usageCount.toLocaleString()}</span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">创建时间：</span>
              <span className="text-sm">{formatDate(apiKey.createdAt)}</span>
            </div>
            
            {apiKey.lastUsedAt && (
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">最后使用：</span>
                <span className="text-sm">{formatDate(apiKey.lastUsedAt)}</span>
              </div>
            )}
          </div>

          <div className="bg-red-50 border border-red-200 p-3 rounded-md">
            <p className="text-sm text-red-800 font-medium mb-2">⚠️ 删除后果：</p>
            <ul className="text-sm text-red-700 space-y-1">
              <li>• 使用此密钥的所有应用程序将立即失去访问权限</li>
              <li>• API 调用将返回 401 未授权错误</li>
              <li>• 此操作无法撤销，需要重新创建密钥</li>
              {apiKey.usageCount > 0 && (
                <li>• 该密钥已被使用 {apiKey.usageCount} 次，可能影响正在运行的服务</li>
              )}
            </ul>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={loading}
          >
            取消
          </Button>
          <Button
            variant="destructive"
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? "删除中..." : "确认删除"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}