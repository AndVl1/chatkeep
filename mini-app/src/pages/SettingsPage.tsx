import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback } from 'react';
import { Button } from '@telegram-apps/telegram-ui';
import { SettingsForm } from '@/components/settings/SettingsForm';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useSettings } from '@/hooks/api/useSettings';
import { useNotification } from '@/hooks/ui/useNotification';
import type { ChatSettings } from '@/types';

export function SettingsPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: settings, isLoading, isSaving, error, mutate, refetch } = useSettings(numericChatId);
  const { showError } = useNotification();

  const handleChange = useCallback(async (updates: Partial<ChatSettings>) => {
    try {
      await mutate(updates);
    } catch (err) {
      showError((err as Error).message || 'Failed to save settings');
    }
  }, [mutate, showError]);

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
          ← Back
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {settings.chatTitle}
        </h1>
      </div>

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
          Manage Blocklist
        </Button>
      </div>

      <div style={{ padding: '16px 0' }}>
        <Button
          size="l"
          stretched
          onClick={() => navigate(`/chat/${chatId}/locks`)}
        >
          Configure Locks
        </Button>
      </div>
    </div>
  );
}
