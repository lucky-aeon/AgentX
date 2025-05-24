"use client"

import { useState } from "react"
import { Clock, Plus, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ScheduledTaskList } from "@/components/scheduled-task-list"
import { ScheduledTaskDialog } from "@/components/scheduled-task-dialog"

interface ScheduledTaskPanelProps {
  conversationId: string
  agentId?: string
  onClose?: () => void
}

export function ScheduledTaskPanel({ conversationId, agentId, onClose }: ScheduledTaskPanelProps) {
  const [createDialogOpen, setCreateDialogOpen] = useState(false)

  return (
    <div className="flex flex-col h-full bg-white">
      {/* 头部 */}
      <div className="flex items-center justify-between p-4 border-b">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-blue-600" />
          <h2 className="font-semibold text-gray-900">定时任务</h2>
        </div>
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            onClick={() => setCreateDialogOpen(true)}
            className="bg-blue-600 hover:bg-blue-700"
          >
            <Plus className="h-4 w-4 mr-1" />
            新建
          </Button>
          {onClose && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onClose}
            >
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      {/* 任务列表 */}
      <div className="flex-1 overflow-y-auto p-4">
        <ScheduledTaskList conversationId={conversationId} />
      </div>

      {/* 创建任务对话框 */}
      <ScheduledTaskDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        conversationId={conversationId}
        agentId={agentId}
        onTaskCreated={() => {
          // 任务创建成功后刷新列表
          window.location.reload() // 简单的刷新方式，实际项目中应该使用更优雅的方式
        }}
      />
    </div>
  )
} 