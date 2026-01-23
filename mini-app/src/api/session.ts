import client from './client';
import type { AdminSession } from '@/types';

export async function getSession(chatId: number): Promise<AdminSession> {
  return client.get(`chats/${chatId}/session`).json<AdminSession>();
}

export async function connectSession(chatId: number): Promise<AdminSession> {
  return client.post(`chats/${chatId}/session/connect`).json<AdminSession>();
}

export async function disconnectSession(chatId: number): Promise<void> {
  await client.delete(`chats/${chatId}/session`);
}
