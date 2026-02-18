import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Member, TokenResponse } from '@/types';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  member: Member | null;
  isAuthenticated: boolean;

  setTokens: (tokens: TokenResponse) => void;
  setMember: (member: Member) => void;
  login: (tokens: TokenResponse, member: Member) => void;
  logout: () => void;
  isAdmin: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      member: null,
      isAuthenticated: false,

      setTokens: (tokens) =>
        set({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          isAuthenticated: true,
        }),

      setMember: (member) => set({ member }),

      login: (tokens, member) =>
        set({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          member,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          member: null,
          isAuthenticated: false,
        }),

      isAdmin: () => get().member?.role === 'ADMIN',
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        member: state.member,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
