import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useMemo } from 'react';
import { Button } from '@telegram-apps/telegram-ui';
import { SettingsForm } from '@/components/settings/SettingsForm';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useSettings } from '@/hooks/api/useSettings';
import { useMainButton } from '@/hooks/telegram/useMainButton';
import { useNotification } from '@/hooks/ui/useNotification';
import { useSettingsStore } from '@/stores/settingsStore';
import type { ChatSettings } from '@/types';

export function SettingsPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: settings, isLoading, isSaving, error, mutate, refetch } = useSettings(numericChatId);
  const { showSuccess, showError } = useNotification();
  const updatePending = useSettingsStore(s => s.updatePending);
  const clearPending = useSettingsStore(s => s.clearPending);
  const pendingChanges = useSettingsStore(s => s.pendingChanges[numericChatId]);

  const hasChanges = useMemo(() => !!pendingChanges && Object.keys(pendingChanges).length > 0, [pendingChanges]);

  const handleChange = useCallback((updates: Partial<ChatSettings>) => {
    updatePending(numericChatId, updates);
  }, [numericChatId, updatePending]);

  const handleSave = useCallback(async () => {
    if (!hasChanges || !pendingChanges) return;

    try {
      await mutate(pendingChanges);
      clearPending(numericChatId);
      showSuccess('Settings saved successfully');
    } catch (err) {
      showError((err as Error).message || 'Failed to save settings');
    }
  }, [hasChanges, pendingChanges, mutate, clearPending, numericChatId, showSuccess, showError]);

  useMainButton({
    text: 'Save Settings',
    onClick: handleSave,
    disabled: !hasChanges || isSaving,
    visible: hasChanges,
  });

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !settings) {
    return <ErrorState error={error || new Error('Failed to load settings')} onRetry={refetch} />;
  }

  const effectiveSettings = pendingChanges
    ? { ...settings, ...pendingChanges }
    : settings;

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
        settings={effectiveSettings}
        onChange={handleChange}
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
