"use client";

import { ReactNode } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { cn } from '@/lib/utils';
import type { ChatLayout } from '@/types/rag-dataset';

interface ResponsiveDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: ReactNode;
  layout: ChatLayout;
  className?: string;
}

export function ResponsiveDialog({
  open,
  onOpenChange,
  title,
  children,
  layout,
  className
}: ResponsiveDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent 
        className={cn(
          "p-0 flex flex-col",
          layout === 'single' 
            ? "max-w-4xl h-[80vh]" 
            : "max-w-7xl h-[85vh]",
          className
        )}
      >
        <DialogHeader className="px-6 py-4 border-b flex-shrink-0">
          <DialogTitle className="flex items-center gap-3">
            {title}
          </DialogTitle>
        </DialogHeader>
        
        <div className="flex-1 overflow-hidden">
          {children}
        </div>
      </DialogContent>
    </Dialog>
  );
}