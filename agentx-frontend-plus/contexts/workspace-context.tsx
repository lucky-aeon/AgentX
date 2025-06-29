"use client"

import { createContext, useContext, useState, useEffect, type ReactNode } from "react"

type WorkspaceContextType = {
  selectedWorkspaceId: string | null
  selectedConversationId: string | null
  setSelectedWorkspaceId: (id: string | null) => void
  setSelectedConversationId: (id: string | null) => void
  refreshWorkspace: () => void
  refreshTrigger: number
}

const WorkspaceContext = createContext<WorkspaceContextType | undefined>(undefined)

// 对话数据
const conversations = [
  {
    id: "conv-1",
    workspaceId: "workspace-3",
    name: "聊天测试",
    icon: "📝",
    messages: [{ id: "m1", role: "assistant", content: "你好！我是你的 AI 助手。有什么可以帮助你的吗？" }],
  },
  {
    id: "conv-2",
    workspaceId: "workspace-3",
    name: "1",
    icon: "📝",
    messages: [{ id: "m2", role: "assistant", content: "这是测试助手1。请问有什么需要帮助的吗?" }],
  },
  {
    id: "conv-3",
    workspaceId: "workspace-3",
    name: "测试工具",
    icon: "🔧",
    messages: [{ id: "m3", role: "assistant", content: "这是测试工具助手。我可以帮助您测试各种功能。" }],
  },
  {
    id: "conv-4",
    workspaceId: "workspace-1",
    name: "图像生成",
    icon: "🖼️",
    messages: [{ id: "m4", role: "assistant", content: "你好！我是文生图助理。请告诉我你想要生成什么样的图像。" }],
  },
  {
    id: "conv-5",
    workspaceId: "workspace-2",
    name: "网络搜索",
    icon: "🔍",
    messages: [{ id: "m5", role: "assistant", content: "你好！我是深度搜索助理。我可以帮你搜索和分析网络上的信息。" }],
  },
]

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<string | null>(null)
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null)
  const [refreshTrigger, setRefreshTrigger] = useState(0)

  // 刷新工作区的方法
  const refreshWorkspace = () => {
    setRefreshTrigger(prev => prev + 1)
  }

  // 当工作区变化时，自动选择第一个对话
  useEffect(() => {
    if (selectedWorkspaceId && !selectedConversationId) {
      const workspaceConversations = conversations.filter((c) => c.workspaceId === selectedWorkspaceId)
      if (workspaceConversations.length > 0) {
        setSelectedConversationId(workspaceConversations[0].id)
      }
    }
  }, [selectedWorkspaceId, selectedConversationId])

  return (
    <WorkspaceContext.Provider
      value={{
        selectedWorkspaceId,
        selectedConversationId,
        setSelectedWorkspaceId,
        setSelectedConversationId,
        refreshWorkspace,
        refreshTrigger,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  )
}

export function useWorkspace() {
  const context = useContext(WorkspaceContext)
  if (context === undefined) {
    throw new Error("useWorkspace must be used within a WorkspaceProvider")
  }
  return context
}

