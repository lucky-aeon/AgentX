import { useRef, useCallback, useEffect } from 'react';

export function useSmartScroll() {
  const scrollAreaRef = useRef<HTMLDivElement>(null);
  const isUserScrolling = useRef(false);
  const lastScrollTop = useRef(0);
  const scrollingTimeout = useRef<NodeJS.Timeout>();
  const autoScrollTimeout = useRef<NodeJS.Timeout>();
  const isAutoScrolling = useRef(false);

  // 检查是否在底部附近（阈值30px）
  const isNearBottom = useCallback((element: Element) => {
    const threshold = 30;
    return element.scrollHeight - element.scrollTop - element.clientHeight < threshold;
  }, []);

  // 智能滚动到底部
  const scrollToBottom = useCallback(() => {
    if (scrollAreaRef.current && !isUserScrolling.current) {
      const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]');
      if (scrollElement) {
        // 设置自动滚动标志
        isAutoScrolling.current = true;
        scrollElement.scrollTop = scrollElement.scrollHeight;
        
        // 验证滚动是否成功
        setTimeout(() => {
          isAutoScrolling.current = false;
        }, 100);
      }
    }
  }, []);

  // 处理滚动事件
  const handleScroll = useCallback((event: Event) => {
    const scrollElement = event.target as Element;
    const currentScrollTop = scrollElement.scrollTop;
    
    // 如果是自动滚动触发的事件，忽略
    if (isAutoScrolling.current) {
      lastScrollTop.current = currentScrollTop;
      return;
    }
    
    // 防抖处理，避免频繁触发
    if (scrollingTimeout.current) {
      clearTimeout(scrollingTimeout.current);
    }
    
    scrollingTimeout.current = setTimeout(() => {
      // 检测用户主动滚动的几种情况：
      // 1. 向上滚动
      // 2. 不在底部附近
      // 3. 滚动速度较快（表示用户主动操作）
      const scrollDelta = Math.abs(currentScrollTop - lastScrollTop.current);
      const isScrollingUp = currentScrollTop < lastScrollTop.current;
      const isAwayFromBottom = !isNearBottom(scrollElement);
      const isFastScrolling = scrollDelta > 10; // 快速滚动阈值
      
      if (isScrollingUp || isAwayFromBottom || isFastScrolling) {
        // 用户主动滚动
        isUserScrolling.current = true;
      } else if (isNearBottom(scrollElement)) {
        // 用户滚动到底部附近，恢复自动滚动
        isUserScrolling.current = false;
      }
      
      lastScrollTop.current = currentScrollTop;
    }, 100); // 100ms防抖延迟
  }, [isNearBottom]);

  // 手动滚动到底部
  const handleScrollToBottom = useCallback(() => {
    isUserScrolling.current = false;
    if (scrollAreaRef.current) {
      const scrollElement = scrollAreaRef.current.querySelector('[data-radix-scroll-area-viewport]');
      if (scrollElement) {
        scrollElement.scrollTo({
          top: scrollElement.scrollHeight,
          behavior: 'smooth'
        });
      }
    }
  }, []);

  // 智能滚动（用于内容更新时）
  const smartScroll = useCallback((isFirstContent: boolean = false) => {
    if (!isUserScrolling.current) {
      // 清除之前的自动滚动定时器
      if (autoScrollTimeout.current) {
        clearTimeout(autoScrollTimeout.current);
      }
      
      if (isFirstContent) {
        // 首次内容到达时，立即滚动到底部
        scrollToBottom();
      } else {
        // 后续内容到达时，只有当用户在底部附近时才自动滚动
        const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]');
        if (scrollElement) {
          const nearBottom = isNearBottom(scrollElement);
          
          if (nearBottom) {
            autoScrollTimeout.current = setTimeout(() => {
              scrollToBottom();
            }, 50);
          }
        }
      }
    }
  }, [scrollToBottom, isNearBottom]);

  // 思考内容滚动（延迟稍长，避免过于频繁）
  const smartScrollForThinking = useCallback(() => {
    if (!isUserScrolling.current) {
      const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]');
      if (scrollElement) {
        const nearBottom = isNearBottom(scrollElement);
        
        if (nearBottom) {
          // 清除之前的自动滚动定时器
          if (autoScrollTimeout.current) {
            clearTimeout(autoScrollTimeout.current);
          }
          // 思考内容滚动延迟稍长，避免过于频繁
          autoScrollTimeout.current = setTimeout(() => {
            scrollToBottom();
          }, 100);
        }
      }
    }
  }, [scrollToBottom, isNearBottom]);

  // 设置滚动事件监听器
  useEffect(() => {
    const scrollElement = scrollAreaRef.current?.querySelector('[data-radix-scroll-area-viewport]');
    if (scrollElement) {
      scrollElement.addEventListener('scroll', handleScroll);
      return () => {
        scrollElement.removeEventListener('scroll', handleScroll);
      };
    }
  }, [handleScroll]);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (scrollingTimeout.current) {
        clearTimeout(scrollingTimeout.current);
      }
      if (autoScrollTimeout.current) {
        clearTimeout(autoScrollTimeout.current);
      }
    };
  }, []);

  // 检查是否应该显示"滚动到底部"按钮
  const shouldShowScrollToBottom = useCallback(() => {
    return isUserScrolling.current;
  }, []);

  return {
    scrollAreaRef,
    scrollToBottom,
    handleScrollToBottom,
    smartScroll,
    smartScrollForThinking,
    shouldShowScrollToBottom
  };
}