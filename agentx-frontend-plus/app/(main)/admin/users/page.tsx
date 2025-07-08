"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Search } from "lucide-react";
import { getUsersWithToast, User, PageResponse } from "@/lib/admin-user-service";
import { useToast } from "@/hooks/use-toast";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [pageData, setPageData] = useState<PageResponse<User>>({
    records: [],
    total: 0,
    size: 15,
    current: 1,
    pages: 0
  });
  const { toast } = useToast();

  // 加载用户数据
  const loadUsers = async (page: number = 1, keyword?: string) => {
    setLoading(true);
    try {
      const response = await getUsersWithToast({
        page,
        pageSize: 15,
        keyword: keyword?.trim() || undefined
      });

      if (response.code === 200) {
        setPageData(response.data);
        setUsers(response.data.records || []);
      } else {
        toast({
          title: "获取用户列表失败",
          description: response.message,
          variant: "destructive",
        });
      }
    } catch (error) {
      toast({
        title: "获取用户列表失败",
        description: "网络错误，请稍后重试",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    loadUsers();
  }, []);

  // 搜索处理
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      loadUsers(1, searchQuery);
    }, 500); // 防抖500ms

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('zh-CN');
  };

  const formatPlatform = (platform?: string) => {
    const platformMap: { [key: string]: { name: string; color: string } } = {
      normal: { name: '普通登录', color: 'bg-gray-100 text-gray-800' },
      github: { name: 'GitHub', color: 'bg-gray-900 text-white' },
      community: { name: '敲鸭', color: 'bg-blue-100 text-blue-800' },
      google: { name: 'Google', color: 'bg-red-100 text-red-800' },
      wechat: { name: '微信', color: 'bg-green-100 text-green-800' }
    };
    
    const platformInfo = platformMap[platform || 'normal'] || platformMap.normal;
    return (
      <Badge className={platformInfo.color}>
        {platformInfo.name}
      </Badge>
    );
  };

  // 处理分页点击
  const handlePageChange = (page: number) => {
    if (page < 1 || page > pageData.pages) return;
    loadUsers(page, searchQuery);
  };

  // 生成分页页码数组
  const generatePageNumbers = () => {
    const current = pageData.current;
    const total = pageData.pages;
    const pages: (number | string)[] = [];

    if (total <= 7) {
      // 如果总页数少于等于7，显示所有页码
      for (let i = 1; i <= total; i++) {
        pages.push(i);
      }
    } else {
      // 总是显示第一页
      pages.push(1);

      if (current <= 4) {
        // 当前页在前面
        for (let i = 2; i <= 5; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      } else if (current >= total - 3) {
        // 当前页在后面
        pages.push('...');
        for (let i = total - 4; i <= total; i++) {
          pages.push(i);
        }
      } else {
        // 当前页在中间
        pages.push('...');
        for (let i = current - 1; i <= current + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(total);
      }
    }

    return pages;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-lg">加载中...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">用户管理</h1>
        <p className="text-gray-600 mt-1">查看系统中的所有用户账户信息</p>
      </div>

      {/* 搜索和过滤 */}
      <Card>
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
            <Input
              placeholder="搜索用户邮箱或用户名..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* 用户列表 */}
      <Card>
        <CardHeader>
          <CardTitle>用户列表 ({pageData.total})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>用户信息</TableHead>
                <TableHead>登录平台</TableHead>
                <TableHead>GitHub信息</TableHead>
                <TableHead>注册时间</TableHead>
                <TableHead>更新时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>
                    <div className="flex items-center space-x-3">
                      {user.avatarUrl && (
                        <img 
                          src={user.avatarUrl} 
                          alt={user.nickname} 
                          className="w-8 h-8 rounded-full"
                        />
                      )}
                      <div>
                        <div className="font-medium">{user.nickname}</div>
                        <div className="text-sm text-gray-500">{user.email}</div>
                        {user.phone && (
                          <div className="text-sm text-gray-400">{user.phone}</div>
                        )}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    {formatPlatform(user.loginPlatform)}
                  </TableCell>
                  <TableCell>
                    {user.githubLogin ? (
                      <div className="text-sm">
                        <div className="font-medium">{user.githubLogin}</div>
                        <div className="text-gray-500">ID: {user.githubId}</div>
                      </div>
                    ) : (
                      <div className="text-sm text-gray-400">未绑定</div>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{formatDate(user.createdAt)}</div>
                  </TableCell>
                  <TableCell>
                    <div className="text-sm">{formatDate(user.updatedAt)}</div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          
          {users.length === 0 && !loading && (
            <div className="text-center py-8 text-gray-500">
              {searchQuery ? "没有找到匹配的用户" : "暂无用户数据"}
            </div>
          )}
        </CardContent>
      </Card>

      {/* 分页组件 */}
      {pageData.pages > 1 && (
        <div className="flex justify-center">
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
    </div>
  );
}