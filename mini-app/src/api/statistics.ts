import client from './client';
import type { ChatStatistics } from '@/types';

export async function getStatistics(chatId: number): Promise<ChatStatistics> {
  return client.get(`chats/${chatId}/stats`).json<ChatStatistics>();
}
