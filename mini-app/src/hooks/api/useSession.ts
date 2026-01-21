import { useState, useEffect, useCallback } from 'react';
import { getSession, connectSession, disconnectSession } from '@/api';
import type { AdminSession } from '@/types';

interface UseSessionResult {
  data: AdminSession | null;
  isLoading: boolean;
  isConnecting: boolean;
  error: Error | null;
  connect: () => Promise<void>;
  disconnect: () => Promise<void>;
  refetch: () => Promise<void>;
}

export function useSession(chatId: number): UseSessionResult {
  const [data, setData] = useState<AdminSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isConnecting, setIsConnecting] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchSession = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const session = await getSession(chatId);
      setData(session);
    } catch (err) {
      // If 404 (session not found), treat as not connected - not an error
      const httpError = err as { response?: { status?: number } };
      if (httpError.response?.status === 404) {
        // Return empty session object so user can connect
        setData({ chatId, isConnected: false, connectedAt: undefined, lastActivity: undefined });
      } else {
        setError(err as Error);
      }
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchSession();
  }, [fetchSession]);

  const connect = useCallback(async () => {
    try {
      setIsConnecting(true);
      setError(null);
      const session = await connectSession(chatId);
      setData(session);
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsConnecting(false);
    }
  }, [chatId]);

  const disconnect = useCallback(async () => {
    try {
      setIsConnecting(true);
      setError(null);
      await disconnectSession(chatId);
      await fetchSession();
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsConnecting(false);
    }
  }, [chatId, fetchSession]);

  return {
    data,
    isLoading,
    isConnecting,
    error,
    connect,
    disconnect,
    refetch: fetchSession,
  };
}
