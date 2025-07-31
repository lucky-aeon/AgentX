"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { X } from "lucide-react"

import type { RagDataset } from "@/types/rag-dataset"
import type { PublishRagRequest } from "@/types/rag-publish"
import { publishRagVersionWithToast, getLatestVersionNumber } from "@/lib/rag-publish-service"

interface PublishRagDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  dataset: RagDataset | null
  onSuccess?: () => void
}

export function PublishRagDialog({ 
  open, 
  onOpenChange, 
  dataset, 
  onSuccess 
}: PublishRagDialogProps) {
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formData, setFormData] = useState({
    version: "",
    changeLog: "",
    labels: [] as string[]
  })
  const [currentLabel, setCurrentLabel] = useState("")

  // 版本号自动递增逻辑
  useEffect(() => {
    if (open && dataset) {
      loadAndSetVersion()
    }
  }, [open, dataset])

  const loadAndSetVersion = async () => {
    if (!dataset) return
    
    try {
      const response = await getLatestVersionNumber(dataset.id)
      
      if (response.code === 200 && response.data) {
        // 有最新版本号，自动递增
        const currentVersion = response.data
        const nextVersion = incrementVersion(currentVersion)
        setFormData(prev => ({ ...prev, version: nextVersion }))
      } else {
        // 没有版本号，使用默认的1.0.0
        setFormData(prev => ({ ...prev, version: "1.0.0" }))
      }
    } catch (error) {
      // 出错时使用默认版本号
      setFormData(prev => ({ ...prev, version: "1.0.0" }))
    }
  }

  const incrementVersion = (version: string): string => {
    const parts = version.split('.')
    if (parts.length >= 3) {
      const major = parseInt(parts[0])
      const minor = parseInt(parts[1])
      const patch = parseInt(parts[2]) + 1
      return `${major}.${minor}.${patch}`
    }
    return "1.0.0"
  }

  // 重置表单
  const resetForm = () => {
    setFormData({
      version: "",
      changeLog: "",
      labels: []
    })
    setCurrentLabel("")
  }

  // 处理对话框关闭
  const handleOpenChange = (open: boolean) => {
    if (!open) {
      resetForm()
    }
    onOpenChange(open)
  }

  // 添加标签
  const addLabel = () => {
    const label = currentLabel.trim()
    if (label && !formData.labels.includes(label)) {
      setFormData(prev => ({
        ...prev,
        labels: [...prev.labels, label]
      }))
      setCurrentLabel("")
    }
  }

  // 删除标签
  const removeLabel = (labelToRemove: string) => {
    setFormData(prev => ({
      ...prev,
      labels: prev.labels.filter(label => label !== labelToRemove)
    }))
  }

  // 处理键盘事件
  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      addLabel()
    }
  }

  // 提交表单
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!dataset) return

    // 表单验证
    if (!formData.version.trim()) {
      return
    }

    setIsSubmitting(true)

    try {
      const request: PublishRagRequest = {
        ragId: dataset.id,
        version: formData.version.trim(),
        changeLog: formData.changeLog.trim() || undefined,
        labels: formData.labels.length > 0 ? formData.labels : undefined
      }

      const response = await publishRagVersionWithToast(request)
      
      if (response.code === 200) {
        handleOpenChange(false)
        onSuccess?.()
      }
    } catch (error) {
      // 错误已由withToast处理
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!dataset) return null

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>发布到市场</DialogTitle>
          <DialogDescription>
            将知识库"{dataset.name}"发布到市场供其他用户使用
          </DialogDescription>
        </DialogHeader>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="version">版本号 *</Label>
            <Input
              id="version"
              placeholder="例如: 1.0.0"
              value={formData.version}
              onChange={(e) => setFormData(prev => ({ ...prev, version: e.target.value }))}
              required
            />
            <p className="text-xs text-muted-foreground">
              版本号会自动递增，您也可以手动修改为更高的版本号
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="changeLog">更新日志</Label>
            <Textarea
              id="changeLog"
              placeholder="描述本次发布的更新内容..."
              value={formData.changeLog}
              onChange={(e) => setFormData(prev => ({ ...prev, changeLog: e.target.value }))}
              rows={3}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="labels">标签</Label>
            <div className="flex gap-2">
              <Input
                id="labels"
                placeholder="输入标签并按回车"
                value={currentLabel}
                onChange={(e) => setCurrentLabel(e.target.value)}
                onKeyPress={handleKeyPress}
              />
              <Button 
                type="button" 
                variant="outline" 
                onClick={addLabel}
                disabled={!currentLabel.trim()}
              >
                添加
              </Button>
            </div>
            {formData.labels.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-2">
                {formData.labels.map((label, index) => (
                  <Badge key={index} variant="secondary" className="gap-1">
                    {label}
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-4 w-4 p-0 hover:bg-transparent"
                      onClick={() => removeLabel(label)}
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  </Badge>
                ))}
              </div>
            )}
          </div>

          <div className="bg-muted/50 p-3 rounded-lg">
            <h4 className="font-medium mb-2">发布信息</h4>
            <div className="space-y-1 text-sm text-muted-foreground">
              <div>数据集: {dataset.name}</div>
              <div>文件数量: {dataset.fileCount}</div>
              <div>描述: {dataset.description || "无"}</div>
            </div>
          </div>

          <DialogFooter>
            <Button 
              type="button" 
              variant="outline" 
              onClick={() => handleOpenChange(false)}
            >
              取消
            </Button>
            <Button 
              type="submit" 
              disabled={isSubmitting || !formData.version.trim()}
            >
              {isSubmitting ? "发布中..." : "发布"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}