"use client"

import React, { useEffect, useState } from 'react'
import { AgentSidebar } from "@/components/agent-sidebar"
import { ChatPanel } from "@/components/chat-panel"
import { getAgentSessionsWithToast, type SessionDTO } from "@/lib/agent-session-service"
import { getAgentBySessionId } from "@/lib/agent-service"
import type { Agent } from "@/types/agent"

export default function ChatPage({ params }: { params: { id: string } }) {
  const [currentAgent, setCurrentAgent] = useState<Agent | null>(null)
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [loading, setLoading] = useState(true)

  // 通过会话ID获取Agent信息
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        
        // 通过会话ID获取Agent信息
        const agentResponse = await getAgentBySessionId(params.id)
        if (agentResponse.code === 200 && agentResponse.data) {
          setCurrentAgent(agentResponse.data)
          
          // 获取该Agent的所有会话来找到当前会话的多模态设置
          const sessionsResponse = await getAgentSessionsWithToast(agentResponse.data.id)
          if (sessionsResponse.code === 200 && sessionsResponse.data) {
            setSessions(sessionsResponse.data)
          }
        }
        
      } catch (error) {
        console.error("获取数据失败:", error)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [params.id])

  // 获取当前会话的多模态设置
  const currentSession = sessions.find(session => session.id === params.id)
  const multiModal = currentSession?.multiModal || false
  
  return (
    <div className="flex h-[calc(100vh-3.5rem)] w-full">
      {/* 左侧边栏 */}
      <AgentSidebar />

      {/* 右侧聊天面板 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <ChatPanel 
          conversationId={params.id} 
          agentName={currentAgent?.name || "AI助手"}
          multiModal={multiModal}
        />
      </div>
    </div>
  )
}

