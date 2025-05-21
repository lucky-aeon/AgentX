import React from "react";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Upload, Trash } from "lucide-react";

interface AgentFormData {
  name: string;
  avatar: string | null;
  description: string;
  enabled: boolean;
}

interface AgentBasicInfoFormProps {
  formData: AgentFormData;
  selectedType: "chat" | "agent";
  updateFormField: (field: string, value: any) => void;
  triggerFileInput: () => void;
  handleAvatarUpload: (event: React.ChangeEvent<HTMLInputElement>) => void;
  removeAvatar: () => void;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
}

const AgentBasicInfoForm: React.FC<AgentBasicInfoFormProps> = ({
  formData,
  selectedType,
  updateFormField,
  triggerFileInput,
  handleAvatarUpload,
  removeAvatar,
  fileInputRef,
}) => {
  return (
    <div className="space-y-6">
      {/* 名称和头像 */}
      <div>
        <h2 className="text-lg font-medium mb-4">名称 & 头像</h2>
        <div className="flex gap-4 items-center">
          <div className="flex-1">
            <Label htmlFor="agent-name" className="mb-2 block">
              名称
            </Label>
            <Input
              id="agent-name"
              placeholder={`给你的${selectedType === "chat" ? "聊天助理" : "功能性助理"}起个名字`}
              value={formData.name}
              onChange={(e) => updateFormField("name", e.target.value)}
              className="mb-2"
            />
          </div>
          <div>
            <Label className="mb-2 block">头像</Label>
            <div className="flex items-center gap-2">
              <Avatar className="h-12 w-12">
                <AvatarImage src={formData.avatar || ""} alt="Avatar" />
                <AvatarFallback className="bg-blue-100 text-blue-600">
                  {formData.name ? formData.name.charAt(0).toUpperCase() : "🤖"}
                </AvatarFallback>
              </Avatar>
              <div className="flex flex-col gap-1">
                <Button variant="outline" size="sm" onClick={triggerFileInput}>
                  <Upload className="h-4 w-4 mr-2" />
                  上传
                </Button>
                {formData.avatar && (
                  <Button variant="outline" size="sm" onClick={removeAvatar}>
                    <Trash className="h-4 w-4 mr-2" />
                    移除
                  </Button>
                )}
              </div>
              <input
                type="file"
                ref={fileInputRef}
                className="hidden"
                accept="image/*"
                onChange={handleAvatarUpload}
              />
            </div>
          </div>
        </div>
      </div>

      {/* 描述 */}
      <div>
        <h2 className="text-lg font-medium mb-2">描述</h2>
        <Textarea
          placeholder={`输入${selectedType === "chat" ? "聊天助理" : "功能性助理"}的描述`}
          value={formData.description}
          onChange={(e) => updateFormField("description", e.target.value)}
          rows={4}
        />
      </div>

      {/* 状态信息 */}
      <div>
        <h2 className="text-lg font-medium mb-4">状态信息</h2>
        <div className="flex items-center gap-2 p-4 bg-gray-50 rounded-lg border">
          <div className="flex-1">
            <p className="text-sm text-muted-foreground">当前状态</p>
            <p className="font-medium">{formData.enabled ? "已启用" : "已禁用"}</p>
          </div>
          <Badge variant={formData.enabled ? "default" : "outline"}>
            {formData.enabled ? "已启用" : "已禁用"}
          </Badge>
        </div>
      </div>
    </div>
  );
};

export default AgentBasicInfoForm; 