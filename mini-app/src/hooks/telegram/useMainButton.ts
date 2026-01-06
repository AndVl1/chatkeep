import { useEffect, useCallback } from 'react';
import { mainButton } from '@telegram-apps/sdk-react';

interface UseMainButtonOptions {
  text: string;
  onClick: () => void | Promise<void>;
  disabled?: boolean;
  visible?: boolean;
}

export function useMainButton({
  text,
  onClick,
  disabled = false,
  visible = true,
}: UseMainButtonOptions) {
  useEffect(() => {
    if (!mainButton.mount.isAvailable()) return;

    mainButton.mount();
    if (mainButton.setParams.isAvailable()) {
      mainButton.setParams({
        text,
        isEnabled: !disabled,
        isVisible: visible,
      });
    }

    return () => {
      if (mainButton.unmount) {
        mainButton.unmount();
      }
    };
  }, [text, disabled, visible]);

  useEffect(() => {
    if (!mainButton.onClick.isAvailable()) return;

    const handler = async () => {
      try {
        await onClick();
      } catch (err) {
        if (import.meta.env.DEV) {
          console.error('Main button click handler error:', err);
        }
      }
    };

    return mainButton.onClick(handler);
  }, [onClick]);

  const showProgress = useCallback(() => {
    // Progress is handled automatically by SDK
  }, []);

  const hideProgress = useCallback(() => {
    // Progress is handled automatically by SDK
  }, []);

  return { showProgress, hideProgress };
}
