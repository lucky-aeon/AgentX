import Link from "next/link"
import { ArrowLeft, Bot, FileText, MessageCircle, Sparkles } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"

// 模板数据
const templates = [
  {
    id: "1",
    title: "客服助手",
    description: "快速回答客户问题，提供产品信息和解决方案",
    category: "聊天助手",
    icon: MessageCircle,
    color: "bg-blue-100 text-blue-600",
  },
  {
    id: "2",
    title: "内容创作助手",
    description: "帮助创作者生成文章、博客、社交媒体内容等",
    category: "文本生成",
    icon: FileText,
    color: "bg-emerald-100 text-emerald-600",
  },
  {
    id: "3",
    title: "研究助手",
    description: "帮助用户进行深度研究，提供信息汇总和分析",
    category: "Agent",
    icon: Bot,
    color: "bg-purple-100 text-purple-600",
  },
  {
    id: "4",
    title: "产品推荐助手",
    description: "根据用户需求推荐合适的产品和服务",
    category: "聊天助手",
    icon: MessageCircle,
    color: "bg-blue-100 text-blue-600",
  },
  {
    id: "5",
    title: "学习辅导助手",
    description: "帮助学生解答问题，提供学习资料和指导",
    category: "Agent",
    icon: Bot,
    color: "bg-purple-100 text-purple-600",
  },
  {
    id: "6",
    title: "营销文案生成器",
    description: "生成各种营销场景的专业文案",
    category: "文本生成",
    icon: FileText,
    color: "bg-emerald-100 text-emerald-600",
  },
]

export default function TemplatesPage() {
  return (
    <div className="container py-8">
      <div className="flex items-center mb-8">
        <Button variant="ghost" size="icon" asChild className="mr-2">
          <Link href="/studio/new">
            <ArrowLeft className="h-5 w-5" />
            <span className="sr-only">返回</span>
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">应用模板</h1>
          <p className="text-muted-foreground">选择一个模板快速开始</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {templates.map((template) => (
          <Card key={template.id} className="overflow-hidden group">
            <CardHeader className="p-4 pb-0 flex flex-row items-start gap-3">
              <div className={`${template.color} w-10 h-10 rounded-lg flex items-center justify-center`}>
                <template.icon className="h-5 w-5" />
              </div>
              <div className="flex-1">
                <CardTitle className="text-base">{template.title}</CardTitle>
                <CardDescription className="text-xs mt-1">{template.category}</CardDescription>
              </div>
            </CardHeader>
            <CardContent className="p-4 pt-3">
              <p className="text-sm text-muted-foreground">{template.description}</p>
            </CardContent>
            <CardFooter className="p-4 pt-0">
              <Button variant="outline" size="sm" className="w-full" asChild>
                <Link href={`/studio/new?template=${template.id}`}>
                  <Sparkles className="mr-2 h-4 w-4" />
                  使用此模板
                </Link>
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  )
}

