"use client"

import { useState } from "react"
import { Copy, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { toast } from "@/hooks/use-toast"
import { ApiKeyResponse } from "@/types/api-key"
import type { Agent } from "@/types/agent"

interface CreateApiKeyDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  agents: Agent[]
  onCreateKey: (agentId: string, name: string) => Promise<ApiKeyResponse | null>
}

export function CreateApiKeyDialog({
  open,
  onOpenChange,
  agents,
  onCreateKey
}: CreateApiKeyDialogProps) {
  const [selectedAgentId, setSelectedAgentId] = useState<string>("")
  const [keyName, setKeyName] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [createdKey, setCreatedKey] = useState<ApiKeyResponse | null>(null)
  const [showKey, setShowKey] = useState(false)

  // 重置表单
  const resetForm = () => {
    setSelectedAgentId("")
    setKeyName("")
    setCreatedKey(null)
    setShowKey(false)
    setIsLoading(false)
  }

  // 处理对话框关闭
  const handleOpenChange = (open: boolean) => {
    if (!open) {
      resetForm()
    }
    onOpenChange(open)
  }

  // 创建API密钥
  const handleCreate = async () => {
    if (!selectedAgentId || !keyName.trim()) {
      toast({
        title: "请填写完整信息",
        description: "请选择Agent并输入密钥名称",
        variant: "destructive",
      })
      return
    }

    try {
      setIsLoading(true)
      const result = await onCreateKey(selectedAgentId, keyName.trim())
      
      if (result) {
        setCreatedKey(result)
        toast({
          title: "API 密钥已创建",
          description: "请保存您的 API 密钥，它只会显示一次。",
        })
      }
    } catch (error) {
      console.error("创建API密钥失败:", error)
    } finally {
      setIsLoading(false)
    }
  }

  // 复制API密钥
  const handleCopyKey = () => {
    if (createdKey) {
      navigator.clipboard.writeText(createdKey.apiKey)
      toast({
        title: "已复制到剪贴板",
        description: "API 密钥已复制到剪贴板。",
      })
    }
  }

  // 完成创建
  const handleFinish = () => {
    resetForm()
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>创建 API 密钥</DialogTitle>
          <DialogDescription>
            创建一个新的 API 密钥以访问 AgentX API。
          </DialogDescription>
        </DialogHeader>

        {!createdKey ? (
          // 创建表单
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="agent-select">选择 Agent</Label>
              <Select
                value={selectedAgentId}
                onValueChange={setSelectedAgentId}
                disabled={isLoading}
              >
                <SelectTrigger id="agent-select">
                  <SelectValue placeholder="请选择一个 Agent" />
                </SelectTrigger>
                <SelectContent>
                  {agents.map((agent) => (
                    <SelectItem key={agent.id} value={agent.id}>
                      {agent.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="key-name">密钥名称</Label>
              <Input
                id="key-name"
                placeholder="例如：开发环境"
                value={keyName}
                onChange={(e) => setKeyName(e.target.value)}
                disabled={isLoading}
              />
            </div>

            <div className="text-sm text-muted-foreground">
              <p className="font-medium mb-2">注意事项：</p>
              <ul className="list-disc list-inside space-y-1">
                <li>API 密钥只会显示一次，请妥善保存</li>
                <li>密钥具有对应 Agent 的完全访问权限</li>
                <li>不要在客户端代码中使用 API 密钥</li>
                <li>请定期更换 API 密钥以确保安全</li>
              </ul>
            </div>
          </div>
        ) : (
          // 显示创建的密钥
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="new-key">您的新 API 密钥</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="new-key"
                  value={showKey ? createdKey.apiKey : "••••••••••••••••••••••••••••••••"}
                  readOnly
                  className="font-mono"
                />
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setShowKey(!showKey)}
                >
                  {showKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={handleCopyKey}
                >
                  <Copy className="h-4 w-4" />
                </Button>
              </div>
              <p className="text-sm text-muted-foreground flex items-center gap-1">
                <EyeOff className="h-3 w-3" />
                此密钥只会显示一次，请立即复制并安全存储
              </p>
            </div>

            <div className="bg-muted p-3 rounded-md">
              <p className="text-sm font-medium mb-1">密钥信息：</p>
              <p className="text-sm">名称：{createdKey.name}</p>
              <p className="text-sm">关联Agent：{createdKey.agentName || createdKey.agentId}</p>
              <p className="text-sm">创建时间：{new Date(createdKey.createdAt).toLocaleString("zh-CN")}</p>
            </div>
          </div>
        )}

        <DialogFooter>
          {!createdKey ? (
            <>
              <Button
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                取消
              </Button>
              <Button
                onClick={handleCreate}
                disabled={isLoading || !selectedAgentId || !keyName.trim()}
              >
                {isLoading ? "创建中..." : "创建密钥"}
              </Button>
            </>
          ) : (
            <Button onClick={handleFinish}>
              完成
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}