import { useCallback } from 'react';
import { popup } from '@telegram-apps/sdk-react';
import { useConfirmDialogContext } from '@/components/common/ConfirmDialog';

export function useConfirmDialog() {
  const { confirm: showCustomDialog } = useConfirmDialogContext();

  const confirm = useCallback(async (
    message: string,
    title?: string
  ): Promise<boolean> => {
    // Try Telegram SDK popup first (works in native app)
    if (popup.open.isAvailable()) {
      const result = await popup.open({
        title: title || 'Confirm',
        message,
        buttons: [
          { id: 'cancel', type: 'cancel' },
          { id: 'ok', type: 'destructive', text: 'Delete' },
        ],
      });
      return result === 'ok';
    }

    // Fallback to custom dialog (works in web/browser)
    return showCustomDialog({
      title,
      message,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      destructive: true,
    });
  }, [showCustomDialog]);

  return { confirm };
}
