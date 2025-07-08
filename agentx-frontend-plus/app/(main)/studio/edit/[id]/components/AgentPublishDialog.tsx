import React from "react"
import { RefreshCw } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import type { AgentVersion } from "@/types/agent"

interface AgentPublishDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
  isPublishing: boolean
  isLoadingLatestVersion: boolean
  latestVersion: AgentVersion | null
  versionNumber: string
  onVersionNumberChange: (value: string) => void
  changeLog: string
  onChangeLogChange: (value: string) => void
}

const AgentPublishDialog: React.FC<AgentPublishDialogProps> = ({
  open,
  onOpenChange,
  onConfirm,
  isPublishing,
  isLoadingLatestVersion,
  latestVersion,
  versionNumber,
  onVersionNumberChange,
  changeLog,
  onChangeLogChange,
}) => {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>发布新版本</DialogTitle>
          <DialogDescription>发布新版本将创建当前配置的快照，用户可以使用此版本。</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          {isLoadingLatestVersion ? (
            <div className="flex items-center justify-center py-2">
              <RefreshCw className="h-4 w-4 animate-spin text-blue-500 mr-2" />
              <span className="text-sm">加载版本信息...</span>
            </div>
          ) : latestVersion ? (
            <div className="flex items-center p-2 bg-blue-50 rounded-md border border-blue-100 mb-2">
              <span className="text-sm text-blue-600">当前最新版本：{latestVersion.versionNumber}</span>
            </div>
          ) : (
            <div className="flex items-center p-2 bg-gray-50 rounded-md border border-gray-200 mb-2">
              <span className="text-sm text-gray-600">当前还没有发布过版本</span>
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="version-number">版本号</Label>
            <Input
              id="version-number"
              placeholder="例如: 1.0.0"
              value={versionNumber}
              onChange={(e) => onVersionNumberChange(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="change-log">更新日志</Label>
            <Textarea
              id="change-log"
              placeholder="描述此版本的更新内容"
              rows={4}
              value={changeLog}
              onChange={(e) => onChangeLogChange(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button onClick={onConfirm} disabled={isPublishing}>
            {isPublishing ? "发布中..." : "发布版本"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default AgentPublishDialog 