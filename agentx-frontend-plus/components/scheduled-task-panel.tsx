"use client"

import { useState } from "react"
import { Clock, Plus, X } from "lucide-react"
import { Button } from "@/components/ui/button"
import { ScheduledTaskList } from "./scheduled-task-list"
import { ScheduledTaskDialog } from "./scheduled-task-dialog"
import { type ScheduledTaskDTO } from "@/lib/scheduled-task-service"

interface ScheduledTaskPanelProps {
  isOpen: boolean
  onClose: () => void
  conversationId: string
  agentId?: string
}

export function ScheduledTaskPanel({ 
  isOpen, 
  onClose, 
  conversationId,
  agentId 
}: ScheduledTaskPanelProps) {
  const [showCreateDialog, setShowCreateDialog] = useState(false)
  const [editingTask, setEditingTask] = useState<ScheduledTaskDTO | null>(null)
  const [refreshKey, setRefreshKey] = useState(0)

  const handleTaskCreated = () => {
    setRefreshKey(prev => prev + 1)
  }

  const handleTaskUpdate = () => {
    setRefreshKey(prev => prev + 1)
  }

  const handleEditTask = (task: ScheduledTaskDTO) => {
    setEditingTask(task)
    setShowCreateDialog(true)
  }

  const handleCloseDialog = (open: boolean) => {
    setShowCreateDialog(open)
    if (!open) {
      setEditingTask(null)
    }
  }

  if (!isOpen) return null

  return (
    <div className="w-[400px] h-full border-l bg-white flex flex-col">
      {/* 头部 */}
      <div className="flex items-center justify-between p-4 border-b">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-gray-600" />
          <h2 className="font-semibold text-gray-900">定时任务</h2>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowCreateDialog(true)}
            className="h-8"
          >
            <Plus className="h-4 w-4 mr-1" />
            新建
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="h-8 w-8 p-0"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* 任务列表 */}
      <div className="flex-1 overflow-y-auto p-4">
        <ScheduledTaskList 
          key={refreshKey}
          onTaskUpdate={handleTaskUpdate}
          onEditTask={handleEditTask}
          agentId={agentId}
        />
      </div>

      {/* 创建/编辑任务对话框 */}
      <ScheduledTaskDialog
        open={showCreateDialog}
        onOpenChange={handleCloseDialog}
        conversationId={conversationId}
        agentId={agentId}
        onTaskCreated={handleTaskCreated}
        editingTask={editingTask}
        onTaskUpdated={handleTaskUpdate}
      />
    </div>
  )
} 