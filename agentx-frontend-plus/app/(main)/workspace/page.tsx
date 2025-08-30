"use client"

import React, { useEffect, useState } from "react"
import { useSearchParams } from "next/navigation"
import { Sidebar } from "@/components/sidebar"
import { ChatPanel } from "@/components/chat-panel"
import { EmptyState } from "@/components/empty-state"
import { ConversationList } from "@/components/conversation-list"
import { useWorkspace } from "@/contexts/workspace-context"
import { getWorkspaceAgents, deleteWorkspaceAgent, deleteWorkspaceAgentWithToast } from "@/lib/agent-service"
import { getAgentSessions, createAgentSession, type SessionDTO, getAgentSessionsWithToast, createAgentSessionWithToast, updateAgentSessionWithToast } from "@/lib/agent-session-service"
import type { Agent } from "@/types/agent"
import { toast } from "@/hooks/use-toast"
import { MoreHorizontal, Trash2, Settings, Grid, Terminal } from 'lucide-react'
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

// 导入模型选择对话框组件
import { ModelSelectDialog } from "@/components/model-select-dialog"
import { ScheduledTaskPanel } from "@/components/scheduled-task-panel"

import { Metadata } from "next"
import { redirect } from "next/navigation"

export default function WorkspacePage() {
  const { selectedWorkspaceId, selectedConversationId, setSelectedWorkspaceId, setSelectedConversationId } =
    useWorkspace()
  const searchParams = useSearchParams()
  const workspaceId = searchParams?.get("id")

  const [agents, setAgents] = useState<Agent[]>([])
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [loadingAgents, setLoadingAgents] = useState(true)
  const [loadingSessions, setLoadingSessions] = useState(false)
  const [agentToDelete, setAgentToDelete] = useState<Agent | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  
  // 定时任务面板相关状态
  const [showScheduledTaskPanel, setShowScheduledTaskPanel] = useState(false)
  
  // 模型选择相关状态
  const [selectedAgent, setSelectedAgent] = useState<Agent | null>(null)
  const [modelDialogOpen, setModelDialogOpen] = useState(false)

  // 如果URL中有工作区ID，则设置为当前选中的工作区
  useEffect(() => {
    if (workspaceId && workspaceId !== selectedWorkspaceId) {
      setSelectedWorkspaceId(workspaceId)
    }
  }, [workspaceId, selectedWorkspaceId, setSelectedWorkspaceId])

  // Fetch workspace agents
  const fetchAgents = async () => {
    try {
      setLoadingAgents(true)
      const response = await getWorkspaceAgents()
      if (response.code === 200) {
        setAgents(response.data)
      }
    } catch (error) {
 
    } finally {
      setLoadingAgents(false)
    }
  }

  useEffect(() => {
    fetchAgents()
  }, [])

  // Fetch agent sessions when workspace is selected
  useEffect(() => {
    async function fetchSessions() {
      if (!selectedWorkspaceId) return

      try {
        setLoadingSessions(true)
        const response = await getAgentSessionsWithToast(selectedWorkspaceId)

        if (response.code === 200) {
          setSessions(response.data)
          
          // 自动选择第一个会话（如果有且未选择任何会话）
          if (response.data.length > 0 && !selectedConversationId) {
            setSelectedConversationId(response.data[0].id)
          }
        }
      } catch (error) {
 
      } finally {
        setLoadingSessions(false)
      }
    }

    fetchSessions()
  }, [selectedWorkspaceId, selectedConversationId])

  // Create a new session for the selected agent
  const handleCreateSession = async (agentId: string) => {
    try {
      const response = await createAgentSessionWithToast(agentId)
      if (response.code === 200 && response.data) {
        // Add the new session to the list
        setSessions(prev => [...prev, response.data])
        
        // Select the new session
        setSelectedConversationId(response.data.id)
        
        // 如果会话创建成功，使用默认标题"新会话"更新会话
        const defaultTitle = "新会话"
        await updateAgentSessionWithToast(response.data.id, defaultTitle)
      }
    } catch (error) {
 
    }
  }

  // Delete agent from workspace
  const handleDeleteAgent = async () => {
    if (!agentToDelete) return

    try {
      setIsDeleting(true)
      const response = await deleteWorkspaceAgentWithToast(agentToDelete.id)

      if (response.code === 200) {
        // toast已由withToast处理
        // 更新列表，移除已删除的助理
        setAgents(agents.filter((agent) => agent.id !== agentToDelete.id))
        
        // 如果删除的是当前选中的助理，清除选择
        if (selectedWorkspaceId === agentToDelete.id) {
          setSelectedWorkspaceId(null)
          setSelectedConversationId(null)
        }
      } else {
        // 错误已由withToast处理
      }
    } catch (error) {
 
      // 错误已由withToast处理
    } finally {
      setIsDeleting(false)
      setAgentToDelete(null)
    }
  }

  // Find the current session title
  const currentSession = sessions.find(session => session.id === selectedConversationId)
  const sessionTitle = currentSession?.title || `会话 ${selectedConversationId?.substring(0, 8) || ''}`

  // 当模型设置成功后重新加载agent列表
  const handleModelSetSuccess = () => {
    fetchAgents()
  }

  // 获取当前选中的Agent信息
  const currentAgent = agents.find(agent => agent.id === selectedWorkspaceId)
  
  // 始终显示定时任务功能
  const isFunctionalAgent = true
  
  // 获取当前会话的多模态设置
  const multiModal = currentSession?.multiModal || false

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
      <div className="flex-1 flex">
        {!selectedConversationId ? (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            {loadingAgents ? (
              <div className="text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                <p className="text-muted-foreground">加载中...</p>
              </div>
            ) : selectedWorkspaceId ? (
              // 如果已选择工作区但没有选择会话，显示创建会话提示
              <EmptyState
                title="选择或开始一个对话"
                description="从中间列表选择一个对话，或者创建一个新的对话"
                actionLabel="开启新会话"
                onAction={() => {
                  if (selectedWorkspaceId) {
                    handleCreateSession(selectedWorkspaceId)
                  }
                }}
              />
            ) : (
              // 只有在未选择工作区时才显示助理列表
              <div className="w-full max-w-3xl p-6">
                <h2 className="text-xl font-semibold mb-4">您的助理</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {agents.map((agent) => (
                    <div
                      key={agent.id}
                      className="border rounded-lg p-4 flex items-start gap-3 cursor-pointer hover:bg-gray-50 group relative"
                    >
                      <div 
                        className="flex-1 flex items-start gap-3"
                        onClick={() => {
                          // Set the selected workspace to this agent
                          setSelectedWorkspaceId(agent.id)
                          // Create a new session for this agent
                          handleCreateSession(agent.id)
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
                          {agent.modelId && (
                            <div className="mt-1 flex items-center text-xs">
                              <Terminal className="h-3 w-3 mr-1" />
                              <span className="text-muted-foreground">
                                模型: {agent.modelName || agent.modelId || "未设置"}
                              </span>
                            </div>
                          )}
                        </div>
                      </div>
                      
                      {/* 操作菜单 */}
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" className="h-8 w-8 p-0">
                            <MoreHorizontal className="h-4 w-4" />
                            <span className="sr-only">菜单</span>
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedAgent(agent);
                              setModelDialogOpen(true);
                            }}
                          >
                            <Settings className="mr-2 h-4 w-4" />
                            设置模型
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={(e) => {
                              e.stopPropagation();
                              setAgentToDelete(agent);
                            }}
                            className="text-red-500 focus:text-red-500"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            移除
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex-1 flex w-full h-full">
            <div className="flex-1 flex flex-col">
              <ChatPanel 
                conversationId={selectedConversationId}
                isFunctionalAgent={isFunctionalAgent}
                agentName={currentAgent?.name || "AI助手"}
                onToggleScheduledTaskPanel={() => setShowScheduledTaskPanel(!showScheduledTaskPanel)}
                multiModal={multiModal}
              />
            </div>
            {showScheduledTaskPanel && isFunctionalAgent && (
              <ScheduledTaskPanel 
                isOpen={showScheduledTaskPanel}
                onClose={() => setShowScheduledTaskPanel(false)}
                conversationId={selectedConversationId}
                agentId={selectedWorkspaceId || undefined}
              />
            )}
          </div>
        )}
      </div>

      {/* 删除确认对话框 */}
      <Dialog open={!!agentToDelete} onOpenChange={(open) => !open && setAgentToDelete(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认移除</DialogTitle>
            <DialogDescription>
              您确定要将助理 "{agentToDelete?.name}" 从工作区移除吗？此操作不会删除助理，但会移除与此助理的关联。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAgentToDelete(null)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteAgent} disabled={isDeleting}>
              {isDeleting ? "移除中..." : "确认移除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      
      {/* 模型选择对话框 */}
      <ModelSelectDialog
        open={modelDialogOpen}
        onOpenChange={setModelDialogOpen}
        agentId={selectedAgent?.id || ""}
        agentName={selectedAgent?.name}
        currentModelId={selectedAgent?.modelId}
        onSuccess={handleModelSetSuccess}
      />
    </div>
  )
}

