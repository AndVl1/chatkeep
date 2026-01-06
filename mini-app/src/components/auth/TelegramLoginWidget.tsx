import { useEffect, useRef } from 'react';
import type { TelegramAuthData } from '@/types';

interface TelegramLoginWidgetProps {
  botName: string;
  onAuth?: (data: TelegramAuthData) => void;
  authUrl?: string;
  size?: 'large' | 'medium' | 'small';
  requestAccess?: boolean;
}

/**
 * Renders the Telegram Login Widget for web authentication.
 *
 * Supports two modes:
 * 1. Callback mode (onAuth) - Uses popup, works best on localhost
 * 2. Redirect mode (authUrl) - Redirects user, works on external hosts
 *
 * @see https://core.telegram.org/widgets/login
 */
export function TelegramLoginWidget({
  botName,
  onAuth,
  authUrl,
  size = 'large',
  requestAccess = true,
}: TelegramLoginWidgetProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const callbackName = useRef(`telegramCallback_${Date.now()}`);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const callback = callbackName.current;

    // Set up global callback only if using callback mode
    if (onAuth && !authUrl) {
      (window as any)[callback] = (data: TelegramAuthData) => {
        onAuth(data);
      };
    }

    // Create script element
    const script = document.createElement('script');
    script.src = 'https://telegram.org/js/telegram-widget.js?22';
    script.async = true;
    script.setAttribute('data-telegram-login', botName);
    script.setAttribute('data-size', size);
    script.setAttribute('data-request-access', requestAccess ? 'write' : '');

    // Choose mode: redirect (authUrl) or callback (onAuth)
    if (authUrl) {
      // Redirect mode - more reliable for external hosts
      script.setAttribute('data-auth-url', authUrl);
    } else if (onAuth) {
      // Callback mode - better UX for localhost
      script.setAttribute('data-onauth', `${callback}(user)`);
    }

    container.appendChild(script);

    return () => {
      // Cleanup global callback
      if (onAuth && !authUrl) {
        delete (window as any)[callback];
      }

      // Remove script and clean container
      if (script.parentNode) {
        script.parentNode.removeChild(script);
      }
      if (container) {
        container.innerHTML = '';
      }
    };
  }, [botName, size, requestAccess, onAuth, authUrl]);

  return <div ref={containerRef} />;
}
