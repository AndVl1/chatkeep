import client from './client';
import type { LockSettings, UpdateLocksRequest } from '@/types';

export async function getLocks(chatId: number): Promise<LockSettings> {
  return client.get(`chats/${chatId}/locks`).json<LockSettings>();
}

export async function updateLocks(
  chatId: number,
  updates: UpdateLocksRequest
): Promise<LockSettings> {
  return client
    .put(`chats/${chatId}/locks`, { json: updates })
    .json<LockSettings>();
}
