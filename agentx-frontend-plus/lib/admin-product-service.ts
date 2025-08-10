// 管理员商品管理API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { 
  Product, 
  CreateProductRequest, 
  UpdateProductRequest, 
  QueryProductRequest 
} from '@/types/product';
import { PageResponse } from '@/types/billing';

// API端点
const API_ENDPOINTS = {
  PRODUCTS: '/admin/products',
  PRODUCT_BY_ID: (id: string) => `/admin/products/${id}`,
  PRODUCT_BY_BUSINESS: '/admin/products/business',
  PRODUCT_ENABLE: (id: string) => `/admin/products/${id}/enable`,
  PRODUCT_DISABLE: (id: string) => `/admin/products/${id}/disable`,
  PRODUCT_EXISTS: (id: string) => `/admin/products/${id}/exists`,
  BUSINESS_EXISTS: '/admin/products/business/exists'
} as const;

export class AdminProductService {
  // 分页获取商品列表
  static async getProducts(params?: QueryProductRequest): Promise<ApiResponse<PageResponse<Product>>> {
    try {
      return await httpClient.get(API_ENDPOINTS.PRODUCTS, { params });
    } catch (error) {
      return {
        code: 500,
        message: '获取商品列表失败',
        data: { records: [], total: 0, size: 15, current: 1, pages: 0 },
        timestamp: Date.now()
      };
    }
  }

  // 获取所有商品列表
  static async getAllProducts(): Promise<ApiResponse<Product[]>> {
    try {
      return await httpClient.get(`${API_ENDPOINTS.PRODUCTS}/all`);
    } catch (error) {
      return {
        code: 500,
        message: '获取所有商品失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }

  // 根据ID获取商品详情
  static async getProductById(id: string): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.get(API_ENDPOINTS.PRODUCT_BY_ID(id));
    } catch (error) {
      return {
        code: 500,
        message: '获取商品详情失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 根据业务标识获取商品
  static async getProductByBusinessKey(type: string, serviceId: string): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.get(API_ENDPOINTS.PRODUCT_BY_BUSINESS, {
        params: { type, serviceId }
      });
    } catch (error) {
      return {
        code: 500,
        message: '获取商品失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 创建商品
  static async createProduct(data: CreateProductRequest): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.post(API_ENDPOINTS.PRODUCTS, data);
    } catch (error) {
      return {
        code: 500,
        message: '创建商品失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 更新商品
  static async updateProduct(id: string, data: UpdateProductRequest): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.put(API_ENDPOINTS.PRODUCT_BY_ID(id), data);
    } catch (error) {
      return {
        code: 500,
        message: '更新商品失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 删除商品
  static async deleteProduct(id: string): Promise<ApiResponse<void>> {
    try {
      return await httpClient.delete(API_ENDPOINTS.PRODUCT_BY_ID(id));
    } catch (error) {
      return {
        code: 500,
        message: '删除商品失败',
        data: undefined,
        timestamp: Date.now()
      };
    }
  }

  // 启用商品
  static async enableProduct(id: string): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.post(API_ENDPOINTS.PRODUCT_ENABLE(id));
    } catch (error) {
      return {
        code: 500,
        message: '启用商品失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 禁用商品
  static async disableProduct(id: string): Promise<ApiResponse<Product>> {
    try {
      return await httpClient.post(API_ENDPOINTS.PRODUCT_DISABLE(id));
    } catch (error) {
      return {
        code: 500,
        message: '禁用商品失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 检查商品是否存在
  static async existsProduct(id: string): Promise<ApiResponse<boolean>> {
    try {
      return await httpClient.get(API_ENDPOINTS.PRODUCT_EXISTS(id));
    } catch (error) {
      return {
        code: 500,
        message: '检查商品存在性失败',
        data: false,
        timestamp: Date.now()
      };
    }
  }

  // 检查业务标识是否存在
  static async existsByBusinessKey(type: string, serviceId: string): Promise<ApiResponse<boolean>> {
    try {
      return await httpClient.get(API_ENDPOINTS.BUSINESS_EXISTS, {
        params: { type, serviceId }
      });
    } catch (error) {
      return {
        code: 500,
        message: '检查业务标识存在性失败',
        data: false,
        timestamp: Date.now()
      };
    }
  }
}

// 带Toast提示的API服务方法
export const AdminProductServiceWithToast = {
  async getProducts(params?: QueryProductRequest) {
    return AdminProductService.getProducts(params);
  },

  async createProduct(data: CreateProductRequest) {
    return httpClient.post(API_ENDPOINTS.PRODUCTS, data, {}, { showToast: true });
  },

  async updateProduct(id: string, data: UpdateProductRequest) {
    return httpClient.put(API_ENDPOINTS.PRODUCT_BY_ID(id), data, {}, { showToast: true });
  },

  async deleteProduct(id: string) {
    return httpClient.delete(API_ENDPOINTS.PRODUCT_BY_ID(id), {}, { showToast: true });
  },

  async enableProduct(id: string) {
    return httpClient.post(API_ENDPOINTS.PRODUCT_ENABLE(id), {}, {}, { showToast: true });
  },

  async disableProduct(id: string) {
    return httpClient.post(API_ENDPOINTS.PRODUCT_DISABLE(id), {}, {}, { showToast: true });
  }
};

export default AdminProductService;