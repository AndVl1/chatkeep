import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Spinner } from '@telegram-apps/telegram-ui';
import { useAuthStore } from '@/stores/authStore';
import type { TelegramUser, TelegramAuthData } from '@/types';

interface LoginResponse {
  token: string;
  user: TelegramUser;
}

/**
 * Handles Telegram Login Widget redirect callback.
 * Telegram redirects here with auth data in URL params.
 */
export function AuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const processAuth = async () => {
      // Extract auth data from URL params
      const id = searchParams.get('id');
      const firstName = searchParams.get('first_name');
      const authDate = searchParams.get('auth_date');
      const hash = searchParams.get('hash');

      if (!id || !firstName || !authDate || !hash) {
        setError('Missing authentication data');
        return;
      }

      const authData: TelegramAuthData = {
        id: parseInt(id, 10),
        first_name: firstName,
        last_name: searchParams.get('last_name') || undefined,
        username: searchParams.get('username') || undefined,
        photo_url: searchParams.get('photo_url') || undefined,
        auth_date: parseInt(authDate, 10),
        hash,
      };

      try {
        // Send auth data to backend
        const response = await fetch('/api/v1/auth/telegram-login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(authData),
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
          console.error('[AuthCallback] Authentication error:', err);
        }
        setError(err instanceof Error ? err.message : 'Authentication failed');
      }
    };

    processAuth();
  }, [searchParams, login, navigate]);

  if (error) {
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
        <div
          style={{
            padding: '16px',
            borderRadius: '8px',
            backgroundColor: 'var(--tg-theme-destructive-text-color, #ec3942)',
            color: '#ffffff',
            marginBottom: '16px',
          }}
        >
          {error}
        </div>
        <button
          onClick={() => navigate('/login', { replace: true })}
          style={{
            padding: '12px 24px',
            borderRadius: '8px',
            border: 'none',
            backgroundColor: 'var(--tg-theme-button-color, #5288c1)',
            color: 'var(--tg-theme-button-text-color, #ffffff)',
            cursor: 'pointer',
          }}
        >
          Try Again
        </button>
      </div>
    );
  }

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
      <Spinner size="l" />
      <p style={{ marginTop: '16px', color: 'var(--tg-theme-hint-color, #708499)' }}>
        Authenticating...
      </p>
    </div>
  );
}
