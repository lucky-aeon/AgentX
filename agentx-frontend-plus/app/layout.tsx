import type React from "react"
import { ThemeProvider } from "@/components/theme-provider"
import { Toaster } from "@/components/ui/toaster"
import { NavigationBar } from "@/components/navigation-bar"
import { WorkspaceProvider } from "@/contexts/workspace-context"
import { cn } from "@/lib/utils"
import "./globals.css"

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <head>
        <title>AgentX Plus - AI 代理平台</title>
        <meta name="description" content="您的全方位 AI 代理平台" />
      </head>
      <body className={cn("min-h-screen bg-background font-sans antialiased")}>
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
          <WorkspaceProvider>
            <div className="relative flex min-h-screen flex-col">
              <NavigationBar />
              <div className="flex-1 flex">{children}</div>
            </div>
            <Toaster />
          </WorkspaceProvider>
        </ThemeProvider>
      </body>
    </html>
  )
}

