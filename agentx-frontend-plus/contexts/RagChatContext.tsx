"use client";

import { createContext, useContext, useReducer, ReactNode } from 'react';
import type { ChatUIState, RetrievedFileInfo, FileContentData, ChatLayout } from '@/types/rag-dataset';

// 定义动作类型
type RagChatAction =
  | { type: 'SELECT_FILE'; payload: RetrievedFileInfo }
  | { type: 'CLOSE_FILE_DETAIL' }
  | { type: 'SET_LAYOUT'; payload: ChatLayout }
  | { type: 'SET_FILE_DETAIL_LOADING'; payload: boolean }
  | { type: 'SET_FILE_DETAIL_DATA'; payload: FileContentData | null }
  | { type: 'SET_FILE_DETAIL_ERROR'; payload: string | null }
  | { type: 'RESET_STATE' };

// 初始状态
const initialState: ChatUIState = {
  layout: 'single',
  selectedFile: null,
  showFileDetail: false,
  fileDetailData: null,
  fileDetailLoading: false,
  fileDetailError: null
};

// Reducer
function ragChatReducer(state: ChatUIState, action: RagChatAction): ChatUIState {
  switch (action.type) {
    case 'SELECT_FILE':
      return {
        ...state,
        selectedFile: action.payload,
        showFileDetail: true,
        layout: 'split',
        fileDetailData: null,
        fileDetailLoading: false,
        fileDetailError: null
      };
    
    case 'CLOSE_FILE_DETAIL':
      return {
        ...state,
        selectedFile: null,
        showFileDetail: false,
        layout: 'single',
        fileDetailData: null,
        fileDetailLoading: false,
        fileDetailError: null
      };
    
    case 'SET_LAYOUT':
      return {
        ...state,
        layout: action.payload
      };
    
    case 'SET_FILE_DETAIL_LOADING':
      return {
        ...state,
        fileDetailLoading: action.payload
      };
    
    case 'SET_FILE_DETAIL_DATA':
      return {
        ...state,
        fileDetailData: action.payload,
        fileDetailLoading: false,
        fileDetailError: null
      };
    
    case 'SET_FILE_DETAIL_ERROR':
      return {
        ...state,
        fileDetailError: action.payload,
        fileDetailLoading: false
      };
    
    case 'RESET_STATE':
      return initialState;
    
    default:
      return state;
  }
}

// Context 类型
interface RagChatContextType {
  state: ChatUIState;
  selectFile: (file: RetrievedFileInfo) => void;
  closeFileDetail: () => void;
  setLayout: (layout: ChatLayout) => void;
  setFileDetailLoading: (loading: boolean) => void;
  setFileDetailData: (data: FileContentData | null) => void;
  setFileDetailError: (error: string | null) => void;
  resetState: () => void;
}

// 创建 Context
const RagChatContext = createContext<RagChatContextType | null>(null);

// Provider 组件
interface RagChatProviderProps {
  children: ReactNode;
}

export function RagChatProvider({ children }: RagChatProviderProps) {
  const [state, dispatch] = useReducer(ragChatReducer, initialState);

  const selectFile = (file: RetrievedFileInfo) => {
    dispatch({ type: 'SELECT_FILE', payload: file });
  };

  const closeFileDetail = () => {
    dispatch({ type: 'CLOSE_FILE_DETAIL' });
  };

  const setLayout = (layout: ChatLayout) => {
    dispatch({ type: 'SET_LAYOUT', payload: layout });
  };

  const setFileDetailLoading = (loading: boolean) => {
    dispatch({ type: 'SET_FILE_DETAIL_LOADING', payload: loading });
  };

  const setFileDetailData = (data: FileContentData | null) => {
    dispatch({ type: 'SET_FILE_DETAIL_DATA', payload: data });
  };

  const setFileDetailError = (error: string | null) => {
    dispatch({ type: 'SET_FILE_DETAIL_ERROR', payload: error });
  };

  const resetState = () => {
    dispatch({ type: 'RESET_STATE' });
  };

  const value: RagChatContextType = {
    state,
    selectFile,
    closeFileDetail,
    setLayout,
    setFileDetailLoading,
    setFileDetailData,
    setFileDetailError,
    resetState
  };

  return (
    <RagChatContext.Provider value={value}>
      {children}
    </RagChatContext.Provider>
  );
}

// Hook 来使用 Context
export function useRagChatContext() {
  const context = useContext(RagChatContext);
  if (!context) {
    throw new Error('useRagChatContext must be used within a RagChatProvider');
  }
  return context;
}