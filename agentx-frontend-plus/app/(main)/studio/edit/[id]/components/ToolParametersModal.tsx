import React, { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Key, Save, AlertCircle, Check } from 'lucide-react';
import { toast } from 'sonner';
import type { Tool } from '@/types/tool';

interface ToolParametersModalProps {
  isOpen: boolean;
  onClose: () => void;
  tool: Tool;
  toolFunctions: any[];
  presetParameters?: {
    [functionName: string]: {
      [paramName: string]: string
    }
  };
  onSavePresetParameters?: (toolId: string, presetParams: Record<string, Record<string, string>>) => void;
}

const ToolParametersModal: React.FC<ToolParametersModalProps> = ({
  isOpen,
  onClose,
  tool,
  toolFunctions,
  presetParameters = {},
  onSavePresetParameters
}) => {
  const [localPresetParams, setLocalPresetParams] = useState<Record<string, Record<string, string>>>({});
  const [activeTab, setActiveTab] = useState<string>("");

  // 判断功能是否有参数
  const hasParameters = (func: any): boolean => {
    return func && 
           func.parameters && 
           func.parameters.properties && 
           Object.keys(func.parameters.properties).filter(key => 
             !['additionalProperties', 'definitions', 'required'].includes(key)
           ).length > 0;
  };

  // 初始化预设参数和激活的标签页
  useEffect(() => {
    if (isOpen && tool) {
      // 确保presetParameters是正确的格式
      // 后端可能返回的是字符串形式的参数，需要解析为对象
      setLocalPresetParams(presetParameters || {});
      
      // 过滤出有参数的功能
      const functionsWithParams = toolFunctions.filter(func => hasParameters(func));
      
      // 默认选择第一个有参数的功能作为激活标签
      if (functionsWithParams.length > 0) {
        setActiveTab(functionsWithParams[0].name);
      }

      // 添加调试日志
 
    }
  }, [isOpen, tool, toolFunctions, presetParameters]);

  // 获取预设参数值
  const getPresetValue = (functionName: string, paramName: string): string => {
    try {
      if (!localPresetParams || !localPresetParams[functionName]) {
        return '';
      }
      return localPresetParams[functionName]?.[paramName] || '';
    } catch (e) {
 
      return '';
    }
  };

  // 更新预设参数值
  const updatePresetValue = (functionName: string, paramName: string, value: string) => {
    setLocalPresetParams(prev => {
      const newParams = {...prev};
      if (!newParams[functionName]) {
        newParams[functionName] = {};
      }
      newParams[functionName][paramName] = value;
      return newParams;
    });
  };

  // 保存预设参数
  const savePresetParameters = () => {
    if (onSavePresetParameters && tool && tool.toolId) {
      onSavePresetParameters(tool.toolId, localPresetParams);
      toast.success("参数预设已保存");
      onClose();
    } else if (!tool?.toolId) {
      toast.error("工具ID不存在，无法保存参数");
    }
  };

  // 清除某个功能的所有预设参数
  const clearFunctionPresets = (functionName: string) => {
    setLocalPresetParams(prev => {
      const newParams = {...prev};
      delete newParams[functionName];
      return newParams;
    });
    toast.success(`已清除 ${functionName} 的所有预设参数`);
  };

  // 清理参数名称，移除前缀的花括号
  const cleanParamName = (name: string): string => {
    return name.replace(/^\{/, '').replace(/\}$/, '');
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-4xl max-h-[85vh] flex flex-col overflow-hidden p-0">
        <DialogHeader className="px-6 py-4 border-b">
          <DialogTitle className="flex items-center gap-2">
            <Key className="h-5 w-5" />
            <span>{tool.name} 参数预设</span>
          </DialogTitle>
          <DialogDescription>
            预设参数将在调用工具时自动传入，无需用户手动输入。
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-1 overflow-hidden">{/* 移除顶部边距 */}
          {toolFunctions.filter(func => hasParameters(func)).length > 0 ? (
            <Tabs 
              value={activeTab} 
              onValueChange={setActiveTab} 
              orientation="vertical" 
              className="flex flex-1 overflow-hidden"
            >
              <div className="w-[220px] border-r overflow-hidden flex flex-col">
                <h3 className="text-xs font-semibold uppercase text-muted-foreground px-4 py-3 border-b">功能列表</h3>
                <ScrollArea className="flex-1">
                  <TabsList className="flex flex-col h-auto gap-1 bg-transparent p-2">
                    {toolFunctions
                      .filter(func => hasParameters(func))
                      .map((func) => {
                        // 检查是否有预设参数
                        const hasPresets = !!localPresetParams[func.name] && 
                          Object.keys(localPresetParams[func.name] || {}).length > 0;
                        
                        // 清理函数名
                        const cleanedName = cleanParamName(func.name);
                        
                        return (
                          <TabsTrigger 
                            key={func.name} 
                            value={func.name}
                            className="justify-start w-full relative hover:bg-gray-100 data-[state=active]:bg-blue-50 data-[state=active]:border-l-4 data-[state=active]:border-blue-500 data-[state=active]:font-medium data-[state=active]:pl-3"
                          >
                            <span className="truncate">{cleanedName}</span>
                            {hasPresets && (
                              <Badge variant="secondary" className="ml-auto text-[10px] absolute right-2">
                                <Check className="h-2 w-2 mr-1" />
                                已设置
                              </Badge>
                            )}
                          </TabsTrigger>
                        );
                      })}
                  </TabsList>
                </ScrollArea>
              </div>

              <div className="flex-1 overflow-hidden">
                {toolFunctions.map((func) => (
                  <TabsContent 
                    key={func.name} 
                    value={func.name} 
                    className="flex-1 h-full m-0"
                  >
                    <div className="flex flex-col h-full">
                      <div className="flex justify-between items-center px-6 py-3 border-b">
                        <div>
                          <h3 className="font-medium text-lg">{cleanParamName(func.name)}</h3>
                          {func.description && (
                            <p className="text-sm text-muted-foreground mt-1">{func.description}</p>
                          )}
                        </div>
                        {localPresetParams[func.name] && Object.keys(localPresetParams[func.name]).length > 0 && (
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => clearFunctionPresets(func.name)}
                          >
                            清除预设
                          </Button>
                        )}
                      </div>

                      <ScrollArea className="flex-1 px-6 py-4">
                        {hasParameters(func) ? (
                          <div className="space-y-6">
                            {Object.entries(func.parameters.properties || {})
                              .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                              .map(([paramName, paramConfig]: [string, any]) => {
                                return (
                                  <div key={paramName} className="space-y-2 bg-gray-50 p-4 rounded-md border border-gray-100">
                                    <div className="flex items-center justify-between">
                                      <div className="flex items-center gap-2">
                                        <label 
                                          htmlFor={`preset-${func.name}-${paramName}`} 
                                          className="text-sm font-medium"
                                        >
                                          {cleanParamName(paramName)}
                                        </label>
                                      </div>
                                    </div>
                                    
                                    <p className="text-xs text-muted-foreground mb-2">
                                      {paramConfig.description || `${cleanParamName(paramName)}参数的预设值`}
                                    </p>
                                    
                                    <div className="relative">
                                      <Input
                                        id={`preset-${func.name}-${paramName}`}
                                        type="text"
                                        className="w-full"
                                        placeholder={`输入${cleanParamName(paramName)}预设值`}
                                        value={getPresetValue(func.name, paramName)}
                                        onChange={(e) => updatePresetValue(func.name, paramName, e.target.value)}
                                      />
                                    </div>
                                  </div>
                                );
                              })
                            }
                          </div>
                        ) : (
                          <div className="flex items-center justify-center h-full p-8">
                            <div className="text-center bg-gray-50 p-8 rounded-lg border border-dashed border-gray-200 w-full max-w-md">
                              <AlertCircle className="h-10 w-10 text-muted-foreground mx-auto mb-4" />
                              <p className="text-lg font-medium text-muted-foreground mb-2">该功能没有可预设的参数</p>
                              <p className="text-sm text-muted-foreground">
                                可以选择左侧其他功能进行参数预设，或直接点击底部"保存"按钮。
                              </p>
                            </div>
                          </div>
                        )}
                      </ScrollArea>
                    </div>
                  </TabsContent>
                ))}
              </div>
            </Tabs>
          ) : (
            <div className="flex items-center justify-center w-full p-12">
              <div className="text-center bg-gray-50 p-8 rounded-lg border border-dashed border-gray-200 w-full max-w-md">
                <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-lg font-medium text-muted-foreground mb-2">该工具没有可预设的参数</p>
                <p className="text-sm text-muted-foreground mb-4">
                  此工具没有任何可配置的参数，无需进行参数预设。
                </p>
                <Button variant="outline" onClick={onClose}>返回</Button>
              </div>
            </div>
          )}
        </div>

        <DialogFooter className="px-6 py-4 border-t">
          <Button variant="outline" onClick={onClose}>取消</Button>
          <Button 
            onClick={savePresetParameters}
            className="flex items-center gap-1"
          >
            <Save className="w-4 h-4" />
            保存参数预设
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default ToolParametersModal; 