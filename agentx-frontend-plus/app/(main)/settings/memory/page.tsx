"use client"

import { useEffect, useMemo, useState } from "react";
import { Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Pagination, PaginationContent, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious, PaginationEllipsis } from "@/components/ui/pagination";

import { MemoryFilters } from "@/components/memory/memory-filters";
import { MemoryList } from "@/components/memory/memory-list";
import { CreateMemoryDialog } from "@/components/memory/create-memory-dialog";
import { DeleteMemoryDialog } from "@/components/memory/delete-memory-dialog";

import { createMemoryWithToast, deleteMemoryWithToast, getMemoriesWithToast } from "@/lib/memory-service";
import type { CreateMemoryRequest, MemoryItem, MemoryType, PageResponse } from "@/types/memory";

export default function MemorySettingsPage() {
  const [records, setRecords] = useState<MemoryItem[]>([]);
  const [pagination, setPagination] = useState<{ total: number; size: number; current: number; pages: number }>({ total: 0, size: 15, current: 1, pages: 0 });
  const [typeFilter, setTypeFilter] = useState<MemoryType | "ALL" | string>("ALL");
  const [loading, setLoading] = useState(false);

  const [createOpen, setCreateOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [selected, setSelected] = useState<MemoryItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchList = async () => {
    setLoading(true);
    try {
      const params = {
        page: pagination.current,
        pageSize: pagination.size,
        type: typeFilter !== "ALL" ? (typeFilter as string) : undefined,
      };
      const response = await getMemoriesWithToast(params);
      if (response.code === 200) {
        const data = response.data as PageResponse<MemoryItem>;
        setRecords(data.records || []);
        setPagination({ total: data.total, size: data.size, current: data.current, pages: data.pages });
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.size, typeFilter]);

  const handleCreate = async (req: CreateMemoryRequest) => {
    const res = await createMemoryWithToast(req);
    if (res.code === 200) {
      // 回到第一页以便看到最新记录
      setPagination((p) => ({ ...p, current: 1 }));
      await fetchList();
      return true;
    }
    return false;
  };

  const handleDelete = (item: MemoryItem) => {
    setSelected(item);
    setDeleteOpen(true);
  };

  const confirmDelete = async () => {
    if (!selected) return;
    try {
      setDeleting(true);
      const res = await deleteMemoryWithToast(selected.id);
      if (res.code === 200) {
        setDeleteOpen(false);
        setSelected(null);
        await fetchList();
      }
    } finally {
      setDeleting(false);
    }
  };

  // 生成分页页码（与现有分页风格一致）
  const pageNumbers = useMemo(() => {
    const pages: number[] = [];
    const { current, pages: totalPages } = pagination;
    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      const start = Math.max(1, current - 1);
      const end = Math.min(totalPages, current + 1);
      pages.push(1);
      if (start > 2) pages.push(-1); // ellipsis
      for (let i = start; i <= end; i++) pages.push(i);
      if (end < totalPages - 1) pages.push(-2); // ellipsis
      pages.push(totalPages);
    }
    return pages;
  }, [pagination]);

  const handlePageChange = (page: number) => {
    if (page < 1 || page > pagination.pages || page === pagination.current) return;
    setPagination((p) => ({ ...p, current: page }));
  };

  return (
    <div className="container py-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">记忆管理</h1>
          <p className="text-muted-foreground">查看、创建和删除您的用户记忆</p>
        </div>
        <div className="flex items-center gap-3">
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" /> 新增记忆
          </Button>
        </div>
      </div>

      <div className="mb-6">
        <MemoryFilters
          typeFilter={typeFilter}
          onTypeFilterChange={setTypeFilter}
          pageSize={pagination.size}
          onPageSizeChange={(size) => setPagination((p) => ({ ...p, size, current: 1 }))}
          loading={loading}
          onRefresh={fetchList}
        />
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>记忆列表</CardTitle>
          <CardDescription>按类型筛选与分页查看</CardDescription>
        </CardHeader>
        <CardContent>
          <MemoryList items={records} loading={loading} onDelete={handleDelete} />

          {pagination.pages > 1 && (
            <div className="mt-4">
              <Pagination>
                <PaginationContent>
                  <PaginationItem>
                    <PaginationPrevious
                      className={pagination.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                      onClick={() => handlePageChange(pagination.current - 1)}
                    />
                  </PaginationItem>

                  {pageNumbers.map((n, idx) => (
                    <PaginationItem key={idx}>
                      {n < 0 ? (
                        <PaginationEllipsis />
                      ) : (
                        <PaginationLink
                          isActive={n === pagination.current}
                          onClick={() => handlePageChange(n)}
                          className="cursor-pointer"
                        >
                          {n}
                        </PaginationLink>
                      )}
                    </PaginationItem>
                  ))}

                  <PaginationItem>
                    <PaginationNext
                      className={pagination.current >= pagination.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                      onClick={() => handlePageChange(pagination.current + 1)}
                    />
                  </PaginationItem>
                </PaginationContent>
              </Pagination>
            </div>
          )}
        </CardContent>
      </Card>

      <CreateMemoryDialog open={createOpen} onOpenChange={setCreateOpen} onCreate={handleCreate} />
      <DeleteMemoryDialog open={deleteOpen} onOpenChange={setDeleteOpen} item={selected} onConfirm={confirmDelete} loading={deleting} />
    </div>
  );
}

