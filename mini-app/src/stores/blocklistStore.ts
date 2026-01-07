import { create } from 'zustand';
import type { BlocklistPattern } from '@/types';

interface BlocklistState {
  patternsCache: Record<number, BlocklistPattern[]>;

  setPatterns: (chatId: number, patterns: BlocklistPattern[]) => void;
  addPattern: (chatId: number, pattern: BlocklistPattern) => void;
  removePattern: (chatId: number, patternId: number) => void;
  getPatterns: (chatId: number) => BlocklistPattern[];
}

export const useBlocklistStore = create<BlocklistState>((set, get) => ({
  patternsCache: {},

  setPatterns: (chatId, patterns) => {
    set(state => ({
      patternsCache: { ...state.patternsCache, [chatId]: patterns },
    }));
  },

  addPattern: (chatId, pattern) => {
    set(state => {
      const existing = state.patternsCache[chatId] || [];
      return {
        patternsCache: {
          ...state.patternsCache,
          [chatId]: [pattern, ...existing],
        },
      };
    });
  },

  removePattern: (chatId, patternId) => {
    set(state => {
      const existing = state.patternsCache[chatId] || [];
      return {
        patternsCache: {
          ...state.patternsCache,
          [chatId]: existing.filter(p => p.id !== patternId),
        },
      };
    });
  },

  getPatterns: (chatId) => {
    return get().patternsCache[chatId] || [];
  },
}));
