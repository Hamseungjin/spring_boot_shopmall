import api from '@/lib/axios';
import type { ApiResponse } from '@/types';

export interface TrackEventPayload {
  eventType: string;
  page?: string;
  referrer?: string;
  productId?: number;
  metadata?: Record<string, string>;
}

export const eventApi = {
  track: (payload: TrackEventPayload) =>
    api.post<ApiResponse<void>>('/events', payload),

  trackBatch: (events: TrackEventPayload[]) =>
    api.post<ApiResponse<void>>('/events/batch', events),

  getOnlineUsers: () =>
    api.get<ApiResponse<number>>('/events/realtime/online'),

  getProductViews: (productId: number) =>
    api.get<ApiResponse<number>>(`/events/realtime/product/${productId}/views`),

  exportLogs: (from: string, to: string) =>
    api.get('/events/export', { params: { from, to }, responseType: 'blob' }),
};
