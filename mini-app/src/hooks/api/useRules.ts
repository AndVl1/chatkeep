import { useState, useEffect, useCallback } from 'react';
import { getRules, updateRules } from '@/api';
import type { ChatRules, UpdateRulesRequest } from '@/types';

interface UseRulesResult {
  data: ChatRules | null;
  isLoading: boolean;
  isSaving: boolean;
  error: Error | null;
  mutate: (updates: UpdateRulesRequest) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useRules(chatId: number): UseRulesResult {
  const [data, setData] = useState<ChatRules | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchRules = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const rules = await getRules(chatId);
      setData(rules);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchRules();
  }, [fetchRules]);

  const mutate = useCallback(async (updates: UpdateRulesRequest) => {
    if (!data) return;

    // Optimistic update
    const optimisticData = { ...data, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateRules(chatId, updates);
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
    refetch: fetchRules,
  };
}
