import client from './client';
import type { Chat } from '@/types';

export async function getChats(): Promise<Chat[]> {
  return client.get('chats').json<Chat[]>();
}
