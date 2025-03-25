import { API_CONFIG } from "@/lib/api-config"
import { toast } from "@/components/ui/use-toast"

// 请求配置类型
export interface RequestConfig extends RequestInit {
  params?: Record<string, any>; // 查询参数
  timeout?: number; // 超时时间（毫秒）
}

// 拦截器类型
export interface Interceptor {
  request?: (config: RequestConfig) => RequestConfig;
  response?: (response: Response) => Promise<any>;
  error?: (error: any) => Promise<any>;
}

// 默认拦截器
const defaultInterceptor: Interceptor = {
  // 请求拦截器
  request: (config) => {
    // 默认添加Content-Type和Accept头
    config.headers = {
      "Content-Type": "application/json",
      Accept: "*/*",
      ...(config.headers || {}),
    };

    // 可以在这里添加认证令牌
    // const token = localStorage.getItem('token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }

    return config;
  },

  // 响应拦截器
  response: async (response) => {
    // 如果响应不成功，抛出错误
    if (!response.ok) {
      // 尝试解析错误响应
      const errorData = await response.json().catch(() => null);
      throw {
        status: response.status,
        message: errorData?.message || response.statusText,
        data: errorData,
      };
    }

    // 解析响应JSON
    return response.json();
  },

  // 错误拦截器
  error: async (error) => {
    // 处理超时错误
    if (error.name === "AbortError") {
      return {
        code: 408,
        message: "请求超时",
        data: null,
        timestamp: Date.now(),
      };
    }

    // 处理网络错误
    if (error instanceof TypeError && error.message.includes("fetch")) {
      return {
        code: 503,
        message: "网络连接失败",
        data: null,
        timestamp: Date.now(),
      };
    }

    // 返回标准化的错误响应
    return {
      code: error.status || 500,
      message: error.message || "未知错误",
      data: error.data || null,
      timestamp: Date.now(),
    };
  },
};

// HTTP客户端类
class HttpClient {
  private baseUrl: string;
  private interceptors: Interceptor[];
  private defaultTimeout: number;

  constructor(
    baseUrl: string = API_CONFIG.BASE_URL,
    interceptors: Interceptor[] = [defaultInterceptor],
    defaultTimeout: number = 30000
  ) {
    this.baseUrl = baseUrl;
    this.interceptors = interceptors;
    this.defaultTimeout = defaultTimeout;
  }

  // 添加拦截器
  addInterceptor(interceptor: Interceptor): void {
    this.interceptors.push(interceptor);
  }

  // 构建URL
  private buildUrl(endpoint: string, params?: Record<string, any>): string {
    let url = this.baseUrl + endpoint;

    if (params && Object.keys(params).length > 0) {
      const query = Object.entries(params)
        .filter(([_, value]) => value !== undefined && value !== null)
        .map(([key, value]) => {
          if (typeof value === "boolean") {
            return value ? key : null;
          }
          return `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;
        })
        .filter(Boolean)
        .join("&");

      if (query) {
        url += `?${query}`;
      }
    }

    return url;
  }

  // 执行请求
  private async request<T>(endpoint: string, config: RequestConfig = {}): Promise<T> {
    // 应用请求拦截器 - 使用reduce串联所有拦截器确保只应用一次
    const processedConfig = this.interceptors.reduce(
      (cfg, interceptor) => (interceptor.request ? interceptor.request(cfg) : cfg),
      { ...config }
    );

    // 构建完整URL
    const url = this.buildUrl(endpoint, processedConfig.params);

    try {
      // 处理超时
      let timeoutId: NodeJS.Timeout | null = null;
      let abortController: AbortController | null = null;
      
      if (processedConfig.timeout || this.defaultTimeout) {
        abortController = new AbortController();
        processedConfig.signal = abortController.signal;
        
        timeoutId = setTimeout(() => {
          if (abortController) {
            abortController.abort();
          }
        }, processedConfig.timeout || this.defaultTimeout);
      }

      // 发送请求
      console.log(`[HttpClient] ${processedConfig.method || 'GET'} ${url}`);
      const response = await fetch(url, processedConfig);
      
      // 清除超时定时器
      if (timeoutId) {
        clearTimeout(timeoutId);
      }

      // 应用响应拦截器 - 只应用第一个有效的响应拦截器
      for (const interceptor of this.interceptors) {
        if (interceptor.response) {
          return await interceptor.response(response);
        }
      }

      // 默认处理
      return await response.json();
    } catch (error) {
      // 应用错误拦截器 - 只应用第一个有效的错误拦截器
      for (const interceptor of this.interceptors) {
        if (interceptor.error) {
          return await interceptor.error(error) as T;
        }
      }

      // 默认错误处理
      console.error(`[HttpClient] Error in ${endpoint}:`, error);
      throw error;
    }
  }

  // HTTP方法
  async get<T>(endpoint: string, config: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: "GET" });
  }

  async post<T>(endpoint: string, data?: any, config: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: any, config: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string, config: RequestConfig = {}): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: "DELETE" });
  }
}

// 导出单例实例
export const httpClient = new HttpClient();

// 添加toast提示拦截器
export const addToastInterceptor = (
  options: {
    showSuccessToast?: boolean;
    showErrorToast?: boolean;
    successTitle?: string;
    errorTitle?: string;
  } = {}
) => {
  const {
    showSuccessToast = false,
    showErrorToast = true,
    successTitle = "操作成功",
    errorTitle = "操作失败"
  } = options;

  httpClient.addInterceptor({
    response: async (response) => {
      const result = await response.json();
      
      // 假设API返回格式为 { code: number, message: string, data: any }
      if (result.code === 200 && showSuccessToast) {
        toast({
          title: successTitle,
          description: result.message || "操作已完成",
        });
      }
      
      return result;
    },
    error: async (error) => {
      const result = await defaultInterceptor.error!(error);
      
      if (showErrorToast) {
        toast({
          title: errorTitle,
          description: result.message,
          variant: "destructive",
        });
      }
      
      return result;
    }
  });
};

// 可选：添加认证拦截器
export const addAuthInterceptor = () => {
  httpClient.addInterceptor({
    request: (config) => {
      const token = localStorage.getItem("auth_token");
      if (token) {
        config.headers = {
          ...config.headers,
          Authorization: `Bearer ${token}`
        };
      }
      return config;
    },
    response: async (response) => {
      // 处理401认证错误
      if (response.status === 401) {
        // 可以在这里处理登录过期逻辑
        // 例如：重定向到登录页面
        window.location.href = "/login";
      }
      
      // 继续处理响应
      const result = await response.json();
      return result;
    }
  });
};

// 初始化：添加常用拦截器
addToastInterceptor(); 