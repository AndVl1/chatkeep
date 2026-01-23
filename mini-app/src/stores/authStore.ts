import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { TelegramUser } from '@/types';

const TOKEN_STORAGE_KEY = 'chatkeep_auth_token';
const USER_STORAGE_KEY = 'chatkeep_auth_user';

interface AuthState {
  // State
  token: string | null;
  user: TelegramUser | null;
  isAuthenticated: boolean;

  // Actions
  login: (token: string, user: TelegramUser) => void;
  logout: () => void;
  initialize: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      // Initial state
      token: null,
      user: null,
      isAuthenticated: false,

      // Actions
      login: (token, user) => {
        set({ token, user, isAuthenticated: true });
      },

      logout: () => {
        set({ token: null, user: null, isAuthenticated: false });
      },

      initialize: () => {
        // Load from localStorage on mount
        try {
          const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
          const storedUser = localStorage.getItem(USER_STORAGE_KEY);

          if (storedToken && storedUser) {
            const user = JSON.parse(storedUser) as TelegramUser;
            set({ token: storedToken, user, isAuthenticated: true });
          } else {
            // If either is missing, logout
            set({ token: null, user: null, isAuthenticated: false });
          }
        } catch (error) {
          if (import.meta.env.DEV) {
            console.error('[AuthStore] Failed to initialize from localStorage:', error);
          }
          set({ token: null, user: null, isAuthenticated: false });
        }
      },
    }),
    {
      name: 'chatkeep-auth-storage',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);

// Listen for storage events to sync logout across tabs and from API client
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    // Check if auth tokens were removed
    if (
      event.key === TOKEN_STORAGE_KEY ||
      event.key === USER_STORAGE_KEY ||
      event.key === null // localStorage.clear()
    ) {
      const token = localStorage.getItem(TOKEN_STORAGE_KEY);
      const user = localStorage.getItem(USER_STORAGE_KEY);

      // If either is missing, logout
      if (!token || !user) {
        useAuthStore.getState().logout();
      }
    }
  });
}

// Selector hooks for performance
export const useAuthToken = () => useAuthStore(s => s.token);
export const useAuthUser = () => useAuthStore(s => s.user);
export const useIsAuthenticated = () => useAuthStore(s => s.isAuthenticated);
