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
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { createModelWithToast, updateModelWithToast, getModelTypesWithToast } from "@/lib/api-services"
import { Loader2 } from "lucide-react"

// 模型类型映射
const MODEL_TYPE_LABELS: Record<string, string> = {
  "CHAT": "对话模型",
  "EMBEDDING": "嵌入模型",
  "IMAGE": "图像模型"
}

// 模型配置类型
interface ModelConfig {
  maxContextLength?: number;
  [key: string]: any;
}

// 模型表单数据类型
interface ModelFormData {
  id?: string;
  providerId: string;
  modelId: string;
  name: string;
  description: string;
  type: string;
  modelEndpoint?: string;
  config: ModelConfig;
}

interface ModelDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  providerId: string;
  providerName?: string;
  model?: any; // 编辑模式下传入现有模型
  onSuccess?: () => void;
}

export function ModelDialog({ 
  open, 
  onOpenChange, 
  providerId, 
  providerName, 
  model, 
  onSuccess 
}: ModelDialogProps) {
  const isEditMode = !!model;
  const [formData, setFormData] = useState<ModelFormData>({
    providerId: providerId,
    modelId: "",
    name: "",
    description: "",
    type: "CHAT",
    modelEndpoint: "",
    config: {
      maxContextLength: 4096
    }
  });
  
  const [loading, setLoading] = useState(false);
  const [modelTypes, setModelTypes] = useState<string[]>([]);
  const [typesLoading, setTypesLoading] = useState(false);
  
  // 加载模型类型列表
  useEffect(() => {
    async function loadModelTypes() {
      setTypesLoading(true);
      try {
        const response = await getModelTypesWithToast();
        if (response.code === 200 && Array.isArray(response.data)) {
          setModelTypes(response.data);
        }
      } catch (error) {
        console.error("获取模型类型列表失败:", error);
      } finally {
        setTypesLoading(false);
      }
    }
    
    if (open) {
      loadModelTypes();
    }
  }, [open]);
  
  // 编辑模式下，初始化表单数据
  useEffect(() => {
    if (model && isEditMode) {
      setFormData({
        id: model.id,
        providerId: model.providerId || providerId,
        modelId: model.modelId || "",
        name: model.name || "",
        description: model.description || "",
        type: model.type || "CHAT",
        modelEndpoint: model.modelEndpoint || "",
        config: model.config || { maxContextLength: 4096 }
      });
    } else {
      // 新建模式下重置表单
      setFormData({
        providerId: providerId,
        modelId: "",
        name: "",
        description: "",
        type: "CHAT",
        modelEndpoint: "",
        config: {
          maxContextLength: 4096
        }
      });
    }
  }, [model, isEditMode, providerId, open]);
  
  // 处理输入变化
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  // 处理类型选择变化
  const handleTypeChange = (value: string) => {
    setFormData(prev => ({
      ...prev,
      type: value
    }));
  };
  
  // 处理配置字段变化
  const handleConfigChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      config: {
        ...prev.config,
        [name]: value === "" ? undefined : Number(value)
      }
    }));
  };
  
  // 提交表单
  const handleSubmit = async () => {
    setLoading(true);
    try {
      let response;
      if (isEditMode) {
        response = await updateModelWithToast(formData);
      } else {
        response = await createModelWithToast(formData);
      }
      
      if (response.code === 200) {
        // 关闭对话框
        onOpenChange(false);
        // 然后调用onSuccess回调来刷新数据
        if (onSuccess) onSuccess();
      }
    } catch (error) {
      console.error("提交模型失败:", error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditMode ? "编辑模型" : "添加模型"}</DialogTitle>
          <DialogDescription>
            {isEditMode 
              ? "修改模型配置信息" 
              : `为${providerName || "服务提供商"}添加一个新模型`}
          </DialogDescription>
        </DialogHeader>
        
        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="modelId">模型ID <span className="text-red-500">*</span></Label>
            <Input
              id="modelId"
              name="modelId"
              value={formData.modelId}
              onChange={handleInputChange}
              placeholder="模型真正的 ID，用于高可用"
              required
            />
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="name">名称 <span className="text-red-500">*</span></Label>
            <Input
              id="name"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder="例如：GPT-3.5 Turbo"
              required
            />
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="description">描述</Label>
            <Textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="可选描述"
              rows={3}
            />
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="type">类型 <span className="text-red-500">*</span></Label>
            <Select 
              value={formData.type} 
              onValueChange={handleTypeChange}
              disabled={isEditMode || typesLoading}
            >
              <SelectTrigger>
                <SelectValue placeholder={typesLoading ? "加载中..." : "选择模型类型"} />
              </SelectTrigger>
              <SelectContent>
                {modelTypes.map(type => (
                  <SelectItem key={type} value={type}>
                    {MODEL_TYPE_LABELS[type] || type}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="modelEndpoint">模型部署名称</Label>
            <Input
              id="modelEndpoint"
              name="modelEndpoint"
              value={formData.modelEndpoint}
              onChange={handleInputChange}
              placeholder="部署名称"
            />
          </div>
       
        </div>
        
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            取消
          </Button>
          <Button 
            onClick={handleSubmit} 
            disabled={loading || !formData.modelId || !formData.name || !formData.type}
          >
            {loading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                保存中...
              </>
            ) : (
              isEditMode ? "保存修改" : "添加"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 