import { useState, useEffect, useCallback } from 'react';
import { getStatistics } from '@/api';
import type { ChatStatistics } from '@/types';

interface UseStatisticsResult {
  data: ChatStatistics | null;
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export function useStatistics(chatId: number): UseStatisticsResult {
  const [data, setData] = useState<ChatStatistics | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchStatistics = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const stats = await getStatistics(chatId);
      setData(stats);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchStatistics();
  }, [fetchStatistics]);

  return {
    data,
    isLoading,
    error,
    refetch: fetchStatistics,
  };
}
