"use client";

import { useToast } from "@/hooks/use-toast";

export function useCopy() {
  const { toast } = useToast();
  
  const copyToClipboard = async (text: string, successMessage = "已复制") => {
    try {
      await navigator.clipboard.writeText(text);
      toast({
        title: successMessage,
        duration: 2000,
      });
      return true;
    } catch (error) {
      // 降级方案：使用传统方法
      try {
        const textArea = document.createElement("textarea");
        textArea.value = text;
        textArea.style.position = "fixed";
        textArea.style.left = "-999999px";
        textArea.style.top = "-999999px";
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand("copy");
        textArea.remove();
        
        toast({
          title: successMessage,
          duration: 2000,
        });
        return true;
      } catch (fallbackError) {
        toast({
          title: "复制失败",
          description: "您的浏览器不支持复制功能",
          variant: "destructive",
          duration: 3000,
        });
        return false;
      }
    }
  };
  
  const copyMarkdown = async (content: string) => {
    return copyToClipboard(content, "消息已复制");
  };
  
  const copyCode = async (code: string) => {
    return copyToClipboard(code, "代码已复制");
  };
  
  return {
    copyToClipboard,
    copyMarkdown,
    copyCode,
  };
}