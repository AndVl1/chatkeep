import client from './client';
import type { ChatRules, UpdateRulesRequest } from '@/types';

export async function getRules(chatId: number): Promise<ChatRules> {
  return client.get(`chats/${chatId}/rules`).json<ChatRules>();
}

export async function updateRules(chatId: number, data: UpdateRulesRequest): Promise<ChatRules> {
  return client.put(`chats/${chatId}/rules`, { json: data }).json<ChatRules>();
}
