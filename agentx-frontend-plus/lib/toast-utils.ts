import { toast } from "@/hooks/use-toast"

interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * 处理API响应并根据响应结果显示toast消息
 * @param response API响应对象
 * @param options 配置选项
 * @returns 处理后的响应数据
 */
export function handleApiResponse<T>(
  response: ApiResponse<T>,
  options: {
    showSuccessToast?: boolean; // 是否显示成功toast
    showErrorToast?: boolean; // 是否显示错误toast
    successTitle?: string; // 成功toast标题
    errorTitle?: string; // 错误toast标题
  } = {}
): ApiResponse<T> {
  const {
    showSuccessToast = true,
    showErrorToast = true,
    successTitle = "操作成功",
    errorTitle = "操作失败"
  } = options;

  // 判断响应是否成功 (通常是 code === 200)
  const isSuccess = response.code === 200;

  if (isSuccess && showSuccessToast) {
    toast({
      description: response.message,
      variant: "default",
    });
  } else if (!isSuccess && showErrorToast) {
    toast({
      description: response.message,
      variant: "destructive",
    });
  }

  return response;
}

/**
 * 包装API请求函数并自动处理toast展示
 * @param apiFn 原始API请求函数
 * @param options toast配置选项
 * @returns 包装后的API函数
 */
export function withToast<T extends (...args: any[]) => Promise<ApiResponse<any>>>(
  apiFn: T,
  options: {
    showSuccessToast?: boolean;
    showErrorToast?: boolean;
    successTitle?: string;
    errorTitle?: string;
  } = {}
): (...args: Parameters<T>) => Promise<ReturnType<T>> {
  return async (...args: Parameters<T>): Promise<ReturnType<T>> => {
    try {
      const response = await apiFn(...args);
      return handleApiResponse(response, options) as ReturnType<T>;
    } catch (error) {
      // 处理异常情况
      if (options.showErrorToast) {
        toast({
          description: error instanceof Error ? error.message : "未知错误",
          variant: "destructive",
        });
      }
      throw error;
    }
  };
} 