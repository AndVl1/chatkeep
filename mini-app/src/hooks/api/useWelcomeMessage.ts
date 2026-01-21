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
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchWelcomeMessage();
  }, [fetchWelcomeMessage]);

  const mutate = useCallback(async (updates: UpdateWelcomeMessageRequest) => {
    if (!data) return;

    // Optimistic update
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateWelcomeMessage(chatId, updates);
      setData(updated);
    } catch (err) {
      // Rollback on error
      setData(data);
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
