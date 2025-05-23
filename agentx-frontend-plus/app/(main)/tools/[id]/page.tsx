"use client"

import { useEffect } from "react"
import { useRouter } from "next/navigation"
import { Skeleton } from "@/components/ui/skeleton"
import { getMarketToolDetailWithToast, getMarketToolVersionsWithToast } from "@/lib/tool-service"

export default function ToolDetailRedirectPage({ params }: { params: { id: string } & Promise<{ id: string }> }) {
  const router = useRouter()
  const { id } = params;

  useEffect(() => {
    async function redirectToNewFormat() {
      try {
        // 尝试获取工具信息，确定是工具ID还是版本ID
        const detailResponse = await getMarketToolDetailWithToast(id)
        
        if (detailResponse.code === 200) {
          // 如果是工具ID，获取最新版本
          const versionsResponse = await getMarketToolVersionsWithToast(id)
          if (versionsResponse.code === 200 && versionsResponse.data.length > 0) {
            // 找到最新版本并重定向
            const latestVersion = versionsResponse.data[0]
            router.replace(`/tools/${id}/${latestVersion.version}`)
          } else {
            // 无法获取版本信息，使用默认版本
            router.replace(`/tools/${id}/0.0.1`)
          }
        } else {
          // ID可能是版本ID，尝试查找工具ID
          try {
            // 这里可能需要通过版本ID查询工具ID的API
            // 由于没有直接的API，我们可能需要查询工具市场列表
            // 此处简化处理，暂时使用市场工具列表API
            const marketResponse = await fetch(`/api/tools/market?versionId=${id}`)
            if (marketResponse.ok) {
              const data = await marketResponse.json()
              if (data.code === 200 && data.data.records && data.data.records.length > 0) {
                const toolInfo = data.data.records[0]
                router.replace(`/tools/${toolInfo.toolId}/${toolInfo.version}`)
              } else {
                // 无法找到对应的工具，返回工具市场
                router.replace('/tools-market')
              }
            } else {
              // 查询失败，返回工具市场
              router.replace('/tools-market')
            }
          } catch (error) {
            console.error("重定向查询失败", error)
            router.replace('/tools-market')
          }
        }
      } catch (error) {
        console.error("重定向失败", error)
        router.replace('/tools-market')
      }
    }

    redirectToNewFormat()
  }, [id, router])

  // 显示加载状态
  return (
    <div className="container py-6">
      <div className="text-center py-20">
        <Skeleton className="h-8 w-64 mx-auto mb-4" />
        <Skeleton className="h-4 w-96 mx-auto mb-2" />
        <Skeleton className="h-4 w-80 mx-auto" />
        <p className="mt-8 text-muted-foreground">正在加载工具详情...</p>
            </div>
    </div>
  )
} 