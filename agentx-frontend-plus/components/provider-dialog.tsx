"use client"

import { useState, useEffect } from "react"
import { X } from "lucide-react"
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
import { createProviderWithToast, getProviderProtocolsWithToast, updateProviderWithToast } from "@/lib/api-services"
import { Loader2 } from "lucide-react"

// 协议配置项映射
interface ProtocolConfigFields {
  [key: string]: {
    label: string;
    placeholder: string;
    required: boolean;
    type: "text" | "password" | "url";
  }[];
}

// 服务商配置类型
interface ProviderConfig {
  apiKey: string;
  baseUrl?: string;
  [key: string]: string | undefined;
}

// 服务商表单数据类型
interface ProviderFormData {
  id: string;
  protocol: string;
  name: string;
  description: string;
  config: ProviderConfig;
  isOfficial: boolean;
  status: boolean;
}

// 默认的协议配置字段
const DEFAULT_PROTOCOL_CONFIG: ProtocolConfigFields = {
  "OPENAI": [
    { 
      label: "API Key", 
      placeholder: "输入API Key", 
      required: true, 
      type: "text" 
    },
    { 
      label: "基础URL", 
      placeholder: "可选，例如：https://api.openai.com/v1", 
      required: true, 
      type: "url" 
    }
  ],
  "ANTHROPIC": [
    { 
      label: "API Key", 
      placeholder: "输入Anthropic API Key", 
      required: true, 
      type: "text" 
    },
    { 
      label: "基础URL", 
      placeholder: "可选，例如：https://api.anthropic.com", 
      required: true, 
      type: "url" 
    }
  ]
  // 预留其他协议的配置字段
}

interface ProviderDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  provider?: any; // 编辑模式下传入现有提供商
  onSuccess?: () => void;
}

