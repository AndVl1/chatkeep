import client from './client';
import type { AntiFloodSettings, UpdateAntiFloodRequest } from '@/types';

export async function getAntiFloodSettings(chatId: number): Promise<AntiFloodSettings> {
  return client.get(`chats/${chatId}/antiflood`).json<AntiFloodSettings>();
}

export async function updateAntiFloodSettings(
  chatId: number,
  data: UpdateAntiFloodRequest
): Promise<AntiFloodSettings> {
  return client.put(`chats/${chatId}/antiflood`, { json: data }).json<AntiFloodSettings>();
}
