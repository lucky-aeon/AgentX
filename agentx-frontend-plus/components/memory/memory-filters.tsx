"use client"

import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { MemoryType } from "@/types/memory";

interface MemoryFiltersProps {
  typeFilter: MemoryType | "ALL" | string;
  onTypeFilterChange: (type: MemoryType | "ALL" | string) => void;
  pageSize: number;
  onPageSizeChange: (size: number) => void;
  loading?: boolean;
  onRefresh?: () => void;
}

export function MemoryFilters({
  typeFilter,
  onTypeFilterChange,
  pageSize,
  onPageSizeChange,
  loading,
  onRefresh,
}: MemoryFiltersProps) {
  return (
    <div className="flex flex-wrap items-end gap-4">
      <div className="space-y-2">
        <Label>记忆类型</Label>
        <Select value={typeFilter} onValueChange={onTypeFilterChange}>
          <SelectTrigger className="w-[200px]">
            <SelectValue placeholder="全部类型" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部</SelectItem>
            <SelectItem value="PROFILE">档案</SelectItem>
            <SelectItem value="TASK">任务</SelectItem>
            <SelectItem value="FACT">事实</SelectItem>
            <SelectItem value="EPISODIC">情景</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label>每页数量</Label>
        <Select value={String(pageSize)} onValueChange={(v) => onPageSizeChange(Number(v))}>
          <SelectTrigger className="w-[120px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="10">10</SelectItem>
            <SelectItem value="15">15</SelectItem>
            <SelectItem value="20">20</SelectItem>
            <SelectItem value="50">50</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="flex-1" />

      <Button variant="outline" onClick={onRefresh} disabled={loading}>
        刷新
      </Button>
    </div>
  );
}
