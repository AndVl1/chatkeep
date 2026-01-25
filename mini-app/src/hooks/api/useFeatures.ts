import { useState, useEffect, useCallback } from 'react';
import { getFeatures } from '@/api';
import type { GatedFeature } from '@/types';

interface UseFeaturesResult {
  features: GatedFeature[];
  isLoading: boolean;
  error: Error | null;
  hasFeature: (key: string) => boolean;
  refetch: () => Promise<void>;
}

export function useFeatures(chatId: number): UseFeaturesResult {
  const [features, setFeatures] = useState<GatedFeature[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchFeatures = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getFeatures(chatId);
      setFeatures(data);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchFeatures();
  }, [fetchFeatures]);

  const hasFeature = useCallback(
    (key: string) => {
      return features.some(f => f.key === key && f.enabled);
    },
    [features]
  );

  return {
    features,
    isLoading,
    error,
    hasFeature,
    refetch: fetchFeatures,
  };
}
