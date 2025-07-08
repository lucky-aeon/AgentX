"use client"

import { Search, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ApiKeyStatus } from "@/types/api-key"
import type { Agent } from "@/types/agent"

interface ApiKeyFiltersProps {
  searchQuery: string
  onSearchChange: (value: string) => void
  statusFilter: boolean | "ALL"
  onStatusFilterChange: (value: boolean | "ALL") => void
  agentFilter: string
  onAgentFilterChange: (value: string) => void
  agents: Agent[]
  onRefresh: () => void
  loading: boolean
}

export function ApiKeyFilters({
  searchQuery,
  onSearchChange,
  statusFilter,
  onStatusFilterChange,
  agentFilter,
  onAgentFilterChange,
  agents,
  onRefresh,
  loading
}: ApiKeyFiltersProps) {
  return (
    <div className="flex flex-col sm:flex-row gap-4">
      {/* 搜索框 */}
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
        <Input
          placeholder="搜索API密钥名称..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-10"
        />
      </div>
      
      {/* 状态筛选 */}
      <Select
        value={statusFilter === "ALL" ? "ALL" : statusFilter.toString()}
        onValueChange={(value) => {
          if (value === "ALL") {
            onStatusFilterChange("ALL")
          } else {
            onStatusFilterChange(value === "true")
          }
        }}
      >
        <SelectTrigger className="w-[180px]">
          <SelectValue placeholder="选择状态" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">全部状态</SelectItem>
          <SelectItem value="true">已启用</SelectItem>
          <SelectItem value="false">已禁用</SelectItem>
        </SelectContent>
      </Select>
      
      {/* Agent筛选 */}
      <Select value={agentFilter} onValueChange={onAgentFilterChange}>
        <SelectTrigger className="w-[200px]">
          <SelectValue placeholder="选择Agent" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="ALL">全部Agent</SelectItem>
          {agents.map((agent) => (
            <SelectItem key={agent.id} value={agent.id}>
              {agent.name}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      
      {/* 刷新按钮 */}
      <Button
        variant="outline"
        size="icon"
        onClick={onRefresh}
        disabled={loading}
      >
        <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
      </Button>
    </div>
  )
}