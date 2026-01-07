import { createContext, useContext, useState, useCallback, ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { Button } from '@telegram-apps/telegram-ui';

interface ConfirmDialogOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  destructive?: boolean;
}

interface ConfirmDialogContextValue {
  confirm: (options: ConfirmDialogOptions) => Promise<boolean>;
}

const ConfirmDialogContext = createContext<ConfirmDialogContextValue | null>(null);

export function useConfirmDialogContext() {
  const context = useContext(ConfirmDialogContext);
  if (!context) {
    throw new Error('useConfirmDialogContext must be used within ConfirmDialogProvider');
  }
  return context;
}

interface ConfirmDialogProviderProps {
  children: ReactNode;
}

export function ConfirmDialogProvider({ children }: ConfirmDialogProviderProps) {
  const [dialogState, setDialogState] = useState<{
    options: ConfirmDialogOptions;
    resolve: (value: boolean) => void;
  } | null>(null);

  const confirm = useCallback((options: ConfirmDialogOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      setDialogState({ options, resolve });
    });
  }, []);

  const handleConfirm = useCallback(() => {
    dialogState?.resolve(true);
    setDialogState(null);
  }, [dialogState]);

  const handleCancel = useCallback(() => {
    dialogState?.resolve(false);
    setDialogState(null);
  }, [dialogState]);

  return (
    <ConfirmDialogContext.Provider value={{ confirm }}>
      {children}
      {dialogState && createPortal(
        <div
          data-testid="confirm-dialog-overlay"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 10000,
            padding: '16px',
          }}
          onClick={handleCancel}
        >
          <div
            data-testid="confirm-dialog"
            style={{
              backgroundColor: 'var(--tgui--secondary_bg_color, #1c1c1e)',
              borderRadius: '14px',
              padding: '20px',
              maxWidth: '320px',
              width: '100%',
              boxShadow: '0 4px 24px rgba(0, 0, 0, 0.3)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {dialogState.options.title && (
              <h3
                style={{
                  margin: '0 0 8px 0',
                  fontSize: '17px',
                  fontWeight: 600,
                  color: 'var(--tgui--text_color, #fff)',
                  textAlign: 'center',
                }}
              >
                {dialogState.options.title}
              </h3>
            )}
            <p
              style={{
                margin: '0 0 20px 0',
                fontSize: '15px',
                color: 'var(--tgui--hint_color, #98989e)',
                textAlign: 'center',
                lineHeight: 1.4,
              }}
            >
              {dialogState.options.message}
            </p>
            <div
              style={{
                display: 'flex',
                gap: '8px',
                justifyContent: 'center',
              }}
            >
              <Button
                data-testid="confirm-dialog-cancel"
                size="m"
                mode="bezeled"
                onClick={handleCancel}
                style={{ flex: 1 }}
              >
                {dialogState.options.cancelText || 'Cancel'}
              </Button>
              <Button
                data-testid="confirm-dialog-confirm"
                size="m"
                mode={dialogState.options.destructive ? 'filled' : 'filled'}
                onClick={handleConfirm}
                style={{
                  flex: 1,
                  ...(dialogState.options.destructive && {
                    backgroundColor: 'var(--tgui--destructive_text_color, #ff3b30)',
                  }),
                }}
              >
                {dialogState.options.confirmText || 'OK'}
              </Button>
            </div>
          </div>
        </div>,
        document.body
      )}
    </ConfirmDialogContext.Provider>
  );
}
