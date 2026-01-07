import { useCallback } from 'react';
import { popup } from '@telegram-apps/sdk-react';
import { useToast } from '@/components/common/Toast';

export function useNotification() {
  const { showToast } = useToast();

  const showNotification = useCallback((message: string) => {
    if (popup.open.isAvailable()) {
      popup.open({
        title: 'Notification',
        message,
        buttons: [{ id: 'ok', type: 'default', text: 'OK' }],
      });
    } else {
      showToast(message, 'info');
    }
  }, [showToast]);

  const showError = useCallback((message: string) => {
    if (popup.open.isAvailable()) {
      popup.open({
        title: 'Error',
        message,
        buttons: [{ id: 'ok', type: 'destructive', text: 'OK' }],
      });
    } else {
      showToast(message, 'error');
    }
  }, [showToast]);

  const showSuccess = useCallback((message: string) => {
    if (popup.open.isAvailable()) {
      popup.open({
        title: 'Success',
        message,
        buttons: [{ id: 'ok', type: 'default', text: 'OK' }],
      });
    } else {
      showToast(message, 'success');
    }
  }, [showToast]);

  return {
    showNotification,
    showError,
    showSuccess,
  };
}
