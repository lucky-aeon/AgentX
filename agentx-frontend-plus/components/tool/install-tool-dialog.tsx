import { useState } from "react"
import { Wrench, Download } from "lucide-react"
import { 
  AlertDialog, 
  AlertDialogAction, 
  AlertDialogCancel, 
  AlertDialogContent, 
  AlertDialogFooter, 
  AlertDialogHeader, 
  AlertDialogTitle 
} from "@/components/ui/alert-dialog"
import { Badge } from "@/components/ui/badge"
import { toast } from "@/hooks/use-toast"
import { Tool } from "@/types/tool"
import { installToolWithToast } from "@/lib/tool-service"

interface InstallToolDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  tool: Tool | null
  onSuccess?: () => void
  version?: string
}

export function InstallToolDialog({ 
  open, 
  onOpenChange, 
  tool, 
  onSuccess,
  version
}: InstallToolDialogProps) {
  const [installing, setInstalling] = useState(false)
  const displayVersion = version || "当前版本"

  // 处理安装工具
  const handleInstallTool = async () => {
    if (!tool) return
    
    try {
      setInstalling(true)
      
      // 使用传入的version或默认版本
      const versionToUse = version || tool.current_version || "0.0.1"
      // 优先使用toolId
      const actualToolId = tool.toolId || tool.id
      
      if (!actualToolId) {
        toast({
          title: "安装失败", 
          description: "工具ID不存在",
          variant: "destructive"
        });
        setInstalling(false);
        return;
      }
      
      // 直接调用API
      const response = await installToolWithToast(actualToolId, versionToUse)
      
      if (response.code !== 200) {
        // 错误处理由withToast处理
        setInstalling(false)
        onOpenChange(false)
        return
      }
      
      toast({
        title: "安装成功",
        description: `${tool.name} (${displayVersion}) 已成功安装`,
      })
      
      if (onSuccess) {
        onSuccess()
      }
      
      onOpenChange(false)
    } catch (error) {
      console.error("安装工具失败", error)
    } finally {
      setInstalling(false)
    }
  }

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className="max-w-md">
        <AlertDialogHeader>
          <AlertDialogTitle className="text-xl text-center">安装插件</AlertDialogTitle>
        </AlertDialogHeader>
        
        {tool && (
          <>
            <div className="p-6 border rounded-lg my-4">
              <div className="flex items-center gap-4">
                {tool?.icon ? (
                  <img src={tool.icon} alt={tool.name} className="h-16 w-16 rounded-lg object-cover" />
                ) : (
                  <div className="h-16 w-16 flex items-center justify-center rounded-lg bg-primary/10">
                    <Wrench className="h-8 w-8" />
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-2">
                    <div className="font-semibold text-lg">{tool?.name}</div>
                    {tool?.is_office && <Badge>官方</Badge>}
                  </div>
                  <div className="text-sm text-muted-foreground">{tool.author}</div>
                  <div className="flex items-center mt-1">
                    <span className="text-sm">{tool?.subtitle}</span>
                    <Badge variant="outline" className="ml-2">v{displayVersion}</Badge>
                  </div>
                </div>
              </div>
            </div>
            
            
          </>
        )}
        
        <AlertDialogFooter className="gap-2 mt-4">
          <AlertDialogCancel className="flex-1">取消</AlertDialogCancel>
          <AlertDialogAction 
            className="flex-1"
            onClick={handleInstallTool}
            disabled={installing}
          >
            {installing ? "安装中..." : "安装"}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
} 