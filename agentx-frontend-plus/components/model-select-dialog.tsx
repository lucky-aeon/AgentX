"use client"

import { useState, useEffect } from "react"
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle, 
  DialogDescription,
  DialogFooter 
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Label } from "@/components/ui/label"
import { getModels, setAgentModelWithToast } from "@/lib/api-services"
import { Loader2, Check } from "lucide-react"

interface Model {
  id: string;
  userId: string;
  providerId: string;
  providerName: string | null;
  modelId: string;
  name: string;
  description: string;
  type: string;
  config: any;
  isOfficial: boolean;
  status: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ModelSelectDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  agentId: string;
  agentName?: string;
  currentModelId?: string;
  onSuccess?: () => void;
}

export function ModelSelectDialog({
  open,
  onOpenChange,
  agentId,
  agentName,
  currentModelId,
  onSuccess
}: ModelSelectDialogProps) {
  const [models, setModels] = useState<Model[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedModelId, setSelectedModelId] = useState<string | null>(currentModelId || null);
  const [saving, setSaving] = useState(false);

  // 加载模型列表
  useEffect(() => {
    async function loadModels() {
      setLoading(true);
      try {
        const response = await getModels("CHAT");
        if (response.code === 200 && Array.isArray(response.data)) {
          // 过滤出已启用的模型
          const enabledModels = response.data.filter(model => model.status);
          setModels(enabledModels);
        }
      } catch (error) {
        console.error("获取模型列表失败:", error);
      } finally {
        setLoading(false);
      }
    }
    
    if (open) {
      loadModels();
    }
  }, [open]);
  
  // 保存选择的模型
  const handleSave = async () => {
    if (!selectedModelId || !agentId) return;
    
    setSaving(true);
    try {
      const response = await setAgentModelWithToast(agentId, selectedModelId);
      if (response.code === 200) {
        if (onSuccess) onSuccess();
        onOpenChange(false);
      }
    } catch (error) {
      console.error("设置模型失败:", error);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[80vh] flex flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle>选择模型</DialogTitle>
          <DialogDescription>
            {agentName 
              ? `为助理 "${agentName}" 选择一个模型`
              : "选择要使用的模型"}
          </DialogDescription>
        </DialogHeader>
        
        {loading ? (
          <div className="flex justify-center py-10">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : models.length === 0 ? (
          <div className="text-center py-10 text-muted-foreground">
            暂无可用模型，请先在设置中添加模型
          </div>
        ) : (
          <ScrollArea className="flex-1 overflow-auto my-4">
            <RadioGroup
              value={selectedModelId || ""}
              onValueChange={setSelectedModelId}
              className="space-y-3"
            >
              {models.map(model => (
                <div key={model.id} className="flex items-center">
                  <RadioGroupItem
                    value={model.id}
                    id={model.id}
                    className="peer sr-only"
                  />
                  <Label
                    htmlFor={model.id}
                    className="flex flex-1 p-3 border rounded-md cursor-pointer hover:border-primary peer-data-[state=checked]:border-primary peer-data-[state=checked]:bg-primary/5"
                  >
                    <div className="flex-1">
                      <div className="font-medium flex items-center">
                        {model.name || model.modelId}
                        {model.isOfficial && (
                          <span className="ml-2 text-xs bg-blue-100 text-blue-800 py-0.5 px-2 rounded-full">官方</span>
                        )}
                      </div>
                      {model.description && (
                        <div className="text-sm text-muted-foreground mt-1">{model.description}</div>
                      )}
                      <div className="text-xs text-muted-foreground mt-2 flex items-center space-x-2">
                        <span>提供者: {model.providerName || "未知"}</span>
                        <span>•</span>
                        <span>型号: {model.modelId}</span>
                      </div>
                    </div>
                    <div className="ml-2 flex items-center justify-center">
                      {selectedModelId === model.id && (
                        <Check className="h-5 w-5 text-primary" />
                      )}
                    </div>
                  </Label>
                </div>
              ))}
            </RadioGroup>
          </ScrollArea>
        )}
        
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            取消
          </Button>
          <Button 
            onClick={handleSave} 
            disabled={!selectedModelId || saving || loading || models.length === 0}
          >
            {saving ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                保存中...
              </>
            ) : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 