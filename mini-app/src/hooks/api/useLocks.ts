import { useState, useEffect, useCallback } from 'react';
import { getLocks, updateLocks } from '@/api';
import type { LockSettings } from '@/types';
import { useLocksStore } from '@/stores/locksStore';

interface UseLocksResult {
  data: LockSettings | null;
  isLoading: boolean;
  isSaving: boolean;
  error: Error | null;
  toggleLock: (lockType: string, locked: boolean) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useLocks(chatId: number): UseLocksResult {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const setLocks = useLocksStore(s => s.setLocks);
  const cachedLocks = useLocksStore(s => s.getLocks(chatId));

  const [data, setData] = useState<LockSettings | null>(cachedLocks || null);

  const fetchLocks = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const locks = await getLocks(chatId);
      setLocks(chatId, locks);
      setData(locks);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId, setLocks]);

  // Initial load: fetch if no cache, otherwise use cache
  useEffect(() => {
    if (!cachedLocks) {
      fetchLocks();
    } else {
      setData(cachedLocks);
      setIsLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chatId]); // Intentionally not including cachedLocks/fetchLocks - only run on mount or chatId change

  const toggleLock = useCallback(async (lockType: string, locked: boolean) => {
    try {
      setIsSaving(true);
      setError(null);
      const updated = await updateLocks(chatId, {
        locks: { [lockType]: { locked } }
      });
      setLocks(chatId, updated);
      setData(updated);
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsSaving(false);
    }
  }, [chatId, setLocks]);

  return {
    data,
    isLoading,
    isSaving,
    error,
    toggleLock,
    refetch: fetchLocks,
  };
}
