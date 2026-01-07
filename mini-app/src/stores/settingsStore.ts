import { create } from 'zustand';
import type { ChatSettings } from '@/types';

interface SettingsState {
  settingsCache: Record<number, ChatSettings>;
  setSettings: (chatId: number, settings: ChatSettings) => void;
  getSettings: (chatId: number) => ChatSettings | undefined;
}

export const useSettingsStore = create<SettingsState>((set, get) => ({
  settingsCache: {},

  setSettings: (chatId, settings) => {
    set(state => ({
      settingsCache: { ...state.settingsCache, [chatId]: settings },
    }));
  },

  getSettings: (chatId) => {
    return get().settingsCache[chatId];
  },
}));
