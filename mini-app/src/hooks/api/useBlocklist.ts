import { useState, useEffect, useCallback } from 'react';
import { getBlocklist, addBlocklistPattern, deleteBlocklistPattern } from '@/api';
import type { BlocklistPattern, AddBlocklistPatternRequest } from '@/types';
import { useBlocklistStore } from '@/stores/blocklistStore';

interface UseBlocklistResult {
  patterns: BlocklistPattern[];
  isLoading: boolean;
  error: Error | null;
  addPattern: (pattern: AddBlocklistPatternRequest) => Promise<void>;
  removePattern: (patternId: number) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useBlocklist(chatId: number): UseBlocklistResult {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const patterns = useBlocklistStore(s => s.getPatterns(chatId));
  const setPatterns = useBlocklistStore(s => s.setPatterns);
  const addToStore = useBlocklistStore(s => s.addPattern);
  const removeFromStore = useBlocklistStore(s => s.removePattern);

  const fetchPatterns = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getBlocklist(chatId);
      setPatterns(chatId, data);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId, setPatterns]);

  useEffect(() => {
    fetchPatterns();
  }, [fetchPatterns]);

  const addPattern = useCallback(async (pattern: AddBlocklistPatternRequest) => {
    try {
      const newPattern = await addBlocklistPattern(chatId, pattern);
      addToStore(chatId, newPattern);
    } catch (err) {
      setError(err as Error);
      throw err;
    }
  }, [chatId, addToStore]);

  const removePattern = useCallback(async (patternId: number) => {
    try {
      await deleteBlocklistPattern(chatId, patternId);
      removeFromStore(chatId, patternId);
    } catch (err) {
      setError(err as Error);
      throw err;
    }
  }, [chatId, removeFromStore]);

  return {
    patterns,
    isLoading,
    error,
    addPattern,
    removePattern,
    refetch: fetchPatterns,
  };
}
