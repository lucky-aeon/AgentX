"use client"

import { DialogTrigger } from "@/components/ui/dialog"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Skeleton } from "@/components/ui/skeleton"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { ChevronLeft, ChevronRight, Edit, MoreHorizontal, Plus, Trash2, Clock } from "lucide-react"
import { useWorkspace } from "@/contexts/workspace-context"
import { 
  getAgentSessionsWithToast, 
  createAgentSessionWithToast, 
  updateAgentSessionWithToast, 
  deleteAgentSessionWithToast,
  type SessionDTO 
} from "@/lib/agent-session-service"
import { toast } from "@/hooks/use-toast"

interface ConversationListProps {
  workspaceId: string
}

export function ConversationList({ workspaceId }: ConversationListProps) {
  const { selectedConversationId, setSelectedConversationId } = useWorkspace()
  const [sessions, setSessions] = useState<SessionDTO[]>([])
  const [loading, setLoading] = useState(true)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isRenameDialogOpen, setIsRenameDialogOpen] = useState(false)
  const [newSessionTitle, setNewSessionTitle] = useState("")
  const [sessionToRename, setSessionToRename] = useState<SessionDTO | null>(null)
  const [renameTitle, setRenameTitle] = useState("")
  const [sessionToDelete, setSessionToDelete] = useState<string | null>(null)
  const [isDeletingSession, setIsDeletingSession] = useState(false)
  const [searchText, setSearchText] = useState("")
  const [isCollapsed, setIsCollapsed] = useState(false)

  // 获取会话列表
  const fetchSessions = async () => {
    try {
      setLoading(true)
      const response = await getAgentSessionsWithToast(workspaceId)

      if (response.code === 200) {
        setSessions(response.data)
        // 如果有会话但没有选中的会话，则选中第一个
        if (response.data.length > 0 && !selectedConversationId) {
          setSelectedConversationId(response.data[0].id)
        }
      }
    } catch (error) {
 
    } finally {
      setLoading(false)
    }
  }

  // 创建新会话
  const handleCreateSession = async () => {
    if (!newSessionTitle.trim()) {
      toast({
        description: "会话标题不能为空",
        variant: "destructive",
      })
      return
    }

    try {
      const response = await createAgentSessionWithToast(workspaceId)

      if (response.code === 200) {
        // 更新会话标题
        const updateResponse = await updateAgentSessionWithToast(response.data.id, newSessionTitle)

        if (updateResponse.code === 200) {
          // 重新获取会话列表
          fetchSessions()
          // 清空表单
          setNewSessionTitle("")
          // 关闭对话框
          setIsCreateDialogOpen(false)
          // 选中新创建的会话
          setSelectedConversationId(response.data.id)
        }
      }
    } catch (error) {
 
    }
  }

  // 选择会话
  const selectConversation = (sessionId: string) => {
 
    setSelectedConversationId(sessionId)
  }

  // 删除会话
  const handleDeleteSession = async (sessionId: string) => {
 
    setSessionToDelete(sessionId)
  }

  // 确认删除会话
  const confirmDeleteSession = async () => {
    if (!sessionToDelete) return

    try {
      setIsDeletingSession(true)
      
      // 直接删除会话，后端会自动处理级联删除定时任务
      const response = await deleteAgentSessionWithToast(sessionToDelete)

      if (response.code === 200) {
        // 重新获取会话列表
        fetchSessions()
        // 如果删除的是当前选中的会话，则清除选中状态
        if (selectedConversationId === sessionToDelete) {
          setSelectedConversationId(null)
        }
        
        toast({
          title: "删除成功",
          description: "会话及其关联的定时任务已删除"
        })
      }
    } catch (error) {
 
    } finally {
      setIsDeletingSession(false)
      setSessionToDelete(null)
    }
  }

  // 打开重命名对话框
  const openRenameDialog = (session: SessionDTO) => {
 
    setSessionToRename(session)
    setRenameTitle(session.title)
    setIsRenameDialogOpen(true)
  }

  // 重命名会话
  const handleRenameSession = async () => {
    if (!sessionToRename) return

    if (!renameTitle.trim()) {
      toast({
        description: "会话标题不能为空",
        variant: "destructive",
      })
      return
    }

    try {
      const response = await updateAgentSessionWithToast(sessionToRename.id, renameTitle)

      if (response.code === 200) {
        // 重新获取会话列表
        fetchSessions()
        // 关闭对话框
        setIsRenameDialogOpen(false)
        setSessionToRename(null)
      }
    } catch (error) {
 
    }
  }

  // 初始加载时获取会话列表
  useEffect(() => {
    fetchSessions()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId])

  // 快速创建新会话，无需对话框
  const handleQuickCreateSession = async () => {
    try {
      // 直接创建新会话
      const response = await createAgentSessionWithToast(workspaceId)

      if (response.code === 200) {
        // 使用默认标题"新会话"更新会话
        const defaultTitle = "新会话"
        const updateResponse = await updateAgentSessionWithToast(response.data.id, defaultTitle)

        if (updateResponse.code === 200) {
          // 重新获取会话列表
          fetchSessions()
          // 选中新创建的会话
          setSelectedConversationId(response.data.id)
        }
      }
    } catch (error) {
 
    }
  }

  return (
    <div className={`border-r flex flex-col h-full bg-white transition-all duration-300 ${isCollapsed ? 'w-[40px]' : 'w-[320px]'}`}>
      <div className={`${isCollapsed ? 'py-4 px-0' : 'p-4'} border-b flex items-center relative ${isCollapsed ? 'h-full' : ''}`}>
        {!isCollapsed && (
          <>
            <h2 className="text-lg font-semibold">会话列表</h2>
            <div style={{ position: 'absolute', right: '65px' }}>
              <Button size="icon" variant="ghost" onClick={() => handleQuickCreateSession()}>
                <Plus className="h-4 w-4" />
                <span className="sr-only">新建会话</span>
              </Button>
            </div>
          </>
        )}
        
        {/* 收缩/展开按钮 - 明确右侧位置 */}
        <div 
          className={`absolute ${isCollapsed ? 'w-full h-12' : 'w-12 border-l h-full'} right-0 top-0 flex items-center justify-center cursor-pointer hover:bg-gray-50`}
          onClick={() => setIsCollapsed(!isCollapsed)}
        >
          {isCollapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </div>
      </div>
      
      {!isCollapsed && (
        <>
          <div className="relative p-4">
            <Input
              placeholder="搜索会话..."
              className="pl-8"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
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
            </svg>
          </div>
          
          <ScrollArea className="flex-1">
            <div className="p-2 space-y-1">
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
              ) : sessions.length > 0 ? (
                sessions.map((session) => (
                  <div
                    key={session.id}
                    style={{
                      position: 'relative',
                      padding: '12px',
                      borderRadius: '6px',
                      marginBottom: '4px',
                      cursor: 'pointer',
                      backgroundColor: selectedConversationId === session.id ? '#ebf4ff' : 'transparent',
                      border: selectedConversationId === session.id ? '1px solid #bfdbfe' : '1px solid transparent',
                      transition: 'background-color 0.2s',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                    }}
                    onClick={() => selectConversation(session.id)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                      <div style={{ flex: 1, minWidth: 0, marginRight: '8px' }}>
                        <div style={{ fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {session.title}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                          {new Date(session.createdAt).toLocaleString()}
                        </div>
                      </div>
                      
                      <div style={{ display: 'inline-block' }}>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              style={{ 
                                height: '32px', 
                                width: '32px', 
                                minWidth: '32px',
                                flexShrink: 0,
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center'
                              }}
                              onClick={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
 
                              }}
                            >
                              <MoreHorizontal style={{ height: '16px', width: '16px' }} />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem 
                              onClick={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
 
                                openRenameDialog(session);
                              }}
                            >
                              <Edit className="mr-2 h-4 w-4" />
                              重命名
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              className="text-red-600"
                              onClick={(e) => {
                                e.stopPropagation();
                                e.preventDefault();
 
                                handleDeleteSession(session.id);
                              }}
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              删除
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                // 没有会话时显示提示
                <div className="text-center py-8 text-muted-foreground">
                  暂无会话
                </div>
              )}
            </div>
          </ScrollArea>
        </>
      )}
      
      {/* 删除确认对话框 */}
      <Dialog open={!!sessionToDelete} onOpenChange={(open) => !open && setSessionToDelete(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>删除会话</DialogTitle>
            <DialogDescription>
              "确定要删除这个会话吗？此操作无法撤销。"
            </DialogDescription>
          </DialogHeader>
          
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setSessionToDelete(null)
              }}
              disabled={isDeletingSession}
            >
              取消
            </Button>
            <Button
              variant="destructive"
              onClick={confirmDeleteSession}
              disabled={isDeletingSession}
            >
              {isDeletingSession ? "删除中..." : "删除"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 重命名对话框 */}
      <Dialog open={isRenameDialogOpen} onOpenChange={setIsRenameDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>重命名会话</DialogTitle>
            <DialogDescription>
              为会话设置一个新的标题。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="name">会话标题</Label>
              <Input
                id="name"
                value={renameTitle}
                onChange={(e) => setRenameTitle(e.target.value)}
                placeholder="输入新的标题..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setIsRenameDialogOpen(false)} variant="outline">取消</Button>
            <Button onClick={handleRenameSession}>保存</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

