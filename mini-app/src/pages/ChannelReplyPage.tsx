import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { ChannelReplyForm } from '@/components/channel-reply/ChannelReplyForm';
import { NoLinkedChannelWarning } from '@/components/channel-reply/NoLinkedChannelWarning';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { AdminWarningBanner } from '@/components/common/AdminWarningBanner';
import { useChannelReply } from '@/hooks/api/useChannelReply';
import { useNotification } from '@/hooks/ui/useNotification';
import { useDebouncedValue } from '@/hooks/ui/useDebouncedValue';
import { useSelectedChat } from '@/stores/chatStore';
import type { ChannelReply } from '@/types';

export function ChannelReplyPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const {
    data: channelReply,
    isLoading,
    isSaving,
    isUploading,
    isDeleting,
    error,
    mutate,
    refetch,
    uploadMedia,
    deleteMedia,
  } = useChannelReply(numericChatId);

  const { showError, showSuccess } = useNotification();
  const selectedChat = useSelectedChat();

  // Local state for text with debounce
  const [localText, setLocalText] = useState('');
  const debouncedText = useDebouncedValue(localText, 500);

  // Track if user has edited text (to distinguish user edits from data sync)
  const userHasEdited = useRef(false);
  // Track the last synced chatId to detect chat switches
  const lastSyncedChatId = useRef<number | null>(null);

  // Sync local text with channelReply data on load or chat switch
  useEffect(() => {
    if (channelReply && lastSyncedChatId.current !== numericChatId) {
      setLocalText(channelReply.replyText ?? '');
      userHasEdited.current = false;
      lastSyncedChatId.current = numericChatId;
    }
  }, [channelReply, numericChatId]);

  // Handle user text input
  const handleTextChange = useCallback((text: string) => {
    setLocalText(text);
    userHasEdited.current = true;
  }, []);

  const handleChange = useCallback(async (updates: Partial<ChannelReply>) => {
    try {
      await mutate(updates);
    } catch (err) {
      showError((err as Error).message || t('channelReply.saveError'));
    }
  }, [mutate, showError, t]);

  // Auto-save debounced text changes - only if user has edited
  useEffect(() => {
    if (!userHasEdited.current) return;
    if (!channelReply) return;
    if (debouncedText === channelReply.replyText) return;

    handleChange({ replyText: debouncedText });
  }, [debouncedText, channelReply, handleChange]);

  const handleMediaUpload = useCallback(async (file: File) => {
    try {
      await uploadMedia(file);
      showSuccess(t('channelReply.uploadSuccess'));
    } catch (err) {
      showError((err as Error).message || t('channelReply.uploadError'));
      // Don't re-throw - error already handled
    }
  }, [uploadMedia, showSuccess, showError, t]);

  const handleMediaDelete = useCallback(async () => {
    try {
      await deleteMedia();
      showSuccess(t('channelReply.deleteSuccess'));
    } catch (err) {
      showError((err as Error).message || t('channelReply.deleteError'));
      // Don't re-throw - error already handled
    }
  }, [deleteMedia, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !channelReply) {
    return <ErrorState error={error || new Error('Failed to load channel reply settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('settings.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('channelReply.pageTitle')}
        </h1>
      </div>

      {selectedChat && !selectedChat.isBotAdmin && <AdminWarningBanner />}

      {channelReply.linkedChannel === null && <NoLinkedChannelWarning />}

      <ChannelReplyForm
        data={channelReply}
        localText={localText}
        onTextChange={handleTextChange}
        onChange={handleChange}
        onMediaUpload={handleMediaUpload}
        onMediaDelete={handleMediaDelete}
        disabled={isSaving || channelReply.linkedChannel === null}
        isUploading={isUploading}
        isDeleting={isDeleting}
      />
    </div>
  );
}
