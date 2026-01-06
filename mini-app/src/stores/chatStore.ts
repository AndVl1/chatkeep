import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Chat } from '@/types';

interface ChatState {
  selectedChatId: number | null;
  chats: Chat[];

  setSelectedChat: (chatId: number | null) => void;
  setChats: (chats: Chat[]) => void;
  getSelectedChat: () => Chat | undefined;
}

export const useChatStore = create<ChatState>()(
  persist(
    (set, get) => ({
      selectedChatId: null,
      chats: [],

      setSelectedChat: (chatId) => set({ selectedChatId: chatId }),
      setChats: (chats) => set({ chats }),

      getSelectedChat: () => {
        const { selectedChatId, chats } = get();
        if (selectedChatId === null) return undefined;
        return chats.find(c => c.chatId === selectedChatId);
      },
    }),
    {
      name: 'chatkeep-chat-storage',
      partialize: (state) => ({ selectedChatId: state.selectedChatId }),
    }
  )
);

export const useSelectedChatId = () => useChatStore(s => s.selectedChatId);
export const useChats = () => useChatStore(s => s.chats);
export const useSelectedChat = () => useChatStore(s => s.getSelectedChat());
