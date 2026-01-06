import { useMemo } from 'react';

export interface AuthMode {
  isMiniApp: boolean;
  isWeb: boolean;
}

/**
 * Detects whether the app is running in Mini App mode (Telegram) or Web mode (browser)
 *
 * Detection logic:
 * - Mini App mode: window.Telegram.WebApp.initData is present and non-empty
 * - Web mode: initData is missing or empty
 */
export function useAuthMode(): AuthMode {
  const mode = useMemo<AuthMode>(() => {
    try {
      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      return {
        isMiniApp: hasInitData,
        isWeb: !hasInitData,
      };
    } catch {
      // If any error occurs, assume web mode
      return {
        isMiniApp: false,
        isWeb: true,
      };
    }
  }, []);

  return mode;
}
