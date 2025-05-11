"use client"

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useRouter, usePathname } from "next/navigation"
import { useEffect, useState } from "react"
import MarketPage from "./page"
import MyToolsPage from "./my-tools"

export default function ToolsLayout() {
  const router = useRouter()
  const pathname = usePathname()
  const [activeTab, setActiveTab] = useState<string>("market")
  
  // 根据当前路径设置活动标签
  useEffect(() => {
    if (pathname === "/tools") {
      setActiveTab("market")
    } else if (pathname === "/tools/my-tools") {
      setActiveTab("my-tools")
    }
  }, [pathname])

  // 处理标签切换
  const handleTabChange = (value: string) => {
    setActiveTab(value)
    if (value === "market") {
      router.push("/tools")
    } else if (value === "my-tools") {
      router.push("/tools/my-tools")
    }
  }

  return (
    <div className="container py-6">
      <Tabs value={activeTab} onValueChange={handleTabChange} className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h1 className="text-3xl font-bold tracking-tight">工具中心</h1>
          <TabsList>
            <TabsTrigger value="market">工具市场</TabsTrigger>
            <TabsTrigger value="my-tools">我的工具</TabsTrigger>
          </TabsList>
        </div>
      </Tabs>
      
      {activeTab === "market" && <MarketPage />}
      {activeTab === "my-tools" && <MyToolsPage />}
    </div>
  )
} 