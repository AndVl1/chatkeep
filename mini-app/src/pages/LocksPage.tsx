import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { LocksGrid } from '@/components/locks/LocksGrid';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { AdminWarningBanner } from '@/components/common/AdminWarningBanner';
import { useLocks } from '@/hooks/api/useLocks';
import { useNotification } from '@/hooks/ui/useNotification';
import { useSelectedChat } from '@/stores/chatStore';
import type { LockType } from '@/types';

export function LocksPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: locks, isLoading, isSaving, error, toggleLock, refetch } = useLocks(numericChatId);
  const { showError } = useNotification();
  const selectedChat = useSelectedChat();

  const handleToggle = useCallback(async (lockType: LockType, locked: boolean) => {
    try {
      await toggleLock(lockType, locked);
    } catch (err) {
      showError((err as Error).message || t('locks.saveError'));
    }
  }, [toggleLock, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !locks) {
    return <ErrorState error={error || new Error('Failed to load locks')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('settings.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('locks.title')}
        </h1>
      </div>

      {selectedChat && !selectedChat.isBotAdmin && <AdminWarningBanner />}

      <LocksGrid
        locks={locks.locks}
        onToggle={handleToggle}
        disabled={isSaving}
      />
    </div>
  );
}
