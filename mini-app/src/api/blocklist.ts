import client from './client';
import type { BlocklistPattern, AddBlocklistPatternRequest } from '@/types';

export async function getBlocklist(chatId: number): Promise<BlocklistPattern[]> {
  return client.get(`chats/${chatId}/blocklist`).json<BlocklistPattern[]>();
}

export async function addBlocklistPattern(
  chatId: number,
  pattern: AddBlocklistPatternRequest
): Promise<BlocklistPattern> {
  return client
    .post(`chats/${chatId}/blocklist`, { json: pattern })
    .json<BlocklistPattern>();
}

export async function deleteBlocklistPattern(
  chatId: number,
  patternId: number
): Promise<void> {
  await client.delete(`chats/${chatId}/blocklist/${patternId}`);
}
