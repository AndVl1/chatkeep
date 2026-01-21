import client from './client';
import type { WelcomeMessage, UpdateWelcomeMessageRequest } from '@/types';

export async function getWelcomeMessage(chatId: number): Promise<WelcomeMessage> {
  return client.get(`chats/${chatId}/welcome`).json<WelcomeMessage>();
}

export async function updateWelcomeMessage(
  chatId: number,
  data: UpdateWelcomeMessageRequest
): Promise<WelcomeMessage> {
  return client.put(`chats/${chatId}/welcome`, { json: data }).json<WelcomeMessage>();
}
