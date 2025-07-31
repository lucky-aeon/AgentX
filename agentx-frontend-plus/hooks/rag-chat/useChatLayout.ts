import { useState, useCallback } from 'react';
import type { ChatLayout, RetrievedFileInfo, ChatUIState } from '@/types/rag-dataset';

export function useChatLayout() {
  const [uiState, setUIState] = useState<ChatUIState>({
    layout: 'single',
    selectedFile: null,
    showFileDetail: false,
    fileDetailData: null,
    fileDetailLoading: false,
    fileDetailError: null
  });

  // 切换布局
  const switchLayout = useCallback((layout: ChatLayout) => {
    setUIState(prev => ({
      ...prev,
      layout
    }));
  }, []);

  // 选择文件并切换到分栏布局
  const selectFile = useCallback((file: RetrievedFileInfo) => {
    setUIState(prev => ({
      ...prev,
      selectedFile: file,
      showFileDetail: true,
      layout: 'split',
      fileDetailData: null, // 清空之前的数据
      fileDetailLoading: false,
      fileDetailError: null
    }));
  }, []);

  // 关闭文件详情
  const closeFileDetail = useCallback(() => {
    setUIState(prev => ({
      ...prev,
      selectedFile: null,
      showFileDetail: false,
      layout: 'single',
      fileDetailData: null,
      fileDetailLoading: false,
      fileDetailError: null
    }));
  }, []);

  // 设置文件详情加载状态
  const setFileDetailLoading = useCallback((loading: boolean) => {
    setUIState(prev => ({
      ...prev,
      fileDetailLoading: loading
    }));
  }, []);

  // 设置文件详情数据
  const setFileDetailData = useCallback((data: ChatUIState['fileDetailData']) => {
    setUIState(prev => ({
      ...prev,
      fileDetailData: data,
      fileDetailLoading: false,
      fileDetailError: null
    }));
  }, []);

  // 设置文件详情错误
  const setFileDetailError = useCallback((error: string | null) => {
    setUIState(prev => ({
      ...prev,
      fileDetailError: error,
      fileDetailLoading: false
    }));
  }, []);

  // 重置所有状态
  const resetState = useCallback(() => {
    setUIState({
      layout: 'single',
      selectedFile: null,
      showFileDetail: false,
      fileDetailData: null,
      fileDetailLoading: false,
      fileDetailError: null
    });
  }, []);

  return {
    uiState,
    switchLayout,
    selectFile,
    closeFileDetail,
    setFileDetailLoading,
    setFileDetailData,
    setFileDetailError,
    resetState
  };
}