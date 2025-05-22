import { useState } from "react"
import { Wrench, Download } from "lucide-react"
import { 
  AlertDialog, 
  AlertDialogAction, 
  AlertDialogCancel, 
  AlertDialogContent, 
  AlertDialogFooter, 
  AlertDialogHeader, 
  AlertDialogTitle,
  AlertDialogDescription 
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"

interface InstallToolDialogProps {
  isOpen: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => Promise<void>;
  onSuccess?: () => void;
  toolName: string;
  isLoading: boolean;
}

export function InstallToolDialog({ 
  isOpen, 
  onOpenChange, 
  onConfirm, 
  onSuccess,
  toolName,
  isLoading
}: InstallToolDialogProps) {
  const handleConfirm = async () => {
    await onConfirm();
    if (onSuccess) {
      onSuccess();
    }
  };

  return (
    <AlertDialog open={isOpen} onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-xl text-center">安装工具</AlertDialogTitle>
          <AlertDialogDescription className="text-center">
            确定要安装 {toolName} 吗？
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        <AlertDialogFooter className="gap-2 mt-4">
          <AlertDialogCancel className="flex-1" disabled={isLoading}>取消</AlertDialogCancel>
          <AlertDialogAction 
            className="flex-1"
            onClick={handleConfirm}
            disabled={isLoading}
          >
            {isLoading ? "安装中..." : "安装"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
} 