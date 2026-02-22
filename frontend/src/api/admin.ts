import api from '@/lib/axios';
import type { ApiResponse, DashboardData, DailySales, CategorySales, KpiSummary, PageResponse, Order } from '@/types';

export const adminApi = {
  getDashboard: (from: string, to: string) =>
    api.get<ApiResponse<DashboardData>>('/admin/dashboard', { params: { from, to } }),

  getKpi: (from?: string, to?: string) =>
    api.get<ApiResponse<KpiSummary>>('/admin/dashboard/kpi', { params: { from, to } }),

  getDailySales: (from: string, to: string) =>
    api.get<ApiResponse<DailySales[]>>('/admin/dashboard/sales/daily', { params: { from, to } }),

  getCategorySales: (from: string, to: string) =>
    api.get<ApiResponse<CategorySales[]>>('/admin/dashboard/sales/category', { params: { from, to } }),

  exportEventLogs: (from: string, to: string) =>
    api.get('/events/export', { params: { from, to }, responseType: 'blob' }),

  getOnlineUsers: () =>
    api.get<ApiResponse<number>>('/events/realtime/online'),

  getAllOrders: (page = 0, size = 20, status?: string) =>
    api.get<ApiResponse<PageResponse<Order>>>('/orders/my', {
      params: { page, size, status: status || undefined },
    }),
};
