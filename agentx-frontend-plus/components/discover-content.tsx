"use client"

import { useState } from "react"
import { Bot, Book, FileText, Search, Sparkles, Terminal, Zap } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Input } from "@/components/ui/input"

// Mock data for applications
const applications = [
  {
    id: "1",
    title: "GPT-Researcher EN",
    description:
      "GPT-Reasearcher is an expert in internet topic research. It can efficiently decompose a topic into sub-questions and provide a professional research report from a comprehensive perspective.",
    category: "工作流",
    type: "Agent",
  },
  {
    id: "2",
    title: "Different types of exception handling",
    description:
      "Different types of exception handling. This is used to demonstrate how the downstream of the fail branch defines different strategies according to the error type of the error node for targeted processing.",
    category: "CHATFLOW",
    type: "助手",
  },
  {
    id: "3",
    title: "help me think(deepseek r1)",
    description: "An AI assistant that helps you think through complex problems step by step.",
    category: "CHATFLOW",
    type: "DeepSeek",
  },
  {
    id: "4",
    title: "ChatPaper",
    description: "Let's chat with paper!",
    category: "CHATFLOW",
    type: "媒体",
  },
  {
    id: "5",
    title: "URL-to-Cross-Platform-Copywriting",
    description:
      "This Chatflow allows users to input a URL and convert the full text of the webpage into a specified Tone writing style, while simultaneously outputting content for Instagram, X(Twitter), Threads, and RED(Xiaohongshu).",
    category: "CHATFLOW",
    type: "工作流",
  },
  {
    id: "6",
    title: "DeepResearch",
    description: "Input what you want to search for, and it will repeatedly execute searches to create a report.",
    category: "CHATFLOW",
    type: "工作流",
  },
  {
    id: "7",
    title: "File Translation",
    description: "An app that lets you upload files and translate them into any language you need.",
    category: "CHATFLOW",
    type: "工具",
  },
  {
    id: "8",
    title: "Error-handling-demo",
    description:
      "fail-branch function demonstration, enabling nodes to have built-in if-else branches and automatically switching to the failure branch when an exception is caught.",
    category: "工作流",
    type: "工作流",
  },
  {
    id: "9",
    title: "Vocabulary Flashcards (chatflow ver.)",
    description: "AI-powered vocabulary learning assistant with spaced repetition.",
    category: "CHATFLOW",
    type: "教育",
  },
  {
    id: "10",
    title: "Thinking Claude (OpenAI O1 Alternative)",
    description: "Let Claude think comprehensively before responding!",
    category: "AGENT",
    type: "Agent",
  },
  {
    id: "11",
    title: "NotebookLM by Dify.AI",
    description:
      "Using Dify.AI to easily replicate NotebookLM, demonstrating the entire life cycle from file upload, file parsing, and file referencing.",
    category: "CHATFLOW",
    type: "媒体",
  },
  {
    id: "12",
    title: "Intent recognition customer service",
    description:
      "Use conversation variables to manage the status of questions and solutions in customer service scenarios.",
    category: "CHATFLOW",
    type: "客服",
  },
]

// Function to get icon based on type
function getIconByType(type: string) {
  switch (type) {
    case "Agent":
      return <Bot className="h-8 w-8 text-yellow-500" />
    case "助手":
      return <Zap className="h-8 w-8 text-orange-500" />
    case "DeepSeek":
      return <Sparkles className="h-8 w-8 text-blue-500" />
    case "媒体":
      return <FileText className="h-8 w-8 text-purple-500" />
    case "工作流":
      return <Terminal className="h-8 w-8 text-green-500" />
    case "工具":
      return <Zap className="h-8 w-8 text-red-500" />
    case "教育":
      return <Book className="h-8 w-8 text-cyan-500" />
    case "客服":
      return <Zap className="h-8 w-8 text-pink-500" />
    default:
      return <Bot className="h-8 w-8 text-gray-500" />
  }
}

export function DiscoverContent() {
  const [searchQuery, setSearchQuery] = useState("")

  const filteredApps = applications.filter(
    (app) =>
      app.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      app.description.toLowerCase().includes(searchQuery.toLowerCase()),
  )

  return (
    <div className="container py-6 px-4">
      <div className="mb-6">
        <h1 className="text-2xl font-bold tracking-tight text-blue-600">探索 AgentX 的应用</h1>
        <p className="text-muted-foreground mt-1">使用这些模板应用程序，或根据模板自定义您自己的应用程序。</p>
      </div>

      <Tabs defaultValue="推荐" className="space-y-6">
        <div className="flex justify-between items-center">
          <TabsList>
            <TabsTrigger value="推荐" className="flex items-center gap-1">
              <Sparkles className="h-4 w-4" />
              推荐
            </TabsTrigger>
            <TabsTrigger value="Agent">Agent</TabsTrigger>
            <TabsTrigger value="助手">助手</TabsTrigger>
            <TabsTrigger value="DeepSeek">DeepSeek</TabsTrigger>
            <TabsTrigger value="媒体">媒体</TabsTrigger>
            <TabsTrigger value="工作流">工作流</TabsTrigger>
            <TabsTrigger value="写作">写作</TabsTrigger>
          </TabsList>

          <div className="relative">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="搜索..."
              className="pl-8 w-[250px]"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        <TabsContent value="推荐" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredApps.map((app) => (
              <Card key={app.id} className="overflow-hidden group relative">
                <CardHeader className="p-4 pb-0 flex flex-row items-start gap-3">
                  {getIconByType(app.type)}
                  <div className="flex-1">
                    <CardTitle className="text-base">{app.title}</CardTitle>
                    <CardDescription className="text-xs mt-1">{app.category}</CardDescription>
                  </div>
                </CardHeader>
                <CardContent className="p-4 pt-3">
                  <p className="text-sm text-muted-foreground line-clamp-3">{app.description}</p>
                </CardContent>
                <div className="absolute inset-0 flex items-end justify-center opacity-0 group-hover:opacity-100 transition-opacity pb-4">
                  <Button className="bg-blue-500 hover:bg-blue-600 text-white w-[90%]">添加到工作区</Button>
                </div>
              </Card>
            ))}
          </div>
        </TabsContent>

        {["Agent", "助手", "DeepSeek", "媒体", "工作流", "写作"].map((tab) => (
          <TabsContent key={tab} value={tab} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredApps
                .filter((app) => app.type === tab)
                .map((app) => (
                  <Card key={app.id} className="overflow-hidden group relative">
                    <CardHeader className="p-4 pb-0 flex flex-row items-start gap-3">
                      {getIconByType(app.type)}
                      <div className="flex-1">
                        <CardTitle className="text-base">{app.title}</CardTitle>
                        <CardDescription className="text-xs mt-1">{app.category}</CardDescription>
                      </div>
                    </CardHeader>
                    <CardContent className="p-4 pt-3">
                      <p className="text-sm text-muted-foreground line-clamp-3">{app.description}</p>
                    </CardContent>
                    <div className="absolute inset-0 flex items-end justify-center opacity-0 group-hover:opacity-100 transition-opacity pb-4">
                      <Button className="bg-blue-500 hover:bg-blue-600 text-white w-[90%]">添加到工作区</Button>
                    </div>
                  </Card>
                ))}
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}

