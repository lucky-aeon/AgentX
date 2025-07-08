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
import { Loader2, CheckCircle, Settings, ZapIcon, Sliders } from "lucide-react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Slider } from "@/components/ui/slider"

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

interface ModelConfig {
  modelId: string;
  temperature: number;
  topP: number;
  topK: number;
  maxTokens: number;
  strategyType: string;
  reserveRatio: number;
  summaryThreshold: number;
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

  // 参数设置
  const [temperature, setTemperature] = useState(0.7);
  const [topP, setTopP] = useState(0.9);
  const [topK, setTopK] = useState(50);
  const [maxTokens, setMaxTokens] = useState(4096);
  const [reserveRatio, setReserveRatio] = useState(0.2);
  const [summaryThreshold, setSummaryThreshold] = useState(35);
  
  // 策略选择
  const [strategyType, setStrategyType] = useState("NONE");

  // 加载当前Agent的模型ID和模型列表
  useEffect(() => {
    async function loadData() {
      setLoading(true);
      try {
        // 并行加载模型列表和当前模型ID
        const [modelsResponse, currentModelResponse] = await Promise.all([
          getModels("CHAT"),
          getAgentModel(agentId)
        ]);

        // 处理模型列表
        if (modelsResponse.code === 200 && Array.isArray(modelsResponse.data)) {
          setModels(modelsResponse.data);
        }

        // 处理当前模型ID和配置
        if (currentModelResponse.code === 200 && currentModelResponse.data) {
          const { modelId, temperature: temp, topP: top, topK: k, maxTokens: max, 
                 strategyType: strategy, reserveRatio: ratio, summaryThreshold: threshold } = currentModelResponse.data;
          
          if (modelId) setSelectedModelId(modelId);
          if (temp !== undefined) setTemperature(temp);
          if (top !== undefined) setTopP(top);
          if (k !== undefined) setTopK(k);
          if (max !== undefined) setMaxTokens(max);
          if (strategy !== undefined) setStrategyType(strategy);
          if (ratio !== undefined) setReserveRatio(ratio);
          if (threshold !== undefined) setSummaryThreshold(threshold);
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
      // 构建模型配置
      const modelConfig = {
        modelId: selectedModelId,
        temperature,
        topP,
        topK,
        maxTokens,
        strategyType,
        reserveRatio,
        summaryThreshold
      };
      
      // 调用API保存模型ID和配置
      const response = await setAgentModelWithToast(agentId, modelConfig);
      if (response.code === 200) {
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
      <DialogContent className="max-w-5xl max-h-[85vh] flex flex-col overflow-hidden">
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
        
        <Tabs defaultValue="modelSelect" className="w-full mt-4">
          <TabsList>
            <TabsTrigger value="modelSelect">模型选择</TabsTrigger>
            <TabsTrigger value="modelParams">模型参数</TabsTrigger>
            <TabsTrigger value="tokenStrategy">Token策略</TabsTrigger>
          </TabsList>
          
          <TabsContent value="modelSelect" className="pt-4">
            {loading ? (
              <div className="flex justify-center py-10">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            ) : models.length === 0 ? (
              <div className="text-center py-10 text-muted-foreground">
                暂无可用模型，请先在设置中添加模型
              </div>
            ) : (
              <ScrollArea className="flex-1 overflow-auto pr-4" style={{maxHeight: "60vh"}}>
                <RadioGroup 
                  value={selectedModelId || ""} 
                  onValueChange={setSelectedModelId}
                  className="space-y-6"
                >
                  {Object.entries(modelsByProvider).map(([provider, providerModels]) => (
                    <div key={provider} className="space-y-3">
                      <h3 className="font-medium text-sm text-muted-foreground uppercase tracking-wider">
                        {provider}
                      </h3>
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                        {providerModels.map(model => (
                          <div 
                            key={model.id}
                            className={`
                              relative border rounded-lg p-4 transition-colors cursor-pointer
                              ${selectedModelId === model.id ? 'border-primary bg-primary/5 ring-1 ring-primary' : 'border-border hover:border-primary/50'}
                              ${!model.status ? 'opacity-60' : ''}
                              h-full flex flex-col
                            `}
                          >
                            {model.isOfficial && (
                              <div className="absolute -top-2 right-2 z-10">
                                <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-100 px-2 py-0.5">
                                  官方
                                </Badge>
                              </div>
                            )}
                            
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
                                </div>
                                {selectedModelId === model.id && (
                                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-blue-600">
                                    <circle cx="12" cy="12" r="10" fill="#4285F4" stroke="none" />
                                    <path d="M8 12l2 2 6-6" stroke="white" strokeWidth="2" />
                                  </svg>
                                )}
                              </div>
                              
                              <div className="text-sm text-muted-foreground mb-2 flex-1">
                                {model.description || "无描述"} 
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
          </TabsContent>
          
          <TabsContent value="modelParams" className="space-y-6 py-4">
            <div className="space-y-4">
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium">Temperature</span>
                  <span>{temperature}</span>
                </div>
                <Slider
                  value={[temperature]}
                  min={0}
                  max={2}
                  step={0.1}
                  onValueChange={(value) => setTemperature(value[0])}
                />
                <p className="text-sm text-muted-foreground">控制输出的随机性，值越高输出越随机</p>
              </div>
              
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium">Top P</span>
                  <span>{topP}</span>
                </div>
                <Slider
                  value={[topP]}
                  min={0}
                  max={1}
                  step={0.01}
                  onValueChange={(value) => setTopP(value[0])}
                />
                <p className="text-sm text-muted-foreground">控制模型输出的多样性，值越低输出越确定</p>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium">Top K</span>
                  <span>{topK}</span>
                </div>
                <Slider
                  value={[topK]}
                  min={1}
                  max={100}
                  step={1}
                  onValueChange={(value) => setTopK(value[0])}
                />
                <p className="text-sm text-muted-foreground">限制每次采样时考虑的token数量，值越小生成的文本越确定</p>
              </div>
            </div>
          </TabsContent>
          
          <TabsContent value="tokenStrategy" className="space-y-6 py-4">
            <div>
              <h3 className="mb-4 font-medium">策略选择</h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div 
                  className={`border rounded-lg p-4 cursor-pointer ${strategyType === "NONE" ? "bg-blue-50 border-blue-500" : ""}`}
                  onClick={() => setStrategyType("NONE")}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <div className={`w-4 h-4 rounded-full ${strategyType === "NONE" ? "bg-blue-500" : "border border-gray-400"}`}>
                      {strategyType === "NONE" && <div className="w-2 h-2 bg-white rounded-full m-1"></div>}
                    </div>
                    <h4 className="font-medium">无策略</h4>
                  </div>
                  <p className="text-sm text-muted-foreground">不对历史对话进行处理，可能导致超限错误</p>
                </div>
                
                <div 
                  className={`border rounded-lg p-4 cursor-pointer ${strategyType === "SLIDING_WINDOW" ? "bg-blue-50 border-blue-500" : ""}`}
                  onClick={() => setStrategyType("SLIDING_WINDOW")}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <div className={`w-4 h-4 rounded-full ${strategyType === "SLIDING_WINDOW" ? "bg-blue-500" : "border border-gray-400"}`}>
                      {strategyType === "SLIDING_WINDOW" && <div className="w-2 h-2 bg-white rounded-full m-1"></div>}
                    </div>
                    <h4 className="font-medium">滑动窗口</h4>
                  </div>
                  <p className="text-sm text-muted-foreground">对历史对话进行滑动窗口处理</p>
                </div>
                
                <div 
                  className={`border rounded-lg p-4 cursor-pointer ${strategyType === "SUMMARIZE" ? "bg-blue-50 border-blue-500" : ""}`}
                  onClick={() => setStrategyType("SUMMARIZE")}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <div className={`w-4 h-4 rounded-full ${strategyType === "SUMMARIZE" ? "bg-blue-500" : "border border-gray-400"}`}>
                      {strategyType === "SUMMARIZE" && <div className="w-2 h-2 bg-white rounded-full m-1"></div>}
                    </div>
                    <h4 className="font-medium">摘要策略</h4>
                  </div>
                  <p className="text-sm text-muted-foreground">将旧消息转换为摘要，保留关键信息</p>
                </div>
              </div>
            </div>

            <div className="space-y-2 mt-6">
              <div className="flex items-center justify-between">
                <span className="font-medium">最大上下文Token</span>
                <span>{maxTokens}</span>
              </div>
              <Slider
                value={[maxTokens]}
                min={1000}
                max={32000}
                step={1000}
                onValueChange={(value) => setMaxTokens(value[0])}
              />
              <p className="text-sm text-muted-foreground">模型可接受的最大上下文长度</p>
            </div>
            
            {strategyType === "SUMMARIZE" && (
              <>
                <div className="space-y-2 mt-4">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">保留比例</span>
                    <span>{(reserveRatio * 100).toFixed(0)}%</span>
                  </div>
                  <Slider
                    value={[reserveRatio]}
                    min={0}
                    max={1}
                    step={0.1}
                    onValueChange={(value) => setReserveRatio(value[0])}
                  />
                  <p className="text-sm text-muted-foreground">保留最近对话的比例</p>
                </div>
                
                <div className="space-y-2 mt-4">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">摘要触发阈值</span>
                    <span>{summaryThreshold}%</span>
                  </div>
                  <Slider
                    value={[summaryThreshold]}
                    min={30}
                    max={90}
                    step={5}
                    onValueChange={(value) => setSummaryThreshold(value[0])}
                  />
                  <p className="text-sm text-muted-foreground">当上下文Token数超过此阈值时触发摘要生成</p>
                </div>
              </>
            )}
          </TabsContent>
        </Tabs>

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
  )
}