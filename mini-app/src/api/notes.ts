import client from './client';
import type { Note, AddNoteRequest, UpdateNoteRequest } from '@/types';

export async function getNotes(chatId: number): Promise<Note[]> {
  return client.get(`chats/${chatId}/notes`).json<Note[]>();
}

export async function addNote(chatId: number, data: AddNoteRequest): Promise<Note> {
  return client.post(`chats/${chatId}/notes`, { json: data }).json<Note>();
}

export async function updateNote(chatId: number, noteId: number, data: UpdateNoteRequest): Promise<Note> {
  return client.put(`chats/${chatId}/notes/${noteId}`, { json: data }).json<Note>();
}

export async function deleteNote(chatId: number, noteId: number): Promise<void> {
  await client.delete(`chats/${chatId}/notes/${noteId}`);
}
