import React from "react";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";

interface AgentFormData {
  systemPrompt: string;
  welcomeMessage: string;
}

interface AgentPromptFormProps {
  formData: AgentFormData;
  updateFormField: (field: string, value: any) => void;
}

const AgentPromptForm: React.FC<AgentPromptFormProps> = ({
  formData,
  updateFormField,
}) => {
  return (
    <div className="space-y-6">
      {/* 系统提示词 */}
      <div>
        <h2 className="text-lg font-medium mb-2">系统提示词</h2>
        <p className="text-sm text-muted-foreground mb-2">定义聊天助理的角色、能力和行为限制</p>
        <Textarea
          placeholder="输入系统提示词"
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