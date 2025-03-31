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
import { Badge } from "@/components/ui/badge"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { getModels, setAgentModelWithToast, getAgentModel } from "@/lib/api-services"
import { Loader2, CheckCircle, Settings, ZapIcon } from "lucide-react"

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

  // 加载当前Agent的模型ID和模型列表
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        // 并行加载模型列表和当前模型ID
        const [modelsResponse, currentModelResponse] = await Promise.all([
          getModels("all"),
          getAgentModel(agentId)
        ]);

        // 处理模型列表
        if (modelsResponse.code === 200 && Array.isArray(modelsResponse.data)) {
          const chatModels = modelsResponse.data.filter(model => 
            model.type === "CHAT"
          );
          setModels(chatModels);
        }

        // 处理当前模型ID
        if (currentModelResponse.code === 200 && currentModelResponse.data?.modelId) {
          setSelectedModelId(currentModelResponse.data.modelId);
        }
      } catch (error) {
        console.error("加载数据失败:", error);
      } finally {
        setLoading(false);
      }
    }
    
    if (open) {
      loadData();
    }
  }, [open, agentId]);
  
  // 保存选择的模型
  const handleSave = async () => {
    if (!selectedModelId || !agentId) return;
    
    setSaving(true);
    try {
      const response = await setAgentModelWithToast(agentId, selectedModelId);
      if (response.code === 200) {
        // 直接关闭对话框，不触发onSuccess回调
        onOpenChange(false);
      }
    } catch (error) {
      console.error("设置模型失败:", error);
    } finally {
      setSaving(false);
    }
  };

  // 按提供商分组
  const modelsByProvider = models.reduce((groups, model) => {
    const provider = model.providerName || '其他';
    if (!groups[provider]) {
      groups[provider] = [];
    }
    groups[provider].push(model);
    return groups;
  }, {} as Record<string, Model[]>);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[85vh] flex flex-col overflow-hidden">
        <DialogHeader>
          <div className="flex items-center">
            <Settings className="h-6 w-6 mr-2 text-primary" />
            <div>
              <DialogTitle className="text-xl">配置对话模型</DialogTitle>
              <DialogDescription className="mt-1">
                {agentName 
                  ? `为助理 "${agentName}" 选择合适的大语言模型`
                  : "选择合适的大语言模型"}
              </DialogDescription>
            </div>
          </div>
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
          <ScrollArea className="flex-1 overflow-auto pr-4 mt-4" style={{maxHeight: "60vh"}}>
            <RadioGroup 
              value={selectedModelId || ""} 
              onValueChange={setSelectedModelId}
              className="space-y-6"
            >
              {Object.entries(modelsByProvider).map(([provider, providerModels]) => (
                <div key={provider} className="space-y-3">
                  <h3 className="font-medium text-sm text-muted-foreground uppercase tracking-wider">
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {providerModels.map(model => (
                      <div 
                        key={model.id}
                        className={`
                          relative border rounded-lg p-4 transition-colors cursor-pointer
                          ${selectedModelId === model.id ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'border-border hover:border-primary/50'}
                          ${!model.status ? 'opacity-60' : ''}
                        `}
                      >
                        <RadioGroupItem 
                          value={model.id} 
                          id={model.id} 
                          className="sr-only"
                          disabled={!model.status}
                        />
                        <label 
                          htmlFor={model.id}
                          className="flex flex-col h-full cursor-pointer"
                        >
                          <div className="flex items-start justify-between mb-2">
                            <div className="flex items-center">
                              <span className="font-medium text-base">
                                {model.name || model.modelId}
                              </span>
                              <div className="flex items-center ml-2 space-x-1">
                                {model.isOfficial && (
                                  <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-100">
                                    官方
                                  </Badge>
                                )}
                                {!model.status && (
                                  <Badge variant="outline" className="text-gray-500 border-gray-300">
                                    未激活
                                  </Badge>
                                )}
                              </div>
                            </div>
                            {selectedModelId === model.id && (
                              <CheckCircle className="h-5 w-5 text-primary" />
                            )}
                          </div>
                          
                          <div className="text-sm text-muted-foreground mb-2 flex-1">
                            {model.description} 
                          </div>
                          
                          <div className="flex items-center text-xs text-muted-foreground mt-auto">
                            <div className="flex items-center">
                              <span className="mr-3">模型ID: {model.modelId}</span>
                             
                            </div>
                          </div>
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </RadioGroup>
          </ScrollArea>
        )}
        
        <DialogFooter className="mt-4 pt-4 border-t">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={saving}>
            取消
          </Button>
          <Button 
            onClick={handleSave} 
            disabled={!selectedModelId || saving || loading}
            className="gap-1"
          >
            {saving ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                保存中...
              </>
            ) : (
              <>
                <CheckCircle className="h-4 w-4" />
                保存
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 