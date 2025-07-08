import React from 'react';
import { ToolFunction } from '../../utils/types';
import { Command } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface ToolFunctionCardProps {
  func: ToolFunction;
  className?: string;
}

/**
 * 工具功能卡片组件，显示工具功能的名称、描述和参数
 */
export function ToolFunctionCard({ func, className }: ToolFunctionCardProps) {
  // 获取参数，可能来自parameters或inputSchema
  const parameters = func.parameters?.properties || func.inputSchema?.properties || {};
  const requiredParams = func.parameters?.required || func.inputSchema?.required || [];

  return (
    <div className={cn("rounded-md border overflow-hidden", className)}>
      {/* 功能头部 */}
      <div className="px-3 py-2 bg-muted/10 flex items-center gap-2">
        <div className="flex h-5 w-5 items-center justify-center rounded-md bg-primary/10">
          <Command className="h-3 w-3" />
        </div>
        <div className="font-medium text-sm">{func.name}</div>
      </div>

      {/* 功能描述 */}
      <div className="px-3 py-2 text-xs text-muted-foreground">
        {func.description}
      </div>

      {/* 参数列表 */}
      {Object.keys(parameters).length > 0 && (
        <div className="px-3 py-2 bg-muted/5">
          <div className="text-xs uppercase font-medium text-muted-foreground mb-1">参数</div>
          <div className="space-y-1.5">
            {Object.entries(parameters)
              .filter(([key]) => !['additionalProperties', 'definitions', 'required'].includes(key))
              .map(([key, value]) => {
                // 处理特殊键名，移除可能的前缀
                const cleanKey = key.replace(/^\{/, '');
                // 确保value是对象并且有description属性
                const description = typeof value === 'object' && value ? (value as any).description : null;
                
                if (description === null) return null;
                
                return (
                  <div key={key} className="space-y-0.5">
                    <div className="flex items-center gap-1.5">
                      <code className="text-xs text-primary bg-primary/5 px-1 py-0.5 rounded">{cleanKey}</code>
                      {requiredParams.includes(cleanKey) && (
                        <Badge variant="outline" className="text-[10px] h-4 px-1">必填</Badge>
                      )}
                    </div>
                    <div className="text-xs text-muted-foreground">{description}</div>
                  </div>
                );
              })
              .filter(Boolean)}
          </div>
        </div>
      )}
    </div>
  );
} 