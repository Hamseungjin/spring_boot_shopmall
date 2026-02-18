import api from '@/lib/axios';
import type { ApiResponse, PageResponse, Product, Category } from '@/types';

export interface ProductSearchParams {
  keyword?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}

export const productApi = {
  search: (params: ProductSearchParams) =>
    api.get<ApiResponse<PageResponse<Product>>>('/products/search', { params }),

  getById: (id: number) =>
    api.get<ApiResponse<Product>>(`/products/${id}`),

  getCategories: () =>
    api.get<ApiResponse<Category[]>>('/categories'),

  getCategoryTree: () =>
    api.get<ApiResponse<Category[]>>('/categories/tree'),
};
