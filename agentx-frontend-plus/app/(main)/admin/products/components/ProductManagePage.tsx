"use client";

import { useState, useEffect } from "react";
import { Plus, Search, Filter, MoreHorizontal, Edit, Trash, Power, PowerOff } from "lucide-react";
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

import { Product, QueryProductRequest } from "@/types/product";
import { PageResponse, BillingType, BillingTypeNames, ProductStatus, ProductStatusNames } from "@/types/billing";
import { AdminProductService, AdminProductServiceWithToast } from "@/lib/admin-product-service";
import { BusinessService } from "@/lib/business-service";
import { ProductCreateDialog } from "./ProductCreateDialog";
import { ProductEditDialog } from "./ProductEditDialog";
import { ProductDetailDialog } from "./ProductDetailDialog";

export function ProductManagePage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageData, setPageData] = useState<PageResponse<Product>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  
  // 搜索和筛选状态
  const [searchQuery, setSearchQuery] = useState("");
  const [typeFilter, setTypeFilter] = useState<string>("all");
  const [statusFilter, setStatusFilter] = useState<string>("all");
  
  // 业务名称映射
  const [businessNameMap, setBusinessNameMap] = useState<Map<string, string>>(new Map());

  // 对话框状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  // 加载商品列表
  const loadProducts = async (page: number = 1, keyword?: string, type?: string, status?: string) => {
    setLoading(true);
    try {
      const params: QueryProductRequest = {
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined,
        type: type && type !== "all" ? type : undefined,
        status: status && status !== "all" ? parseInt(status) as ProductStatus : undefined
      };

      const response = await AdminProductService.getProducts(params);

      if (response.code === 200) {
        setPageData(response.data);
        setProducts(response.data.records || []);
      } else {
        toast({
          title: "获取商品列表失败",
          description: response.message,
          variant: "destructive"
        });
      }
    } catch (error) {
      toast({
        title: "获取商品列表失败",
        description: "网络错误，请稍后重试",
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  // 加载业务名称映射
  const loadBusinessNameMap = async () => {
    try {
      const nameMap = await BusinessService.getBusinessNameMappings();
      setBusinessNameMap(nameMap);
    } catch (error) {
      console.error('加载业务名称映射失败:', error);
    }
  };

  // 获取业务显示名称
  const getBusinessDisplayName = (product: Product): string => {
    return BusinessService.getBusinessDisplayName(
      product.type,
      product.serviceId,
      businessNameMap
    );
  };

  // 初始加载
  useEffect(() => {
    loadProducts();
    loadBusinessNameMap();
  }, []);

  // 搜索防抖
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadProducts(1, searchQuery, typeFilter, statusFilter);
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery, typeFilter, statusFilter]);

  // 分页处理
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadProducts(page, searchQuery, typeFilter, statusFilter);
  };

  // 创建商品成功回调
  const handleCreateSuccess = () => {
    setCreateDialogOpen(false);
    loadProducts(pageData.current, searchQuery, typeFilter, statusFilter);
  };

  // 编辑商品成功回调
  const handleEditSuccess = () => {
    setEditDialogOpen(false);
    setSelectedProduct(null);
    loadProducts(pageData.current, searchQuery, typeFilter, statusFilter);
  };

  // 删除商品
  const handleDelete = async (product: Product) => {
    if (!confirm(`确定要删除商品"${product.name}"吗？`)) return;

    const response = await AdminProductServiceWithToast.deleteProduct(product.id);
    if (response.code === 200) {
      loadProducts(pageData.current, searchQuery, typeFilter, statusFilter);
    }
  };

  // 切换商品状态
  const handleToggleStatus = async (product: Product) => {
    const isActive = product.status === ProductStatus.ACTIVE;
    const action = isActive ? "禁用" : "启用";
    
    if (!confirm(`确定要${action}商品"${product.name}"吗？`)) return;

    const response = isActive 
      ? await AdminProductServiceWithToast.disableProduct(product.id)
      : await AdminProductServiceWithToast.enableProduct(product.id);

    if (response.code === 200) {
      loadProducts(pageData.current, searchQuery, typeFilter, statusFilter);
    }
  };

  // 查看详情
  const handleViewDetail = (product: Product) => {
    setSelectedProduct(product);
    setDetailDialogOpen(true);
  };

  // 编辑商品
  const handleEdit = (product: Product) => {
    setSelectedProduct(product);
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
          <h1 className="text-3xl font-bold tracking-tight">商品管理</h1>
          <p className="text-muted-foreground">
            管理计费系统中的商品配置
          </p>
        </div>
        <Button onClick={() => setCreateDialogOpen(true)}>
          <Plus className="mr-2 h-4 w-4" />
          创建商品
        </Button>
      </div>

      {/* 搜索和筛选 */}
      <Card>
        <CardHeader>
          <CardTitle>搜索和筛选</CardTitle>
          <CardDescription>
            根据商品名称、类型和状态筛选商品
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="搜索商品名称..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
            <div className="flex gap-2">
              <Select value={typeFilter} onValueChange={setTypeFilter}>
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="商品类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部类型</SelectItem>
                  {Object.entries(BillingTypeNames).map(([key, name]) => (
                    <SelectItem key={key} value={key}>
                      {name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-[120px]">
                  <SelectValue placeholder="状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">全部状态</SelectItem>
                  <SelectItem value="1">激活</SelectItem>
                  <SelectItem value="0">禁用</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 商品列表 */}
      <Card>
        <CardHeader>
          <CardTitle>商品列表</CardTitle>
          <CardDescription>
            共 {pageData.total} 个商品
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-sm text-muted-foreground">加载中...</div>
            </div>
          ) : products.length === 0 ? (
            <div className="flex items-center justify-center py-8">
              <div className="text-sm text-muted-foreground">暂无商品</div>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>商品名称</TableHead>
                    <TableHead>商品类型</TableHead>
                    <TableHead>关联业务</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {products.filter(product => product && product.id).map((product) => (
                    <TableRow key={product.id}>
                      <TableCell className="font-medium">
                        <div className="cursor-pointer hover:text-blue-600" 
                             onClick={() => handleViewDetail(product)}>
                          {product?.name || "未知商品"}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">
                          {BillingTypeNames[product?.type as BillingType] || product?.type || "未知类型"}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center">
                          <span className="font-medium">{getBusinessDisplayName(product)}</span>
                          {product?.serviceId !== getBusinessDisplayName(product) && (
                            <span className="ml-2 text-xs text-muted-foreground">
                              (ID: {product?.serviceId})
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={product?.status === ProductStatus.ACTIVE ? "default" : "secondary"}>
                          {ProductStatusNames[product?.status as ProductStatus] || '未知'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {product?.createdAt ? new Date(product.createdAt).toLocaleString('zh-CN') : "未知时间"}
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" className="h-8 w-8 p-0">
                              <MoreHorizontal className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem onClick={() => handleViewDetail(product)}>
                              查看详情
                            </DropdownMenuItem>
                            <DropdownMenuItem onClick={() => handleEdit(product)}>
                              <Edit className="mr-2 h-4 w-4" />
                              编辑
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => handleToggleStatus(product)}
                              className={product?.status === ProductStatus.ACTIVE ? "text-orange-600" : "text-green-600"}
                            >
                              {product?.status === ProductStatus.ACTIVE ? (
                                <>
                                  <PowerOff className="mr-2 h-4 w-4" />
                                  禁用
                                </>
                              ) : (
                                <>
                                  <Power className="mr-2 h-4 w-4" />
                                  启用
                                </>
                              )}
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem 
                              onClick={() => handleDelete(product)}
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
      <ProductCreateDialog
        open={createDialogOpen}
        onOpenChange={setCreateDialogOpen}
        onSuccess={handleCreateSuccess}
      />

      {selectedProduct && (
        <>
          <ProductEditDialog
            open={editDialogOpen}
            onOpenChange={setEditDialogOpen}
            product={selectedProduct}
            onSuccess={handleEditSuccess}
          />
          
          <ProductDetailDialog
            open={detailDialogOpen}
            onOpenChange={setDetailDialogOpen}
            product={selectedProduct}
          />
        </>
      )}
    </div>
  );
}