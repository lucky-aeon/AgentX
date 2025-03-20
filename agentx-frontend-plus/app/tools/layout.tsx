import type { ReactNode } from "react"
import { Sidebar } from "@/components/sidebar"

export default function ToolsLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <Sidebar />
      <div className="flex-1">{children}</div>
    </>
  )
}

