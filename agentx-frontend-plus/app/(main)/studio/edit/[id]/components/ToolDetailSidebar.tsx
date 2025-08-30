import React, { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetFooter } from '@/components/ui/sheet';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import type { Tool, ToolVersion } from '@/types/tool';
import { getToolDetail, getMarketToolVersions, getMarketToolVersionDetail } from '@/lib/tool-service';
import { X, RefreshCw, Puzzle, Command, Key, Save, Settings } from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { toast } from 'sonner';
import { Input } from '@/components/ui/input';
import ToolParametersModal from './ToolParametersModal';

// 缓存已请求过的工具详情
const toolDetailsCache = new Map<string, any>();

interface ToolDetailSidebarProps {
  tool: Tool | null;
  isOpen: boolean;
  onClose: () => void;
  presetParameters?: {
    [functionName: string]: {
      [paramName: string]: string
    }
  };
  onSavePresetParameters?: (toolId: string, presetParams: Record<string, Record<string, string>>) => void;
}

const ToolDetailSidebar: React.FC<ToolDetailSidebarProps> = ({ 
  tool: initialTool, 
  isOpen, 
  onClose,
  presetParameters = {},
  onSavePresetParameters
}) => {
  const [detailedTool, setDetailedTool] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showParametersModal, setShowParametersModal] = useState(false);

  useEffect(() => {
    if (isOpen && initialTool) {
      const cacheKey = `${initialTool.id}-${(initialTool as any).version || ''}`;
      
      // 如果缓存中有这个工具的详情，直接使用
      if (toolDetailsCache.has(cacheKey)) {
        setDetailedTool(toolDetailsCache.get(cacheKey));
        return;
      }
      
      const fetchDetails = async () => {
        setIsLoading(true);
        setDetailedTool(null);
        
        try {
          // 使用getMarketToolVersionDetail方法获取详细信息，传入toolId和version
          const toolId = initialTool.toolId || '';
          // 注意：Tool类型上没有version属性，尝试从initialTool对象获取，如果没有则使用空字符串
          const version = (initialTool as any).version || '';
          
          if (toolId && version) {
            const detailResponse = await getMarketToolVersionDetail(toolId, version);
            if (detailResponse.code === 200 && detailResponse.data) {
              setDetailedTool(detailResponse.data);
              // 保存到缓存
              toolDetailsCache.set(cacheKey, detailResponse.data);
            } else {
 
              setDetailedTool(initialTool);
              // 即使是失败的情况，也缓存初始工具数据，避免重复请求
              toolDetailsCache.set(cacheKey, initialTool);
            }
          } else {
            // 如果没有toolId或version，尝试使用普通工具详情接口
            const fallbackResponse = await getToolDetail(initialTool.id);
            if (fallbackResponse.code === 200 && fallbackResponse.data) {
              setDetailedTool(fallbackResponse.data);
              // 保存到缓存
              toolDetailsCache.set(cacheKey, fallbackResponse.data);
            } else {
              setDetailedTool(initialTool);
              // 即使是失败的情况，也缓存初始工具数据，避免重复请求
              toolDetailsCache.set(cacheKey, initialTool);
            }
          }
        } catch (error) {
 
          setDetailedTool(initialTool);
          // 即使是失败的情况，也缓存初始工具数据，避免重复请求
          toolDetailsCache.set(cacheKey, initialTool);
        } finally {
          setIsLoading(false);
        }
      };
      fetchDetails();
    } else if (!isOpen) {
      setDetailedTool(null);
      setShowParametersModal(false);
    }
  }, [isOpen, initialTool]);

  const displayData = detailedTool || initialTool;

  if (!displayData && !isLoading) {
    return null;
  }
  
  const handleOpenParametersModal = () => {
    setShowParametersModal(true);
  };

  const handleCloseParametersModal = () => {
    setShowParametersModal(false);
  };

  const renderContent = () => {
    if (isLoading && !detailedTool) {
      return (
        <>
          <SheetHeader className="p-6 border-b">
            <SheetTitle className="text-xl">工具详情</SheetTitle>
            <SheetDescription>加载中...</SheetDescription>
          </SheetHeader>
          <div className="p-6 space-y-4">
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
            <Separator />
            <Skeleton className="h-6 w-1/4 mb-2" />
            <Skeleton className="h-4 w-1/2" />
            <Separator />
            <Skeleton className="h-6 w-1/4 mb-2" />
            <Skeleton className="h-10 w-full" />
            <Separator />
            <Skeleton className="h-6 w-1/4 mb-2" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
          </div>
        </>
      );
    }

    if (!displayData) return (
      <>
        <SheetHeader className="p-6 border-b">
          <SheetTitle className="text-xl">工具详情</SheetTitle>
          <SheetDescription>无法加载工具信息</SheetDescription>
        </SheetHeader>
        <div className="p-6 text-center">无法加载工具信息。请稍后再试。</div>
      </>
    );
    
    // 工具功能列表
    const toolFunctions = displayData.toolList || displayData.tool_list || [];
    
    return (
      <>
        <SheetHeader className="p-6 border-b">
          <div className="flex items-center justify-between">
            <SheetTitle className="text-xl">{displayData.name || '工具详情'}</SheetTitle>
          </div>
          <SheetDescription>{displayData.subtitle || displayData.description || '暂无详细描述。'}</SheetDescription>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* 显示基本信息 */}
          {displayData.author && (
            <>
              <div className="space-y-1">
                <h4 className="font-medium text-sm text-muted-foreground">作者</h4>
                <p className="text-sm">{displayData.author}</p>
              </div>
              <Separator />
            </>
          )}
          
          {/* 详细描述 */}
          <div className="space-y-1">
            <h4 className="font-medium text-sm text-muted-foreground">描述</h4>
            <p className="text-sm whitespace-pre-line">{displayData.description || displayData.subtitle || '暂无描述'}</p>
          </div>
          <Separator />
          
          {/* 工具功能列表 */}
          {toolFunctions.length > 0 && (
            <>
              <div className="space-y-1">
                <h4 className="font-medium text-sm text-muted-foreground flex items-center">
                  <Puzzle className="w-4 h-4 mr-2" /> 功能列表 ({toolFunctions.length})
                </h4>
                <div className="space-y-2 mt-3">
                  {toolFunctions.map((item: any, index: number) => (
                    <div key={item.name || index} className="rounded-md border overflow-hidden">
                      {/* 功能名称 */}
                      <div className="px-3 py-2 bg-muted/5 flex items-center gap-2">
                        <div className="flex h-5 w-5 items-center justify-center rounded-md bg-primary/10">
                          <Command className="h-3 w-3" />
                        </div>
                        <div className="font-medium text-sm">{item.name}</div>
                        {item.enabled === false && (
                          <Badge variant="outline" className="ml-auto text-xs">已禁用</Badge>
                        )}
                      </div>
                      
                      {/* 功能描述 */}
                      {item.description && (
                        <div className="px-3 py-2 text-xs text-muted-foreground">
                          {item.description}
                        </div>
                      )}
                      
                      {/* 参数展示 */}
                      {item.parameters && Object.keys(item.parameters.properties || {}).length > 0 && (
                        <div className="px-2 py-2 border-t border-gray-100">
                          <div className="text-[10px] uppercase font-medium text-muted-foreground mb-1 px-1">参数</div>
                          <div className="space-y-1">
                            {Object.entries(item.parameters.properties || {})
                              .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                              .map(([key, value]) => {
                                const cleanKey = key.replace(/^\{/, '');
                                return (
                                  <div key={key} className="flex items-center gap-2 px-1">
                                    <code className="text-[10px] text-primary bg-primary/5 px-1 py-0.5 rounded">{cleanKey}</code>
                                    {item.parameters.required?.includes(cleanKey) && (
                                      <Badge variant="outline" className="text-[8px] h-3 px-1">必填</Badge>
                                    )}
                                  </div>
                                );
                              })}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>

        <SheetFooter className="p-6 border-t flex justify-between">
          <Button variant="outline" onClick={onClose}>关闭</Button>
          {toolFunctions.length > 0 && (
            <Button 
              variant="secondary" 
              onClick={handleOpenParametersModal}
              className="flex items-center gap-1"
            >
              <Settings className="w-4 h-4" />
              配置参数预设
            </Button>
          )}
        </SheetFooter>

        {/* 参数预设弹窗 */}
        {showParametersModal && initialTool && (
          <ToolParametersModal
            isOpen={showParametersModal}
            onClose={handleCloseParametersModal}
            tool={initialTool}
            toolFunctions={toolFunctions}
            presetParameters={presetParameters}
            onSavePresetParameters={onSavePresetParameters}
          />
        )}
      </>
    );
  }

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent className="w-[400px] sm:w-[540px] p-0 flex flex-col overflow-hidden">
        {renderContent()}
      </SheetContent>
    </Sheet>
  );
}

export default ToolDetailSidebar; 