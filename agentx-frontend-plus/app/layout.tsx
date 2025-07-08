import type React from "react"
import { Providers } from "./providers"
import { ThemeProvider } from "@/components/theme-provider"
import "@/styles/globals.css"

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN" suppressHydrationWarning>
      <head>
        <title>AgentX</title>
        <meta name="description" content="您的全方位 AI 代理平台" />
      </head>
      <body>
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
          <Providers>{children}</Providers>
        </ThemeProvider>
      </body>
    </html>
  )
}

