import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { SettingsForm } from '@/components/settings/SettingsForm';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { AdminWarningBanner } from '@/components/common/AdminWarningBanner';
import { useSettings } from '@/hooks/api/useSettings';
import { useNotification } from '@/hooks/ui/useNotification';
import { useSelectedChat } from '@/stores/chatStore';
import type { ChatSettings } from '@/types';

export function SettingsPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: settings, isLoading, isSaving, error, mutate, refetch } = useSettings(numericChatId);
  const { showError } = useNotification();
  const selectedChat = useSelectedChat();

  const handleChange = useCallback(async (updates: Partial<ChatSettings>) => {
    try {
      await mutate(updates);
    } catch (err) {
      showError((err as Error).message || t('settings.saveError'));
    }
  }, [mutate, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !settings) {
    return <ErrorState error={error || new Error('Failed to load settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate('/')}>
          {t('settings.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {settings.chatTitle}
        </h1>
      </div>

      {selectedChat && !selectedChat.isBotAdmin && <AdminWarningBanner />}

      <SettingsForm
        settings={settings}
        onChange={handleChange}
        disabled={isSaving}
      />

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/blocklist`)}
        >
          {t('settings.manageBlocklist')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/locks`)}
        >
          {t('settings.configureLocks')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/channel-reply`)}
        >
          {t('settings.channelReply')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/statistics`)}
        >
          {t('settings.statistics')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/welcome`)}
        >
          {t('settings.welcome')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/rules`)}
        >
          {t('settings.rules')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/notes`)}
        >
          {t('settings.notes')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/antiflood`)}
        >
          {t('settings.antiflood')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/admin-logs`)}
        >
          {t('settings.adminLogs')}
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/session`)}
        >
          {t('settings.session')}
        </Button>
      </div>
    </div>
  );
}
