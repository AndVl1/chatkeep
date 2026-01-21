import client from './client';
import type {
  ChatMember,
  ModerationAction,
  ModerationResponse,
  UserWarnings,
} from '@/types';

export async function getChatMembers(chatId: number): Promise<ChatMember[]> {
  return client.get(`chats/${chatId}/members`).json<ChatMember[]>();
}

export async function getUserWarnings(chatId: number, userId: number): Promise<UserWarnings> {
  return client.get(`chats/${chatId}/members/${userId}/warnings`).json<UserWarnings>();
}

export async function warnUser(chatId: number, action: ModerationAction): Promise<ModerationResponse> {
  return client.post(`chats/${chatId}/moderation/warn`, { json: action }).json<ModerationResponse>();
}

export async function muteUser(chatId: number, action: ModerationAction): Promise<ModerationResponse> {
  return client.post(`chats/${chatId}/moderation/mute`, { json: action }).json<ModerationResponse>();
}

export async function banUser(chatId: number, action: ModerationAction): Promise<ModerationResponse> {
  return client.post(`chats/${chatId}/moderation/ban`, { json: action }).json<ModerationResponse>();
}

export async function kickUser(chatId: number, action: ModerationAction): Promise<ModerationResponse> {
  return client.post(`chats/${chatId}/moderation/kick`, { json: action }).json<ModerationResponse>();
}

export async function clearWarnings(chatId: number, userId: number): Promise<ModerationResponse> {
  return client.delete(`chats/${chatId}/moderation/warnings/${userId}`).json<ModerationResponse>();
}

export async function unmuteUser(chatId: number, userId: number): Promise<ModerationResponse> {
  return client.delete(`chats/${chatId}/moderation/mute/${userId}`).json<ModerationResponse>();
}

export async function unbanUser(chatId: number, userId: number): Promise<ModerationResponse> {
  return client.delete(`chats/${chatId}/moderation/ban/${userId}`).json<ModerationResponse>();
}
