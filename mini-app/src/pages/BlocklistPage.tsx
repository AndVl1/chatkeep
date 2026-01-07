import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useState, useCallback } from 'react';
import { Button } from '@telegram-apps/telegram-ui';
import { BlocklistList } from '@/components/blocklist/BlocklistList';
import { AddPatternForm } from '@/components/blocklist/AddPatternForm';
import { useBlocklist } from '@/hooks/api/useBlocklist';
import { useConfirmDialog } from '@/hooks/ui/useConfirmDialog';
import { useNotification } from '@/hooks/ui/useNotification';

export function BlocklistPage() {
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const [isAdding, setIsAdding] = useState(false);
  const { patterns, isLoading, error, addPattern, removePattern, refetch } = useBlocklist(numericChatId);
  const { confirm } = useConfirmDialog();
  const { showError } = useNotification();

  const handleDelete = useCallback(async (patternId: number) => {
    const confirmed = await confirm(
      'Are you sure you want to delete this pattern?',
      'Delete Pattern'
    );

    if (!confirmed) return;

    try {
      await removePattern(patternId);
    } catch (err) {
      showError((err as Error).message || 'Failed to delete pattern');
    }
  }, [removePattern, confirm, showError]);

  const handleAdd = useCallback(async (pattern: Parameters<typeof addPattern>[0]) => {
    try {
      await addPattern(pattern);
      setIsAdding(false);
    } catch (err) {
      showError((err as Error).message || 'Failed to add pattern');
      throw err;
    }
  }, [addPattern, showError]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          ← Back
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          Blocklist
        </h1>
      </div>

      {isAdding ? (
        <AddPatternForm
          onAdd={handleAdd}
          onCancel={() => setIsAdding(false)}
        />
      ) : (
        <>
          <div style={{ marginBottom: '16px' }}>
            <Button size="l" stretched onClick={() => setIsAdding(true)}>
              Add Pattern
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
