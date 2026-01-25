import client from './client';
import type { GatedFeature } from '@/types';

export async function getFeatures(chatId: number): Promise<GatedFeature[]> {
  return client.get(`chats/${chatId}/features`).json<GatedFeature[]>();
}
