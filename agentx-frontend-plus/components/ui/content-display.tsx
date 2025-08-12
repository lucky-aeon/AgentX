"use client"

import React, { useState } from "react"
import { ChevronDown, ChevronUp, Copy, Check } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { cn } from "@/lib/utils"

interface ContentDisplayProps {
  content: string
  maxLength?: number
  className?: string
  showCopy?: boolean
  jsonFormat?: boolean
}

export function ContentDisplay({ 
  content, 
  maxLength = 150, 
  className,
  showCopy = true,
  jsonFormat = false
}: ContentDisplayProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [copied, setCopied] = useState(false)

  // 如果内容为空或null，显示占位符
  if (!content) {
    return (
      <div className={cn("text-muted-foreground italic", className)}>
        无内容
      </div>
    )
  }

  // 尝试检测并格式化JSON
  const formatContent = (rawContent: string) => {
    if (jsonFormat) {
      try {
        const parsed = JSON.parse(rawContent)
        return JSON.stringify(parsed, null, 2)
      } catch {
        // 如果不是有效的JSON，返回原内容
        return rawContent
      }
    }
    return rawContent
  }

  const formattedContent = formatContent(content)
  const isLong = formattedContent.length > maxLength
  const truncatedContent = isLong ? formattedContent.substring(0, maxLength) + "..." : formattedContent

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(formattedContent)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error("复制失败:", err)
    }
  }

  // 如果内容不长，直接显示
  if (!isLong) {
    return (
      <div className={cn("relative group", className)}>
        <pre className={cn(
          "whitespace-pre-wrap break-all text-sm font-mono p-3 bg-muted rounded-md",
          jsonFormat && "bg-slate-50 border"
        )}>
          {formattedContent}
        </pre>
        {showCopy && (
          <Button
            variant="ghost"
            size="sm"
            onClick={handleCopy}
            className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity h-6 w-6 p-0"
          >
            {copied ? (
              <Check className="h-3 w-3 text-green-500" />
            ) : (
              <Copy className="h-3 w-3" />
            )}
          </Button>
        )}
      </div>
    )
  }

  // 长内容使用可折叠显示
  return (
    <div className={cn("relative group", className)}>
      <Collapsible open={isOpen} onOpenChange={setIsOpen}>
        <div className="space-y-2">
          {/* 预览内容 */}
          <pre className={cn(
            "whitespace-pre-wrap break-all text-sm font-mono p-3 bg-muted rounded-md",
            jsonFormat && "bg-slate-50 border"
          )}>
            {isOpen ? formattedContent : truncatedContent}
          </pre>
          
          {/* 展开/收起按钮 */}
          <CollapsibleTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="flex items-center gap-2 text-muted-foreground hover:text-foreground"
            >
              {isOpen ? (
                <>
                  收起内容
                  <ChevronUp className="h-4 w-4" />
                </>
              ) : (
                <>
                  展开完整内容 ({formattedContent.length} 字符)
                  <ChevronDown className="h-4 w-4" />
                </>
              )}
            </Button>
          </CollapsibleTrigger>
          
          {/* 隐藏的完整内容 */}
          <CollapsibleContent className="space-y-2">
            {/* 这里内容已经在上面的pre标签中显示了，所以这个区域可以为空 */}
          </CollapsibleContent>
        </div>
      </Collapsible>
      
      {/* 复制按钮 */}
      {showCopy && (
        <Button
          variant="ghost"
          size="sm"
          onClick={handleCopy}
          className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity h-6 w-6 p-0"
        >
          {copied ? (
            <Check className="h-3 w-3 text-green-500" />
          ) : (
            <Copy className="h-3 w-3" />
          )}
        </Button>
      )}
    </div>
  )
}

// 自动检测JSON的版本
export function SmartContentDisplay({ 
  content, 
  maxLength = 150, 
  className,
  showCopy = true
}: Omit<ContentDisplayProps, 'jsonFormat'>) {
  // 自动检测是否为JSON
  const isJson = React.useMemo(() => {
    if (!content || typeof content !== 'string') return false
    
    const trimmed = content.trim()
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) return false
    
    try {
      JSON.parse(trimmed)
      return true
    } catch {
      return false
    }
  }, [content])

  return (
    <ContentDisplay
      content={content}
      maxLength={maxLength}
      className={className}
      showCopy={showCopy}
      jsonFormat={isJson}
    />
  )
}