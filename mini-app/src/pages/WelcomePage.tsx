import { useParams, Navigate } from 'react-router-dom';
import { useCallback, useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Textarea, Section, Switch } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { CustomBackButton } from '@/components/common/CustomBackButton';
import { useWelcomeMessage } from '@/hooks/api/useWelcomeMessage';
import { useNotification } from '@/hooks/ui/useNotification';

export function WelcomePage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const numericChatId = Number(chatId);

  const { data: welcome, isLoading, isSaving, error, mutate, refetch } = useWelcomeMessage(numericChatId);
  const { showSuccess, showError } = useNotification();

  const [enabled, setEnabled] = useState(false);
  const [messageText, setMessageText] = useState('');
  const [sendToChat, setSendToChat] = useState(true);
  const [deleteAfterMinutes, setDeleteAfterMinutes] = useState('');

  useEffect(() => {
    if (welcome) {
      setEnabled(welcome.enabled);
      setMessageText(welcome.messageText || '');
      setSendToChat(welcome.sendToChat);
      // Convert seconds to minutes for UI
      const minutes = welcome.deleteAfterSeconds ? Math.floor(welcome.deleteAfterSeconds / 60) : null;
      setDeleteAfterMinutes(minutes?.toString() || '');
    }
  }, [welcome]);

  const handleSave = useCallback(async () => {
    try {
      // Convert minutes to seconds for API
      const deleteAfterSeconds = deleteAfterMinutes ? Number(deleteAfterMinutes) * 60 : null;

      await mutate({
        enabled,
        messageText: messageText || null,
        sendToChat,
        deleteAfterSeconds,
      });
      showSuccess(t('welcome.saveSuccess'));
    } catch (err) {
      showError((err as Error).message || t('welcome.saveError'));
    }
  }, [enabled, messageText, sendToChat, deleteAfterMinutes, mutate, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !welcome) {
    return <ErrorState error={error || new Error('Failed to load welcome settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <CustomBackButton to={`/chat/${chatId}/settings`} />
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('welcome.title')}
        </h1>
      </div>

      <Section>
        <div style={{ padding: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>{t('welcome.enabled')}</span>
          <Switch checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        </div>
      </Section>

      <Section header={t('welcome.welcomeMessage')}>
        <div style={{ padding: '12px' }}>
          <Textarea
            placeholder={t('welcome.welcomePlaceholder')}
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            rows={4}
          />
        </div>
      </Section>

      <Section header={t('welcome.options')}>
        <div style={{ padding: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div>
              <div>{t('welcome.sendToChat')}</div>
              <div style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)' }}>
                {t('welcome.sendToChatHint')}
              </div>
            </div>
            <Switch checked={sendToChat} onChange={(e) => setSendToChat(e.target.checked)} />
          </div>

          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('welcome.deleteAfter')}
          </label>
          <Input
            type="number"
            placeholder="0"
            value={deleteAfterMinutes}
            onChange={(e) => setDeleteAfterMinutes(e.target.value)}
            style={{ marginBottom: '4px' }}
          />
          <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '0' }}>
            {t('welcome.deleteAfterHint')}
          </p>
        </div>
      </Section>

      <div style={{ padding: '16px 0' }}>
        {isSaving ? (
          <LoadingSpinner />
        ) : (
          <Button size="l" stretched onClick={handleSave}>
            {t('common.save')}
          </Button>
        )}
      </div>
    </div>
  );
}
