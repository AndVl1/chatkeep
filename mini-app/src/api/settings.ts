import client from './client';
import type { ChatSettings } from '@/types';

export async function getSettings(chatId: number): Promise<ChatSettings> {
  return client.get(`chats/${chatId}/settings`).json<ChatSettings>();
}

export async function updateSettings(
  chatId: number,
  updates: Partial<ChatSettings>
): Promise<ChatSettings> {
  return client
    .put(`chats/${chatId}/settings`, { json: updates })
    .json<ChatSettings>();
}
