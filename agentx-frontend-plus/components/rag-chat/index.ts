// 导出所有RAG聊天相关组件
export { RagChatDialog as RagChatDialogNew } from './RagChatDialogNew';
export { ChatMessageList } from './ChatMessageList';
export { ChatInputArea } from './ChatInputArea';
export { MessageItem } from './MessageItem';
export { ThinkingProcess } from './ThinkingProcess';
export { RetrievalProcess } from './RetrievalProcess';
export { FileDetailPanel } from './FileDetailPanel';
export { ClickableFileLink } from './ClickableFileLink';

// 导出相关hooks
export { useRagChatSession } from '@/hooks/rag-chat/useRagChatSession';
export { useSmartScroll } from '@/hooks/rag-chat/useSmartScroll';
export { useChatLayout } from '@/hooks/rag-chat/useChatLayout';
export { useFileDetail } from '@/hooks/rag-chat/useFileDetail';

// 导出类型
export type { Message } from '@/hooks/rag-chat/useRagChatSession';