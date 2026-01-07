import { useState, useEffect, useCallback } from 'react';
import { getSettings, updateSettings } from '@/api';
import type { ChatSettings } from '@/types';
import { useSettingsStore } from '@/stores/settingsStore';

interface UseSettingsResult {
  data: ChatSettings | null;
  isLoading: boolean;
  isSaving: boolean;
  error: Error | null;
  mutate: (updates: Partial<ChatSettings>) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useSettings(chatId: number): UseSettingsResult {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const setSettings = useSettingsStore(s => s.setSettings);
  const cachedSettings = useSettingsStore(s => s.getSettings(chatId));

  const [data, setData] = useState<ChatSettings | null>(cachedSettings || null);

  const fetchSettings = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const settings = await getSettings(chatId);
      setSettings(chatId, settings);
      setData(settings);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId, setSettings]);

  useEffect(() => {
    if (!cachedSettings) {
      fetchSettings();
    } else {
      setData(cachedSettings);
      setIsLoading(false);
    }
  }, [chatId, fetchSettings]);

  const mutate = useCallback(async (updates: Partial<ChatSettings>) => {
    if (!data) return;

    // Optimistic update
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateSettings(chatId, updates);
      setSettings(chatId, updated);
      setData(updated);
    } catch (err) {
      // Rollback on error
      setData(data);
      throw err;
    } finally {
      setIsSaving(false);
    }
  }, [chatId, data, setSettings]);

  return {
    data,
    isLoading,
    isSaving,
    error,
    mutate,
    refetch: fetchSettings,
  };
}
