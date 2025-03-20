"use client"

import { useEffect } from "react"
import { useSearchParams } from "next/navigation"
import { Sidebar } from "@/components/sidebar"
import { ConversationList } from "@/components/conversation-list"
import { ChatPanel } from "@/components/chat-panel"
import { EmptyState } from "@/components/empty-state"
import { useWorkspace } from "@/contexts/workspace-context"

export default function WorkspacePage() {
  const { selectedWorkspaceId, selectedConversationId, setSelectedWorkspaceId } = useWorkspace()
  const searchParams = useSearchParams()
  const workspaceId = searchParams.get("id")

  // 如果URL中有工作区ID，则设置为当前选中的工作区
  useEffect(() => {
    if (workspaceId && workspaceId !== selectedWorkspaceId) {
      setSelectedWorkspaceId(workspaceId)
    }
  }, [workspaceId, selectedWorkspaceId, setSelectedWorkspaceId])

  return (
    <div className="flex h-[calc(100vh-3.5rem)]">
      {/* 左侧边栏 */}
      <Sidebar />

      {/* 中间会话列表 */}
      {selectedWorkspaceId ? (
        <ConversationList workspaceId={selectedWorkspaceId} />
      ) : (
        <div className="flex-1 flex items-center justify-center bg-gray-50 border-r">
          <EmptyState title="选择一个工作区" description="从左侧选择一个工作区来查看对话" />
        </div>
      )}

      {/* 右侧聊天面板 */}
      {selectedConversationId ? (
        <ChatPanel conversationId={selectedConversationId} />
      ) : (
        <div className="flex-1 flex items-center justify-center bg-gray-50">
          <EmptyState title="选择或开始一个对话" description="从中间列表选择一个对话，或者创建一个新的对话" />
        </div>
      )}
    </div>
  )
}

