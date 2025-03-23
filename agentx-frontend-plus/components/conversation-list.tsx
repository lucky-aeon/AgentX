"use client"

import { useEffect, useState } from "react"
import { Plus, MoreHorizontal, Archive, Edit, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { useWorkspace } from "@/contexts/workspace-context"
import type { Session } from "@/types/conversation"
import { createSession, deleteSession, getSessions, updateSession } from "@/lib/api-services"
import { toast } from "@/components/ui/use-toast"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"

interface ConversationListProps {
  workspaceId: string
}

export function ConversationList({ workspaceId }: ConversationListProps) {
  const { selectedConversationId, setSelectedConversationId } = useWorkspace()
  const [sessions, setSessions] = useState<Session[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState("")
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isRenameDialogOpen, setIsRenameDialogOpen] = useState(false)
  const [newSessionTitle, setNewSessionTitle] = useState("")
  const [newSessionDescription, setNewSessionDescription] = useState("")
  const [sessionToRename, setSessionToRename] = useState<Session | null>(null)
  const [renameTitle, setRenameTitle] = useState("")
  const [renameDescription, setRenameDescription] = useState("")

  // 获取会话列表
  const fetchSessions = async () => {
    try {
      setLoading(true)
      // 这里使用固定的userId=1，实际应用中应该从用户认证中获取
      const response = await getSessions({ userId: "1" })
      if (response.code === 200) {
        setSessions(response.data)
        // 如果有会话但没有选中的会话，则选中第一个
        if (response.data.length > 0 && !selectedConversationId) {
          setSelectedConversationId(response.data[0].id)
        }
      } else {
        toast({
          title: "获取会话列表失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("获取会话列表错误:", error)
      toast({
        title: "获取会话列表失败",
        description: "请检查网络连接并稍后再试",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  // 创建新会话
  const handleCreateSession = async () => {
    if (!newSessionTitle.trim()) {
      toast({
        title: "创建失败",
        description: "会话标题不能为空",
        variant: "destructive",
      })
      return
    }

    try {
      const response = await createSession({
        title: newSessionTitle,
        userId: "1", // 固定用户ID
        description: newSessionDescription || undefined,
      })

      if (response.code === 200) {
        toast({
          title: "创建成功",
          description: "新会话已创建",
        })
        // 重新获取会话列表
        fetchSessions()
        // 清空表单
        setNewSessionTitle("")
        setNewSessionDescription("")
        // 关闭对话框
        setIsCreateDialogOpen(false)
        // 选中新创建的会话
        setSelectedConversationId(response.data.id)
      } else {
        toast({
          title: "创建失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("创建会话错误:", error)
      toast({
        title: "创建失败",
        description: "请检查网络连接并稍后再试",
        variant: "destructive",
      })
    }
  }

  // 打开重命名对话框
  const openRenameDialog = (session: Session) => {
    setSessionToRename(session)
    setRenameTitle(session.title)
    setRenameDescription(session.description || "")
    setIsRenameDialogOpen(true)
  }

  // 重命名会话
  const handleRenameSession = async () => {
    if (!sessionToRename) return

    if (!renameTitle.trim()) {
      toast({
        title: "重命名失败",
        description: "会话标题不能为空",
        variant: "destructive",
      })
      return
    }

    try {
      const response = await updateSession(sessionToRename.id, {
        title: renameTitle,
        description: renameDescription || undefined,
      })

      if (response.code === 200) {
        toast({
          title: "重命名成功",
          description: "会话已更新",
        })
        // 重新获取会话列表
        fetchSessions()
        // 关闭对话框
        setIsRenameDialogOpen(false)
        setSessionToRename(null)
      } else {
        toast({
          title: "重命名失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("重命名会话错误:", error)
      toast({
        title: "重命名失败",
        description: "请检查网络连接并稍后再试",
        variant: "destructive",
      })
    }
  }

  // 归档会话
  const handleArchiveSession = async (sessionId: string) => {
    try {
      const response = await updateSession(sessionId, { archived: true })
      if (response.code === 200) {
        toast({
          title: "归档成功",
          description: "会话已归档",
        })
        // 重新获取会话列表
        fetchSessions()
        // 如果归档的是当前选中的会话，则清除选中状态
        if (selectedConversationId === sessionId) {
          setSelectedConversationId(null)
        }
      } else {
        toast({
          title: "归档失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("归档会话错误:", error)
      toast({
        title: "归档失败",
        description: "请检查网络连接并稍后再试",
        variant: "destructive",
      })
    }
  }

  // 删除会话
  const handleDeleteSession = async (sessionId: string) => {
    try {
      const response = await deleteSession(sessionId)
      if (response.code === 200) {
        toast({
          title: "删除成功",
          description: "会话已删除",
        })
        // 重新获取会话列表
        fetchSessions()
        // 如果删除的是当前选中的会话，则清除选中状态
        if (selectedConversationId === sessionId) {
          setSelectedConversationId(null)
        }
      } else {
        toast({
          title: "删除失败",
          description: response.message,
          variant: "destructive",
        })
      }
    } catch (error) {
      console.error("删除会话错误:", error)
      toast({
        title: "删除失败",
        description: "请检查网络连接并稍后再试",
        variant: "destructive",
      })
    }
  }

  // 选择会话
  const selectConversation = (sessionId: string) => {
    setSelectedConversationId(sessionId)
  }

  // 过滤会话列表
  const filteredSessions = sessions.filter(
    (session) =>
      !session.archived && // 只显示未归档的会话
      session.title.toLowerCase().includes(searchQuery.toLowerCase()),
  )

  // 初始加载时获取会话列表
  useEffect(() => {
    fetchSessions()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId])

  return (
    <div className="w-[320px] border-r flex flex-col h-full bg-white">
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">会话列表</h2>
          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="ghost" size="icon" className="h-6 w-6 conversation-list-create-button">
                <Plus className="h-4 w-4" />
                <span className="sr-only">新建会话</span>
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>创建新会话</DialogTitle>
                <DialogDescription>创建一个新的会话，开始与AI助手交流。</DialogDescription>
              </DialogHeader>
              <div className="grid gap-4 py-4">
                <div className="grid gap-2">
                  <Label htmlFor="title">标题</Label>
                  <Input
                    id="title"
                    placeholder="输入会话标题"
                    value={newSessionTitle}
                    onChange={(e) => setNewSessionTitle(e.target.value)}
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="description">描述 (可选)</Label>
                  <Input
                    id="description"
                    placeholder="输入会话描述"
                    value={newSessionDescription}
                    onChange={(e) => setNewSessionDescription(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                  取消
                </Button>
                <Button onClick={handleCreateSession}>创建</Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
        <div className="relative">
          <Input
            type="search"
            placeholder="搜索会话..."
            className="pl-8"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
        </div>
      </div>

      <ScrollArea className="flex-1">
        <div className="p-2">
          {loading ? (
            // 加载状态显示骨架屏
            Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="flex items-center gap-3 rounded-lg px-3 py-2 mb-2">
                <Skeleton className="h-9 w-9 rounded-full" />
                <div className="space-y-1 flex-1">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/2" />
                </div>
              </div>
            ))
          ) : filteredSessions.length > 0 ? (
            // 显示会话列表
            filteredSessions.map((session) => (
              <div key={session.id} className="relative group">
                <div
                  onClick={() => selectConversation(session.id)}
                  className={`flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors cursor-pointer ${
                    selectedConversationId === session.id ? "bg-blue-100 text-blue-900" : "hover:bg-gray-100"
                  }`}
                >
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-blue-100 text-blue-900">
                    {session.title.charAt(0).toUpperCase()}
                  </div>
                  <div className="flex-1 overflow-hidden">
                    <div className="font-medium">{session.title}</div>
                    <div className="text-xs text-muted-foreground truncate">
                      {new Date(session.updatedAt).toLocaleString()}
                    </div>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 opacity-0 group-hover:opacity-100"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <MoreHorizontal className="h-4 w-4" />
                        <span className="sr-only">操作</span>
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => openRenameDialog(session)}>
                        <Edit className="mr-2 h-4 w-4" />
                        <span>重命名</span>
                      </DropdownMenuItem>
                      <DropdownMenuItem onClick={() => handleArchiveSession(session.id)}>
                        <Archive className="mr-2 h-4 w-4" />
                        <span>归档</span>
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-red-600" onClick={() => handleDeleteSession(session.id)}>
                        <Trash2 className="mr-2 h-4 w-4" />
                        <span>删除</span>
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            ))
          ) : (
            // 没有会话时显示提示
            <div className="text-center py-8 text-muted-foreground">
              {searchQuery ? "没有找到匹配的会话" : "暂无会话"}
            </div>
          )}
        </div>
      </ScrollArea>

      <div className="p-3 border-t">
        <Button
          variant="outline"
          className="w-full justify-center items-center gap-2 text-blue-600 border-blue-200 bg-blue-50"
          onClick={() => setIsCreateDialogOpen(true)}
        >
          <Plus className="h-4 w-4" />
          开启新会话
        </Button>
      </div>

      {/* 重命名会话对话框 */}
      <Dialog open={isRenameDialogOpen} onOpenChange={setIsRenameDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>重命名会话</DialogTitle>
            <DialogDescription>更新会话的标题和描述。</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="rename-title">标题</Label>
              <Input
                id="rename-title"
                placeholder="输入会话标题"
                value={renameTitle}
                onChange={(e) => setRenameTitle(e.target.value)}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="rename-description">描述 (可选)</Label>
              <Input
                id="rename-description"
                placeholder="输入会话描述"
                value={renameDescription}
                onChange={(e) => setRenameDescription(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsRenameDialogOpen(false)}>
              取消
            </Button>
            <Button onClick={handleRenameSession}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

