import api from '@/lib/axios';
import type {
  ApiResponse,
  PageResponse,
  Product,
  Category,
  ProductCreatePayload,
  ProductUpdatePayload,
} from '@/types';

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
  getAll: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Product>>>('/products', { params: { page, size } }),

  search: (params: ProductSearchParams) =>
    api.get<ApiResponse<PageResponse<Product>>>('/products/search', { params }),

  getById: (id: number) =>
    api.get<ApiResponse<Product>>(`/products/${id}`),

  create: (payload: ProductCreatePayload) =>
    api.post<ApiResponse<Product>>('/products', payload),

  update: (id: number, payload: ProductUpdatePayload) =>
    api.put<ApiResponse<Product>>(`/products/${id}`, payload),

  remove: (id: number) =>
    api.delete<ApiResponse<void>>(`/products/${id}`),

  uploadImage: (id: number, file: File) => {
    const formData = new FormData();
    formData.append('image', file);
    return api.post<ApiResponse<Product>>(`/products/${id}/image`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  updateStock: (id: number, stockQuantity: number) =>
    api.patch<ApiResponse<Product>>(`/products/${id}/stock`, { stockQuantity }),

  getCategories: () =>
    api.get<ApiResponse<Category[]>>('/categories'),

  getCategoryTree: () =>
    api.get<ApiResponse<Category[]>>('/categories/tree'),

  createCategory: (payload: { name: string; description?: string; parentId?: number; sortOrder?: number }) =>
    api.post<ApiResponse<Category>>('/categories', payload),
};
