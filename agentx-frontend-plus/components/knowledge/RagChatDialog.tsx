// 重构后的RAG聊天对话框组件
// 使用新的模块化架构，支持文件详情查看功能

import { RagChatDialog as RagChatDialogNew } from '@/components/rag-chat/RagChatDialogNew';
import type { RagDataset } from '@/types/rag-dataset';

interface RagChatDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  dataset: RagDataset;
}

export function RagChatDialog({ open, onOpenChange, dataset }: RagChatDialogProps) {
  return (
    <RagChatDialogNew
      open={open}
      onOpenChange={onOpenChange}
      dataset={dataset}
    />
  );
}