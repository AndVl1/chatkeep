import client from './client';
import type {
  TwitchChannel,
  TwitchSearchResult,
  TwitchSettings,
  AddTwitchChannelRequest,
  UpdateTwitchSettingsRequest,
} from '@/types';

export async function getTwitchChannels(chatId: number): Promise<TwitchChannel[]> {
  return client.get(`chats/${chatId}/twitch/channels`).json<TwitchChannel[]>();
}

export async function addTwitchChannel(
  chatId: number,
  data: AddTwitchChannelRequest
): Promise<TwitchChannel> {
  return client
    .post(`chats/${chatId}/twitch/channels`, { json: data })
    .json<TwitchChannel>();
}

export async function removeTwitchChannel(
  chatId: number,
  channelId: number
): Promise<void> {
  await client.delete(`chats/${chatId}/twitch/channels/${channelId}`);
}

export async function searchTwitchChannels(query: string): Promise<TwitchSearchResult[]> {
  return client
    .get('twitch/search', { searchParams: { query } })
    .json<TwitchSearchResult[]>();
}

export async function getTwitchSettings(chatId: number): Promise<TwitchSettings> {
  return client.get(`chats/${chatId}/twitch/settings`).json<TwitchSettings>();
}

export async function updateTwitchSettings(
  chatId: number,
  data: UpdateTwitchSettingsRequest
): Promise<TwitchSettings> {
  return client
    .put(`chats/${chatId}/twitch/settings`, { json: data })
    .json<TwitchSettings>();
}
