import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppRoot } from '@telegram-apps/telegram-ui';
import { useEffect, useState } from 'react';
import { retrieveLaunchParams, miniApp, themeParams, viewport, initData } from '@telegram-apps/sdk';
import { AppLayout } from '@/components/layout/AppLayout';
import { HomePage } from '@/pages/HomePage';
import { SettingsPage } from '@/pages/SettingsPage';
import { LocksPage } from '@/pages/LocksPage';
import { BlocklistPage } from '@/pages/BlocklistPage';
import { LoginPage } from '@/pages/LoginPage';
import { AuthCallbackPage } from '@/pages/AuthCallbackPage';
import { useTelegramAuth } from '@/hooks/telegram/useTelegramAuth';
import { useAuthMode } from '@/hooks/auth/useAuthMode';
import { useAuthStore } from '@/stores/authStore';
import { setAuthHeader } from '@/api';
import { ConfirmDialogProvider } from '@/components/common/ConfirmDialog';
import { ToastProvider } from '@/components/common/Toast';
import '@telegram-apps/telegram-ui/dist/styles.css';

function AuthProvider({ children }: { children: React.ReactNode }) {
  const { isMiniApp, isWeb } = useAuthMode();
  const { isAuthenticated: isMiniAppAuthenticated, getAuthHeader } = useTelegramAuth();
  const { isAuthenticated: isWebAuthenticated, token, initialize } = useAuthStore();

  // Initialize web auth store on mount
  useEffect(() => {
    if (isWeb) {
      initialize();
    }
  }, [isWeb, initialize]);

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
          <p>Please open this app from Telegram</p>
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
    // In development mode, only restore initData (mockEnv provides window globals)
    if (import.meta.env.DEV) {
      console.log('[SDK] Development mode - using mock environment');
      try {
        initData.restore();
        console.log('[SDK] initData restored from mock environment');
      } catch (error) {
        console.warn('[SDK] Failed to restore initData:', error);
      }
      setSdkInitialized(true);
      return;
    }

    // Production: Initialize all SDK components
    try {
      const launchParams = retrieveLaunchParams();
      console.log('[SDK] Launch params retrieved:', launchParams.platform);

      miniApp.mount();
      themeParams.mount();
      viewport.mount();
      initData.restore();

      miniApp.ready();
      console.log('[SDK] Initialized successfully');
      setSdkInitialized(true);
    } catch (error) {
      console.error('[SDK] Initialization failed:', error);
      throw error; // Fail in production
    }
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
                        <Route path="chat/:chatId">
                          <Route path="settings" element={<SettingsPage />} />
                          <Route path="locks" element={<LocksPage />} />
                          <Route path="blocklist" element={<BlocklistPage />} />
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
