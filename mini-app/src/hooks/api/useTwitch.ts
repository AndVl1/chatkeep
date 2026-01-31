import { useState, useEffect, useCallback } from 'react';
import {
  getTwitchChannels,
  addTwitchChannel,
  removeTwitchChannel,
  getTwitchSettings,
  updateTwitchSettings,
  searchTwitchChannels,
  pinTwitchChannel,
  unpinTwitchChannel,
} from '@/api';
import type {
  TwitchChannel,
  TwitchSettings,
  TwitchSearchResult,
  AddTwitchChannelRequest,
  UpdateTwitchSettingsRequest,
} from '@/types';

interface UseTwitchResult {
  channels: TwitchChannel[];
  settings: TwitchSettings | null;
  searchResults: TwitchSearchResult[];
  isLoading: boolean;
  error: Error | null;
  addChannel: (data: AddTwitchChannelRequest) => Promise<TwitchChannel>;
  removeChannel: (channelId: number) => Promise<void>;
  updateSettings: (data: UpdateTwitchSettingsRequest) => Promise<void>;
  search: (query: string) => Promise<void>;
  refetch: () => Promise<void>;
  pinChannel: (channelId: number, pinSilently: boolean) => Promise<void>;
  unpinChannel: (channelId: number) => Promise<void>;
}

export function useTwitch(chatId: number): UseTwitchResult {
  const [channels, setChannels] = useState<TwitchChannel[]>([]);
  const [settings, setSettings] = useState<TwitchSettings | null>(null);
  const [searchResults, setSearchResults] = useState<TwitchSearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchData = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const [channelsData, settingsData] = await Promise.all([
        getTwitchChannels(chatId),
        getTwitchSettings(chatId),
      ]);
      setChannels(channelsData);
      setSettings(settingsData);
    } catch (err) {
      setError(err as Error);
    } finally {
      setIsLoading(false);
    }
  }, [chatId]);

  // Silent refresh - updates channels without showing loading state
  const refreshChannels = useCallback(async () => {
    try {
      const channelsData = await getTwitchChannels(chatId);
      setChannels(channelsData);
    } catch (err) {
      // Silently fail on background refresh - don't show error to user
      console.error('Failed to refresh channels:', err);
    }
  }, [chatId]);

  // Initial fetch
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Polling to refresh live status (silent, no loading state)
  // Default 30 seconds, can be overridden via VITE_TWITCH_POLL_INTERVAL for testing
  useEffect(() => {
    if (!chatId || channels.length === 0) return;

    const pollInterval = parseInt(import.meta.env.VITE_TWITCH_POLL_INTERVAL || '30000', 10);
    const interval = setInterval(() => {
      refreshChannels();
    }, pollInterval);

    return () => clearInterval(interval);
  }, [chatId, channels.length, refreshChannels]);

  const handleAddChannel = useCallback(
    async (data: AddTwitchChannelRequest) => {
      // Defensive check: prevent adding duplicates
      if (
        channels.some(
          c => c.twitchLogin.toLowerCase() === data.twitchLogin.toLowerCase()
        )
      ) {
        throw new Error('Channel already added');
      }

      const newChannel = await addTwitchChannel(chatId, data);
      // Refetch to get correct isLive status from backend
      await fetchData();
      return newChannel;
    },
    [chatId, channels, fetchData]
  );

  const handleRemoveChannel = useCallback(
    async (channelId: number) => {
      await removeTwitchChannel(chatId, channelId);
      setChannels(prev => prev.filter(c => c.id !== channelId));
    },
    [chatId]
  );

  const handleUpdateSettings = useCallback(
    async (data: UpdateTwitchSettingsRequest) => {
      const updated = await updateTwitchSettings(chatId, data);
      setSettings(updated);
    },
    [chatId]
  );

  const handleSearch = useCallback(
    async (query: string) => {
      const results = await searchTwitchChannels(query);
      // Filter out already-added channels
      const addedLogins = new Set(
        channels.map(c => c.twitchLogin.toLowerCase())
      );
      const filtered = results.filter(
        r => !addedLogins.has(r.login.toLowerCase())
      );
      setSearchResults(filtered);
    },
    [channels]
  );

  const handlePinChannel = useCallback(
    async (channelId: number, pinSilently: boolean) => {
      // Optimistic update: unpin all others, pin this one
      const previousChannels = channels;
      setChannels(prev =>
        prev.map(ch => ({
          ...ch,
          isPinned: ch.id === channelId,
          pinSilently: ch.id === channelId ? pinSilently : ch.pinSilently,
        }))
      );

      try {
        await pinTwitchChannel(chatId, channelId, pinSilently);
        // Refetch to sync state with backend
        await refreshChannels();
      } catch (err) {
        // Rollback on error
        setChannels(previousChannels);
        throw err;
      }
    },
    [chatId, channels, refreshChannels]
  );

  const handleUnpinChannel = useCallback(
    async (channelId: number) => {
      // Optimistic update
      const previousChannels = channels;
      setChannels(prev =>
        prev.map(ch => (ch.id === channelId ? { ...ch, isPinned: false } : ch))
      );

      try {
        await unpinTwitchChannel(chatId, channelId);
        await refreshChannels();
      } catch (err) {
        // Rollback on error
        setChannels(previousChannels);
        throw err;
      }
    },
    [chatId, channels, refreshChannels]
  );

  return {
    channels,
    settings,
    searchResults,
    isLoading,
    error,
    addChannel: handleAddChannel,
    removeChannel: handleRemoveChannel,
    updateSettings: handleUpdateSettings,
    search: handleSearch,
    refetch: fetchData,
    pinChannel: handlePinChannel,
    unpinChannel: handleUnpinChannel,
  };
}
