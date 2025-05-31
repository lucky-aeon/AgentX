import React from "react"
import { RefreshCw } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { AgentVersion } from "@/types/agent"
import { PublishStatus } from "@/types/agent"

interface AgentVersionHistoryDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  isLoadingVersions: boolean
  versions: AgentVersion[]
  onViewVersion: (version: AgentVersion) => void
  onRollbackToVersion: (version: AgentVersion) => void
  isRollingBack: boolean
}

const AgentVersionHistoryDialog: React.FC<AgentVersionHistoryDialogProps> = ({
  open,
  onOpenChange,
  isLoadingVersions,
  versions,
  onViewVersion,
  onRollbackToVersion,
  isRollingBack,
}) => {
  // 获取发布状态文本
  const getPublishStatusText = (status: number) => {
    switch (status) {
      case PublishStatus.REVIEWING:
        return "审核中"
      case PublishStatus.PUBLISHED:
        return "已发布"
      case PublishStatus.REJECTED:
        return "已拒绝"
      case PublishStatus.REMOVED:
        return "已下架"
      default:
        return "未知状态"
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>版本历史</DialogTitle>
          <DialogDescription>查看和管理助理的历史版本</DialogDescription>
        </DialogHeader>
        <div className="flex-1 overflow-auto py-4">
          {isLoadingVersions ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-blue-500" />
              <span className="ml-2">加载版本历史...</span>
            </div>
          ) : versions.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">暂无版本历史</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>版本号</TableHead>
                  <TableHead>发布时间</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>更新日志</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {versions.map((version) => (
                  <TableRow key={version.id}>
                    <TableCell className="font-medium">{version.versionNumber}</TableCell>
                    <TableCell>{new Date(version.publishedAt).toLocaleString()}</TableCell>
                    <TableCell>
                      <Badge variant={version.publishStatus === PublishStatus.PUBLISHED ? "default" : "outline"}>
                        {getPublishStatusText(version.publishStatus)}
                      </Badge>
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate">{version.changeLog}</TableCell>
                    <TableCell className="text-right">
                      <Button variant="outline" size="sm" className="mr-2" onClick={() => onViewVersion(version)}>
                        查看
                      </Button>
                      <Button size="sm" onClick={() => onRollbackToVersion(version)} disabled={isRollingBack}>
                        {isRollingBack ? "回滚中..." : "回滚"}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default AgentVersionHistoryDialog 