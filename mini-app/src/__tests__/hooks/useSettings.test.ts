import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useSettings } from '@/hooks/api/useSettings';
import * as settingsApi from '@/api/settings';
import { useSettingsStore } from '@/stores/settingsStore';
import type { ChatSettings } from '@/types';

// Mock the API module
vi.mock('@/api/settings', () => ({
  getSettings: vi.fn(),
  updateSettings: vi.fn(),
}));

const mockSettings: ChatSettings = {
  chatId: 123,
  chatTitle: 'Test Chat',
  collectionEnabled: true,
  cleanServiceEnabled: false,
  maxWarnings: 3,
  warningTtlHours: 24,
  thresholdAction: 'WARN',
  thresholdDurationMinutes: null,
  defaultBlocklistAction: 'NOTHING',
  logChannelId: null,
  lockWarnsEnabled: false,
};

describe('useSettings', () => {
  beforeEach(() => {
    // Clear store before each test
    useSettingsStore.setState({
      settingsCache: {},
      pendingChanges: {},
    });
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should fetch settings on mount', async () => {
    vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);

    const { result } = renderHook(() => useSettings(123));

    expect(result.current.isLoading).toBe(true);
    expect(result.current.data).toBeNull();

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(settingsApi.getSettings).toHaveBeenCalledWith(123);
    expect(result.current.data).toEqual(mockSettings);
    expect(result.current.error).toBeNull();
  });

  it('should use cached settings if available', () => {
    // Pre-populate cache
    useSettingsStore.setState({
      settingsCache: { 123: mockSettings },
      pendingChanges: {},
    });

    const { result } = renderHook(() => useSettings(123));

    expect(result.current.isLoading).toBe(false);
    expect(result.current.data).toEqual(mockSettings);
    expect(settingsApi.getSettings).not.toHaveBeenCalled();
  });

  it('should handle fetch error', async () => {
    const error = new Error('Network error');
    vi.mocked(settingsApi.getSettings).mockRejectedValue(error);

    const { result } = renderHook(() => useSettings(123));

    await waitFor(() => {
      expect(result.current.isLoading).toBe(false);
    });

    expect(result.current.error).toEqual(error);
    expect(result.current.data).toBeNull();
  });

  describe('mutate', () => {
    it('should call updateSettings with correct chatId and payload', async () => {
      const updatedSettings = { ...mockSettings, collectionEnabled: false };
      vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);
      vi.mocked(settingsApi.updateSettings).mockResolvedValue(updatedSettings);

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const updates = { collectionEnabled: false };
      await result.current.mutate(updates);

      expect(settingsApi.updateSettings).toHaveBeenCalledWith(123, updates);
    });

    it('should perform optimistic update', async () => {
      vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);
      vi.mocked(settingsApi.updateSettings).mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ ...mockSettings, collectionEnabled: false }), 100))
      );

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;
      expect(originalData?.collectionEnabled).toBe(true);

      // Start mutation
      const mutatePromise = result.current.mutate({ collectionEnabled: false });

      // Check optimistic update happened immediately
      await waitFor(() => {
        expect(result.current.data?.collectionEnabled).toBe(false);
      });

      // Wait for mutation to complete
      await mutatePromise;

      expect(result.current.data?.collectionEnabled).toBe(false);
    });

    it('should rollback on error', async () => {
      vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);
      const updateError = new Error('Update failed');
      vi.mocked(settingsApi.updateSettings).mockRejectedValue(updateError);

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      const originalData = result.current.data;
      expect(originalData?.collectionEnabled).toBe(true);

      // Attempt mutation
      await expect(result.current.mutate({ collectionEnabled: false })).rejects.toThrow('Update failed');

      // Data should be rolled back
      expect(result.current.data).toEqual(originalData);
    });

    it('should update store cache on successful mutation', async () => {
      const updatedSettings = { ...mockSettings, cleanServiceEnabled: true };
      vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);
      vi.mocked(settingsApi.updateSettings).mockResolvedValue(updatedSettings);

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      await result.current.mutate({ cleanServiceEnabled: true });

      // Check store was updated
      const cachedSettings = useSettingsStore.getState().settingsCache[123];
      expect(cachedSettings).toEqual(updatedSettings);
    });

    it('should set isSaving flag during mutation', async () => {
      vi.mocked(settingsApi.getSettings).mockResolvedValue(mockSettings);
      vi.mocked(settingsApi.updateSettings).mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({ ...mockSettings, lockWarnsEnabled: true }), 100))
      );

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.isSaving).toBe(false);

      const mutatePromise = result.current.mutate({ lockWarnsEnabled: true });

      await waitFor(() => {
        expect(result.current.isSaving).toBe(true);
      });

      await mutatePromise;

      await waitFor(() => {
        expect(result.current.isSaving).toBe(false);
      });
    });
  });

  describe('refetch', () => {
    it('should refetch settings from API', async () => {
      const updatedSettings = { ...mockSettings, maxWarnings: 5 };
      vi.mocked(settingsApi.getSettings)
        .mockResolvedValueOnce(mockSettings)
        .mockResolvedValueOnce(updatedSettings);

      const { result } = renderHook(() => useSettings(123));

      await waitFor(() => {
        expect(result.current.isLoading).toBe(false);
      });

      expect(result.current.data?.maxWarnings).toBe(3);

      await result.current.refetch();

      await waitFor(() => {
        expect(result.current.data?.maxWarnings).toBe(5);
      });

      expect(settingsApi.getSettings).toHaveBeenCalledTimes(2);
    });
  });
});
