"use client"

import { useState } from "react"
import { Copy, EyeOff, Key, Plus, Trash } from "lucide-react"
import { Metadata } from "next"
import { redirect } from "next/navigation"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { toast } from "@/hooks/use-toast"

// Mock data for API keys
const apiKeys = [
  { id: "1", name: "开发环境", prefix: "ak_dev_", created: "2024-01-15", lastUsed: "2024-03-18" },
  { id: "2", name: "生产环境", prefix: "ak_prod_", created: "2024-02-10", lastUsed: "2024-03-20" },
  { id: "3", name: "测试环境", prefix: "ak_test_", created: "2024-03-05", lastUsed: "2024-03-15" },
]

export default function ApiKeysPage() {
  const [newKeyName, setNewKeyName] = useState("")
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [newKey, setNewKey] = useState<string | null>(null)

  const handleCreateKey = () => {
    // 模拟创建新密钥
    const mockKey = `ak_${Math.random().toString(36).substring(2, 10)}_${Math.random().toString(36).substring(2, 15)}`
    setNewKey(mockKey)
    toast({
      title: "API 密钥已创建",
      description: "请保存您的 API 密钥，它只会显示一次。",
    })
  }

  const handleCopyKey = () => {
    if (newKey) {
      navigator.clipboard.writeText(newKey)
      toast({
        title: "已复制到剪贴板",
        description: "API 密钥已复制到剪贴板。",
      })
    }
  }

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">API 密钥管理</h1>
        <p className="text-muted-foreground">创建和管理您的 API 密钥</p>
      </div>

      <div className="grid gap-6">
        <Card>
          <CardHeader>
            <CardTitle>API 密钥</CardTitle>
            <CardDescription>管理您的 API 访问密钥</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名称</TableHead>
                  <TableHead>密钥前缀</TableHead>
                  <TableHead>创建日期</TableHead>
                  <TableHead>最后使用</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {apiKeys.map((key) => (
                  <TableRow key={key.id}>
                    <TableCell className="font-medium">{key.name}</TableCell>
                    <TableCell>{key.prefix}••••••••</TableCell>
                    <TableCell>{key.created}</TableCell>
                    <TableCell>{key.lastUsed}</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon">
                        <Trash className="h-4 w-4" />
                        <span className="sr-only">删除</span>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
          <CardFooter>
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="mr-2 h-4 w-4" />
                  创建新密钥
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>创建 API 密钥</DialogTitle>
                  <DialogDescription>创建一个新的 API 密钥以访问 AgentX API。</DialogDescription>
                </DialogHeader>
                {!newKey ? (
                  <div className="space-y-4 py-4">
                    <div className="space-y-2">
                      <Label htmlFor="key-name">密钥名称</Label>
                      <Input
                        id="key-name"
                        placeholder="例如：开发环境"
                        value={newKeyName}
                        onChange={(e) => setNewKeyName(e.target.value)}
                      />
                    </div>
                    <div className="text-sm text-muted-foreground">
                      <p>注意事项：</p>
                      <ul className="list-disc list-inside">
                        <li>API 密钥只会显示一次，请妥善保存</li>
                        <li>密钥具有完全访问权限，请确保其安全</li>
                        <li>不要在客户端代码中使用 API 密钥</li>
                      </ul>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4 py-4">
                    <div className="space-y-2">
                      <Label htmlFor="new-key">您的新 API 密钥</Label>
                      <div className="flex items-center gap-2">
                        <Input id="new-key" value={newKey} readOnly className="font-mono" />
                        <Button variant="outline" size="icon" onClick={handleCopyKey}>
                          <Copy className="h-4 w-4" />
                          <span className="sr-only">复制</span>
                        </Button>
                      </div>
                      <p className="text-sm text-muted-foreground flex items-center gap-1">
                        <EyeOff className="h-3 w-3" />
                        此密钥只会显示一次，请立即复制并安全存储
                      </p>
                    </div>
                  </div>
                )}
                <DialogFooter>
                  {!newKey ? (
                    <Button onClick={handleCreateKey}>创建密钥</Button>
                  ) : (
                    <Button onClick={() => setIsDialogOpen(false)}>完成</Button>
                  )}
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </CardFooter>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>API 使用指南</CardTitle>
            <CardDescription>了解如何使用 API 密钥</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <h3 className="font-medium mb-2">身份验证</h3>
              <div className="bg-muted p-3 rounded-md">
                <pre className="text-sm overflow-x-auto">
                  <code>
                    {`curl -X POST https://api.agentx.plus/v1/chat \\
  -H "Authorization: Bearer YOUR_API_KEY" \\
  -H "Content-Type: application/json" \\
  -d '{"messages": [{"role": "user", "content": "Hello!"}]}'`}
                  </code>
                </pre>
              </div>
            </div>
            <div>
              <h3 className="font-medium mb-2">速率限制</h3>
              <p className="text-sm text-muted-foreground">
                根据您的订阅计划，API 请求有不同的速率限制。专业版计划每分钟最多可发送 60 个请求。
              </p>
            </div>
            <div>
              <h3 className="font-medium mb-2">了解更多</h3>
              <div className="flex items-center gap-2">
                <Button variant="outline" className="gap-2">
                  <Key className="h-4 w-4" />
                  API 文档
                </Button>
                <Button variant="outline" className="gap-2">
                  示例代码
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

