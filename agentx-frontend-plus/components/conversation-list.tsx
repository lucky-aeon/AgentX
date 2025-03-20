"use client"

import { useState } from "react"
import { Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useWorkspace } from "@/contexts/workspace-context"

// 工作区数据
const workspaces = [
  { id: "workspace-1", name: "文生图助理", icon: "🖼️" },
  { id: "workspace-2", name: "深度搜索助理", icon: "🔍" },
  { id: "workspace-3", name: "对话助理", icon: "💬" },
]

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

interface ConversationListProps {
  workspaceId: string
}

export function ConversationList({ workspaceId }: ConversationListProps) {
  const { selectedConversationId, setSelectedConversationId } = useWorkspace()
  const [hoveredConversationId, setHoveredConversationId] = useState<string | null>(null)

  // 获取当前工作区
  const currentWorkspace = workspaces.find((w) => w.id === workspaceId)

  // 获取当前工作区下的对话列表
  const filteredConversations = conversations.filter((c) => c.workspaceId === workspaceId)

  // 选择对话
  const selectConversation = (conversationId: string) => {
    setSelectedConversationId(conversationId)
  }

  // 创建新对话
  const createNewConversation = () => {
    // 这里可以添加创建新对话的逻辑
    console.log("创建新对话")
  }

  return (
    <div className="w-[320px] border-r flex flex-col h-full bg-white">
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-lg font-semibold">{currentWorkspace?.name || "对话列表"}</h2>
          <Button variant="ghost" size="icon" className="h-6 w-6">
            <Plus className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="flex-1 overflow-auto py-4 px-3 space-y-2">
        {filteredConversations.map((conversation) => (
          <div
            key={conversation.id}
            className="relative group"
            onMouseEnter={() => setHoveredConversationId(conversation.id)}
            onMouseLeave={() => setHoveredConversationId(null)}
          >
            <div
              onClick={() => selectConversation(conversation.id)}
              className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors cursor-pointer ${
                selectedConversationId === conversation.id ? "bg-blue-100 text-blue-900" : "hover:bg-gray-100"
              }`}
            >
              <div className="flex h-6 w-6 items-center justify-center">{conversation.icon}</div>
              <span className="flex-1 truncate">{conversation.name}</span>
            </div>

            {hoveredConversationId === conversation.id && selectedConversationId !== conversation.id && (
              <Button
                variant="ghost"
                size="icon"
                className="absolute right-2 top-1/2 -translate-y-1/2 h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <Plus className="h-4 w-4" />
                <span className="sr-only">Add</span>
              </Button>
            )}
          </div>
        ))}
      </div>

      <div className="p-3 border-t">
        <Button
          variant="outline"
          className="w-full justify-center items-center gap-2 text-blue-600 border-blue-200 bg-blue-50"
          onClick={createNewConversation}
        >
          <Plus className="h-4 w-4" />
          开启新对话
        </Button>
      </div>
    </div>
  )
}

