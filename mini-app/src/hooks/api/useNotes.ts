import { useState, useEffect, useCallback } from 'react';
import { getNotes, addNote, updateNote, deleteNote } from '@/api';
import type { Note, AddNoteRequest, UpdateNoteRequest } from '@/types';

interface UseNotesResult {
  data: Note[];
  isLoading: boolean;
  isOperating: boolean;
  error: Error | null;
  add: (note: AddNoteRequest) => Promise<void>;
  update: (noteId: number, updates: UpdateNoteRequest) => Promise<void>;
  remove: (noteId: number) => Promise<void>;
  refetch: () => Promise<void>;
}

export function useNotes(chatId: number): UseNotesResult {
  const [data, setData] = useState<Note[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isOperating, setIsOperating] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const fetchNotes = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const notes = await getNotes(chatId);
      setData(notes);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  useEffect(() => {
    fetchNotes();
  }, [fetchNotes]);

  const add = useCallback(async (note: AddNoteRequest) => {
    try {
      setIsOperating(true);
      setError(null);
      const newNote = await addNote(chatId, note);
      setData(prev => [...prev, newNote]);
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsOperating(false);
    }
  }, [chatId]);

  const update = useCallback(async (noteId: number, updates: UpdateNoteRequest) => {
    try {
      setIsOperating(true);
      setError(null);
      const updatedNote = await updateNote(chatId, noteId, updates);
      setData(prev => prev.map(note => note.id === noteId ? updatedNote : note));
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsOperating(false);
    }
  }, [chatId]);

  const remove = useCallback(async (noteId: number) => {
    try {
      setIsOperating(true);
      setError(null);
      await deleteNote(chatId, noteId);
      setData(prev => prev.filter(note => note.id !== noteId));
    } catch (err) {
      setError(err as Error);
      throw err;
    } finally {
      setIsOperating(false);
    }
  }, [chatId]);

  return {
    data,
    isLoading,
    isOperating,
    error,
    add,
    update,
    remove: remove,
    refetch: fetchNotes,
  };
}
