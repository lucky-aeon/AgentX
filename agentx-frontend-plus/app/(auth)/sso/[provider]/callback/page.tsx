"use client"

import { useEffect, use } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import { handleSsoCallbackApi } from "@/lib/api-services"
import { setCookie } from "@/lib/utils"

interface SsoCallbackPageProps {
  params: Promise<{ provider: string }>
}

export default function SsoCallbackPage({ params }: SsoCallbackPageProps) {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { provider } = use(params)

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const code = searchParams.get("code")
        if (!code) {
          toast({
            variant: "destructive",
            title: "登录失败",
            description: "未获取到授权码"
          })
          router.push("/login")
          return
        }

        const res = await handleSsoCallbackApi(provider, code)
        if (res.code === 200 && res.data?.token) {
          localStorage.setItem("auth_token", res.data.token)
          setCookie("token", res.data.token, 30)
          const providerName = provider === 'community' ? '敲鸭' : provider.toUpperCase()
          toast({
            title: "登录成功",
            description: `使用 ${providerName} 登录成功`
          })
          router.push("/")
        } else {
          toast({
            variant: "destructive",
            title: "登录失败",
            description: res.message || "登录过程中出现错误"
          })
          router.push("/login")
        }
      } catch (error) {
        console.error("SSO回调处理失败:", error)
        toast({
          variant: "destructive",
          title: "登录失败",
          description: "登录过程中出现错误，请重试"
        })
        router.push("/login")
      }
    }

    handleCallback()
  }, [searchParams, router, provider])

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
        <p className="text-muted-foreground">正在处理登录...</p>
      </div>
    </div>
  )
}