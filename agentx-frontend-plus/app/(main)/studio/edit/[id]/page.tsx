"use client"

import React, { useEffect, useState } from "react"
import { useRouter, useParams } from "next/navigation"
import { toast } from "@/hooks/use-toast"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Label } from "@/components/ui/label"
import { RefreshCw } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"

import {
  getAgentDetail,
  updateAgentWithToast,
  publishAgentVersionWithToast,
  deleteAgentWithToast,
  getAgentVersions,
  getAgentLatestVersion,
} from "@/lib/agent-service"
import { PublishStatus } from "@/types/agent"
import type { AgentVersion, AgentTool } from "@/types/agent"
import AgentFormModal from "@/components/agent-form-modal"
import type { AgentFormData } from "@/hooks/use-agent-form"

export default function EditAgentPage() {
  const router = useRouter()
  const params = useParams()
  const agentId = params.id as string
  
  // 编辑特有状态
  const [initialData, setInitialData] = useState<Partial<AgentFormData> | undefined>(undefined)
  const [isSubmitting, setIsSubmitting] = useState(false)
  
  // 版本管理状态
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [showPublishDialog, setShowPublishDialog] = useState(false)
  const [showVersionsDialog, setShowVersionsDialog] = useState(false)
  const [versionNumber, setVersionNumber] = useState("")
  const [changeLog, setChangeLog] = useState("")
  const [versions, setVersions] = useState<AgentVersion[]>([])
  const [selectedVersion, setSelectedVersion] = useState<AgentVersion | null>(null)
  const [latestVersion, setLatestVersion] = useState<AgentVersion | null>(null)
  const [isLoadingLatestVersion, setIsLoadingLatestVersion] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isPublishing, setIsPublishing] = useState(false)
  const [isLoadingVersions, setIsLoadingVersions] = useState(false)
  const [isRollingBack, setIsRollingBack] = useState(false)

  // 加载助理详情
  useEffect(() => {
    async function fetchAgentDetail() {
      try {
        const response = await getAgentDetail(agentId)

        if (response.code === 200 && response.data) {
          const agent = response.data

          // 处理工具数据
          let agentTools: AgentTool[] = []
          
          if (agent.tools && agent.tools.length > 0) {
            agentTools = agent.tools.map(t => ({ 
              id: t.id, 
              name: t.name, 
              description: t.description || undefined,
              presetParameters: t.presetParameters || {},
            }))
          } else if (agent.toolIds && agent.toolIds.length > 0) {
            agentTools = agent.toolIds.map(toolId => ({
                      id: toolId,
                      name: `工具 (ID: ${toolId.substring(0, 8)}...)`,
                      description: undefined,
                      presetParameters: {},
            }))
          }

          // 设置初始数据
          const formData: AgentFormData = {
            name: agent.name,
            avatar: agent.avatar,
            description: agent.description,
            systemPrompt: agent.systemPrompt,
            welcomeMessage: agent.welcomeMessage,
            tools: agentTools,
            knowledgeBaseIds: agent.knowledgeBaseIds || [],
            toolPresetParams: agent.toolPresetParams || {},
            enabled: agent.enabled,
            multiModal: agent.multiModal || false,
          }

          setInitialData(formData)
        } else {
          toast({
            title: "获取助理详情失败",
            description: response.message,
            variant: "destructive",
          })
          router.push("/studio")
        }
      } catch (error) {
 
        toast({
          title: "获取助理详情失败",
          description: "请稍后再试",
          variant: "destructive",
        })
        router.push("/studio")
      }
    }

    fetchAgentDetail()
  }, [agentId, router])

  // 获取助理最新版本
  const fetchLatestVersion = async () => {
    setIsLoadingLatestVersion(true)
    try {
      const response = await getAgentLatestVersion(agentId)
      
      if (response.code === 200) {
        setLatestVersion(response.data)
        
        if (response.data && response.data.versionNumber) {
          const versionParts = response.data.versionNumber.split('.')
          if (versionParts.length >= 3) {
            const major = parseInt(versionParts[0])
            const minor = parseInt(versionParts[1])
            const patch = parseInt(versionParts[2]) + 1
            setVersionNumber(`${major}.${minor}.${patch}`)
          } else {
            setVersionNumber(`${response.data.versionNumber}.1`)
          }
        } else {
          setVersionNumber("1.0.0")
        }
      } else {
        setVersionNumber("1.0.0")
      }
    } catch (error) {
 
      setVersionNumber("1.0.0")
    } finally {
      setIsLoadingLatestVersion(false)
    }
  }

  // 处理更新助理
  const handleUpdateAgent = async (formData: AgentFormData) => {
    if (!formData.name.trim()) {
      toast({
        title: "请输入名称",
        variant: "destructive",
      })
      return
    }

    setIsSubmitting(true)

    try {
      const toolIds = formData.tools.map(tool => tool.id);
      
      const agentData = {
        id: agentId,
        name: formData.name,
        avatar: formData.avatar,
        description: formData.description || "",
        systemPrompt: formData.systemPrompt,
        welcomeMessage: formData.welcomeMessage,
        toolIds: toolIds,
        knowledgeBaseIds: formData.knowledgeBaseIds,
        toolPresetParams: formData.toolPresetParams,
        enabled: formData.enabled,
        multiModal: formData.multiModal,
      }

      const response = await updateAgentWithToast(agentId, agentData)

      if (response.code === 200) {
        // toast已通过withToast处理
        // 更新初始数据以反映最新保存的状态
        setInitialData(formData)
      }
    } catch (error) {
 
      throw error // 重新抛出错误，让AgentFormModal处理loading状态
    } finally {
      setIsSubmitting(false)
    }
  }

  // 处理删除助理
  const handleDeleteAgent = async () => {
    setIsDeleting(true)

    try {
      const response = await deleteAgentWithToast(agentId)

      if (response.code === 200) {
        router.push("/studio")
      }
    } catch (error) {
 
    } finally {
      setIsDeleting(false)
      setShowDeleteDialog(false)
    }
  }



  // 处理发布助理版本
  const handlePublishVersion = async () => {
    if (!versionNumber.trim()) {
      toast({
        title: "请输入版本号",
        variant: "destructive",
      })
      return
    }

    setIsPublishing(true)

    try {
      // 需要从当前formData获取数据
      // 这里暂时使用initialData，实际应该从AgentFormModal获取当前状态
      if (!initialData) return

      const toolIds = initialData.tools?.map(tool => tool.id) || [];
      
      const response = await publishAgentVersionWithToast(agentId, {
        versionNumber,
        changeLog: changeLog || `发布 ${versionNumber} 版本`,
        systemPrompt: initialData.systemPrompt || "",
        welcomeMessage: initialData.welcomeMessage || "",
        toolIds: toolIds,
        knowledgeBaseIds: initialData.knowledgeBaseIds || [],
        toolPresetParams: initialData.toolPresetParams || {},
        multiModal: initialData.multiModal || false,
      })

      if (response.code === 200) {
        setShowPublishDialog(false)
        setVersionNumber("")
        setChangeLog("")
        fetchLatestVersion()
      }
    } catch (error) {
 
    } finally {
      setIsPublishing(false)
    }
  }

  // 打开发布对话框
  const openPublishDialog = async () => {
    await fetchLatestVersion()
    setShowPublishDialog(true)
  }

  // 加载助理版本列表
  const loadVersions = async () => {
    setIsLoadingVersions(true)
    setVersions([])

    try {
      const response = await getAgentVersions(agentId)

      if (response.code === 200) {
        setVersions(response.data)
      } else {
        toast({
          title: "获取版本列表失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
 
      toast({
        title: "获取版本列表失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    } finally {
      setIsLoadingVersions(false)
    }
  }

  // 查看版本详情
  const viewVersionDetail = async (version: AgentVersion) => {
    setSelectedVersion(version)
  }

  // 回滚到特定版本
  const rollbackToVersion = async (version: AgentVersion) => {
    if (!version) return

    setIsRollingBack(true)

    try {
      const formData: AgentFormData = {
        name: version.name,
        avatar: version.avatar,
        description: version.description,
        systemPrompt: version.systemPrompt,
        welcomeMessage: version.welcomeMessage,
        tools: version.tools?.map(t => ({
          id: t.id,
          name: t.name,
          description: t.description || undefined,
          presetParameters: t.presetParameters || {},
        })) || [],
        knowledgeBaseIds: version.knowledgeBaseIds || [],
        toolPresetParams: version.toolPresetParams || {},
        enabled: initialData?.enabled || true,
        multiModal: version.multiModal || false,
      }
      
      setInitialData(formData)

      toast({
        title: "回滚成功",
        description: `已回滚到版本 ${version.versionNumber}`,
      })

      setSelectedVersion(null)
      setShowVersionsDialog(false)
    } catch (error) {
 
      toast({
        title: "回滚失败",
        description: "请稍后再试",
        variant: "destructive",
      })
    } finally {
      setIsRollingBack(false)
    }
  }

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

  // 处理取消
  const handleCancel = () => {
    router.push("/studio")
  }

    return (
    <>
      <AgentFormModal
        mode="edit"
        agentId={agentId}
        initialData={initialData}
        title="编辑助理"
        onSubmit={handleUpdateAgent}
        onCancel={handleCancel}
        onDelete={() => setShowDeleteDialog(true)}
        onPublish={openPublishDialog}

        onShowVersions={() => {
              setShowVersionsDialog(true);
              loadVersions();
            }}
        isSubmitting={isSubmitting}
      >
      {/* 删除确认对话框 */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
              <DialogDescription>确定要删除这个助理吗？此操作无法撤销。</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDeleteAgent} disabled={isDeleting}>
              {isDeleting ? "删除中..." : "确认删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 发布版本对话框 */}
      <Dialog open={showPublishDialog} onOpenChange={setShowPublishDialog}>
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
                onChange={(e) => setVersionNumber(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="change-log">更新日志</Label>
              <Textarea
                id="change-log"
                placeholder="描述此版本的更新内容"
                rows={4}
                value={changeLog}
                onChange={(e) => setChangeLog(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowPublishDialog(false)}>
              取消
            </Button>
            <Button onClick={handlePublishVersion} disabled={isPublishing}>
              {isPublishing ? "发布中..." : "发布版本"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 版本历史对话框 */}
      <Dialog open={showVersionsDialog} onOpenChange={setShowVersionsDialog}>
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
                        <Button variant="outline" size="sm" className="mr-2" onClick={() => viewVersionDetail(version)}>
                          查看
                        </Button>
                        <Button size="sm" onClick={() => rollbackToVersion(version)} disabled={isRollingBack}>
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

      {/* 版本详情对话框 */}
      {selectedVersion && (
        <Dialog open={!!selectedVersion} onOpenChange={(open) => !open && setSelectedVersion(null)}>
          <DialogContent className="max-w-3xl max-h-[80vh] overflow-auto">
            <DialogHeader>
              <DialogTitle>版本详情: {selectedVersion.versionNumber}</DialogTitle>
              <DialogDescription>发布于 {new Date(selectedVersion.publishedAt).toLocaleString()}</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <h3 className="font-medium">更新日志</h3>
                <div className="p-3 bg-gray-50 rounded-md">{selectedVersion.changeLog}</div>
              </div>

              <div className="space-y-2">
                <h3 className="font-medium">配置信息</h3>
                <div className="space-y-1">
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">工具数量</span>
                    <span className="text-sm">{selectedVersion.tools.length}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-sm text-muted-foreground">知识库数量</span>
                    <span className="text-sm">{selectedVersion.knowledgeBaseIds.length}</span>
                  </div>
                </div>
              </div>

                  <div className="space-y-2">
                    <h3 className="font-medium">系统提示词</h3>
                    <div className="p-3 bg-gray-50 rounded-md text-sm">
                      {selectedVersion.systemPrompt || "无系统提示词"}
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h3 className="font-medium">欢迎消息</h3>
                    <div className="p-3 bg-gray-50 rounded-md text-sm">
                      {selectedVersion.welcomeMessage || "无欢迎消息"}
                    </div>
                  </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setSelectedVersion(null)}>
                关闭
              </Button>
              <Button onClick={() => rollbackToVersion(selectedVersion)} disabled={isRollingBack}>
                {isRollingBack ? "回滚中..." : "回滚到此版本"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
      </AgentFormModal>
    </>
  )
} 