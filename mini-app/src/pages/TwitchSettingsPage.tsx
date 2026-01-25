import { useCallback } from 'react';
import { useParams, Navigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { TwitchSettingsForm } from '@/components/twitch/TwitchSettingsForm';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { CustomBackButton } from '@/components/common/CustomBackButton';
import { useTwitch } from '@/hooks/api/useTwitch';
import { useNotification } from '@/hooks/ui/useNotification';
import type { AddTwitchChannelRequest } from '@/types';

export function TwitchSettingsPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const numericChatId = Number(chatId);

  const {
    channels,
    settings,
    isLoading,
    error,
    addChannel,
    removeChannel,
    updateSettings,
    refetch,
  } = useTwitch(numericChatId);

  const { showError } = useNotification();

  const handleAddChannel = useCallback(
    async (data: AddTwitchChannelRequest) => {
      try {
        await addChannel(data);
      } catch (err) {
        showError((err as Error).message || t('twitch.addChannelError'));
        throw err;
      }
    },
    [addChannel, showError, t]
  );

  const handleRemoveChannel = useCallback(
    async (channelId: number) => {
      try {
        await removeChannel(channelId);
      } catch (err) {
        showError((err as Error).message || t('twitch.removeChannelError'));
        throw err;
      }
    },
    [removeChannel, showError, t]
  );

  const handleUpdateSettings = useCallback(
    async (messageTemplate: string, endedMessageTemplate: string, buttonText: string) => {
      try {
        await updateSettings({ messageTemplate, endedMessageTemplate, buttonText });
      } catch (err) {
        showError((err as Error).message || t('twitch.updateSettingsError'));
        throw err;
      }
    },
    [updateSettings, showError, t]
  );

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !settings) {
    return <ErrorState error={error || new Error('Failed to load Twitch settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <CustomBackButton to={`/chat/${chatId}/settings`} />
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('twitch.title')}
        </h1>
      </div>

      <TwitchSettingsForm
        channels={channels}
        settings={settings}
        onAddChannel={handleAddChannel}
        onRemoveChannel={handleRemoveChannel}
        onUpdateSettings={handleUpdateSettings}
      />
    </div>
  );
}
