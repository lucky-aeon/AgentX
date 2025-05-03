import Link from "next/link"
import { Book, Edit, MoreHorizontal, Plus, Trash, Upload } from "lucide-react"

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

// Mock data for knowledge bases
const knowledgeBases = [
  { id: "1", name: "产品文档", description: "公司产品的详细文档", documents: 24, updatedAt: "2024-01-15" },
  { id: "2", name: "技术指南", description: "技术指南和教程", documents: 18, updatedAt: "2024-02-20" },
  { id: "3", name: "市场研究", description: "市场研究和分析报告", documents: 12, updatedAt: "2024-03-05" },
  { id: "4", name: "客户反馈", description: "客户反馈和评价", documents: 36, updatedAt: "2024-03-10" },
]

export default function KnowledgePage() {
  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">知识库</h1>
          <p className="text-muted-foreground">管理您的知识资源</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline">
            <Upload className="mr-2 h-4 w-4" />
            导入
          </Button>
          <Button asChild>
            <Link href="/knowledge/new">
              <Plus className="mr-2 h-4 w-4" />
              创建知识库
            </Link>
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {knowledgeBases.map((kb) => (
          <Card key={kb.id}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
                    <Book className="h-4 w-4" />
                  </div>
                  <CardTitle className="text-base">{kb.name}</CardTitle>
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
                      <Upload className="mr-2 h-4 w-4" />
                      添加文档
                    </DropdownMenuItem>
                    <DropdownMenuItem>
                      <Trash className="mr-2 h-4 w-4" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
              <CardDescription className="text-xs">更新于 {kb.updatedAt}</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground mb-2">{kb.description}</p>
              <div className="text-sm text-muted-foreground">文档数量: {kb.documents}</div>
            </CardContent>
            <CardFooter className="flex justify-between">
              <Button variant="outline" size="sm" asChild>
                <Link href={`/knowledge/edit/${kb.id}`}>
                  <Edit className="mr-2 h-4 w-4" />
                  编辑
                </Link>
              </Button>
              <Button size="sm" asChild>
                <Link href={`/knowledge/view/${kb.id}`}>
                  <Book className="mr-2 h-4 w-4" />
                  查看
                </Link>
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  )
}

