"use client"

import { useState, useEffect } from "react"
import { Plus, Key, ExternalLink } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { toast } from "@/hooks/use-toast"

// API服务
import {
  getUserApiKeysWithToast,
  createApiKeyWithToast,
  deleteApiKeyWithToast,
  updateApiKeyStatusWithToast,
  resetApiKeyWithToast
} from "@/lib/api-key-service"
import { getWorkspaceAgents } from "@/lib/api-services"

// 类型定义
import { ApiKeyResponse, CreateApiKeyRequest, UpdateApiKeyStatusRequest } from "@/types/api-key"
import type { Agent } from "@/types/agent"

// 组件
import { ApiKeyList } from "@/components/api-keys/api-key-list"
import { CreateApiKeyDialog } from "@/components/api-keys/create-api-key-dialog"
import { DeleteApiKeyDialog } from "@/components/api-keys/delete-api-key-dialog"
import { ApiKeyFilters } from "@/components/api-keys/api-key-filters"

export default function ApiKeysPage() {
  // 状态管理
  const [apiKeys, setApiKeys] = useState<ApiKeyResponse[]>([])
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  // 搜索和筛选
  const [searchQuery, setSearchQuery] = useState("")
  const [statusFilter, setStatusFilter] = useState<boolean | "ALL">("ALL")
  const [agentFilter, setAgentFilter] = useState<string>("ALL")
  
  // 对话框状态
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [keyToDelete, setKeyToDelete] = useState<ApiKeyResponse | null>(null)
  
  // 操作状态
  const [operatingKeyId, setOperatingKeyId] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  // 获取Agent列表
  useEffect(() => {
    async function fetchAgents() {
      try {
        const response = await getWorkspaceAgents()
        if (response.code === 200) {
          setAgents(response.data)
        }
      } catch (error) {
 
      }
    }
    fetchAgents()
  }, [])

  // 获取API密钥列表
  const fetchApiKeys = async () => {
    try {
      setLoading(true)
      setError(null)
      
      const params = {
        name: searchQuery || undefined,
        status: statusFilter !== "ALL" ? statusFilter : undefined,
        agentId: agentFilter !== "ALL" ? agentFilter : undefined,
      }
      
      const response = await getUserApiKeysWithToast(params)
      if (response.code === 200) {
        setApiKeys(response.data)
      } else {
        setError(response.message)
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : "未知错误")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchApiKeys()
  }, [searchQuery, statusFilter, agentFilter])

  // 创建API密钥
  const handleCreateKey = async (agentId: string, name: string): Promise<ApiKeyResponse | null> => {
    try {
      const params: CreateApiKeyRequest = { agentId, name }
      const response = await createApiKeyWithToast(params)
      
      if (response.code === 200) {
        setApiKeys(prev => [response.data, ...prev])
        return response.data
      }
      return null
    } catch (error) {
 
      return null
    }
  }

  // 删除API密钥
  const handleDeleteKey = async () => {
    if (!keyToDelete) return

    try {
      setIsDeleting(true)
      const response = await deleteApiKeyWithToast(keyToDelete.id)
      if (response.code === 200) {
        setApiKeys(prev => prev.filter(key => key.id !== keyToDelete.id))
        setIsDeleteDialogOpen(false)
        setKeyToDelete(null)
      }
    } catch (error) {
 
    } finally {
      setIsDeleting(false)
    }
  }

  // 切换API密钥状态
  const handleToggleStatus = async (apiKey: ApiKeyResponse) => {
    try {
      setOperatingKeyId(apiKey.id)
      const newStatus = !apiKey.status
      
      const request: UpdateApiKeyStatusRequest = { status: newStatus }
      const response = await updateApiKeyStatusWithToast(apiKey.id, request)
      
      if (response.code === 200) {
        setApiKeys(prev => prev.map(key => 
          key.id === apiKey.id ? { ...key, status: newStatus } : key
        ))
      }
    } catch (error) {
 
    } finally {
      setOperatingKeyId(null)
    }
  }

  // 重置API密钥
  const handleResetKey = async (apiKey: ApiKeyResponse) => {
    try {
      setOperatingKeyId(apiKey.id)
      const response = await resetApiKeyWithToast(apiKey.id)
      
      if (response.code === 200) {
        setApiKeys(prev => prev.map(key => 
          key.id === apiKey.id ? response.data : key
        ))
      }
    } catch (error) {
 
    } finally {
      setOperatingKeyId(null)
    }
  }

  // 打开删除对话框
  const openDeleteDialog = (apiKey: ApiKeyResponse) => {
    setKeyToDelete(apiKey)
    setIsDeleteDialogOpen(true)
  }

  return (
    <div className="container py-6">
      {/* 页面头部 */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">API 密钥管理</h1>
          <p className="text-muted-foreground">创建和管理您所有 Agent 的 API 访问密钥</p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={() => window.open('https://nz6d48w48i.apifox.cn', '_blank')}
          >
            <ExternalLink className="mr-2 h-4 w-4" />
            API 文档
          </Button>
          <Button onClick={() => setIsCreateDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            创建新密钥
          </Button>
        </div>
      </div>

      {/* 搜索和筛选工具栏 */}
      <div className="mb-6">
        <ApiKeyFilters
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          agentFilter={agentFilter}
          onAgentFilterChange={setAgentFilter}
          agents={agents}
          onRefresh={fetchApiKeys}
          loading={loading}
        />
      </div>

      {/* API密钥列表 */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>API 密钥列表</CardTitle>
          <CardDescription>管理您的 API 访问密钥</CardDescription>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="text-center py-8">
              <p className="text-muted-foreground">{error}</p>
              <Button variant="outline" className="mt-2" onClick={fetchApiKeys}>
                重试
              </Button>
            </div>
          ) : apiKeys.length === 0 && !loading ? (
            <div className="text-center py-8">
              <Key className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">暂无API密钥</p>
              <Button className="mt-2" onClick={() => setIsCreateDialogOpen(true)}>
                创建第一个密钥
              </Button>
            </div>
          ) : (
            <ApiKeyList
              apiKeys={apiKeys}
              loading={loading}
              onToggleStatus={handleToggleStatus}
              onResetKey={handleResetKey}
              onDeleteKey={openDeleteDialog}
              operatingKeyId={operatingKeyId}
            />
          )}
        </CardContent>
      </Card>

      {/* 创建API密钥对话框 */}
      <CreateApiKeyDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        agents={agents}
        onCreateKey={handleCreateKey}
      />

      {/* 删除确认对话框 */}
      <DeleteApiKeyDialog
        open={isDeleteDialogOpen}
        onOpenChange={setIsDeleteDialogOpen}
        apiKey={keyToDelete}
        onConfirm={handleDeleteKey}
        loading={isDeleting}
      />
    </div>
  )
}