"use client"

import { MessageSquare } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { ResponsiveDialog } from "@/components/layout/ResponsiveDialog"
import { SplitLayout } from "@/components/layout/SplitLayout"
import { ChatMessageList } from "@/components/rag-chat/ChatMessageList"
import { ChatInputArea } from "@/components/rag-chat/ChatInputArea"
import { FileDetailPanel } from "@/components/rag-chat/FileDetailPanel"
import { useRagChatSession } from "@/hooks/rag-chat/useRagChatSession"
import { useChatLayout } from "@/hooks/rag-chat/useChatLayout"
import { toast } from "@/hooks/use-toast"
import type { UserRagDTO, RetrievedFileInfo } from "@/types/rag-dataset"

interface InstalledRagChatDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  userRag: UserRagDTO | null
}

export function InstalledRagChatDialog({ 
  open, 
  onOpenChange, 
  userRag 
}: InstalledRagChatDialogProps) {
  const {
    uiState,
    selectFile,
    closeFileDetail,
    setFileDetailData,
    resetState
  } = useChatLayout()

  const {
    messages,
    isLoading,
    sendMessage,
    clearMessages,
    stopGeneration
  } = useRagChatSession({
    onError: (error) => {
      toast({
        title: "对话出错",
        description: error,
        variant: "destructive"
      })
    }
  })

  // 处理文件点击
  const handleFileClick = (file: RetrievedFileInfo) => {
    selectFile(file)
  }

  // 处理文件详情数据加载
  const handleFileDetailDataLoad = (data: any) => {
    setFileDetailData(data)
  }

  // 处理发送消息 - 使用 originalRagId 作为 datasetId
  const handleSendMessage = async (message: string) => {
    if (!userRag?.originalRagId) {
      toast({
        title: "错误",
        description: "无法获取知识库数据源",
        variant: "destructive"
      })
      return
    }

    // 使用原始RAG ID作为搜索数据源，复用现有逻辑
    await sendMessage(message, [userRag.originalRagId])
  }

  // 处理对话框关闭
  const handleDialogClose = (open: boolean) => {
    if (!open) {
      resetState()
      stopGeneration()
    }
    onOpenChange(open)
  }

  // 处理清空对话
  const handleClearMessages = () => {
    clearMessages()
    closeFileDetail()
  }

  if (!userRag) return null

  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={handleDialogClose}
      title={
        <div className="flex items-center gap-3">
          <MessageSquare className="h-5 w-5" />
          <span>RAG 智能问答</span>
          <Badge variant="secondary">{userRag.name}</Badge>
          <Badge variant="outline" className="text-xs">
            v{userRag.version}
          </Badge>
        </div>
      }
      layout={uiState.layout}
    >
      <SplitLayout
        leftPanel={
          <div className="flex flex-col h-full">
            <ChatMessageList
              messages={messages}
              onFileClick={handleFileClick}
              selectedFileId={uiState.selectedFile?.fileId}
              className="flex-1"
            />
            
            <ChatInputArea
              onSend={handleSendMessage}
              onStop={stopGeneration}
              onClear={handleClearMessages}
              isLoading={isLoading}
              hasMessages={messages.length > 0}
            />
          </div>
        }
        rightPanel={
          <FileDetailPanel
            selectedFile={uiState.selectedFile}
            onDataLoad={handleFileDetailDataLoad}
          />
        }
        showRightPanel={uiState.showFileDetail}
        onCloseRightPanel={closeFileDetail}
      />
    </ResponsiveDialog>
  )
}