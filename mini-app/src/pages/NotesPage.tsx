import { useParams, Navigate } from 'react-router-dom';
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Textarea, Section, Card, Modal } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { CustomBackButton } from '@/components/common/CustomBackButton';
import { useNotes } from '@/hooks/api/useNotes';
import { useNotification } from '@/hooks/ui/useNotification';
import { useConfirmDialog } from '@/hooks/ui/useConfirmDialog';

export function NotesPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const numericChatId = Number(chatId);

  const { data: notes, isLoading, isOperating, error, add, update, remove, refetch } = useNotes(numericChatId);
  const { showSuccess, showError } = useNotification();
  const { confirm } = useConfirmDialog();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingNoteId, setEditingNoteId] = useState<number | null>(null);
  const [noteName, setNoteName] = useState('');
  const [noteContent, setNoteContent] = useState('');

  const handleAdd = useCallback(() => {
    setEditingNoteId(null);
    setNoteName('');
    setNoteContent('');
    setIsModalOpen(true);
  }, []);

  const handleEdit = useCallback((note: { id: number; noteName: string; content: string }) => {
    setEditingNoteId(note.id);
    setNoteName(note.noteName);
    setNoteContent(note.content);
    setIsModalOpen(true);
  }, []);

  const handleSave = useCallback(async () => {
    if (!noteName.trim() || !noteContent.trim()) {
      showError(t('notes.emptyFields'));
      return;
    }

    try {
      if (editingNoteId) {
        await update(editingNoteId, { noteName, content: noteContent });
        showSuccess(t('notes.updateSuccess'));
      } else {
        await add({ noteName, content: noteContent });
        showSuccess(t('notes.addSuccess'));
      }
      setIsModalOpen(false);
    } catch (err) {
      showError((err as Error).message || t('notes.saveError'));
    }
  }, [editingNoteId, noteName, noteContent, add, update, showSuccess, showError, t]);

  const handleDelete = useCallback(async (noteId: number) => {
    const confirmed = await confirm(t('notes.deleteConfirm'), t('notes.deleteTitle'));
    if (!confirmed) return;

    try {
      await remove(noteId);
      showSuccess(t('notes.deleteSuccess'));
    } catch (err) {
      showError((err as Error).message || t('notes.deleteError'));
    }
  }, [remove, confirm, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <CustomBackButton to={`/chat/${chatId}/settings`} />
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('notes.title')}
        </h1>
        <Button size="s" onClick={handleAdd}>
          {t('notes.addNote')}
        </Button>
      </div>

      {notes.length === 0 ? (
        <EmptyState
          title={t('notes.noNotes')}
          description={t('notes.noNotesDescription')}
        />
      ) : (
        <Section>
          {notes.map((note) => (
            <Card key={note.id} style={{ padding: '12px', marginBottom: '8px' }}>
              <div style={{ marginBottom: '8px' }}>
                <strong>{note.noteName}</strong>
              </div>
              <div style={{ fontSize: '14px', marginBottom: '8px', whiteSpace: 'pre-wrap' }}>
                {note.content}
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <Button size="s" mode="plain" onClick={() => handleEdit(note)}>
                  {t('common.edit')}
                </Button>
                <Button size="s" mode="outline" onClick={() => handleDelete(note.id)}>
                  {t('common.delete')}
                </Button>
              </div>
            </Card>
          ))}
        </Section>
      )}

      {isModalOpen && (
        <Modal
          open={isModalOpen}
          onOpenChange={setIsModalOpen}
          header={editingNoteId ? t('notes.editNote') : t('notes.addNote')}
        >
          <div style={{ padding: '16px' }}>
            <label style={{ display: 'block', marginBottom: '8px' }}>
              {t('notes.noteName')}
            </label>
            <Input
              placeholder={t('notes.noteNamePlaceholder')}
              value={noteName}
              onChange={(e) => setNoteName(e.target.value)}
              style={{ marginBottom: '16px' }}
            />

            <label style={{ display: 'block', marginBottom: '8px' }}>
              {t('notes.noteContent')}
            </label>
            <Textarea
              placeholder={t('notes.noteContentPlaceholder')}
              value={noteContent}
              onChange={(e) => setNoteContent(e.target.value)}
              rows={6}
              style={{ marginBottom: '16px' }}
            />

            <div style={{ display: 'flex', gap: '8px' }}>
              {isOperating ? (
                <LoadingSpinner />
              ) : (
                <>
                  <Button size="l" stretched onClick={handleSave}>
                    {t('common.save')}
                  </Button>
                  <Button size="l" mode="plain" onClick={() => setIsModalOpen(false)}>
                    {t('common.cancel')}
                  </Button>
                </>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
