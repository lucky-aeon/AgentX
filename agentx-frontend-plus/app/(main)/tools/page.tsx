import Link from "next/link"
import { Edit, MoreHorizontal, Plus, Trash, Wrench } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Badge } from "@/components/ui/badge"

// Mock data for tools
const tools = [
  {
    id: "1",
    name: "网页浏览器",
    description: "允许代理浏览网页和提取信息",
    status: "已启用",
    type: "内置",
  },
  {
    id: "2",
    name: "文件处理器",
    description: "允许代理读取和处理各种文件格式",
    status: "已启用",
    type: "内置",
  },
  {
    id: "3",
    name: "数据分析",
    description: "提供数据分析和可视化功能",
    status: "已禁用",
    type: "自定义",
  },
  {
    id: "4",
    name: "API 连接器",
    description: "连接到外部 API 和服务",
    status: "已启用",
    type: "自定义",
  },
]

export default function ToolsPage() {
  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">工具</h1>
          <p className="text-muted-foreground">管理您的 AI 工具</p>
        </div>
        <Button asChild>
          <Link href="/tools/new">
            <Plus className="mr-2 h-4 w-4" />
            添加工具
          </Link>
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {tools.map((tool) => (
          <Card key={tool.id}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <Wrench className="h-4 w-4" />
                  </div>
                  <div>
                    <CardTitle className="text-base">{tool.name}</CardTitle>
                    <div className="flex items-center gap-2 mt-1">
                      <Badge variant={tool.status === "已启用" ? "default" : "secondary"} className="text-[10px]">
                        {tool.status}
                      </Badge>
                      <Badge variant="outline" className="text-[10px]">
                        {tool.type}
                      </Badge>
                    </div>
                  </div>
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
                      配置
                    </DropdownMenuItem>
                    <DropdownMenuItem>
                      {tool.status === "已启用" ? (
                        <>
                          <Trash className="mr-2 h-4 w-4" />
                          禁用
                        </>
                      ) : (
                        <>
                          <Wrench className="mr-2 h-4 w-4" />
                          启用
                        </>
                      )}
                    </DropdownMenuItem>
                    {tool.type === "自定义" && (
                      <DropdownMenuItem>
                        <Trash className="mr-2 h-4 w-4" />
                        删除
                      </DropdownMenuItem>
                    )}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">{tool.description}</p>
            </CardContent>
            <CardFooter className="flex justify-between">
              <Button variant="outline" size="sm" asChild>
                <Link href={`/tools/configure/${tool.id}`}>
                  <Edit className="mr-2 h-4 w-4" />
                  配置
                </Link>
              </Button>
              <Button size="sm" variant={tool.status === "已启用" ? "destructive" : "default"}>
                {tool.status === "已启用" ? "禁用" : "启用"}
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  )
}

