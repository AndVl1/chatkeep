import { useState, useEffect, useCallback } from 'react';
import { getWelcomeMessage, updateWelcomeMessage } from '@/api';
import type { WelcomeMessage, UpdateWelcomeMessageRequest } from '@/types';

interface UseWelcomeMessageResult {
  data: WelcomeMessage | null;
  isLoading: boolean;
  isSaving: boolean;
  error: Error | null;
  mutate: (updates: UpdateWelcomeMessageRequest) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useWelcomeMessage(chatId: number): UseWelcomeMessageResult {
  const [data, setData] = useState<WelcomeMessage | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchWelcomeMessage = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const message = await getWelcomeMessage(chatId);
      setData(message);
    } catch (err) {
      // If 404 (welcome settings not found), return default settings - not an error
      const apiError = err as { status?: number };
      if (apiError.status === 404) {
        setData({
          enabled: false,
          messageText: null,
          sendToChat: true,
          deleteAfterSeconds: null,
        });
      } else {
        setError(err as Error);
      }
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchWelcomeMessage();
  }, [fetchWelcomeMessage]);

  const mutate = useCallback(async (updates: UpdateWelcomeMessageRequest) => {
    if (!data) return;

    // Capture original before optimistic update to avoid stale closure
    const originalData = data;
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateWelcomeMessage(chatId, updates);
      setData(updated);
    } catch (err) {
      // Rollback to captured original
      setData(originalData);
      throw err;
    } finally {
      setIsSaving(false);
    }
  }, [chatId, data]);

  return {
    data,
    isLoading,
    isSaving,
    error,
    mutate,
    refetch: fetchWelcomeMessage,
  };
}
