import { AgentSidebar } from "@/components/agent-sidebar"
import { ChatPanel } from "@/components/chat-panel"

export default function ChatPage({ params }: { params: { id: string } }) {
  return (
    <div className="flex h-[calc(100vh-3.5rem)] w-full">
      {/* 左侧边栏 */}
      <AgentSidebar />

      {/* 右侧聊天面板 */}
      <div className="flex-1 flex">
        <ChatPanel conversationId={params.id} />
      </div>
    </div>
  )
}

