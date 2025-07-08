"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function AdminPage() {
  const router = useRouter();

  useEffect(() => {
    // 默认重定向到用户列表页面
    router.push("/admin/users");
  }, [router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <h1 className="text-2xl font-bold mb-4">管理后台</h1>
        <p className="text-gray-600">正在跳转到用户列表...</p>
      </div>
    </div>
  );
}