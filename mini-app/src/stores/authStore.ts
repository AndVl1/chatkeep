import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { TelegramUser } from '@/types';

// Note: Zustand persist middleware automatically handles localStorage hydration
// on store initialization. No manual initialize() function needed.

interface AuthState {
  // State
  token: string | null;
  user: TelegramUser | null;
  isAuthenticated: boolean;

  // Actions
  login: (token: string, user: TelegramUser) => void;
  logout: () => void;
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


// Selector hooks for performance
export const useAuthToken = () => useAuthStore(s => s.token);
export const useAuthUser = () => useAuthStore(s => s.user);
export const useIsAuthenticated = () => useAuthStore(s => s.isAuthenticated);
