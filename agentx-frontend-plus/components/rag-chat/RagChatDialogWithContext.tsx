"use client";

import { MessageSquare } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { ResponsiveDialog } from "@/components/layout/ResponsiveDialog";
import { SplitLayout } from "@/components/layout/SplitLayout";
import { ChatMessageList } from "./ChatMessageList";
import { ChatInputArea } from "./ChatInputArea";
import { FileDetailPanel } from "./FileDetailPanel";
import { RagChatProvider, useRagChatContext } from "@/contexts/RagChatContext";
import { useRagChatSession } from "@/hooks/rag-chat/useRagChatSession";
import { toast } from "@/hooks/use-toast";
import type { RagDataset, RetrievedFileInfo } from "@/types/rag-dataset";

interface RagChatDialogInnerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dataset: RagDataset;
}

function RagChatDialogInner({ open, onOpenChange, dataset }: RagChatDialogInnerProps) {
  const {
    state,
    selectFile,
    closeFileDetail,
    setFileDetailData,
    resetState
  } = useRagChatContext();

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
      });
    }
  });

  // 处理文件点击
  const handleFileClick = (file: RetrievedFileInfo) => {
    selectFile(file);
  };

  // 处理文件详情数据加载
  const handleFileDetailDataLoad = (data: any) => {
    setFileDetailData(data);
  };

  // 处理发送消息
  const handleSendMessage = async (message: string) => {
    await sendMessage(message, [dataset.id]);
  };

  // 处理对话框关闭
  const handleDialogClose = (open: boolean) => {
    if (!open) {
      resetState();
    }
    onOpenChange(open);
  };

  // 处理清空对话
  const handleClearMessages = () => {
    clearMessages();
    closeFileDetail();
  };

  return (
    <ResponsiveDialog
      open={open}
      onOpenChange={handleDialogClose}
      title={
        <div className="flex items-center gap-3">
          <MessageSquare className="h-5 w-5" />
          <span>RAG 智能问答</span>
          <Badge variant="secondary">{dataset.name}</Badge>
        </div>
      }
      layout={state.layout}
    >
      <SplitLayout
        leftPanel={
          <div className="flex flex-col h-full">
            <ChatMessageList
              messages={messages}
              onFileClick={handleFileClick}
              selectedFileId={state.selectedFile?.fileId}
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
            selectedFile={state.selectedFile}
            onDataLoad={handleFileDetailDataLoad}
          />
        }
        showRightPanel={state.showFileDetail}
        onCloseRightPanel={closeFileDetail}
      />
    </ResponsiveDialog>
  );
}

interface RagChatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dataset: RagDataset;
}

export function RagChatDialogWithContext({ open, onOpenChange, dataset }: RagChatDialogProps) {
  return (
    <RagChatProvider>
      <RagChatDialogInner
        open={open}
        onOpenChange={onOpenChange}
        dataset={dataset}
      />
    </RagChatProvider>
  );
}