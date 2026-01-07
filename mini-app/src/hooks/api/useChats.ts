import { useState, useEffect, useCallback } from 'react';
import { getChats } from '@/api';
import type { Chat } from '@/types';
import { useChatStore } from '@/stores/chatStore';

interface UseChatsResult {
  chats: Chat[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
}

export function useChats(): UseChatsResult {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const chats = useChatStore(s => s.chats);
  const setChats = useChatStore(s => s.setChats);

  const fetchChats = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getChats();
      setChats(data);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [setChats]);

  useEffect(() => {
    fetchChats();
  }, [fetchChats]);

  return {
    chats,
    isLoading,
    error,
    refetch: fetchChats,
  };
}
