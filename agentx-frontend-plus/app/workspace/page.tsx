"use client"

import { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import { Sidebar } from "@/components/sidebar"
import { ConversationList } from "@/components/conversation-list"
import { ChatPanel } from "@/components/chat-panel"
import { EmptyState } from "@/components/empty-state"
import { useWorkspace } from "@/contexts/workspace-context"
import { getUserAgents } from "@/lib/agent-service"
import type { Agent } from "@/types/agent"

export default function WorkspacePage() {
  const { selectedWorkspaceId, selectedConversationId, setSelectedWorkspaceId } = useWorkspace()
  const searchParams = useSearchParams()
  const workspaceId = searchParams.get("id")

  const [agents, setAgents] = useState<Agent[]>([])
  const [loadingAgents, setLoadingAgents] = useState(true)

  // 如果URL中有工作区ID，则设置为当前选中的工作区
  useEffect(() => {
    if (workspaceId && workspaceId !== selectedWorkspaceId) {
      setSelectedWorkspaceId(workspaceId)
    }
  }, [workspaceId, selectedWorkspaceId, setSelectedWorkspaceId])

  // Fetch user agents
  useEffect(() => {
    async function fetchAgents() {
      try {
        setLoadingAgents(true)
        const response = await getUserAgents({ userId: "1" })
        if (response.code === 200) {
          setAgents(response.data)
        }
      } catch (error) {
        console.error("Error fetching agents:", error)
      } finally {
        setLoadingAgents(false)
      }
    }

    fetchAgents()
  }, [])

  return (
    <div className="flex h-[calc(100vh-3.5rem)] w-full">
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
      {!selectedConversationId ? (
        <div className="flex-1 flex items-center justify-center bg-gray-50">
          {loadingAgents ? (
            <div className="text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
              <p className="text-muted-foreground">加载中...</p>
            </div>
          ) : agents.length > 0 ? (
            <div className="w-full max-w-3xl p-6">
              <h2 className="text-xl font-semibold mb-4">您的助理</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {agents.map((agent) => (
                  <div
                    key={agent.id}
                    className="border rounded-lg p-4 flex items-start gap-3 cursor-pointer hover:bg-gray-50"
                    onClick={() => {
                      // Create a new conversation with this agent
                      const createButton = document.querySelector(
                        ".conversation-list-create-button",
                      ) as HTMLButtonElement
                      if (createButton) {
                        createButton.click()
                      }
                    }}
                  >
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100 text-blue-900 overflow-hidden">
                      {agent.avatar ? (
                        <img
                          src={agent.avatar || "/placeholder.svg"}
                          alt={agent.name}
                          className="h-full w-full object-cover"
                        />
                      ) : (
                        agent.name.charAt(0).toUpperCase()
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="font-medium">{agent.name}</div>
                      <div className="text-xs text-muted-foreground truncate">{agent.description || "无描述"}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <EmptyState
              title="选择或开始一个对话"
              description="从中间列表选择一个对话，或者创建一个新的对话"
              actionLabel="开启新会话"
              onAction={() => {
                // 这里可以触发创建新会话的对话框
                const createButton = document.querySelector(".conversation-list-create-button") as HTMLButtonElement
                if (createButton) {
                  createButton.click()
                }
              }}
            />
          )}
        </div>
      ) : (
        <div className="flex-1 flex">
          <ChatPanel conversationId={selectedConversationId} />
        </div>
      )}
    </div>
  )
}

