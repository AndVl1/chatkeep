import { useState, useEffect, useCallback } from 'react';
import {
  getChannelReply,
  updateChannelReply,
  uploadMedia as uploadMediaApi,
  deleteMedia as deleteMediaApi,
} from '@/api';
import type { ChannelReply, UpdateChannelReplyRequest } from '@/types';
import { useChannelReplyStore } from '@/stores/channelReplyStore';

interface UseChannelReplyResult {
  data: ChannelReply | null;
  isLoading: boolean;
  isSaving: boolean;
  isUploading: boolean;
  isDeleting: boolean;
  error: Error | null;
  mutate: (updates: UpdateChannelReplyRequest) => Promise<void>;
  refetch: () => Promise<void>;
  uploadMedia: (file: File) => Promise<void>;
  deleteMedia: () => Promise<void>;
}

export function useChannelReply(chatId: number): UseChannelReplyResult {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const setChannelReply = useChannelReplyStore(s => s.setChannelReply);
  const cachedChannelReply = useChannelReplyStore(s => s.getChannelReply(chatId));

  const [data, setData] = useState<ChannelReply | null>(cachedChannelReply || null);

  const fetchChannelReply = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const channelReply = await getChannelReply(chatId);
      setChannelReply(chatId, channelReply);
      setData(channelReply);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId, setChannelReply]);

  useEffect(() => {
    // Always fetch fresh data when chatId changes or on mount
    // This ensures we get the latest data even if settings were changed via bot chat
    fetchChannelReply();
  }, [chatId, fetchChannelReply]);

  const mutate = useCallback(async (updates: UpdateChannelReplyRequest) => {
    if (!data) return;

    // Store original data for rollback
    const previousData = data;

    // Optimistic update
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateChannelReply(chatId, updates);
      setChannelReply(chatId, updated);
      setData(updated);
    } catch (err) {
      // Rollback on error
      setData(previousData);
      setChannelReply(chatId, previousData);
      throw err;
    } finally {
      setIsSaving(false);
    }
  }, [chatId, data, setChannelReply]);

  const uploadMedia = useCallback(async (file: File) => {
    if (!data) return;

    // Store original data for rollback
    const previousData = data;

    try {
      setIsUploading(true);
      const response = await uploadMediaApi(chatId, file);

      // Update state with new media
      const updated: ChannelReply = {
        ...data,
        mediaFileId: response.fileId,
        mediaType: response.mediaType,
      };
      setChannelReply(chatId, updated);
      setData(updated);
    } catch (err) {
      // Rollback on error
      setData(previousData);
      setChannelReply(chatId, previousData);
      throw err;
    } finally {
      setIsUploading(false);
    }
  }, [chatId, data, setChannelReply]);

  const deleteMedia = useCallback(async () => {
    if (!data) return;

    // Store original data for rollback
    const previousData = data;

    // Optimistic update
    const optimisticData: ChannelReply = {
      ...data,
      mediaFileId: null,
      mediaType: null,
    };
    setData(optimisticData);

    try {
      setIsDeleting(true);
      await deleteMediaApi(chatId);
      setChannelReply(chatId, optimisticData);
    } catch (err) {
      // Rollback on error
      setData(previousData);
      setChannelReply(chatId, previousData);
      throw err;
    } finally {
      setIsDeleting(false);
    }
  }, [chatId, data, setChannelReply]);

  return {
    data,
    isLoading,
    isSaving,
    isUploading,
    isDeleting,
    error,
    mutate,
    refetch: fetchChannelReply,
    uploadMedia,
    deleteMedia,
  };
}
