import { useMemo } from 'react';

export interface AuthMode {
  isMiniApp: boolean;
  isWeb: boolean;
}

/**
 * Detects whether the app is running in Mini App mode (Telegram) or Web mode (browser)
 *
 * Detection logic:
 * - miniapp.* subdomain: STRICT Mini App mode (requires initData, no web fallback)
 * - chatmoderatorbot.ru: Auto-detect (Mini App if initData present, Web mode otherwise)
 *
 * Subdomain-based enforcement:
 * - miniapp.chatmoderatorbot.ru → Telegram only (nginx also redirects browsers)
 * - chatmoderatorbot.ru → Dual mode (auto-detect Telegram or browser)
 */
export function useAuthMode(): AuthMode {
  const mode = useMemo<AuthMode>(() => {
    try {
      const hostname = window.location.hostname;
      const isMiniAppSubdomain = hostname.startsWith('miniapp.');

      const webApp = window.Telegram?.WebApp;
      const hasInitData = !!webApp?.initData && webApp.initData.length > 0;

      // For miniapp.* subdomain: STRICT requirement for initData
      if (isMiniAppSubdomain) {
        return {
          isMiniApp: hasInitData,
          isWeb: !hasInitData, // Will trigger "Please open from Telegram" message
        };
      }

      // For main domain: auto-detect based on initData presence
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
