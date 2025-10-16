"use client"

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Slider } from "@/components/ui/slider";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { X } from "lucide-react";
import type { CreateMemoryRequest, MemoryType } from "@/types/memory";

interface CreateMemoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreate: (req: CreateMemoryRequest) => Promise<boolean>;
}

export function CreateMemoryDialog({ open, onOpenChange, onCreate }: CreateMemoryDialogProps) {
  const [type, setType] = useState<MemoryType | "">("");
  const [text, setText] = useState("");
  const [importance, setImportance] = useState<number>(0.5);
  const [tags, setTags] = useState<string[]>([]);
  const [newTag, setNewTag] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const reset = () => {
    setType("");
    setText("");
    setImportance(0.5);
    setTags([]);
    setNewTag("");
    setSubmitting(false);
  };

  const handleSubmit = async () => {
    if (!type || !text.trim()) return;
    setSubmitting(true);
    const req: CreateMemoryRequest = {
      type: type as MemoryType,
      text: text.trim(),
      importance,
      tags,
      data: null,
    };
    const ok = await onCreate(req);
    setSubmitting(false);
    if (ok) {
      reset();
      onOpenChange(false);
    }
  };

  const handleOpenChange = (o: boolean) => {
    if (!o) reset();
    onOpenChange(o);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>新增记忆</DialogTitle>
          <DialogDescription>手动创建一条用户记忆，立即入库并向量化。</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-2">
            <Label>类型</Label>
            <Select value={type} onValueChange={(v) => setType(v as MemoryType)}>
              <SelectTrigger>
                <SelectValue placeholder="请选择类型" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="PROFILE">档案</SelectItem>
                <SelectItem value="TASK">任务</SelectItem>
                <SelectItem value="FACT">事实</SelectItem>
                <SelectItem value="EPISODIC">情景</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>内容</Label>
            <Textarea
              placeholder="请输入记忆内容（最多5000字符）"
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows={6}
            />
          </div>

          <div className="space-y-2">
            <Label>重要性（0 - 1）: {importance.toFixed(2)}</Label>
            <Slider value={[importance]} min={0} max={1} step={0.01} onValueChange={([v]) => setImportance(v)} />
          </div>

          <div className="space-y-2">
            <Label>标签</Label>
            <div className="space-y-2">
              <div className="flex flex-wrap gap-1 mb-2">
                {tags.map((t) => (
                  <Badge key={t} className="flex items-center gap-1 px-3 py-1">
                    {t}
                    <X className="h-3 w-3 cursor-pointer" onClick={() => setTags(tags.filter((x) => x !== t))} />
                  </Badge>
                ))}
              </div>
              <div className="flex gap-2">
                <Input
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      const v = newTag.trim();
                      if (v && !tags.includes(v)) setTags([...tags, v]);
                      setNewTag("");
                    }
                  }}
                  placeholder="输入标签并按回车"
                  className="flex-1"
                />
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    const v = newTag.trim();
                    if (v && !tags.includes(v)) setTags([...tags, v]);
                    setNewTag("");
                  }}
                >
                  添加
                </Button>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={submitting}>
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={submitting || !type || !text.trim()}>
            {submitting ? "提交中..." : "提交"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
