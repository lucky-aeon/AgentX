"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { 
  Users, 
  Wrench, 
  Bot, 
  Server,
  Settings,
  Home,
  Shield,
  Container,
  Database
} from "lucide-react";

interface MenuItemProps {
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  isActive: boolean;
}

function MenuItem({ href, icon: Icon, label, isActive }: MenuItemProps) {
  return (
    <Link
      href={href}
      className={cn(
        "flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors",
        isActive
          ? "bg-blue-50 text-blue-700 border-r-2 border-blue-700"
          : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"
      )}
    >
      <Icon className={cn("mr-3 h-5 w-5", isActive ? "text-blue-700" : "text-gray-400")} />
      {label}
    </Link>
  );
}

export function AdminSidebar() {
  const pathname = usePathname();

  const menuItems = [
    {
      href: "/admin",
      icon: Home,
      label: "管理首页",
    },
    {
      href: "/admin/users",
      icon: Users,
      label: "用户列表",
    },
    {
      href: "/admin/tools",
      icon: Wrench,
      label: "工具列表",
    },
    {
      href: "/admin/agents",
      icon: Bot,
      label: "Agent列表",
    },
    {
      href: "/admin/rags",
      icon: Database,
      label: "RAG管理",
    },
    {
      href: "/admin/providers",
      icon: Server,
      label: "服务商管理",
    },
    {
      href: "/admin/containers",
      icon: Container,
      label: "容器管理",
    },
    {
      href: "/admin/auth-settings",
      icon: Shield,
      label: "认证配置",
    },
  ];

  return (
    <div className="w-64 bg-white shadow-sm border-r border-gray-200 flex flex-col" style={{ height: '100vh' }}>
      {/* Logo区域 */}
      <div className="p-6 border-b border-gray-200 flex-shrink-0">
        <div className="flex items-center">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <Settings className="w-5 h-5 text-white" />
          </div>
          <span className="ml-3 text-lg font-semibold text-gray-900">AgentX Admin</span>
        </div>
      </div>

      {/* 菜单列表 */}
      <nav className="p-4 space-y-1 flex-1 overflow-y-auto">
        {menuItems.map((item) => (
          <MenuItem
            key={item.href}
            href={item.href}
            icon={item.icon}
            label={item.label}
            isActive={pathname === item.href || (item.href === "/admin" && pathname === "/admin")}
          />
        ))}
      </nav>

      {/* 底部信息 */}
      <div className="p-4 border-t border-gray-200 flex-shrink-0">
        <div className="text-xs text-gray-500 text-center">
          AgentX 管理后台
          <br />
          v1.0.0
        </div>
      </div>
    </div>
  );
}