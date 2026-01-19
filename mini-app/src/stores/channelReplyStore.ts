import { create } from 'zustand';
import type { ChannelReply } from '@/types';

interface ChannelReplyState {
  channelReplyCache: Record<number, ChannelReply>;
  setChannelReply: (chatId: number, channelReply: ChannelReply) => void;
  getChannelReply: (chatId: number) => ChannelReply | undefined;
}

export const useChannelReplyStore = create<ChannelReplyState>((set, get) => ({
  channelReplyCache: {},

  setChannelReply: (chatId, channelReply) => {
    set(state => ({
      channelReplyCache: { ...state.channelReplyCache, [chatId]: channelReply },
    }));
  },

  getChannelReply: (chatId) => {
    return get().channelReplyCache[chatId];
  },
}));
