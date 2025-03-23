import type { ReactNode } from "react"

export default function StudioLayout({ children }: { children: ReactNode }) {
  return <div className="flex-1">{children}</div>
}

