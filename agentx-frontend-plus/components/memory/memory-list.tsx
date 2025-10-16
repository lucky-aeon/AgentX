"use client"

import { Button } from "@/components/ui/button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { MoreHorizontal, Trash2 } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { MemoryItem } from "@/types/memory";

interface MemoryListProps {
  items: MemoryItem[];
  loading?: boolean;
  onDelete: (item: MemoryItem) => void;
}

export function MemoryList({ items, loading, onDelete }: MemoryListProps) {
  const formatDate = (date?: string) => (date ? new Date(date).toLocaleString("zh-CN") : "-");
  const formatText = (text: string) => (text.length > 120 ? text.slice(0, 120) + "…" : text);
  const typeLabel = (t: string) => {
    switch (t) {
      case "PROFILE":
        return "档案";
      case "TASK":
        return "任务";
      case "FACT":
        return "事实";
      case "EPISODIC":
        return "情景";
      default:
        return t;
    }
  };

  if (loading) {
    return (
      <div className="space-y-3">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="flex items-center space-x-4">
            <Skeleton className="h-12 w-12 rounded" />
            <div className="space-y-2 flex-1">
              <Skeleton className="h-4 w-[70%]" />
              <Skeleton className="h-4 w-[40%]" />
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="border rounded-md">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>类型</TableHead>
            <TableHead>重要性</TableHead>
            <TableHead>内容</TableHead>
            <TableHead>创建时间</TableHead>
            <TableHead className="text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.id}>
              <TableCell className="font-medium">{typeLabel(item.type)}</TableCell>
              <TableCell>{item.importance !== undefined ? item.importance.toFixed(2) : "-"}</TableCell>
              <TableCell className="max-w-[520px] whitespace-pre-wrap">{formatText(item.text)}</TableCell>
              <TableCell>{formatDate(item.createdAt)}</TableCell>
              <TableCell className="text-right">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem className="text-red-600" onClick={() => onDelete(item)}>
                      <Trash2 className="mr-2 h-4 w-4" /> 删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
