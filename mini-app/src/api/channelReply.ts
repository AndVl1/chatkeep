import client from './client';
import type {
  ChannelReply,
  UpdateChannelReplyRequest,
  MediaUploadResponse,
} from '@/types';

export async function getChannelReply(chatId: number): Promise<ChannelReply> {
  return client.get(`chats/${chatId}/channel-reply`).json<ChannelReply>();
}

export async function updateChannelReply(
  chatId: number,
  updates: UpdateChannelReplyRequest
): Promise<ChannelReply> {
  return client.put(`chats/${chatId}/channel-reply`, { json: updates }).json<ChannelReply>();
}

export async function uploadMedia(chatId: number, file: File): Promise<MediaUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  return client.post(`chats/${chatId}/channel-reply/media`, { body: formData }).json<MediaUploadResponse>();
}

export async function deleteMedia(chatId: number): Promise<void> {
  await client.delete(`chats/${chatId}/channel-reply/media`);
}
