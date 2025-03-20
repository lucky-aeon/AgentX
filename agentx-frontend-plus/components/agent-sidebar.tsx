"use client"

import { useState } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { Bot, Plus, Search } from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"

// Mock data for agents
const agents = [
  { id: "1", name: "助手", avatar: "A", lastMessage: "有什么可以帮您的？" },
  { id: "2", name: "数据分析师", avatar: "D", lastMessage: "我已经分析了您的数据" },
  { id: "3", name: "编程助手", avatar: "C", lastMessage: "这是您要的代码示例" },
  { id: "4", name: "营销专家", avatar: "M", lastMessage: "您的广告活动报告已准备好" },
  { id: "5", name: "客服机器人", avatar: "S", lastMessage: "感谢您的反馈" },
]

export function AgentSidebar() {
  const pathname = usePathname()
  const [searchQuery, setSearchQuery] = useState("")

  const filteredAgents = agents.filter((agent) => agent.name.toLowerCase().includes(searchQuery.toLowerCase()))

  return (
    <div className="w-[300px] border-r flex flex-col h-[calc(100vh-3.5rem)]">
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">我的代理</h2>
          <Button size="icon" variant="ghost">
            <Plus className="h-4 w-4" />
            <span className="sr-only">添加新代理</span>
          </Button>
        </div>
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            type="search"
            placeholder="搜索代理..."
            className="pl-8"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>
      <ScrollArea className="flex-1">
        <div className="p-2">
          {filteredAgents.map((agent) => (
            <Link
              key={agent.id}
              href={`/explore/chat/${agent.id}`}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-accent",
                pathname === `/explore/chat/${agent.id}` ? "bg-accent" : "transparent",
              )}
            >
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
                {agent.avatar}
              </div>
              <div className="flex-1 overflow-hidden">
                <div className="font-medium">{agent.name}</div>
                <div className="text-xs text-muted-foreground truncate">{agent.lastMessage}</div>
              </div>
            </Link>
          ))}
        </div>
      </ScrollArea>
      <div className="p-4 border-t">
        <Button className="w-full" asChild>
          <Link href="/studio/new">
            <Bot className="mr-2 h-4 w-4" />
            创建新代理
          </Link>
        </Button>
      </div>
    </div>
  )
}

