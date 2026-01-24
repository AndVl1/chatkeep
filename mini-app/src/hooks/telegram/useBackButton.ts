import { useEffect, useCallback } from 'react';
import { postEvent, on } from '@telegram-apps/sdk';

interface UseBackButtonOptions {
  onClick: () => void;
  visible?: boolean;
}

/**
 * Hook to control Telegram's native back button.
 * Uses postEvent directly to bypass SDK availability checks.
 */
export function useBackButton({
  onClick,
  visible = true,
}: UseBackButtonOptions) {
  // Wrap onClick handler in useCallback to prevent memory leak
  const handleClick = useCallback(() => {
    try {
      onClick();
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('Back button click handler error:', err);
      }
    }
  }, [onClick]);

  // Control visibility using postEvent directly
  useEffect(() => {
    try {
      postEvent('web_app_setup_back_button', { is_visible: visible });
    } catch {
      // Ignore errors - postEvent may not be available outside Telegram
    }

    // Hide on cleanup
    return () => {
      try {
        postEvent('web_app_setup_back_button', { is_visible: false });
      } catch {
        // Ignore cleanup errors
      }
    };
  }, [visible]);

  // Listen for back button press event
  useEffect(() => {
    try {
      const off = on('back_button_pressed', handleClick);
      return off;
    } catch {
      // Ignore errors - event may not be available outside Telegram
    }
  }, [handleClick]);
}
