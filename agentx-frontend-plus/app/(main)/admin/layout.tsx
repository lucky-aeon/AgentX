"use client";

import React from "react";
import { AdminSidebar } from "./components/AdminSidebar";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex w-full bg-gray-50" style={{ height: '100vh' }}>
      {/* 左侧菜单 */}
      <AdminSidebar />
      
      {/* 主内容区域 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 顶部标题栏 */}
        <header className="bg-white shadow-sm border-b border-gray-200 flex-shrink-0">
          <div className="px-6 py-4">
            <h1 className="text-xl font-semibold text-gray-900">管理后台</h1>
          </div>
        </header>
        
        {/* 内容区域 */}
        <main className="flex-1 overflow-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}