import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { BlocklistList } from '@/components/blocklist/BlocklistList';
import { AddPatternForm } from '@/components/blocklist/AddPatternForm';
import { AdminWarningBanner } from '@/components/common/AdminWarningBanner';
import { useBlocklist } from '@/hooks/api/useBlocklist';
import { useConfirmDialog } from '@/hooks/ui/useConfirmDialog';
import { useNotification } from '@/hooks/ui/useNotification';
import { useSelectedChat } from '@/stores/chatStore';

export function BlocklistPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const [isAdding, setIsAdding] = useState(false);
  const { patterns, isLoading, error, addPattern, removePattern, refetch } = useBlocklist(numericChatId);
  const { confirm } = useConfirmDialog();
  const { showError } = useNotification();
  const selectedChat = useSelectedChat();

  const handleDelete = useCallback(async (patternId: number) => {
    const confirmed = await confirm(
      t('blocklist.deleteConfirm'),
      t('blocklist.deleteTitle')
    );

    if (!confirmed) return;

    try {
      await removePattern(patternId);
    } catch (err) {
      showError((err as Error).message || t('blocklist.deleteError'));
    }
  }, [removePattern, confirm, showError, t]);

  const handleAdd = useCallback(async (pattern: Parameters<typeof addPattern>[0]) => {
    try {
      await addPattern(pattern);
      setIsAdding(false);
    } catch (err) {
      showError((err as Error).message || t('blocklist.addError'));
      throw err;
    }
  }, [addPattern, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('settings.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('blocklist.title')}
        </h1>
      </div>

      {selectedChat && !selectedChat.isBotAdmin && <AdminWarningBanner />}

      {isAdding ? (
        <AddPatternForm
          onAdd={handleAdd}
          onCancel={() => setIsAdding(false)}
        />
      ) : (
        <>
          <div style={{ marginBottom: '16px' }}>
            <Button size="l" stretched onClick={() => setIsAdding(true)}>
              {t('blocklist.addPattern')}
            </Button>
          </div>

          <BlocklistList
            patterns={patterns}
            isLoading={isLoading}
            error={error}
            onDelete={handleDelete}
            onRetry={refetch}
          />
        </>
      )}
    </div>
  );
}
