import Link from "next/link"
import { Bot, Edit, MoreHorizontal, Plus, Trash } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

// Mock data for agents
const myAgents = [
  { id: "1", name: "客服助手", description: "处理客户查询和问题", createdAt: "2023-10-15" },
  { id: "2", name: "数据分析师", description: "分析和可视化数据", createdAt: "2023-11-20" },
  { id: "3", name: "内容创作者", description: "生成各种类型的内容", createdAt: "2023-12-05" },
  { id: "4", name: "编程助手", description: "帮助编写和调试代码", createdAt: "2024-01-10" },
]

export default function StudioPage() {
  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">工作室</h1>
          <p className="text-muted-foreground">创建和管理您的 AI 代理</p>
        </div>
        <Button asChild>
          <Link href="/studio/new">
            <Plus className="mr-2 h-4 w-4" />
            创建新代理
          </Link>
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {myAgents.map((agent) => (
          <Card key={agent.id}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <Bot className="h-4 w-4" />
                  </div>
                  <CardTitle className="text-base">{agent.name}</CardTitle>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon">
                      <MoreHorizontal className="h-4 w-4" />
                      <span className="sr-only">打开菜单</span>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuLabel>操作</DropdownMenuLabel>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem>
                      <Edit className="mr-2 h-4 w-4" />
                      编辑
                    </DropdownMenuItem>
                    <DropdownMenuItem>
                      <Trash className="mr-2 h-4 w-4" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
              <CardDescription className="text-xs">创建于 {agent.createdAt}</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">{agent.description}</p>
            </CardContent>
            <CardFooter className="flex justify-between">
              <Button variant="outline" size="sm" asChild>
                <Link href={`/studio/edit/${agent.id}`}>
                  <Edit className="mr-2 h-4 w-4" />
                  编辑
                </Link>
              </Button>
              <Button size="sm" asChild>
                <Link href={`/explore/chat/${agent.id}`}>
                  <Bot className="mr-2 h-4 w-4" />
                  对话
                </Link>
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  )
}

