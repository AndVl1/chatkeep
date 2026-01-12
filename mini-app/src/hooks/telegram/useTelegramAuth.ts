import { useMemo, useCallback, useState, useEffect } from 'react';
import { initData, useSignal } from '@telegram-apps/sdk-react';
import type { TelegramUser } from '@/types';

// Get auth data directly from window.Telegram.WebApp (fallback for SDK signals)
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

  // Fallback state - initialize with WebApp data immediately for first render
  const [fallbackData, setFallbackData] = useState<{
    user: TelegramUser | null;
    initDataRaw: string | null;
  }>(() => getWebAppData());

  // Update fallback if SDK signals change (e.g., after SDK initialization)
  useEffect(() => {
    // Re-check window.Telegram.WebApp if SDK still doesn't have data
    if (!initDataState?.user || !initDataRawValue) {
      const webAppData = getWebAppData();
      if (webAppData.user && !fallbackData.user) {
        setFallbackData(webAppData);
      }
    }
  }, [initDataState, initDataRawValue, fallbackData.user]);

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
