"use client"

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { MemoryItem } from "@/types/memory";

interface DeleteMemoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  item: MemoryItem | null;
  onConfirm: () => void;
  loading?: boolean;
}

export function DeleteMemoryDialog({ open, onOpenChange, item, onConfirm, loading }: DeleteMemoryDialogProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>删除记忆</DialogTitle>
          <DialogDescription>
            确认删除该条记忆吗？此操作为归档（软删除），可在后端恢复。
          </DialogDescription>
        </DialogHeader>
        <div className="text-sm text-muted-foreground whitespace-pre-wrap max-h-40 overflow-auto border rounded-md p-3">
          {item?.text || ""}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            取消
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={loading}>
            {loading ? "删除中..." : "确认删除"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

