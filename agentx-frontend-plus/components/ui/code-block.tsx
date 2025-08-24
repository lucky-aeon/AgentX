"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Copy, Check } from "lucide-react";
import { useCopy } from "@/hooks/use-copy";

interface CodeBlockProps {
  children: React.ReactNode;
  code: string;
  language?: string;
}

export function CodeBlock({ children, code, language }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);
  const { copyCode } = useCopy();

  const handleCopy = async () => {
    const success = await copyCode(code);
    if (success) {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="relative group">
      {/* 复制按钮 */}
      <Button
        variant="ghost" 
        size="sm"
        onClick={handleCopy}
        className="absolute top-2 right-2 h-8 w-8 p-0 bg-white/80 hover:bg-white border shadow-sm z-10"
        aria-label={`复制${language ? language : ''}代码`}
      >
        {copied ? (
          <Check className="h-4 w-4 text-green-600" />
        ) : (
          <Copy className="h-4 w-4" />
        )}
      </Button>
      
      {/* 代码内容 */}
      {children}
    </div>
  );
}