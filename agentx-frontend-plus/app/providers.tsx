"use client"

import { Toaster } from "@/components/ui/toaster"
import { useEffect } from "react"
import { getCookie } from "@/lib/utils"
import { AccountProvider } from "@/contexts/account-context"

export function Providers({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    // 页面加载时，从cookie中恢复token到localStorage
    const tokenFromCookie = getCookie("token")
    if (tokenFromCookie && typeof window !== "undefined") {
      // 如果localStorage中没有token，但cookie中有，则恢复它
      if (!localStorage.getItem("auth_token")) {
        localStorage.setItem("auth_token", tokenFromCookie)
      }
    }
  }, [])

  return (
    <AccountProvider>
      {children}
      <Toaster />
    </AccountProvider>
  )
} 