export function ProviderDialog({ open, onOpenChange, provider, onSuccess }: ProviderDialogProps) {
  const isEditMode = !!provider;
  const isOfficialProvider = provider?.isOfficial || false;
  
  // 如果是官方服务商，则只提供查看功能，不允许编辑
  const isReadOnly = isOfficialProvider;
  
  const [formData, setFormData] = useState<ProviderFormData>({
    id: "",
    protocol: "",
    name: "",
    description: "",
    config: {
      apiKey: "",
      baseUrl: ""
    },
    isOfficial: false,
    status: true
  });
  
  const [loading, setLoading] = useState(false);
  const [protocolsLoading, setProtocolsLoading] = useState(false);
  const [protocols, setProtocols] = useState<string[]>([]);
  const [configFields, setConfigFields] = useState<any[]>([]);
  
  // 加载协议列表
  useEffect(() => {
    async function loadProtocols() {
      setProtocolsLoading(true);
      try {
        const response = await getProviderProtocolsWithToast();
        if (response.code === 200 && Array.isArray(response.data)) {
          setProtocols(response.data);
        }
      } catch (error) {
        console.error("获取协议列表失败:", error);
      } finally {
        setProtocolsLoading(false);
      }
    }
    
    if (open) {
      loadProtocols();
    }
  }, [open]);
  
  // 编辑模式下，初始化表单数据
  useEffect(() => {
    if (provider && isEditMode) {
      setFormData({
        id: provider.id || "",
        protocol: provider.protocol || "",
        name: provider.name || "",
        description: provider.description || "",
        config: provider.config || { apiKey: "" },
        isOfficial: !!provider.isOfficial,
        status: provider.status !== undefined ? provider.status : true
      });
    } else {
      // 新建模式下重置表单
      setFormData({
        id: "",
        protocol: "",
        name: "",
        description: "",
        config: {
          apiKey: "",
          baseUrl: ""
        },
        isOfficial: false,
        status: true
      });
    }
  }, [provider, isEditMode, open]);
  
  // 当选择协议时，设置对应的配置字段
  useEffect(() => {
    if (!formData.protocol) {
      setConfigFields([]);
      return;
    }
    
    // 使用默认配置或从API获取配置
    const fields = DEFAULT_PROTOCOL_CONFIG[formData.protocol] || [];
    setConfigFields(fields);
    
    // 如果是新建提供商，重置配置
    if (!isEditMode) {
      const newConfig: ProviderConfig = { apiKey: "" };
      fields.forEach(field => {
        const key = field.label === "API Key" ? "apiKey" : 
                   field.label === "基础URL" ? "baseUrl" : 
                   field.label.toLowerCase().replace(/\s/g, '');
        newConfig[key] = "";
      });
      setFormData(prev => ({
        ...prev,
        config: newConfig
      }));
    }
  }, [formData.protocol, isEditMode]);
  
  // 处理输入变化
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  
  // 处理配置字段变化
  const handleConfigChange = (field: string, value: string) => {
    const configKey = field === "API Key" ? "apiKey" : 
                     field === "基础URL" ? "baseUrl" : 
                     field.toLowerCase().replace(/\s/g, '');
    
    setFormData(prev => ({
      ...prev,
      config: {
        ...prev.config,
        [configKey]: value
      }
    }));
  };
  
  // 处理选择协议变化
  const handleProtocolChange = (value: string) => {
    setFormData(prev => ({
      ...prev,
      protocol: value
    }));
  };
  
  // 提交表单
  const handleSubmit = async () => {
    setLoading(true);
    try {
      // 构建提交数据，移除空值
      const submitConfig: ProviderConfig = { apiKey: formData.config.apiKey };
      if (formData.config.baseUrl) {
        submitConfig.baseUrl = formData.config.baseUrl;
      }
      
      // 添加其他配置字段
      Object.entries(formData.config).forEach(([key, value]) => {
        if (key !== 'apiKey' && key !== 'baseUrl' && value) {
          submitConfig[key] = value;
        }
      });
      
      const submitData = {
        ...formData,
        config: submitConfig
      };
      
      let response;
      if (isEditMode) {
        response = await updateProviderWithToast(submitData);
      } else {
        response = await createProviderWithToast(submitData);
      }
      
      if (response.code === 200) {
        onOpenChange(false);
        if (onSuccess) onSuccess();
      }
    } catch (error) {
      console.error("提交服务商失败:", error);
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEditMode ? (isReadOnly ? "查看服务商" : "编辑服务商") : "添加服务商"}</DialogTitle>
          <DialogDescription>
            {isReadOnly 
              ? "查看官方服务商信息" 
              : (isEditMode 
                  ? "编辑服务商配置信息" 
                  : "添加新的服务商，选择协议并配置连接信息")}
          </DialogDescription>
        </DialogHeader>
        
        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="protocol">协议 <span className="text-red-500">*</span></Label>
            <Select 
              value={formData.protocol} 
              onValueChange={handleProtocolChange}
              disabled={isEditMode || isReadOnly || protocolsLoading}
            >
              <SelectTrigger className="w-full">
                <SelectValue placeholder="选择协议" />
              </SelectTrigger>
              <SelectContent>
                {protocols.map((protocol) => (
                  <SelectItem key={protocol} value={protocol}>
                    {protocol}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          
          <div className="grid gap-2">
            <Label htmlFor="name">名称 <span className="text-red-500">*</span></Label>
            <Input
              id="name"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder="服务商名称"
              disabled={isReadOnly}
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
              placeholder="服务商描述"
              disabled={isReadOnly}
              rows={3}
            />
          </div>
          
          <div className="space-y-4">
            <Label>配置信息</Label>
            {configFields.map((field, index) => (
              <div key={index} className="grid gap-2">
                <Label htmlFor={field.label.toLowerCase().replace(/\s/g, '')}>
                  {field.label}
                  {field.required && <span className="text-red-500"> *</span>}
                </Label>
                <Input
                  id={field.label.toLowerCase().replace(/\s/g, '')}
                  type={field.type}
                  value={
                    field.label === "API Key" 
                      ? formData.config.apiKey || "" 
                      : field.label === "基础URL" 
                        ? formData.config.baseUrl || "" 
                        : formData.config[field.label.toLowerCase().replace(/\s/g, '')] || ""
                  }
                  onChange={(e) => handleConfigChange(field.label, e.target.value)}
                  placeholder={field.placeholder}
                  disabled={isReadOnly}
                  required={field.required}
                />
              </div>
            ))}
          </div>
        </div>
        
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {isReadOnly ? "关闭" : "取消"}
          </Button>
          {!isReadOnly && (
            <Button 
              onClick={handleSubmit} 
              disabled={loading || !formData.protocol || !formData.name || (formData.config.apiKey === "" && configFields.some(f => f.label === "API Key" && f.required))}
            >
              {loading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {isEditMode ? "保存中..." : "创建中..."}
                </>
              ) : (
                isEditMode ? "保存" : "创建"
              )}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 