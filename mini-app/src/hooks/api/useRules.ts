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
      // If 404 (rules not found), treat as empty rules - not an error
      const httpError = err as { response?: { status?: number } };
      if (httpError.response?.status === 404) {
        // Return empty rules object so user can create new rules
        setData({ chatId, rulesText: null });
      } else {
        setError(err as Error);
      }
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchRules();
  }, [fetchRules]);

  const mutate = useCallback(async (updates: UpdateRulesRequest) => {
    const previousData = data;

    // Optimistic update (use empty rules if data is null)
    const optimisticData = { chatId, rulesText: null, ...previousData, ...updates };
    setData(optimisticData);

    try {
      setIsSaving(true);
      const updated = await updateRules(chatId, updates);
      setData(updated);
    } catch (err) {
      // Rollback on error
      setData(previousData);
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
