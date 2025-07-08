import React from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import {
  ArrowLeft,
  History,
  Power,
  PowerOff,
} from 'lucide-react';

interface AgentEditHeaderProps {
  selectedType: 'chat' | 'agent';
  formDataEnabled: boolean;
  onShowVersionsDialog: () => void;
  onOpenPublishDialog: () => void;
  onToggleStatus: () => void;
  onShowDeleteDialog: () => void;
}

const AgentEditHeader: React.FC<AgentEditHeaderProps> = ({
  selectedType,
  formDataEnabled,
  onShowVersionsDialog,
  onOpenPublishDialog,
  onToggleStatus,
  onShowDeleteDialog,
}) => {
  return (
    <div className="flex items-center justify-between mb-6">
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="icon" asChild className="mr-2">
          <Link href="/studio">
            <ArrowLeft className="h-5 w-5" />
            <span className="sr-only">返回</span>
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">
          编辑{selectedType === 'chat' ? '聊天助理' : '功能性助理'}
        </h1>
      </div>
      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          onClick={onShowVersionsDialog}
        >
          <History className="mr-2 h-4 w-4" />
          版本历史
        </Button>
        <Button variant="outline" onClick={onOpenPublishDialog}>
          发布版本
        </Button>
        <Button
          variant={formDataEnabled ? 'outline' : 'default'}
          onClick={onToggleStatus}
        >
          {formDataEnabled ? (
            <>
              <PowerOff className="mr-2 h-4 w-4" />
              禁用
            </>
          ) : (
            <>
              <Power className="mr-2 h-4 w-4" />
              启用
            </>
          )}
        </Button>
        <div className="flex items-center mt-1 mb-2">
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-blue-500 mr-1"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
          <p className="text-xs text-muted-foreground">启用/禁用状态更改需要点击保存按钮才会生效</p>
        </div>
        <Button variant="destructive" onClick={onShowDeleteDialog}>
          删除
        </Button>
      </div>
    </div>
  );
};

export default AgentEditHeader; 