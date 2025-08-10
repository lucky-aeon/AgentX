"use client";

import { useState, useEffect } from "react";
import { Plus, Search, MoreHorizontal, Edit, Trash, Code } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger,
  DropdownMenuSeparator
} from "@/components/ui/dropdown-menu";
import { 
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
  PaginationEllipsis
} from "@/components/ui/pagination";
import { toast } from "@/hooks/use-toast";

import { Rule, QueryRuleRequest } from "@/types/rule";
import { PageResponse, RuleHandlerKey, RuleHandlerKeyNames } from "@/types/billing";
import { AdminRuleService, AdminRuleServiceWithToast } from "@/lib/admin-rule-service";
import { RuleCreateDialog } from "./RuleCreateDialog";
import { RuleEditDialog } from "./RuleEditDialog";
import { RuleDetailDialog } from "./RuleDetailDialog";

export function RuleManagePage() {
  const [rules, setRules] = useState<Rule[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageData, setPageData] = useState<PageResponse<Rule>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  
  // 搜索和筛选状态
  const [searchQuery, setSearchQuery] = useState("");
  const [handlerKeyFilter, setHandlerKeyFilter] = useState<string>("all");

  // 对话框状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedRule, setSelectedRule] = useState<Rule | null>(null);

  // 加载规则列表
  const loadRules = async (page: number = 1, keyword?: string, handlerKey?: string) => {
    setLoading(true);
    try {
      const params: QueryRuleRequest = {
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined,
        handlerKey: handlerKey && handlerKey !== "all" ? handlerKey : undefined
      };

      const response = await AdminRuleService.getRules(params);

      if (response.code === 200) {
        setPageData(response.data);
        setRules(response.data.records || []);
      } else {
        toast({
          title: "获取规则列表失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "获取规则列表失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    loadRules();
  }, []);

  // 搜索防抖
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadRules(1, searchQuery, handlerKeyFilter);
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery, handlerKeyFilter]);

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadRules(page, searchQuery, handlerKeyFilter);
  };

  // 创建规则成功回调
  const handleCreateSuccess = () => {
    setCreateDialogOpen(false);
    loadRules(pageData.current, searchQuery, handlerKeyFilter);
  };

  // 编辑规则成功回调
  const handleEditSuccess = () => {
    setEditDialogOpen(false);
    setSelectedRule(null);
    loadRules(pageData.current, searchQuery, handlerKeyFilter);
  };

  // 删除规则
  const handleDelete = async (rule: Rule) => {
    if (!confirm(`确定要删除规则"${rule.name}"吗？删除后可能影响关联的商品配置。`)) return;

    const response = await AdminRuleServiceWithToast.deleteRule(rule.id);
    if (response.code === 200) {
      loadRules(pageData.current, searchQuery, handlerKeyFilter);
    }
  };

  // 查看详情
  const handleViewDetail = (rule: Rule) => {
    setSelectedRule(rule);
    setDetailDialogOpen(true);
  };

  // 编辑规则
  const handleEdit = (rule: Rule) => {
    setSelectedRule(rule);
    setEditDialogOpen(true);
  };

  // 生成页码
  const generatePageNumbers = () => {
    const pages = [];
    const totalPages = pageData.pages;
    const current = pageData.current;

    if (totalPages <= 7) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      } else if (current >= totalPages - 3) {
        pages.push(1);
        pages.push('...');
        for (let i = totalPages - 4; i <= totalPages; i++) {
          pages.push(i);
        }
      } else {
        pages.push(1);
        pages.push('...');
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages);
      }
    }

    return pages;
  };

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">规则管理</h1>
          <p className="text-muted-foreground">
            管理计费系统中的计费规则
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          创建规则
        </Button>
      </div>

      {/* 搜索和筛选 */}
      <Card>
        <CardHeader>
          <CardTitle>搜索和筛选</CardTitle>
          <CardDescription>
            根据规则名称和处理器类型筛选规则
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="搜索规则名称..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Select value={handlerKeyFilter} onValueChange={setHandlerKeyFilter}>
                <SelectTrigger className="w-[160px]">
                  <SelectValue placeholder="处理器类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部类型</SelectItem>
                  {Object.entries(RuleHandlerKeyNames).map(([key, name]) => (
                    <SelectItem key={key} value={key}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 规则列表 */}
      <Card>
        <CardHeader>
          <CardTitle>规则列表</CardTitle>
          <CardDescription>
            共 {pageData.total} 个规则
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-sm text-muted-foreground">加载中...</div>
            </div>
          ) : rules.length === 0 ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-sm text-muted-foreground">暂无规则</div>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>规则名称</TableHead>
                    <TableHead>处理器标识</TableHead>
                    <TableHead>规则描述</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rules.filter(rule => rule && rule.id).map((rule) => (
                    <TableRow key={rule.id}>
                      <TableCell className="font-medium">
                        <div className="cursor-pointer hover:text-blue-600" 
                             onClick={() => handleViewDetail(rule)}>
                          {rule?.name || "未知规则"}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="font-mono">
                          <Code className="mr-1 h-3 w-3" />
                          {RuleHandlerKeyNames[rule?.handlerKey as RuleHandlerKey] || rule?.handlerKey || "未知"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground max-w-xs">
                        <div className="truncate">
                          {rule?.description || "无描述"}
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {rule?.createdAt ? new Date(rule.createdAt).toLocaleString('zh-CN') : "未知时间"}
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => handleViewDetail(rule)}>
                              查看详情
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handleEdit(rule)}>
                              <Edit className="mr-2 h-4 w-4" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => handleDelete(rule)}
                              className="text-red-600"
                            >
                              <Trash className="mr-2 h-4 w-4" />
                              删除
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* 分页 */}
              {pageData.pages > 1 && (
                <div className="flex justify-center mt-4">
                  <Pagination>
                    <PaginationContent>
                      <PaginationItem>
                        <PaginationPrevious 
                          onClick={() => handlePageChange(pageData.current - 1)}
                          className={pageData.current <= 1 ? "pointer-events-none opacity-50" : "cursor-pointer"}
                        />
                      </PaginationItem>
                      
                      {generatePageNumbers().map((page, index) => (
                        <PaginationItem key={index}>
                          {page === '...' ? (
                            <PaginationEllipsis />
                          ) : (
                            <PaginationLink
                              onClick={() => handlePageChange(page as number)}
                              isActive={page === pageData.current}
                              className="cursor-pointer"
                            >
                              {page}
                            </PaginationLink>
                          )}
                        </PaginationItem>
                      ))}
                      
                      <PaginationItem>
                        <PaginationNext 
                          onClick={() => handlePageChange(pageData.current + 1)}
                          className={pageData.current >= pageData.pages ? "pointer-events-none opacity-50" : "cursor-pointer"}
                        />
                      </PaginationItem>
                    </PaginationContent>
                  </Pagination>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* 对话框 */}
      <RuleCreateDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onSuccess={handleCreateSuccess}
      />

      {selectedRule && (
        <>
          <RuleEditDialog
            open={editDialogOpen}
            onOpenChange={setEditDialogOpen}
            rule={selectedRule}
            onSuccess={handleEditSuccess}
          />
          
          <RuleDetailDialog
            open={detailDialogOpen}
            onOpenChange={setDetailDialogOpen}
            rule={selectedRule}
          />
        </>
      )}
    </div>
  );
}