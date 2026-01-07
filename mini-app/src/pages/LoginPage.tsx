import { useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { TelegramLoginWidget } from '@/components/auth/TelegramLoginWidget';
import { useAuthStore } from '@/stores/authStore';
import { Spinner } from '@telegram-apps/telegram-ui';
import type { TelegramUser, TelegramAuthData } from '@/types';

interface LoginResponse {
  token: string;
  user: TelegramUser;
}

/**
 * Determines if we should use redirect mode for Telegram Login Widget.
 * Redirect mode is more reliable for external hosts (tunnels, production).
 */
function shouldUseRedirectMode(): boolean {
  const hostname = window.location.hostname;
  // Use callback mode only for localhost
  return hostname !== 'localhost' && hostname !== '127.0.0.1';
}

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const botUsername = import.meta.env.VITE_BOT_USERNAME || 'chatkeep_bot';
  const useRedirectMode = shouldUseRedirectMode();

  // Build auth URL for redirect mode
  const authUrl = useMemo(() => {
    if (!useRedirectMode) return undefined;
    const baseUrl = window.location.origin;
    return `${baseUrl}/auth/callback`;
  }, [useRedirectMode]);

  // Callback handler for localhost (callback mode)
  const handleAuth = useCallback(async (data: TelegramAuthData) => {
    setIsLoading(true);
    setError(null);

    try {
      // Send auth data to backend
      const response = await fetch('/api/v1/auth/telegram-login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Authentication failed');
      }

      const loginResponse: LoginResponse = await response.json();

      // Save token and user to store
      login(loginResponse.token, loginResponse.user);

      // Redirect to home page
      navigate('/', { replace: true });
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('[LoginPage] Authentication error:', err);
      }
      setError(err instanceof Error ? err.message : 'Authentication failed');
      setIsLoading(false);
    }
  }, [login, navigate]);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '24px',
        textAlign: 'center',
        backgroundColor: 'var(--tg-theme-bg-color, #17212b)',
        color: 'var(--tg-theme-text-color, #f5f5f5)',
      }}
    >
      <div style={{ maxWidth: '400px' }}>
        {/* App Logo/Title */}
        <h1
          style={{
            fontSize: '32px',
            fontWeight: 'bold',
            marginBottom: '16px',
            color: 'var(--tg-theme-button-color, #5288c1)',
          }}
        >
          Chatkeep
        </h1>

        <p
          style={{
            fontSize: '16px',
            marginBottom: '32px',
            color: 'var(--tg-theme-hint-color, #708499)',
          }}
        >
          Configure your Telegram bot settings
        </p>

        {/* Loading State */}
        {isLoading && (
          <div style={{ marginBottom: '24px' }}>
            <Spinner size="l" />
            <p style={{ marginTop: '16px', color: 'var(--tg-theme-hint-color, #708499)' }}>
              Authenticating...
            </p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div
            style={{
              marginBottom: '24px',
              padding: '12px',
              borderRadius: '8px',
              backgroundColor: 'var(--tg-theme-destructive-text-color, #ec3942)',
              color: '#ffffff',
            }}
          >
            {error}
          </div>
        )}

        {/* Login Widget */}
        {!isLoading && (
          <div style={{ marginBottom: '16px' }}>
            <TelegramLoginWidget
              botName={botUsername}
              onAuth={useRedirectMode ? undefined : handleAuth}
              authUrl={authUrl}
              size="large"
              requestAccess={true}
            />
          </div>
        )}

        {/* Help Text */}
        <p
          style={{
            fontSize: '14px',
            marginTop: '24px',
            color: 'var(--tg-theme-hint-color, #708499)',
          }}
        >
          You need a Telegram account to use this app.
          <br />
          Click the button above to log in with Telegram.
        </p>
      </div>
    </div>
  );
}
