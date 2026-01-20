import { useState, useCallback } from 'react';
import {
  warnUser,
  muteUser,
  banUser,
  kickUser,
  clearWarnings,
  unmuteUser,
  unbanUser,
} from '@/api';
import type { ModerationAction, ModerationResponse } from '@/types';

interface UseModerationResult {
  isExecuting: boolean;
  error: Error | null;
  warn: (chatId: number, action: ModerationAction) => Promise<ModerationResponse>;
  mute: (chatId: number, action: ModerationAction) => Promise<ModerationResponse>;
  ban: (chatId: number, action: ModerationAction) => Promise<ModerationResponse>;
  kick: (chatId: number, action: ModerationAction) => Promise<ModerationResponse>;
  clearWarnings: (chatId: number, userId: number) => Promise<ModerationResponse>;
  unmute: (chatId: number, userId: number) => Promise<ModerationResponse>;
  unban: (chatId: number, userId: number) => Promise<ModerationResponse>;
}

export function useModeration(): UseModerationResult {
  const [isExecuting, setIsExecuting] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const execute = useCallback(async <T>(action: () => Promise<T>): Promise<T> => {
    try {
      setIsExecuting(true);
      setError(null);
      return await action();
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsExecuting(false);
    }
  }, []);

  return {
    isExecuting,
    error,
    warn: useCallback((chatId, action) => execute(() => warnUser(chatId, action)), [execute]),
    mute: useCallback((chatId, action) => execute(() => muteUser(chatId, action)), [execute]),
    ban: useCallback((chatId, action) => execute(() => banUser(chatId, action)), [execute]),
    kick: useCallback((chatId, action) => execute(() => kickUser(chatId, action)), [execute]),
    clearWarnings: useCallback((chatId, userId) => execute(() => clearWarnings(chatId, userId)), [execute]),
    unmute: useCallback((chatId, userId) => execute(() => unmuteUser(chatId, userId)), [execute]),
    unban: useCallback((chatId, userId) => execute(() => unbanUser(chatId, userId)), [execute]),
  };
}
