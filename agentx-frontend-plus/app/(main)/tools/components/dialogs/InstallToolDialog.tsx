import { MarketTool } from "../../utils/types";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Download, Wrench } from "lucide-react";
import { ToolLabels } from "../shared/ToolLabels";

interface InstallToolDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tool: MarketTool | null;
  isInstalling?: boolean;
  onConfirm: () => Promise<boolean>;
  onSuccess?: () => void;
  version?: string;
}

export function InstallToolDialog({
  open,
  onOpenChange,
  tool,
  isInstalling = false,
  onConfirm,
  onSuccess,
  version
}: InstallToolDialogProps) {
  if (!tool) return null;

  const handleInstall = async () => {
    const success = await onConfirm();
    if (success && onSuccess) {
      onSuccess();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-3 mb-2">
            <div className="flex h-12 w-12 items-center justify-center rounded-md bg-primary/10">
              {tool.icon ? (
                <img src={tool.icon} alt={tool.name} className="h-full w-full object-cover" />
              ) : (
                <Wrench className="h-6 w-6" />
              )}
            </div>
            <div>
              <DialogTitle className="text-xl">安装工具</DialogTitle>
              <DialogDescription>
                确认安装 {tool.name}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>
        
        <div className="py-4">
          <div className="mb-4">
            <h3 className="font-medium mb-1">{tool.name}</h3>
            <p className="text-sm text-muted-foreground mb-2">
              {tool.subtitle} {version && <span className="font-medium">(v{version})</span>}
            </p>
            <ToolLabels 
              labels={tool.labels} 
              className="mb-4 flex flex-wrap gap-2"
            />
          </div>
          
          <div className="bg-muted/20 rounded-md p-4 text-sm">
            <p className="font-medium mb-2">安装此工具后，您将能够：</p>
            <ul className="list-disc pl-5 space-y-1 text-muted-foreground">
              <li>在聊天中直接调用此工具的功能</li>
              <li>通过@{tool.name}使用工具</li>
              <li>随时从"我的工具"中管理或卸载</li>
            </ul>
          </div>
        </div>
        
        <DialogFooter>
          <Button 
            variant="outline" 
            onClick={() => onOpenChange(false)}
          >
            取消
          </Button>
          <Button 
            onClick={handleInstall}
            disabled={isInstalling}
            className="gap-2"
          >
            <Download className="h-4 w-4" />
            {isInstalling ? "安装中..." : "确认安装"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 