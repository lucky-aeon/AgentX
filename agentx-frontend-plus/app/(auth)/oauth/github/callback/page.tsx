'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { handleGithubCallbackApi } from '@/lib/api-services';
import { toast } from '@/hooks/use-toast';
import { Toaster } from '@/components/ui/toaster';
import { setCookie } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

export default function GitHubCallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const code = searchParams.get('code');
    
    if (!code) {
      setErrorMessage('未获取到授权码');
      setLoading(false);
      return;
    }

    async function handleCallback() {
      try {
        // 使用类型断言，因为已经在上面检查了 code 不为 null
        const response = await handleGithubCallbackApi(code as string);
        
        if (response.code !== 200 || !response.data?.token) {
          throw new Error(response.message || '登录失败');
        }
        
        // 储存token
        localStorage.setItem('auth_token', response.data.token);
        setCookie('token', response.data.token, 30);
        
        toast({
          title: "登录成功",
          description: "GitHub账号登录成功",
        });
        
        // 跳转到首页
        router.push('/');
      } catch (error: any) {
        console.error('GitHub登录错误:', error);
        setErrorMessage(error.message || 'GitHub登录失败，请稍后再试');
        
        // 3秒后跳转回登录页
        setTimeout(() => {
          router.push('/login');
        }, 3000);
      } finally {
        setLoading(false);
      }
    }

    handleCallback();
  }, [router, searchParams]);

  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-4">
      {loading ? (
        <>
          <Loader2 className="h-12 w-12 animate-spin text-primary mb-4" />
          <h2 className="text-xl font-semibold mb-2">正在处理GitHub登录...</h2>
          <p className="text-muted-foreground">请稍候，正在验证您的身份</p>
        </>
      ) : errorMessage ? (
        <>
          <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-destructive" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </div>
          <h2 className="text-xl font-semibold mb-2">登录失败</h2>
          <p className="text-muted-foreground mb-4">{errorMessage}</p>
          <p className="text-sm">即将返回登录页面...</p>
        </>
      ) : null}
      <Toaster />
    </div>
  );
} 