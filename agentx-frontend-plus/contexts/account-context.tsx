"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { Account } from "@/types/account";
import { AccountService } from "@/lib/account-service";

interface AccountContextType {
  // 账户数据
  account: Account | null;
  loading: boolean;
  error: string | null;
  
  // 操作方法
  refreshAccount: () => Promise<void>;
  updateAccountData: (accountData: Account) => void;
  clearAccount: () => void;
  
  // 余额相关便捷方法
  isLowBalance: boolean;
  formatAmount: (amount: number) => string;
}

const AccountContext = createContext<AccountContextType | undefined>(undefined);

export function AccountProvider({ children }: { children: ReactNode }) {
  const [account, setAccount] = useState<Account | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 刷新账户数据
  const refreshAccount = useCallback(async () => {
    console.log('[AccountContext] 开始刷新账户数据');
    setLoading(true);
    setError(null);
    
    try {
      const response = await AccountService.getCurrentUserAccount();
      
      if (response.code === 200) {
        console.log('[AccountContext] 账户数据获取成功', response.data);
        setAccount(response.data);
      } else {
        console.error('[AccountContext] 获取账户数据失败', response.message);
        setError(response.message);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '网络错误，请稍后重试';
      console.error('[AccountContext] 获取账户数据异常', error);
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  // 直接更新账户数据（用于支付成功后的即时更新）
  const updateAccountData = useCallback((accountData: Account) => {
    console.log('[AccountContext] 直接更新账户数据', accountData);
    setAccount(accountData);
    setError(null);
  }, []);

  // 清除账户数据
  const clearAccount = useCallback(() => {
    console.log('[AccountContext] 清除账户数据');
    setAccount(null);
    setError(null);
    setLoading(false);
  }, []);

  // 判断余额是否不足
  const isLowBalance = account ? account.balance < 10 : false;

  // 格式化金额
  const formatAmount = useCallback((amount: number) => {
    return `¥${amount.toFixed(2)}`;
  }, []);

  // 组件挂载时自动获取账户数据
  useEffect(() => {
    refreshAccount();
  }, [refreshAccount]);

  // 监听页面焦点变化，自动刷新账户数据
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (!document.hidden && account) {
        console.log('[AccountContext] 页面重新可见，刷新账户数据');
        refreshAccount();
      }
    };

    const handleFocus = () => {
      if (account) {
        console.log('[AccountContext] 页面重新获得焦点，刷新账户数据');
        refreshAccount();
      }
    };

    // 添加事件监听器
    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('focus', handleFocus);

    // 清理函数
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('focus', handleFocus);
    };
  }, [account, refreshAccount]);

  const value: AccountContextType = {
    account,
    loading,
    error,
    refreshAccount,
    updateAccountData,
    clearAccount,
    isLowBalance,
    formatAmount,
  };

  return (
    <AccountContext.Provider value={value}>
      {children}
    </AccountContext.Provider>
  );
}

export function useAccount() {
  const context = useContext(AccountContext);
  if (context === undefined) {
    throw new Error("useAccount must be used within an AccountProvider");
  }
  return context;
}

// 便捷的 Hook：只获取余额信息
export function useBalance() {
  const { account, loading, formatAmount } = useAccount();
  
  return {
    balance: account?.balance || 0,
    credit: account?.credit || 0,
    availableBalance: account?.availableBalance || 0,
    loading,
    formatAmount,
  };
}

// 便捷的 Hook：只获取刷新方法
export function useAccountRefresh() {
  const { refreshAccount, loading } = useAccount();
  
  return {
    refreshAccount,
    loading,
  };
}