import api from '@/lib/axios';
import type { ApiResponse, PageResponse, Order, PaymentResponse } from '@/types';

export interface CreateOrderPayload {
  shippingAddress: string;
  receiverName: string;
  receiverPhone: string;
  items: { productId: number; quantity: number }[];
}

export const orderApi = {
  create: (payload: CreateOrderPayload) =>
    api.post<ApiResponse<Order>>('/orders', payload),

  getMyOrders: (page = 0, size = 10) =>
    api.get<ApiResponse<PageResponse<Order>>>('/orders/my', { params: { page, size } }),

  getById: (id: number) =>
    api.get<ApiResponse<Order>>(`/orders/${id}`),

  cancel: (id: number, reason?: string) =>
    api.post<ApiResponse<Order>>(`/orders/${id}/cancel`, { reason }),

  pay: (orderId: number, idempotencyKey: string, paymentMethod?: string) =>
    api.post<ApiResponse<PaymentResponse>>('/payments', { orderId, idempotencyKey, paymentMethod }),
};
