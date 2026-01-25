import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppRoot } from '@telegram-apps/telegram-ui';
import { useEffect, useState } from 'react';
import { init, retrieveLaunchParams, miniApp, themeParams, viewport, initData, backButton } from '@telegram-apps/sdk';
import { AppLayout } from '@/components/layout/AppLayout';
import { HomePage } from '@/pages/HomePage';
import { SettingsPage } from '@/pages/SettingsPage';
import { LocksPage } from '@/pages/LocksPage';
import { BlocklistPage } from '@/pages/BlocklistPage';
import { ChannelReplyPage } from '@/pages/ChannelReplyPage';
import { StatisticsPage } from '@/pages/StatisticsPage';
import { SessionPage } from '@/pages/SessionPage';
import { AdminLogsPage } from '@/pages/AdminLogsPage';
import { WelcomePage } from '@/pages/WelcomePage';
import { RulesPage } from '@/pages/RulesPage';
import { NotesPage } from '@/pages/NotesPage';
import { AntiFloodPage } from '@/pages/AntiFloodPage';
import { CapabilitiesPage } from '@/pages/CapabilitiesPage';
import { LoginPage } from '@/pages/LoginPage';
import { AuthCallbackPage } from '@/pages/AuthCallbackPage';
import { TwitchSettingsPage } from '@/pages/TwitchSettingsPage';
import { useTelegramAuth } from '@/hooks/telegram/useTelegramAuth';
import { useAuthMode } from '@/hooks/auth/useAuthMode';
import { useAuthStore } from '@/stores/authStore';
import { setAuthHeader, setLogoutHandler } from '@/api';
import { ConfirmDialogProvider } from '@/components/common/ConfirmDialog';
import { ToastProvider } from '@/components/common/Toast';
import '@telegram-apps/telegram-ui/dist/styles.css';

function AuthProvider({ children }: { children: React.ReactNode }) {
  const { isMiniApp, isWeb } = useAuthMode();
  const { isAuthenticated: isMiniAppAuthenticated, getAuthHeader } = useTelegramAuth();
  const { isAuthenticated: isWebAuthenticated, token, logout } = useAuthStore();
  // Note: Zustand persist middleware automatically hydrates state from localStorage on mount

  // Register logout handler for API client
  useEffect(() => {
    if (isWeb) {
      setLogoutHandler(logout);
    }
  }, [isWeb, logout]);

  // Set auth header based on mode
  useEffect(() => {
    if (isMiniApp && isMiniAppAuthenticated) {
      const header = getAuthHeader();
      if (Object.keys(header).length > 0) {
        setAuthHeader(header);
      }
    } else if (isWeb && isWebAuthenticated && token) {
      setAuthHeader({ Authorization: `Bearer ${token}` });
    }
  }, [isMiniApp, isWeb, isMiniAppAuthenticated, isWebAuthenticated, token, getAuthHeader]);

  // Mini App mode: require Telegram auth
  if (isMiniApp && !isMiniAppAuthenticated) {
    const hostname = window.location.hostname;
    const isMiniAppSubdomain = hostname.startsWith('miniapp.');

    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '16px',
        textAlign: 'center',
      }}>
        <div>
          <h1>Authentication Required</h1>
          <p>
            {isMiniAppSubdomain
              ? 'This subdomain is only accessible from Telegram Mini App. Please use chatmoderatorbot.ru for browser access.'
              : 'Please open this app from Telegram'}
          </p>
          {isMiniAppSubdomain && (
            <p style={{ marginTop: '16px' }}>
              <a href="https://chatmoderatorbot.ru" style={{ color: 'var(--tg-theme-link-color, #5288c1)' }}>
                Go to chatmoderatorbot.ru
              </a>
            </p>
          )}
        </div>
      </div>
    );
  }

  // Web mode: show login page if not authenticated
  if (isWeb && !isWebAuthenticated) {
    return <LoginPage />;
  }

  return <>{children}</>;
}

export function App() {
  const [sdkInitialized, setSdkInitialized] = useState(false);

  useEffect(() => {
    // Check if we're inside Telegram (has initData)
    const webApp = (window as { Telegram?: { WebApp?: { initData?: string } } }).Telegram?.WebApp;
    const isTelegramEnv = !!webApp?.initData && webApp.initData.length > 0;

    // In development mode, only restore initData (mockEnv provides window globals)
    if (import.meta.env.DEV) {
      try {
        initData.restore();
      } catch (error) {
        console.warn('[SDK] Failed to restore initData:', error);
      }
      setSdkInitialized(true);
      return;
    }

    // Production: Initialize SDK only if inside Telegram
    if (isTelegramEnv) {
      try {
        // Initialize SDK first (required for all components)
        init();

        retrieveLaunchParams();

        miniApp.mount();
        themeParams.mount();
        viewport.mount();

        // Mount backButton if available (may not be on all platforms)
        if (backButton.mount.isAvailable()) {
          backButton.mount();
        }

        initData.restore();

        miniApp.ready();
      } catch (error) {
        console.error('[SDK] Initialization failed:', error);
        // Don't throw - allow web mode fallback
      }
    }

    setSdkInitialized(true);
  }, []);

  if (!sdkInitialized) {
    return <div>Loading...</div>;
  }

  return (
    <AppRoot>
      <ToastProvider>
        <ConfirmDialogProvider>
          <BrowserRouter>
            <Routes>
              {/* Auth callback route - outside AuthProvider */}
              <Route path="/auth/callback" element={<AuthCallbackPage />} />

              {/* Protected routes - inside AuthProvider */}
              <Route
                path="/*"
                element={
                  <AuthProvider>
                    <Routes>
                      <Route element={<AppLayout />}>
                        <Route index element={<HomePage />} />
                        <Route path="capabilities" element={<CapabilitiesPage />} />
                        <Route path="chat/:chatId">
                          <Route path="settings" element={<SettingsPage />} />
                          <Route path="locks" element={<LocksPage />} />
                          <Route path="blocklist" element={<BlocklistPage />} />
                          <Route path="channel-reply" element={<ChannelReplyPage />} />
                          <Route path="statistics" element={<StatisticsPage />} />
                          <Route path="session" element={<SessionPage />} />
                          <Route path="admin-logs" element={<AdminLogsPage />} />
                          <Route path="welcome" element={<WelcomePage />} />
                          <Route path="rules" element={<RulesPage />} />
                          <Route path="notes" element={<NotesPage />} />
                          <Route path="antiflood" element={<AntiFloodPage />} />
                          <Route path="twitch" element={<TwitchSettingsPage />} />
                        </Route>
                        <Route path="*" element={<Navigate to="/" replace />} />
                      </Route>
                    </Routes>
                  </AuthProvider>
                }
              />
            </Routes>
          </BrowserRouter>
        </ConfirmDialogProvider>
      </ToastProvider>
    </AppRoot>
  );
}
