import React from 'react'
import { AgentSidebar } from "@/components/agent-sidebar"
import { ChatPanel } from "@/components/chat-panel"

export default function ChatPage({ params }: { params: { id: string } }) {
  // 注意: 在未来的 Next.js 版本中，params 将会是一个 Promise 对象
  // 届时需要使用 React.use(params) 解包后再访问其属性
  
  return (
    <div className="flex h-[calc(100vh-3.5rem)] w-full">
      {/* 左侧边栏 */}
      <AgentSidebar />

      {/* 右侧聊天面板 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        <ChatPanel conversationId={params.id} />
      </div>
    </div>
  )
}

