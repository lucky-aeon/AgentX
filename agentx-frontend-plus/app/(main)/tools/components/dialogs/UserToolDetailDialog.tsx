import React, { useMemo } from 'react';
import { UserTool, ToolFunction } from "../../utils/types";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Command, Wrench } from "lucide-react";
import ReactMarkdown from "react-markdown";
import { ToolLabels } from "../shared/ToolLabels";
import IconComponent from '../icons/IconComponent';
import { formatDate } from '@/lib/utils';
import { Separator } from '@/components/ui/separator';
import { ScrollArea } from '@/components/ui/scroll-area';

interface UserToolDetailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tool: UserTool | null;
}

export function UserToolDetailDialog({
  open,
  onOpenChange,
  tool
}: UserToolDetailDialogProps) {
  // 提取工具函数列表，优先使用toolList，其次使用tool_list
  const toolFunctions = useMemo(() => {
    if (!tool) return [];
    return tool.toolList || tool.tool_list || [];
  }, [tool]);

  // 获取作者信息，优先使用userName，其次使用author
  const authorName = useMemo(() => {
    if (!tool) return '';
    // 如果userName为null，则优先使用author，如果都没有则返回空字符串
    return tool.userName || tool.author || '';
  }, [tool]);

  // 格式化创建时间
  const formattedDate = useMemo(() => {
    if (!tool?.createdAt) return '';
    return formatDate(new Date(tool.createdAt));
  }, [tool]);

  if (!tool) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh] flex flex-col">
        <DialogHeader>
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-md border">
              <IconComponent icon={tool.icon} className="h-6 w-6" />
            </div>
            <div>
              <DialogTitle className="text-xl">{tool.name}</DialogTitle>
              <DialogDescription className="text-sm text-muted-foreground">
                {tool.subtitle}
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="space-y-4 flex-1 overflow-hidden">
          {/* 作者和创建日期信息 */}
          <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-muted-foreground">
            {authorName && (
              <div className="flex items-center gap-1">
                <span>作者:</span>
                <span className="font-medium">{authorName}</span>
              </div>
            )}
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
              {tool.description}
            </p>
          </div>
          
          {/* 工具功能列表 */}
          {toolFunctions.length > 0 && (
            <div className="space-y-2">
              <h3 className="text-sm font-medium">工具功能 ({toolFunctions.length})</h3>
              <ScrollArea className="h-[200px] pr-4">
                <div className="space-y-2">
                  {toolFunctions.map((item, i) => (
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
                        <div className="px-4 py-3 bg-muted/5">
                          <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                          <div className="grid grid-cols-12 gap-2">
                            {Object.entries(item.parameters.properties)
                              .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
                              .map(([key, value]) => {
                                // 处理特殊键名，移除可能的前缀如 "{"
                                const cleanKey = key.replace(/^\{/, '');
                                // 确保value是对象并且有description属性
                                const description = typeof value === 'object' && value ? value.description : null;
                                
                                if (description === null) return null;
                                
                                return (
                                  <div key={key} className="col-span-12 sm:col-span-6 xl:col-span-4">
                                    <div className="flex items-center gap-2">
                                      <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{cleanKey}</code>
                                      {item.parameters && item.parameters.required?.includes(cleanKey) && (
                                        <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                      )}
                                    </div>
                                    <div className="text-xs text-muted-foreground mt-1">{description}</div>
                                  </div>
                                );
                              })
                              .filter(Boolean)}
                          </div>
                        </div>
                      ) : item.inputSchema && Object.keys(item.inputSchema.properties).length > 0 ? (
                        // 参数列表 - 处理inputSchema
                        <div className="px-4 py-3 bg-muted/5">
                          <div className="text-xs uppercase font-medium text-muted-foreground mb-2">参数</div>
                          <div className="grid grid-cols-12 gap-2">
                            {Object.entries(item.inputSchema.properties).map(([key, value]) => (
                              <div key={key} className="col-span-12 sm:col-span-6 xl:col-span-4">
                                <div className="flex items-center gap-2">
                                  <code className="text-xs text-primary bg-primary/5 px-1.5 py-0.5 rounded">{key}</code>
                                  {item.inputSchema && item.inputSchema.required?.includes(key) && (
                                    <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                                  )}
                                </div>
                                <div className="text-xs text-muted-foreground mt-1">{(value as any).description}</div>
                              </div>
                            ))}
                          </div>
                        </div>
                      ) : null}
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            关闭
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 