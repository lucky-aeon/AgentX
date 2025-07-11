import { API_CONFIG } from "@/lib/api-config"
import { toast } from "@/hooks/use-toast"

import { log } from "console";

// 请求配置类型
export interface RequestConfig extends RequestInit {
  params?: Record<string, any>; // 查询参数
  timeout?: number; // 超时时间（毫秒）
}

// 请求选项类型
export interface RequestOptions {
  raw?: boolean; // 是否返回原始响应，不自动解析json
  showToast?: boolean; // 是否显示toast提示
}

// 拦截器类型
export interface Interceptor {
  request?: (config: RequestConfig) => RequestConfig;
  response?: (response: Response, options?: RequestOptions) => Promise<any>;
  error?: (error: any) => Promise<any>;
}

// 默认拦截器
const defaultInterceptor: Interceptor = {
  // 请求拦截器
  request: (config) => {
    // 检查是否是FormData，如果是则不设置Content-Type
    const isFormData = config.body instanceof FormData;
    
    // 默认添加Content-Type和Accept头
    config.headers = {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      Accept: "*/*",
      ...(config.headers || {}),
    };

    return config;
  },

  // 响应拦截器
  response: async (response, options) => {
    console.log('ggg')
    if (response.status === 401) {
      // 清除本地存储的 token
      if (typeof window !== "undefined") {
        localStorage.removeItem("auth_token");
        document.cookie = "token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        window.location.href = "/login";
      }
      const error: any = new Error("未登录或登录已过期");
      error.status = 401;
      throw error;
    }

    // 如果是原始响应模式，直接返回response不消费流
    if (options?.raw) {
      return response;
    }

    // 非成功响应
    if (!response.ok) {
      const errorData = await response.json();
      // 不显示toast，只返回错误数据
      return errorData;
    }

    // 正常响应，解析JSON并返回
    return await response.json();
  },

  // 错误拦截器
  error: async (error) => {
    console.log('ggg')
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

    // 处理 401
    if (error.status === 401) {
      if (typeof window !== "undefined") {
        localStorage.removeItem("auth_token");
        document.cookie = "token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        window.location.href = "/login";
      }
      return {
        code: 401,
        message: "未登录或登录已过期",
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
  private async request<T>(endpoint: string, config: RequestConfig = {}, options?: RequestOptions): Promise<T> {
    // 应用请求拦截器 - 使用reduce串联所有拦截器确保只应用一次
    const processedConfig = this.interceptors.reduce(
      (cfg, interceptor) => (interceptor.request ? interceptor.request(cfg) : cfg),
      { ...config }
    );

    // 构建完整URL
    const url = this.buildUrl(endpoint, processedConfig.params);
    console.log("HTTP Request URL:", url, "Params:", processedConfig.params);

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

      const response = await fetch(url, processedConfig);
      
      // 清除超时定时器
      if (timeoutId) {
        clearTimeout(timeoutId);
      }

      // 处理 401 状态码
      if (response.status === 401) {
        // 清除认证信息
        if (typeof window !== "undefined") {
          localStorage.removeItem("auth_token");
          document.cookie = "token=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
          // 如果当前不在登录页面，则重定向到登录页面
          if (!window.location.pathname.includes('/login')) {
            window.location.href = '/login';
            // 抛出错误以中断后续处理
            throw new Error("未登录或登录已过期");
          }
        }
      }

      // 处理响应
      let result: any;
      
      // 如果是原始响应模式，直接返回
      if (options?.raw) {
        return response as unknown as T;
      }

      // 解析响应
      try {
        result = await response.json();
      } catch (e) {
        result = {
          code: response.status,
          message: response.statusText || `请求失败 (${response.status})`,
          data: null
        };
      }

      // 显示toast提示
      if (options?.showToast) {
        toast({
          variant: result.code === 200 ? "default" : "destructive",
          title: result.code === 200 ? "成功" : "错误",
          description: result.message
        });
      }
      
      return result;
      
    } catch (error: any) {
      // 处理异常
      const errorResult = {
        code: error.status || 500,
        message: error.message || "未知错误",
        data: null,
        timestamp: Date.now(),
      };
      
      // 显示toast提示
      if (options?.showToast) {
        toast({
          variant: "destructive",
          title: "错误",
          description: errorResult.message
        });
      }
      
      return errorResult as unknown as T;
    }
  }

  // HTTP方法
  async get<T>(endpoint: string, config: RequestConfig = {}, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: "GET" }, options);
  }

  async post<T>(endpoint: string, data?: any, config: RequestConfig = {}, options?: RequestOptions): Promise<T> {
    // 处理 FormData 类型的数据
    const isFormData = data instanceof FormData;
    
    return this.request<T>(endpoint, {
      ...config,
      method: "POST",
      body: data ? (isFormData ? data : JSON.stringify(data)) : undefined,
      headers: isFormData ? {
        // 对于 FormData，删除 Content-Type 让浏览器自动设置
        ...config.headers,
        // 删除可能存在的 Content-Type
      } : {
        "Content-Type": "application/json",
        ...config.headers,
      },
    }, options);
  }

  async put<T>(endpoint: string, data?: any, config: RequestConfig = {}, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    }, options);
  }

  async delete<T>(endpoint: string, config: RequestConfig = {}, options?: RequestOptions): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: "DELETE" }, options);
  }
}

// 导出单例实例
export const httpClient = new HttpClient();

// 添加认证拦截器
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
    }
  });
};

// 初始化：添加认证拦截器
addAuthInterceptor();

// API响应类型接口
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp?: number;
} 