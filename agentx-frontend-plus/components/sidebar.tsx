"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { useRouter, usePathname } from "next/navigation"
import { ChevronDown, ChevronRight, Compass, FolderOpen, Bot, RefreshCw, MoreHorizontal, Trash2, Settings } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { useWorkspace } from "@/contexts/workspace-context"
import { getUserAgents, deleteWorkspaceAgent } from "@/lib/agent-service"
import { Skeleton } from "@/components/ui/skeleton"
import type { Agent } from "@/types/agent"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { ModelSelectDialog } from "../components/model-select-dialog"
import { 
  getWorkspaceAgentsWithToast
} from "@/lib/api-services"
import { 
  deleteWorkspaceAgentWithToast 
} from "@/lib/agent-service"

type SidebarItem = {
  title: string
  href?: string
  icon?: React.ComponentType<{ className?: string }> | string
  children?: SidebarItem[]
  id?: string
  avatar?: string | null
}

type SidebarItemProps = {
  item: SidebarItem & { id?: string }
  depth?: number
}

type WorkspaceItemProps = {
  id: string
  name: string
  icon?: string
  avatar?: string | null
  onClick?: () => void
}

function WorkspaceItem({ id, name, icon, avatar, onClick }: WorkspaceItemProps) {
  const { selectedWorkspaceId, setSelectedWorkspaceId, setSelectedConversationId } = useWorkspace()
  const isActive = selectedWorkspaceId === id
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const router = useRouter()
  
  // 添加模型选择相关状态
  const [showModelDialog, setShowModelDialog] = useState(false)
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null)

  // Handle delete workspace assistant
  const handleDeleteWorkspace = async (e: React.MouseEvent) => {
    e.stopPropagation()
    setShowDeleteDialog(true)
  }

  // Confirm delete workspace assistant
  const confirmDeleteWorkspace = async () => {
    try {
      setIsDeleting(true)
      const response = await deleteWorkspaceAgent(id)

      if (response.code === 200) {
        // If the deleted workspace was selected, clear the selection
        if (selectedWorkspaceId === id) {
          setSelectedWorkspaceId(null)
          setSelectedConversationId(null)
          router.push("/explore")
        }

        // Refresh the sidebar
        window.location.reload()
      } else {
        throw new Error(response.message || "删除失败")
      }
    } catch (error) {
      console.error("删除工作区助理错误:", error)
      alert("删除失败: " + (error instanceof Error ? error.message : "未知错误"))
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }

  // 打开模型设置对话框
  const handleOpenModelSettings = async (e: React.MouseEvent) => {
    e.stopPropagation()
    
    // 获取agent详情
    try {
      // 这里可以在实际情况下添加获取agent详情的API调用
      // 简化处理，直接使用当前ID和名称
      setSelectedAgent({
        id,
        name,
        // 添加其他必要的字段
        avatar,
      } as Agent)
      setShowModelDialog(true)
    } catch (error) {
      console.error("获取助理详情失败:", error)
    }
  }
  
  // 模型设置成功回调
  const handleModelSetSuccess = () => {
    // 可以添加刷新agent列表的逻辑
    window.location.reload()
  }

  return (
    <div className="relative group">
      <Button
        variant="ghost"
        className={cn(
          "w-full justify-start px-2 py-1.5 text-sm font-medium pl-8 hover:bg-accent hover:text-accent-foreground",
          isActive && "bg-accent text-accent-foreground",
        )}
        onClick={onClick}
      >
        {avatar && avatar.trim() !== '' ? (
          <div className="w-5 h-5 rounded-full overflow-hidden mr-2 flex-shrink-0">
            <img src={avatar} alt={name} className="w-full h-full object-cover" />
          </div>
        ) : icon ? (
          <span className="mr-2">{icon}</span>
        ) : (
          <Bot className="mr-2 h-4 w-4" />
        )}
        <span className="truncate">{name}</span>
      </Button>

      {/* Three-dot menu that appears on hover */}
      <div className="absolute right-2 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" className="h-7 w-7">
              <MoreHorizontal className="h-4 w-4" />
              <span className="sr-only">操作</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={handleOpenModelSettings}>
              <Settings className="mr-2 h-4 w-4" />
              <span>设置模型</span>
            </DropdownMenuItem>
            <DropdownMenuItem className="text-red-600" onClick={handleDeleteWorkspace}>
              <Trash2 className="mr-2 h-4 w-4" />
              <span>从工作区移除</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Delete confirmation dialog */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认移除</DialogTitle>
            <DialogDescription>
              您确定要将助理 "{name}" 从工作区移除吗？此操作不会删除助理，但会移除与此助理的关联。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={confirmDeleteWorkspace} disabled={isDeleting}>
              {isDeleting ? "移除中..." : "确认移除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      
      {/* 模型选择对话框 */}
      {selectedAgent && (
        <ModelSelectDialog
          open={showModelDialog}
          onOpenChange={setShowModelDialog}
          agentId={selectedAgent.id}
          agentName={selectedAgent.name}
          currentModelId={selectedAgent.modelId}
          onSuccess={handleModelSetSuccess}
        />
      )}
    </div>
  )
}

function SidebarItemComponent({ item, depth = 0 }: SidebarItemProps) {
  const router = useRouter()
  const pathname = usePathname()
  const { setSelectedWorkspaceId, setSelectedConversationId } = useWorkspace()
  const [expanded, setExpanded] = useState(true)
  const Icon = item.icon

  // Check if current path matches the menu item's href
  const isActive =
    item.href &&
    (pathname === item.href || pathname.startsWith(`${item.href}/`) || (item.href === "/explore" && pathname === "/"))

  const handleWorkspaceClick = (workspaceId: string) => {
    setSelectedWorkspaceId(workspaceId)
    setSelectedConversationId(null) // 清除选中的对话
    router.push(`/workspace?id=${workspaceId}`)
  }

  if (item.children) {
    return (
      <div className="space-y-1">
        <Button
          variant="ghost"
          className={cn("w-full justify-start px-2 py-1.5 text-sm font-medium", depth > 0 && "pl-8")}
          onClick={() => setExpanded(!expanded)}
        >
          {typeof item.icon === "string" ? (
            <span className="mr-2">{item.icon}</span>
          ) : (
            item.icon && <item.icon className="mr-2 h-4 w-4" />
          )}
          <span className="flex-1 text-left">{item.title}</span>
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </Button>
        {expanded && (
          <div className="space-y-1">
            {item.children.map((child, index) => {
              // 确保每个子项都有一个有效的ID
              const childId = child.id || `item-${index}`
              return (
                <WorkspaceItem
                  key={childId}
                  id={childId}
                  name={child.title}
                  icon={typeof child.icon === "string" ? child.icon : undefined}
                  avatar={child.avatar}
                  onClick={() => {
                    if (childId) {
                      handleWorkspaceClick(childId)
                    }
                  }}
                />
              )
            })}
          </div>
        )}
      </div>
    )
  }

  return (
    <Button
      variant="ghost"
      className={cn(
        "w-full justify-start px-2 py-1.5 text-sm font-medium",
        isActive ? "bg-accent text-accent-foreground" : "hover:bg-accent hover:text-accent-foreground",
        depth > 0 && "pl-8",
      )}
      onClick={() => {
        if (item.href) {
          router.push(item.href)
        }
      }}
    >
      {typeof Icon === "string" ? <span className="mr-2">{Icon}</span> : Icon && <Icon className="mr-2 h-4 w-4" />}
      <span>{item.title}</span>
    </Button>
  )
}

export function Sidebar() {
  const { refreshTrigger } = useWorkspace()
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [modelDialogOpen, setModelDialogOpen] = useState(false)
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null)

  // 加载工作区Agent列表
  const loadWorkspaceAgents = async () => {
    setLoading(true)
    try {
      const response = await getWorkspaceAgentsWithToast()
      if (response.code === 200 && Array.isArray(response.data)) {
        setAgents(response.data)
      }
    } catch (error) {
      console.error("加载工作区Agent失败:", error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadWorkspaceAgents()
  }, [refreshTrigger])

  // 处理移除Agent
  const handleRemoveAgent = async (agentId: string) => {
    try {
      const response = await deleteWorkspaceAgentWithToast(agentId)
      if (response.code === 200) {
        // 重新加载工作区Agent列表
        await loadWorkspaceAgents()
      }
    } catch (error) {
      console.error("移除Agent失败:", error)
    }
  }

  // Create sidebar items with real agent data
  const sidebarItems: SidebarItem[] = [
    {
      title: "探索",
      icon: Compass,
      href: "/explore",
    },
    {
      title: "工作区",
      icon: FolderOpen,
      children: loading
        ? []
        : agents.map((agent) => ({
            title: agent.name,
            icon: undefined,
            avatar: agent.avatar,
            id: agent.id,
          })),
    },
  ]

  return (
    <div className="w-[220px] border-r flex flex-col h-full bg-gray-50">
      <div className="flex-1 overflow-auto py-4 px-3">
        <div className="space-y-2">
          {sidebarItems.map((item, index) => (
            <SidebarItemComponent key={index} item={item} />
          ))}

          {/* Show loading state for workspaces */}
          {loading && (
            <div className="space-y-2 pl-8 pr-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="flex items-center gap-2 py-1.5">
                  <Skeleton className="h-5 w-5 rounded-full" />
                  <Skeleton className="h-4 w-24" />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

