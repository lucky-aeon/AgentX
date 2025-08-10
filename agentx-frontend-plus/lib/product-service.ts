// 用户端商品API服务

import { httpClient, ApiResponse } from '@/lib/http-client';
import { withToast } from '@/lib/toast-utils';
import { Product } from '@/types/product';

// API端点
const API_ENDPOINTS = {
  PRODUCTS: '/products',
  PRODUCT_BY_ID: (id: string) => `/products/${id}`,
  PRODUCT_BY_BUSINESS: '/products/business',
  ACTIVE_PRODUCTS: '/products/active',
  BUSINESS_ACTIVE: '/products/business/active'
} as const;

export class ProductService {
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
        message: '获取商品信息失败',
        data: {} as Product,
        timestamp: Date.now()
      };
    }
  }

  // 获取指定类型的活跃商品列表
  static async getActiveProducts(type?: string): Promise<ApiResponse<Product[]>> {
    try {
      return await httpClient.get(API_ENDPOINTS.ACTIVE_PRODUCTS, {
        params: type ? { type } : undefined
      });
    } catch (error) {
      return {
        code: 500,
        message: '获取商品列表失败',
        data: [],
        timestamp: Date.now()
      };
    }
  }

  // 检查商品是否存在且激活
  static async isProductActive(type: string, serviceId: string): Promise<ApiResponse<boolean>> {
    try {
      return await httpClient.get(API_ENDPOINTS.BUSINESS_ACTIVE, {
        params: { type, serviceId }
      });
    } catch (error) {
      return {
        code: 500,
        message: '检查商品状态失败',
        data: false,
        timestamp: Date.now()
      };
    }
  }

  // 获取所有活跃商品并按类型分组
  static async getActiveProductsByType(): Promise<ApiResponse<Record<string, Product[]>>> {
    try {
      const response = await this.getActiveProducts();
      if (response.code === 200) {
        const groupedProducts = response.data.reduce((acc, product) => {
          if (!acc[product.type]) {
            acc[product.type] = [];
          }
          acc[product.type].push(product);
          return acc;
        }, {} as Record<string, Product[]>);

        return {
          code: 200,
          message: '获取商品分类成功',
          data: groupedProducts,
          timestamp: Date.now()
        };
      }
      return response as ApiResponse<Record<string, Product[]>>;
    } catch (error) {
      return {
        code: 500,
        message: '获取商品分类失败',
        data: {},
        timestamp: Date.now()
      };
    }
  }
}

// 带Toast提示的API服务方法
export const ProductServiceWithToast = {
  async getProductById(id: string) {
    return withToast(ProductService.getProductById.bind(ProductService), {
      showErrorToast: true,
      errorTitle: '获取商品详情失败'
    })(id);
  },

  async getActiveProducts(type?: string) {
    return withToast(ProductService.getActiveProducts.bind(ProductService), {
      showErrorToast: true,
      errorTitle: '获取商品列表失败'
    })(type);
  },

  async getActiveProductsByType() {
    return withToast(ProductService.getActiveProductsByType.bind(ProductService), {
      showErrorToast: true,
      errorTitle: '获取商品分类失败'
    })();
  }
};

export default ProductService;