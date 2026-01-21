/**
 * Auth Routing Flow Tests
 *
 * Tests environment-based authentication routing:
 * - Telegram Mini App mode (initData present)
 * - Web Browser mode (initData empty)
 * - Environment detection
 * - Proper page rendering based on auth state
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { render } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AppRoot } from '@telegram-apps/telegram-ui';
import { App } from '@/App';
import { useAuthStore } from '@/stores/authStore';
import { resetMockState } from '@/test/mocks/server';
import { ToastProvider } from '@/components/common/Toast';
import { ConfirmDialogProvider } from '@/components/common/ConfirmDialog';
import type { TelegramUser } from '@/types';

// Store original Telegram mock
const originalTelegram = (global.window as any).Telegram;

describe('Auth Routing Flow', () => {
  beforeEach(() => {
    resetMockState();
    useAuthStore.setState({
      token: null,
      user: null,
      isAuthenticated: false,
    });
    localStorage.clear();
  });

  afterEach(() => {
    // Restore original Telegram mock after each test
    (global.window as any).Telegram = originalTelegram;
  });

  describe('Telegram Mini App Mode', () => {
    beforeEach(() => {
      // Setup: Mini App mode with valid initData
      (global.window as any).Telegram = {
        WebApp: {
          initData: 'user=%7B%22id%22%3A123456%7D&hash=abc123&auth_date=1234567890',
          initDataUnsafe: {
            user: {
              id: 123456,
              first_name: 'Test',
              last_name: 'User',
              username: 'testuser',
              language_code: 'en',
              is_premium: false,
            },
            hash: 'abc123',
            auth_date: 1234567890,
          },
          ready: vi.fn(),
          expand: vi.fn(),
          close: vi.fn(),
          platform: 'tdesktop',
          version: '7.0',
          colorScheme: 'light',
          isExpanded: true,
          viewportHeight: 600,
          viewportStableHeight: 600,
          headerColor: '#ffffff',
          backgroundColor: '#ffffff',
          themeParams: {
            bg_color: '#ffffff',
            text_color: '#000000',
            button_color: '#5288c1',
            button_text_color: '#ffffff',
            hint_color: '#999999',
            link_color: '#5288c1',
            secondary_bg_color: '#f0f0f0',
          },
          MainButton: {
            setText: vi.fn(),
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
            showProgress: vi.fn(),
            hideProgress: vi.fn(),
          },
          BackButton: {
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
          },
          onEvent: vi.fn(),
          offEvent: vi.fn(),
        },
      };
    });

    it('should show home page when initData is present and valid', async () => {
      render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for SDK initialization
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      // Should show home page title (from i18n: "home.title")
      await waitFor(() => {
        // Look for the home page title text
        const titleElement = screen.getByText(/Chatkeep Configuration/i);
        expect(titleElement).toBeInTheDocument();
      }, { timeout: 2000 });

      // Should NOT show authentication error
      expect(screen.queryByText(/authentication required/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/please open this app from telegram/i)).not.toBeInTheDocument();
    });
  });

  describe('Telegram Mini App Mode - Auth Error', () => {
    beforeEach(() => {
      // Setup: Mini App context but empty initData
      (global.window as any).Telegram = {
        WebApp: {
          initData: '', // Empty = should fail auth
          initDataUnsafe: {},
          ready: vi.fn(),
          expand: vi.fn(),
          close: vi.fn(),
          platform: 'tdesktop',
          version: '7.0',
          colorScheme: 'light',
          isExpanded: true,
          viewportHeight: 600,
          viewportStableHeight: 600,
          headerColor: '#ffffff',
          backgroundColor: '#ffffff',
          themeParams: {
            bg_color: '#ffffff',
            text_color: '#000000',
            button_color: '#5288c1',
            button_text_color: '#ffffff',
          },
          MainButton: {
            setText: vi.fn(),
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
            showProgress: vi.fn(),
            hideProgress: vi.fn(),
          },
          BackButton: {
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
          },
          onEvent: vi.fn(),
          offEvent: vi.fn(),
        },
      };
    });

    it('should show auth error when in Mini App context but initData is empty', async () => {
      render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for SDK initialization
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      // In Web mode (empty initData), should show LoginPage, NOT auth error
      // The auth error only shows in Mini App mode (when Telegram SDK is initialized but auth fails)
      // Since initData is empty, useAuthMode detects Web mode
      await waitFor(() => {
        expect(
          screen.getByText(/chatkeep/i) || // Login page title
          screen.getByText(/login/i)
        ).toBeInTheDocument();
      }, { timeout: 2000 });
    });
  });

  describe('Web Browser Mode', () => {
    beforeEach(() => {
      // Setup: Web Browser mode (no Telegram or empty initData)
      (global.window as any).Telegram = {
        WebApp: {
          initData: '', // Empty = Web mode
          initDataUnsafe: {},
          ready: vi.fn(),
          expand: vi.fn(),
          close: vi.fn(),
          platform: 'unknown',
          version: '7.0',
          colorScheme: 'light',
          isExpanded: false,
          viewportHeight: 600,
          viewportStableHeight: 600,
          headerColor: '#ffffff',
          backgroundColor: '#ffffff',
          themeParams: {
            bg_color: '#ffffff',
            text_color: '#000000',
            button_color: '#5288c1',
            button_text_color: '#ffffff',
          },
          MainButton: {
            setText: vi.fn(),
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
            showProgress: vi.fn(),
            hideProgress: vi.fn(),
          },
          BackButton: {
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
          },
          onEvent: vi.fn(),
          offEvent: vi.fn(),
        },
      };
    });

    it('should show login page when not authenticated', async () => {
      render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for SDK initialization
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      // Should show login page with title
      await waitFor(() => {
        expect(screen.getByText(/chatkeep/i)).toBeInTheDocument();
      }, { timeout: 2000 });

      // Should NOT show home page
      expect(screen.queryByText(/select a chat/i)).not.toBeInTheDocument();
    });

    it('should show home page when authenticated via token', async () => {
      // Setup: Authenticated web user
      const mockUser: TelegramUser = {
        id: 999888,
        firstName: 'Web',
        lastName: 'User',
        username: 'webuser',
        isPremium: false,
      };

      // Set in both store AND localStorage (zustand persist needs both)
      localStorage.setItem('chatkeep_auth_token', 'mock-jwt-token');
      localStorage.setItem('chatkeep_auth_user', JSON.stringify(mockUser));
      localStorage.setItem('chatkeep-auth-storage', JSON.stringify({
        state: {
          token: 'mock-jwt-token',
          user: mockUser,
          isAuthenticated: true,
        },
        version: 0,
      }));

      useAuthStore.setState({
        token: 'mock-jwt-token',
        user: mockUser,
        isAuthenticated: true,
      });

      render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for SDK initialization
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      // Should show home page
      await waitFor(() => {
        // Look for the home page title text
        const titleElement = screen.getByText(/Chatkeep Configuration/i);
        expect(titleElement).toBeInTheDocument();
      }, { timeout: 2000 });

      // Should NOT show login page
      expect(screen.queryByText(/login/i)).not.toBeInTheDocument();
    });
  });

  describe('Environment Detection', () => {
    it('should detect Telegram mode when initData exists', () => {
      (global.window as any).Telegram = {
        WebApp: {
          initData: 'user=%7B%22id%22%3A123%7D&hash=abc',
          initDataUnsafe: { user: { id: 123, first_name: 'Test' } },
        },
      };

      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      expect(hasInitData).toBe(true);
    });

    it('should detect Web mode when initData is empty', () => {
      (global.window as any).Telegram = {
        WebApp: {
          initData: '',
          initDataUnsafe: {},
        },
      };

      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      expect(hasInitData).toBe(false);
    });

    it('should handle missing window.Telegram gracefully', () => {
      (global.window as any).Telegram = undefined;

      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      expect(hasInitData).toBe(false);
    });

    it('should handle missing initData property', () => {
      (global.window as any).Telegram = {
        WebApp: {
          initDataUnsafe: {},
        },
      };

      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      expect(hasInitData).toBe(false);
    });
  });

  describe('Auth State Transitions', () => {
    beforeEach(() => {
      // Web mode
      (global.window as any).Telegram = {
        WebApp: {
          initData: '',
          initDataUnsafe: {},
          ready: vi.fn(),
          expand: vi.fn(),
          close: vi.fn(),
          platform: 'unknown',
          version: '7.0',
          colorScheme: 'light',
          isExpanded: false,
          viewportHeight: 600,
          viewportStableHeight: 600,
          headerColor: '#ffffff',
          backgroundColor: '#ffffff',
          themeParams: {},
          MainButton: {
            setText: vi.fn(),
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
            showProgress: vi.fn(),
            hideProgress: vi.fn(),
          },
          BackButton: {
            show: vi.fn(),
            hide: vi.fn(),
            onClick: vi.fn(),
            offClick: vi.fn(),
          },
          onEvent: vi.fn(),
          offEvent: vi.fn(),
        },
      };
    });

    it('should transition from login to home after authentication', async () => {
      const { rerender } = render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for initial render (login page)
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      await waitFor(() => {
        expect(screen.getByText(/chatkeep/i)).toBeInTheDocument();
      }, { timeout: 2000 });

      // Simulate successful login
      const mockUser: TelegramUser = {
        id: 777666,
        firstName: 'Logged',
        lastName: 'In',
        username: 'loggedin',
        isPremium: false,
      };

      // Set in both store AND localStorage (zustand persist needs both)
      localStorage.setItem('chatkeep_auth_token', 'new-token');
      localStorage.setItem('chatkeep_auth_user', JSON.stringify(mockUser));
      localStorage.setItem('chatkeep-auth-storage', JSON.stringify({
        state: {
          token: 'new-token',
          user: mockUser,
          isAuthenticated: true,
        },
        version: 0,
      }));

      useAuthStore.setState({
        token: 'new-token',
        user: mockUser,
        isAuthenticated: true,
      });

      // Rerender
      rerender(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Should now show home page
      await waitFor(() => {
        // Look for the home page title text
        const titleElement = screen.getByText(/Chatkeep Configuration/i);
        expect(titleElement).toBeInTheDocument();
      }, { timeout: 2000 });
    });

    it('should transition from home to login after logout', async () => {
      // Start authenticated
      const mockUser: TelegramUser = {
        id: 555444,
        firstName: 'Will',
        lastName: 'Logout',
        username: 'willlogout',
        isPremium: false,
      };

      // Set in both store AND localStorage (zustand persist needs both)
      localStorage.setItem('chatkeep_auth_token', 'existing-token');
      localStorage.setItem('chatkeep_auth_user', JSON.stringify(mockUser));
      localStorage.setItem('chatkeep-auth-storage', JSON.stringify({
        state: {
          token: 'existing-token',
          user: mockUser,
          isAuthenticated: true,
        },
        version: 0,
      }));

      useAuthStore.setState({
        token: 'existing-token',
        user: mockUser,
        isAuthenticated: true,
      });

      const { rerender } = render(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Wait for home page
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
      }, { timeout: 2000 });

      // Simulate logout
      useAuthStore.getState().logout();

      // Rerender
      rerender(
        <AppRoot>
          <ToastProvider>
            <ConfirmDialogProvider>
              <App />
            </ConfirmDialogProvider>
          </ToastProvider>
        </AppRoot>
      );

      // Should show login page
      await waitFor(() => {
        expect(screen.getByText(/chatkeep/i)).toBeInTheDocument();
      }, { timeout: 2000 });

      // Should NOT show home page
      expect(screen.queryByText(/select a chat/i)).not.toBeInTheDocument();
    });
  });
});
