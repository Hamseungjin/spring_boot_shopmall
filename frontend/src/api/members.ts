import api from '@/lib/axios';
import type { ApiResponse, Member } from '@/types';

export const memberApi = {
  getMe: () => api.get<ApiResponse<Member>>('/members/me'),
};
