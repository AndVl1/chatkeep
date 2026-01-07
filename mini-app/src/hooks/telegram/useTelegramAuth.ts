import { useMemo, useCallback, useState, useEffect } from 'react';
import { initData, useSignal } from '@telegram-apps/sdk-react';
import type { TelegramUser } from '@/types';

// Get auth data from window.Telegram.WebApp (for development mode)
function getWebAppData(): { user: TelegramUser | null; initDataRaw: string | null } {
  try {
    const webApp = window.Telegram?.WebApp;
    if (!webApp?.initDataUnsafe?.user || !webApp?.initData) {
      return { user: null, initDataRaw: null };
    }

    const userData = webApp.initDataUnsafe.user;
    return {
      user: {
        id: userData.id,
        firstName: userData.first_name,
        lastName: userData.last_name,
        username: userData.username,
        isPremium: userData.is_premium ?? false,
        languageCode: userData.language_code,
      },
      initDataRaw: webApp.initData,
    };
  } catch {
    return { user: null, initDataRaw: null };
  }
}

export function useTelegramAuth() {
  // SDK signals
  const initDataState = useSignal(initData.state);
  const initDataRawValue = useSignal(initData.raw);

  // Fallback state for dev mode
  const [fallbackData, setFallbackData] = useState<{
    user: TelegramUser | null;
    initDataRaw: string | null;
  }>({ user: null, initDataRaw: null });

  // Check window.Telegram.WebApp on mount (for dev mode fallback)
  useEffect(() => {
    // Only use fallback if SDK didn't provide data
    if (!initDataState?.user || !initDataRawValue) {
      const webAppData = getWebAppData();
      if (webAppData.user) {
        if (import.meta.env.DEV) {
          console.log('[Auth] Using window.Telegram.WebApp fallback');
        }
        setFallbackData(webAppData);
      }
    }
  }, [initDataState, initDataRawValue]);

  // Use SDK data if available, otherwise use fallback
  const user = useMemo<TelegramUser | null>(() => {
    const sdkUser = initDataState?.user;
    if (sdkUser) {
      return {
        id: sdkUser.id,
        firstName: sdkUser.firstName,
        lastName: sdkUser.lastName,
        username: sdkUser.username,
        isPremium: sdkUser.isPremium ?? false,
        languageCode: sdkUser.languageCode,
      };
    }
    return fallbackData.user;
  }, [initDataState, fallbackData]);

  const initDataRaw = initDataRawValue || fallbackData.initDataRaw;

  const getAuthHeader = useCallback((): Record<string, string> => {
    if (!initDataRaw) return {};
    return { Authorization: `tma ${initDataRaw}` };
  }, [initDataRaw]);

  return {
    user,
    isAuthenticated: !!user && !!initDataRaw,
    initDataRaw,
    getAuthHeader,
  };
}
