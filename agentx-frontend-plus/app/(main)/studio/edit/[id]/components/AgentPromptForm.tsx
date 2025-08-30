import React, { useState } from "react";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Sparkles } from "lucide-react";
import { generateSystemPromptWithToast } from "@/lib/api-services";
import { toast } from "@/hooks/use-toast";

interface AgentFormData {
  name: string;
  description: string;
  systemPrompt: string;
  welcomeMessage: string;
  tools: Array<{ id: string; name: string; description?: string }>;
}

interface AgentPromptFormProps {
  formData: AgentFormData;
  updateFormField: (field: string, value: any) => void;
}

const AgentPromptForm: React.FC<AgentPromptFormProps> = ({
  formData,
  updateFormField,
}) => {
  const [isGenerating, setIsGenerating] = useState(false);

  // 生成系统提示词
  const handleGenerateSystemPrompt = async () => {
    if (!formData.name || !formData.description) {
      toast({
        title: "请先填写名称和描述",
        description: "生成系统提示词需要Agent的名称和描述信息",
        variant: "destructive",
      });
      return;
    }

    setIsGenerating(true);
    try {
      const response = await generateSystemPromptWithToast({
        agentName: formData.name,
        agentDescription: formData.description,
        toolIds: formData.tools.map(tool => tool.id),
      });

      if (response.code === 200 && response.data) {
        updateFormField("systemPrompt", response.data);
      }
    } catch (error) {
 
    } finally {
      setIsGenerating(false);
    }
  };
  return (
    <div className="space-y-6">
      {/* 系统提示词 */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-lg font-medium">系统提示词</h2>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleGenerateSystemPrompt}
            disabled={isGenerating}
            className="flex items-center gap-2"
          >
            <Sparkles className="h-4 w-4" />
            {isGenerating ? "生成中..." : "AI生成"}
          </Button>
        </div>
        <p className="text-sm text-muted-foreground mb-2">定义聊天助理的角色、能力和行为限制，或使用AI自动生成</p>
        <Textarea
          placeholder="输入系统提示词，或点击AI生成按钮自动生成"
          value={formData.systemPrompt}
          onChange={(e) => updateFormField("systemPrompt", e.target.value)}
          rows={8}
        />
      </div>

      {/* 欢迎消息 */}
      <div>
        <h2 className="text-lg font-medium mb-2">欢迎消息</h2>
        <p className="text-sm text-muted-foreground mb-2">用户首次与聊天助理交互时显示的消息</p>
        <Textarea
          placeholder="输入欢迎消息"
          value={formData.welcomeMessage}
          onChange={(e) => updateFormField("welcomeMessage", e.target.value)}
          rows={4}
        />
      </div>
    </div>
  );
};

export default AgentPromptForm; 