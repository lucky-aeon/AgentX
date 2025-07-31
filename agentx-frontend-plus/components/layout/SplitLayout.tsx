"use client";

import { ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { X } from 'lucide-react';

interface SplitLayoutProps {
  leftPanel: ReactNode;
  rightPanel: ReactNode;
  showRightPanel: boolean;
  onCloseRightPanel: () => void;
  leftPanelClassName?: string;
  rightPanelClassName?: string;
  className?: string;
}

export function SplitLayout({
  leftPanel,
  rightPanel,
  showRightPanel,
  onCloseRightPanel,
  leftPanelClassName,
  rightPanelClassName,
  className
}: SplitLayoutProps) {
  return (
    <div className={cn("flex h-full", className)}>
      {/* 左侧面板 */}
      <div 
        className={cn(
          "flex flex-col transition-all duration-200 ease-in-out",
          showRightPanel ? "w-3/5" : "w-full",
          leftPanelClassName
        )}
      >
        {leftPanel}
      </div>
      
      {/* 分割线 */}
      {showRightPanel && (
        <div className="w-px bg-border flex-shrink-0" />
      )}
      
      {/* 右侧面板 */}
      {showRightPanel && (
        <div 
          className={cn(
            "w-2/5 flex flex-col transition-all duration-200 ease-in-out",
            rightPanelClassName
          )}
        >
          {/* 右侧面板标题栏 */}
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="text-lg font-semibold">文件详情</h3>
            <Button
              variant="ghost"
              size="icon"
              onClick={onCloseRightPanel}
              className="h-8 w-8"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
          
          {/* 右侧面板内容 */}
          <div className="flex-1 overflow-hidden">
            {rightPanel}
          </div>
        </div>
      )}
    </div>
  );
}