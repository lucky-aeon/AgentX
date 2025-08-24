"use client"

import React, { useState, useEffect, useMemo } from 'react';
import { UserTool, ToolFunction } from "../../utils/types";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Command, Wrench, Clock, Download, ChevronDown, History, AlertTriangle } from "lucide-react";
import { Separator } from '@/components/ui/separator';
import { ScrollArea } from '@/components/ui/scroll-area';
import { getMarketToolVersionDetail, getMarketToolVersions, installToolWithToast, getToolDetail } from "@/lib/tool-service";
import { DeleteToolDialog } from "./DeleteToolDialog";
import { ToolHistoryVersionsDialog } from "./ToolHistoryVersionsDialog";
import { formatDate } from '@/lib/utils';
import { toast } from "@/hooks/use-toast";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

interface UserToolDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tool: UserTool | null;
  onDelete?: (tool: UserTool) => void;
}

export function UserToolDetailDialog({
  open,
  onOpenChange,
  tool,
  onDelete
}: UserToolDetailDialogProps) {
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [toolDetailLoading, setToolDetailLoading] = useState(false);
  const [toolDetailData, setToolDetailData] = useState<any>(null);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [versions, setVersions] = useState<any[]>([]);
  const [isHistoryDialogOpen, setIsHistoryDialogOpen] = useState(false);
  const [isVersionPopoverOpen, setIsVersionPopoverOpen] = useState(false);
  const [installingVersion, setInstallingVersion] = useState<string | null>(null);

  useEffect(() => {
    if (!tool || !open) return;
    if (tool.isOwner) {
      // 用户自己创建的工具，直接获取工具详情
      fetchUserToolDetail(tool, open);
    } else {
      // 安装的工具，获取市场版本详情和版本列表
      fetchToolDetail(tool, open);
      fetchToolVersions(tool);
    }
  }, [tool, open]);

  // 获取用户自己创建的工具详情
  async function fetchUserToolDetail(currentTool: UserTool, isOpen: boolean) {
    if (!currentTool || !isOpen) return;
    try {
      setToolDetailLoading(true);
      const toolId = currentTool.toolId || currentTool.id;
      const response = await getToolDetail(toolId);
      if (response.code === 200) {
        setToolDetailData(response.data);
      } else {
        setToolDetailData(null);
      }
    } catch (error) {
      setToolDetailData(null);
    } finally {
      setToolDetailLoading(false);
    }
  }

  // 获取工具详情
  async function fetchToolDetail(currentTool: UserTool, isOpen: boolean) {
    if (!currentTool || !isOpen) return;
    
    try {
      setToolDetailLoading(true);
      
      // 优先使用toolId，其次使用id获取详情
      const toolId = currentTool.toolId || currentTool.id;
      const version = currentTool.current_version || currentTool.currentVersion || "0.0.1";
      
      // 调用API获取工具详情
      const response = await getMarketToolVersionDetail(toolId, version);
      
      if (response.code === 200) {
        setToolDetailData(response.data);
      } else {
        console.error("获取工具详情失败", response.message);
        // 失败时使用传入的tool对象
        setToolDetailData(null);
      }
    } catch (error) {
      console.error("获取工具详情失败", error);
      setToolDetailData(null);
    } finally {
      setToolDetailLoading(false);
    }
  }

  // 获取工具版本列表
  async function fetchToolVersions(currentTool: UserTool) {
    if (!currentTool) return;
    
    try {
      setVersionsLoading(true);
      
      // 优先使用toolId，其次使用id
      const toolId = currentTool.toolId || currentTool.id;
      
      // 调用API获取版本列表
      const response = await getMarketToolVersions(toolId);
      
      if (response.code === 200 && response.data.length > 0) {
        // 按照版本号排序
        const sortedVersions = [...response.data].sort((a, b) => {
          return compareVersions(b.version, a.version);
        });
        setVersions(sortedVersions);
      } else {
        console.error("获取工具版本列表失败", response.message);
        setVersions([]);
      }
    } catch (error) {
      console.error("获取工具版本列表失败", error);
      setVersions([]);
    } finally {
      setVersionsLoading(false);
    }
  }

  // 比较版本号的函数
  function compareVersions(v1: string, v2: string) {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);
    
    for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
      const part1 = parts1[i] || 0;
      const part2 = parts2[i] || 0;
      
      if (part1 > part2) return 1;
      if (part1 < part2) return -1;
    }
    
    return 0;
  }

  // 处理删除确认
  const handleConfirmDelete = async (): Promise<boolean> => {
    if (!tool || !onDelete) return false;
    
    try {
      const success = await onDelete(tool);
      
      // 无论删除是否成功，先关闭删除确认对话框
      setIsDeleteDialogOpen(false);
      
      // 处理onDelete可能返回void或boolean的情况
      if (typeof success === 'undefined' || (typeof success === 'boolean' && success === true)) {
        // 删除成功后关闭主对话框
        onOpenChange(false);
        return true;
      }
      
      return typeof success === 'boolean' ? success : false;
    } catch (error) {
      console.error("删除工具失败", error);
      setIsDeleteDialogOpen(false);
      return false;
    }
  }

  // 合并工具数据，优先使用API获取的详情数据
  const mergedTool = toolDetailData ? {
    ...tool,
    ...toolDetailData,
    // 处理不同字段格式
    name: toolDetailData.name || tool?.name,
    description: toolDetailData.description || tool?.description,
    labels: toolDetailData.labels || tool?.labels || [],
    toolList: toolDetailData.toolList || toolDetailData.tool_list || tool?.toolList || tool?.tool_list || [],
    // 保留原有数据
    isOwner: tool?.isOwner
  } : tool;

  // 提取工具函数列表，优先使用toolList，其次使用tool_list
  const toolFunctions = useMemo(() => {
    if (!mergedTool) return [];
    return mergedTool.toolList || mergedTool.tool_list || [];
  }, [mergedTool]);

  // 获取作者信息，优先使用userName，其次使用author
  const authorName = useMemo(() => {
    if (!mergedTool) return '';
    // 如果userName为null，则优先使用author，如果都没有则返回空字符串
    return mergedTool.userName || mergedTool.author || '';
  }, [mergedTool]);

  // 格式化创建时间
  const formattedDate = useMemo(() => {
    if (!mergedTool?.createdAt) return '';
    return formatDate(new Date(mergedTool.createdAt));
  }, [mergedTool]);

  // 当前版本
  const currentVersion = useMemo(() => {
    return mergedTool?.current_version || mergedTool?.currentVersion || mergedTool?.version || "0.0.1";
  }, [mergedTool]);

  // 处理版本切换（重新安装指定版本）
  const handleSwitchVersion = async (version: string) => {
    if (!tool) return;
    
    try {
      setInstallingVersion(version);
      setIsVersionPopoverOpen(false);
      
      // 优先使用toolId，其次使用id
      const toolId = tool.toolId || tool.id;
      
      // 调用安装API
      const response = await installToolWithToast(toolId, version);
      
      if (response.code === 200) {
        toast({
          title: "版本切换成功",
          description: `已成功切换到版本 ${version}`,
        });
        
        // 更新当前显示的版本信息
        if (toolDetailData) {
          setToolDetailData({
            ...toolDetailData,
            version: version,
            current_version: version
          });
        }
      }
    } catch (error) {
      console.error("版本切换失败", error);
      toast({
        title: "版本切换失败",
        description: error instanceof Error ? error.message : "切换版本时出错",
        variant: "destructive"
      });
    } finally {
      setInstallingVersion(null);
    }
  };

  if (!mergedTool) return null;

  return (
    <>
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-md border">
                {mergedTool.icon ? (
                  <img src={mergedTool.icon} alt={mergedTool.name} className="h-6 w-6" />
                ) : (
                  <Wrench className="h-6 w-6" />
                )}
            </div>
            <div>
                <DialogTitle className="text-xl">{mergedTool.name}</DialogTitle>
              <DialogDescription className="text-sm text-muted-foreground">
                  {mergedTool.subtitle}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        {/* 删除状态警告 */}
        {mergedTool.deleted && (
          <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-amber-600 mt-0.5 flex-shrink-0" />
              <div className="text-amber-800">
                <div className="font-medium mb-1">工具来源已被删除</div>
                <div className="text-sm">
                  该工具的原始来源已被作者删除，但您仍可以继续使用已安装的版本。
                  不过，您将无法获得后续的更新和支持。
                </div>
              </div>
            </div>
          </div>
        )}

        <div className="space-y-4">
            {/* 作者和版本信息 */}
            <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {authorName && (
              <div className="flex items-center gap-1">
                <span>作者:</span>
                <span className="font-medium">{authorName}</span>
              </div>
            )}
              
              {/* 版本信息区域 */}
              <div className="flex items-center gap-1">
                <Clock className="h-3.5 w-3.5" />
                <span>当前版本:</span>
                
                {mergedTool.isOwner ? (
                  <>
                    <span className="font-medium ml-1">{currentVersion}</span>
                    <Button 
                      variant="outline" 
                      size="sm" 
                      className="h-6 px-2 gap-1 ml-2"
                      onClick={() => setIsHistoryDialogOpen(true)}
                    >
                      <History className="h-3.5 w-3.5" />
                      <span className="text-xs">历史版本</span>
                    </Button>
                  </>
                ) : (
                  <Popover open={isVersionPopoverOpen} onOpenChange={setIsVersionPopoverOpen}>
                    <PopoverTrigger asChild>
                      <Button 
                        variant="outline" 
                        size="sm" 
                        className="h-6 px-2 gap-1 ml-1"
                      >
                        <span className="font-medium">{currentVersion}</span>
                        <ChevronDown className="h-3.5 w-3.5" />
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-56 p-0" align="start">
                      <div className="text-xs font-medium p-2 border-b">
                        切换版本
                      </div>
                      <ScrollArea className="h-[200px]">
                        {versionsLoading ? (
                          <div className="p-2 text-center text-sm text-muted-foreground">
                            加载中...
                          </div>
                        ) : versions.length > 0 ? (
                          <div className="py-1">
                            {versions.map((version) => (
                              <Button
                                key={version.version}
                                variant="ghost"
                                size="sm"
                                className={`w-full justify-between rounded-none h-8 px-2 text-sm ${
                                  version.version === currentVersion ? 'bg-muted' : ''
                                }`}
                                onClick={() => handleSwitchVersion(version.version)}
                                disabled={installingVersion !== null}
                              >
                                <div className="flex items-center">
                                  <span className="font-medium">v{version.version}</span>
                                  {version.version === currentVersion && (
                                    <Badge variant="outline" className="ml-2 h-5 px-1 text-[10px]">
                                      当前
                                    </Badge>
                                  )}
                                </div>
                                {installingVersion === version.version ? (
                                  <span className="text-xs text-muted-foreground">切换中...</span>
                                ) : (
                                  <Download className="h-3.5 w-3.5 text-muted-foreground" />
                                )}
                              </Button>
                            ))}
                          </div>
                        ) : (
                          <div className="p-2 text-center text-sm text-muted-foreground">
                            无可用版本
                          </div>
                        )}
                      </ScrollArea>
                    </PopoverContent>
                  </Popover>
                )}
              </div>
              
            {formattedDate && (
              <div className="flex items-center gap-1">
                <span>创建于:</span>
                <span>{formattedDate}</span>
              </div>
            )}
          </div>
          
          <Separator />
          
          {/* 工具描述 */}
          <div className="space-y-2">
            <h3 className="text-sm font-medium">描述</h3>
            <p className="text-sm text-muted-foreground whitespace-pre-line">
                {mergedTool.description}
            </p>
          </div>
          
          {/* 工具功能列表 */}
          {toolFunctions.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-medium">工具功能 ({toolFunctions.length})</h3>
              <div className="space-y-2">
                {toolFunctions.map((item: ToolFunction, i: number) => (
                  <div key={i} className="rounded-md border overflow-hidden">
                    {/* 工具头部信息 */}
                    <div className="px-4 py-3 bg-muted/5 flex items-center gap-3">
                      <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary/10">
                        <Command className="h-3 w-3" />
                      </div>
                      <div className="font-medium">{item.name}</div>
                    </div>
                    
                    {/* 工具描述 */}
                    <div className="px-4 py-2 text-sm text-muted-foreground">
                      {item.description}
                    </div>
                    
                    {/* 参数列表 - 处理parameters */}
                    {item.parameters && Object.keys(item.parameters.properties).length > 0 ? (
                        <div className="px-2 py-2">
                        <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                          <div className="space-y-2">
                          {Object.entries(item.parameters.properties)
                            .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                            .map(([key, value]) => {
                              const cleanKey = key.replace(/^\{/, '');
                                const description = typeof value === 'object' && value && 'description' in value 
                                  ? (value as any).description 
                                  : null;
                              return (
                                  <div key={key} className="flex items-center gap-2">
                                    <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{cleanKey}</code>
                                    {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                      <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                    )}
                                    {description && (
                                      <span className="text-xs text-muted-foreground ml-2">{description}</span>
                                    )}
                                </div>
                              );
                              })}
                          </div>
                        </div>
                      ) : item.inputSchema && Object.keys(item.inputSchema.properties).length > 0 ? (
                        // 参数列表 - 处理inputSchema
                          <div className="px-2 py-2">
                          <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                            <div className="space-y-2">
                            {Object.entries(item.inputSchema.properties).map(([key, value]) => (
                                <div key={key} className="flex items-center gap-2">
                                  <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{key}</code>
                                  {item.inputSchema && item.inputSchema.required?.includes(key) && (
                                    <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                  )}
                              </div>
                            ))}
                          </div>
                        </div>
                      ) : null}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        <DialogFooter className="flex justify-between items-center">
            {/* 左侧操作按钮 */}
            <div className="flex gap-2">
              {/* 显示删除/卸载按钮 */}
              {!mergedTool.isOwner && (
                <Button 
                  variant="outline" 
                  onClick={() => setIsDeleteDialogOpen(true)}
                  className="text-red-500 hover:text-red-600"
                >
                  卸载工具
                </Button>
              )}
            </div>
            
            {/* 右侧关闭按钮 */}
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
      
    {/* 历史版本对话框 */}
    <ToolHistoryVersionsDialog
      open={isHistoryDialogOpen}
      onOpenChange={setIsHistoryDialogOpen}
      tool={tool}
    />
      
    {/* 删除确认对话框 */}
    <DeleteToolDialog
      open={isDeleteDialogOpen}
      onOpenChange={setIsDeleteDialogOpen}
      tool={tool}
      onConfirm={handleConfirmDelete}
      isDeleting={false}
    />
    </>
  );
} 