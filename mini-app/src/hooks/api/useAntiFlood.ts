import { useState, useEffect, useCallback } from 'react';
import { getAntiFloodSettings, updateAntiFloodSettings } from '@/api';
import type { AntiFloodSettings, UpdateAntiFloodRequest } from '@/types';

interface UseAntiFloodResult {
  data: AntiFloodSettings | null;
  isLoading: boolean;
  isSaving: boolean;
  error: Error | null;
  mutate: (updates: UpdateAntiFloodRequest) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useAntiFlood(chatId: number): UseAntiFloodResult {
  const [data, setData] = useState<AntiFloodSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchSettings = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const settings = await getAntiFloodSettings(chatId);
      setData(settings);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const mutate = useCallback(async (updates: UpdateAntiFloodRequest) => {
    if (!data) return;

    // Optimistic update
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateAntiFloodSettings(chatId, updates);
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
    refetch: fetchSettings,
  };
}
