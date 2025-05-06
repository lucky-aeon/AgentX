import type React from "react"
import { Providers } from "../providers"
import { ThemeProvider } from "@/components/theme-provider"
import { NavigationBar } from "@/components/navigation-bar"
import { WorkspaceProvider } from "@/contexts/workspace-context"

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <Providers>
        <WorkspaceProvider>
          <div className="relative flex min-h-screen flex-col">
            <NavigationBar />
            <div className="flex-1 flex">{children}</div>
          </div>
        </WorkspaceProvider>
      </Providers>
    </ThemeProvider>
  )
} 