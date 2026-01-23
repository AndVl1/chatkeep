import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Card, Section } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useSession } from '@/hooks/api/useSession';
import { useNotification } from '@/hooks/ui/useNotification';

export function SessionPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: session, isLoading, isConnecting, error, connect, disconnect, refetch } = useSession(numericChatId);
  const { showSuccess, showError } = useNotification();

  const handleConnect = useCallback(async () => {
    try {
      await connect();
      showSuccess(t('session.connected'));
    } catch (err) {
      showError((err as Error).message || t('session.connectFailed'));
    }
  }, [connect, showSuccess, showError, t]);

  const handleDisconnect = useCallback(async () => {
    try {
      await disconnect();
      showSuccess(t('session.disconnected'));
    } catch (err) {
      showError((err as Error).message || t('session.disconnectFailed'));
    }
  }, [disconnect, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={refetch} />;
  }

  if (!session) {
    return <LoadingSpinner />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('session.title')}
        </h1>
      </div>

      <Section header={t('session.status')}>
        <Card style={{ padding: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('session.isConnected')}</span>
            <strong style={{ color: session.isConnected ? 'var(--tg-theme-link-color)' : 'var(--tg-theme-hint-color)' }}>
              {session.isConnected ? t('session.yes') : t('session.no')}
            </strong>
          </div>
          {session.connectedAt && (
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span>{t('session.connectedAt')}</span>
              <strong>{new Date(session.connectedAt).toLocaleString()}</strong>
            </div>
          )}
          {session.lastActivity && (
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>{t('session.lastActivity')}</span>
              <strong>{new Date(session.lastActivity).toLocaleString()}</strong>
            </div>
          )}
        </Card>
      </Section>

      <div style={{ padding: '16px 0' }}>
        {isConnecting ? (
          <LoadingSpinner />
        ) : session.isConnected ? (
          <Button size="l" stretched mode="outline" onClick={handleDisconnect}>
            {t('session.disconnect')}
          </Button>
        ) : (
          <Button size="l" stretched onClick={handleConnect}>
            {t('session.connect')}
          </Button>
        )}
      </div>
    </div>
  );
}
