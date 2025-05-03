import type { ReactNode } from "react"

export default function KnowledgeLayout({ children }: { children: ReactNode }) {
  return <div className="flex-1">{children}</div>
}

