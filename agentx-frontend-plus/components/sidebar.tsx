"use client"

import type React from "react"

import { useState } from "react"
import { useRouter, usePathname } from "next/navigation"
import { ChevronDown, ChevronRight, Compass, FolderOpen } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { useWorkspace } from "@/contexts/workspace-context"

// 工作区数据
const workspaces = [
  { id: "workspace-1", name: "文生图助理", icon: "🖼️" },
  { id: "workspace-2", name: "深度搜索助理", icon: "🔍" },
  { id: "workspace-3", name: "对话助理", icon: "💬" },
]

type SidebarItem = {
  title: string
  href?: string
  icon?: React.ComponentType<{ className?: string }> | string
  children?: SidebarItem[]
}

const sidebarItems: SidebarItem[] = [
  {
    title: "发现",
    icon: Compass,
    href: "/",
  },
  {
    title: "工作区",
    icon: FolderOpen,
    children: workspaces.map((workspace) => ({
      title: workspace.name,
      icon: workspace.icon,
      id: workspace.id,
    })),
  },
]

type SidebarItemProps = {
  item: SidebarItem & { id?: string }
  depth?: number
}

type WorkspaceItemProps = {
  id: string
  name: string
  icon?: string
  onClick?: () => void
}

function WorkspaceItem({ id, name, icon, onClick }: WorkspaceItemProps) {
  const { selectedWorkspaceId } = useWorkspace()
  const isActive = selectedWorkspaceId === id

  return (
    <Button
      variant="ghost"
      className={cn(
        "w-full justify-start px-2 py-1.5 text-sm font-medium pl-8 hover:bg-accent hover:text-accent-foreground",
        isActive && "bg-accent text-accent-foreground",
      )}
      onClick={onClick}
    >
      {icon && <span className="mr-2">{icon}</span>}
      <span>{name}</span>
    </Button>
  )
}

function SidebarItemComponent({ item, depth = 0 }: SidebarItemProps) {
  const router = useRouter()
  const pathname = usePathname()
  const { setSelectedWorkspaceId, setSelectedConversationId } = useWorkspace()
  const [expanded, setExpanded] = useState(true)
  const Icon = item.icon

  // 检查当前路径是否与菜单项的href匹配
  const isActive = item.href && (pathname === item.href || pathname === item.href + "/")

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
            {item.children.map((child, index) => (
              <WorkspaceItem
                key={child.id || `item-${index}`}
                id={child.id || `item-${index}`}
                name={child.title}
                icon={typeof child.icon === "string" ? child.icon : undefined}
                onClick={() => handleWorkspaceClick(child.id || "")}
              />
            ))}
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
      onClick={() => router.push(item.href || "/")}
    >
      {typeof Icon === "string" ? <span className="mr-2">{Icon}</span> : Icon && <Icon className="mr-2 h-4 w-4" />}
      <span>{item.title}</span>
    </Button>
  )
}

export function Sidebar() {
  return (
    <div className="w-[220px] border-r flex flex-col h-full bg-gray-50">
      <div className="flex-1 overflow-auto py-4 px-3">
        <div className="space-y-2">
          {sidebarItems.map((item, index) => (
            <SidebarItemComponent key={index} item={item} />
          ))}
        </div>
      </div>
    </div>
  )
}

