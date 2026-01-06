import { create } from 'zustand';
import type { LockSettings } from '@/types';

interface LocksState {
  locksCache: Record<number, LockSettings>;

  setLocks: (chatId: number, locks: LockSettings) => void;
  getLocks: (chatId: number) => LockSettings | undefined;
}

export const useLocksStore = create<LocksState>((set, get) => ({
  locksCache: {},

  setLocks: (chatId, locks) => {
    set(state => ({
      locksCache: { ...state.locksCache, [chatId]: locks },
    }));
  },

  getLocks: (chatId) => {
    return get().locksCache[chatId];
  },
}));
