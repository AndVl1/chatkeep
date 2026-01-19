import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useChannelReply } from '@/hooks/api/useChannelReply';
import { useChannelReplyStore } from '@/stores/channelReplyStore';
import { setMockChannelReply, setMockError, clearMockError } from '@/test/mocks/handlers';
import type { ChannelReply } from '@/types';

describe('useChannelReply', () => {
  const chatId = 123;

  const mockChannelReply: ChannelReply = {
    enabled: true,
    replyText: 'Welcome to the channel!',
    mediaFileId: null,
    mediaType: null,
    buttons: [
      { text: 'Visit Website', url: 'https://example.com' },
    ],
  };

  beforeEach(() => {
    // Clear store before each test
    useChannelReplyStore.setState({
      channelReplyCache: {},
    });
    clearMockError();
    vi.clearAllMocks();
  });

  it('should return loading state initially', () => {
    setMockChannelReply(chatId, mockChannelReply);

    const { result } = renderHook(() => useChannelReply(chatId));

    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeNull();
  });

  it('should fetch channel reply data on mount', async () => {
    setMockChannelReply(chatId, mockChannelReply);

    const { result } = renderHook(() => useChannelReply(chatId));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.data).toEqual(mockChannelReply);
    expect(result.current.error).toBeNull();
  });

  it('should always fetch fresh data even if cache exists', async () => {
    // Pre-populate cache with old data
    const cachedData: ChannelReply = {
      ...mockChannelReply,
      replyText: 'Old cached text',
    };
    useChannelReplyStore.setState({
      channelReplyCache: { [chatId]: cachedData },
    });

    // Set up fresh data from server
    setMockChannelReply(chatId, mockChannelReply);

    const { result } = renderHook(() => useChannelReply(chatId));

    // Should be loading initially (fetching fresh data)
    expect(result.current.isLoading).toBe(true);

    // Wait for fetch to complete
    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    // Should have fresh data from server, not cached
    expect(result.current.data?.replyText).toBe('Welcome to the channel!');
  });

  it('should handle fetch error', async () => {
    setMockError('channel-reply', 500, 'Internal server error');

    const { result } = renderHook(() => useChannelReply(chatId));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toBeTruthy();
    expect(result.current.data).toBeNull();
  });

  describe('mutate', () => {
    it('should update channel reply data', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const updates = { enabled: false };
      await act(async () => {
        await result.current.mutate(updates);
      });

      await waitFor(() => {
        expect(result.current.data?.enabled).toBe(false);
      }, { timeout: 2000 });
    });

    it('should perform optimistic update', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;
      expect(originalData?.enabled).toBe(true);

      // Start mutation
      const mutatePromise = result.current.mutate({ enabled: false });

      // Check optimistic update happened immediately
      await waitFor(() => {
        expect(result.current.data?.enabled).toBe(false);
      });

      // Wait for mutation to complete
      await mutatePromise;

      expect(result.current.data?.enabled).toBe(false);
    });

    it('should rollback on error', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;

      // Set error for update
      setMockError('channel-reply', 500, 'Update failed');

      // Attempt mutation
      await expect(result.current.mutate({ enabled: false })).rejects.toThrow();

      // Data should be rolled back
      await waitFor(() => {
        expect(result.current.data).toEqual(originalData);
      }, { timeout: 2000 });
    });

    it('should update store cache on successful mutation', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      await result.current.mutate({ replyText: 'Updated text' });

      // Check store was updated
      const cachedChannelReply = useChannelReplyStore.getState().channelReplyCache[chatId];
      expect(cachedChannelReply?.replyText).toBe('Updated text');
    });

    it('should set isSaving flag during mutation', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isSaving).toBe(false);

      // Start mutation and check flag is set
      let mutatePromise: Promise<void>;
      act(() => {
        mutatePromise = result.current.mutate({ enabled: false });
      });

      // isSaving should be true immediately after starting
      expect(result.current.isSaving).toBe(true);

      await act(async () => {
        await mutatePromise!;
      });

      // isSaving should be false after completion
      expect(result.current.isSaving).toBe(false);
    });
  });

  describe('uploadMedia', () => {
    it('should upload media and update mediaFileId', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data?.mediaFileId).toBeNull();

      const mockFile = new File(['dummy'], 'test.jpg', { type: 'image/jpeg' });
      await result.current.uploadMedia(mockFile);

      await waitFor(() => {
        expect(result.current.data?.mediaFileId).toBeTruthy();
        expect(result.current.data?.mediaType).toBe('PHOTO');
      }, { timeout: 2000 });
    });

    it('should set isUploading flag during upload', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isUploading).toBe(false);

      const mockFile = new File(['dummy'], 'test.jpg', { type: 'image/jpeg' });

      // Start upload and check flag is set
      let uploadPromise: Promise<void>;
      act(() => {
        uploadPromise = result.current.uploadMedia(mockFile);
      });

      // isUploading should be true immediately after starting
      expect(result.current.isUploading).toBe(true);

      await act(async () => {
        await uploadPromise!;
      });

      // isUploading should be false after completion
      expect(result.current.isUploading).toBe(false);
    });

    it('should throw error on upload failure', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;

      setMockError('channel-reply-media', 413, 'File too large');

      const mockFile = new File(['dummy'], 'test.jpg', { type: 'image/jpeg' });
      await expect(result.current.uploadMedia(mockFile)).rejects.toThrow();

      // Data should be rolled back
      await waitFor(() => {
        expect(result.current.data).toEqual(originalData);
      }, { timeout: 2000 });
    });
  });

  describe('deleteMedia', () => {
    it('should delete media and clear mediaFileId', async () => {
      setMockChannelReply(chatId, {
        ...mockChannelReply,
        mediaFileId: 'file-123',
        mediaType: 'PHOTO',
      });

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data?.mediaFileId).toBe('file-123');

      await result.current.deleteMedia();

      await waitFor(() => {
        expect(result.current.data?.mediaFileId).toBeNull();
        expect(result.current.data?.mediaType).toBeNull();
      }, { timeout: 2000 });
    });

    it('should set isDeleting flag during deletion', async () => {
      setMockChannelReply(chatId, {
        ...mockChannelReply,
        mediaFileId: 'file-123',
        mediaType: 'PHOTO',
      });

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isDeleting).toBe(false);

      // Start deletion and check flag is set
      let deletePromise: Promise<void>;
      act(() => {
        deletePromise = result.current.deleteMedia();
      });

      // isDeleting should be true immediately after starting
      expect(result.current.isDeleting).toBe(true);

      await act(async () => {
        await deletePromise!;
      });

      // isDeleting should be false after completion
      expect(result.current.isDeleting).toBe(false);
    });

    it('should rollback on delete error', async () => {
      setMockChannelReply(chatId, {
        ...mockChannelReply,
        mediaFileId: 'file-123',
        mediaType: 'PHOTO',
      });

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;

      setMockError('channel-reply-media', 500, 'Delete failed');

      await expect(result.current.deleteMedia()).rejects.toThrow();

      // Data should be rolled back
      await waitFor(() => {
        expect(result.current.data).toEqual(originalData);
      }, { timeout: 2000 });
    });
  });

  describe('refetch', () => {
    it('should refetch channel reply from API', async () => {
      setMockChannelReply(chatId, mockChannelReply);

      const { result } = renderHook(() => useChannelReply(chatId));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data?.replyText).toBe('Welcome to the channel!');

      // Update mock data
      setMockChannelReply(chatId, { replyText: 'Updated welcome message' });

      await result.current.refetch();

      await waitFor(() => {
        expect(result.current.data?.replyText).toBe('Updated welcome message');
      });
    });
  });
});
