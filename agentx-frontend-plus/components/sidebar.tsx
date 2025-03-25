"use client"

import type React from "react"

import { useState, useEffect } from "react"
import { useRouter, usePathname } from "next/navigation"
import { ChevronDown, ChevronRight, Compass, FolderOpen, Bot, RefreshCw, MoreHorizontal, Trash2 } from "lucide-react"

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

type SidebarItem = {
  title: string
  href?: string
  icon?: React.ComponentType<{ className?: string }> | string
  children?: SidebarItem[]
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
        {avatar ? (
          <div className="w-5 h-5 rounded-full overflow-hidden mr-2 flex-shrink-0">
            <img src={avatar || "/placeholder.svg"} alt={name} className="w-full h-full object-cover" />
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
  const [agents, setAgents] = useState<Agent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  // Fetch user agents
  useEffect(() => {
    async function fetchAgents() {
      try {
        setLoading(true)
        setError(null)
        const response = await getUserAgents()

        if (response.code === 200) {
          setAgents(response.data)
        } else {
          setError(response.message || "获取助理列表失败")
        }
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : "未知错误"
        console.error("获取助理列表错误:", error)
        setError(`获取助理列表失败: ${errorMessage}`)
      } finally {
        setLoading(false)
      }
    }

    fetchAgents()
  }, [retryCount])

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

          {/* Show error state with retry button */}
          {error && (
            <div className="px-2 py-2 text-sm text-red-500">
              <p className="mb-2">{error}</p>
              <Button variant="outline" size="sm" onClick={() => setRetryCount((prev) => prev + 1)} className="w-full">
                <RefreshCw className="mr-2 h-3 w-3" />
                重试
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

