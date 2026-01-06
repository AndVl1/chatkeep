import { create } from 'zustand';
import type { ChatSettings } from '@/types';

interface SettingsState {
  settingsCache: Record<number, ChatSettings>;
  pendingChanges: Record<number, Partial<ChatSettings>>;

  setSettings: (chatId: number, settings: ChatSettings) => void;
  updatePending: (chatId: number, updates: Partial<ChatSettings>) => void;
  getPending: (chatId: number) => Partial<ChatSettings> | undefined;
  clearPending: (chatId: number) => void;
  getSettings: (chatId: number) => ChatSettings | undefined;
}

export const useSettingsStore = create<SettingsState>((set, get) => ({
  settingsCache: {},
  pendingChanges: {},

  setSettings: (chatId, settings) => {
    set(state => ({
      settingsCache: { ...state.settingsCache, [chatId]: settings },
    }));
  },

  updatePending: (chatId, updates) => {
    set(state => ({
      pendingChanges: {
        ...state.pendingChanges,
        [chatId]: { ...state.pendingChanges[chatId], ...updates },
      },
    }));
  },

  getPending: (chatId) => {
    return get().pendingChanges[chatId];
  },

  clearPending: (chatId) => {
    set(state => {
      const newPending = { ...state.pendingChanges };
      delete newPending[chatId];
      return { pendingChanges: newPending };
    });
  },

  getSettings: (chatId) => {
    return get().settingsCache[chatId];
  },
}));
