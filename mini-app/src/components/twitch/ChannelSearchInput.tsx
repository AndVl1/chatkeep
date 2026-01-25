import { useState, useCallback, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Input, List, Cell, Avatar, Spinner } from '@telegram-apps/telegram-ui';
import { searchTwitchChannels } from '@/api';
import type { TwitchSearchResult } from '@/types';
import { useNotification } from '@/hooks/ui/useNotification';

interface ChannelSearchInputProps {
  onSelect: (login: string) => void;
  disabled?: boolean;
}

const TWITCH_URL_REGEX = /(?:https?:\/\/)?(?:www\.)?twitch\.tv\/([a-zA-Z0-9_]{4,25})/i;

export function ChannelSearchInput({ onSelect, disabled = false }: ChannelSearchInputProps) {
  const { t } = useTranslation();
  const { showError } = useNotification();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<TwitchSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const searchTimeoutRef = useRef<number>();

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);

    // Check if URL
    const urlMatch = value.match(TWITCH_URL_REGEX);
    if (urlMatch && urlMatch[1]) {
      const login = urlMatch[1];
      setQuery(login);
      onSelect(login);
      setShowResults(false);
      return;
    }

    // Debounced search
    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    if (value.trim().length < 2) {
      setResults([]);
      setShowResults(false);
      return;
    }

    searchTimeoutRef.current = setTimeout(async () => {
      try {
        setIsSearching(true);
        const data = await searchTwitchChannels(value.trim());
        setResults(data);
        setShowResults(true);
      } catch (err) {
        showError((err as Error).message || t('twitch.searchError'));
      } finally {
        setIsSearching(false);
      }
    }, 500);
  }, [onSelect, showError, t]);

  const handleSelect = useCallback((login: string) => {
    onSelect(login);
    setQuery('');
    setResults([]);
    setShowResults(false);
  }, [onSelect]);

  useEffect(() => {
    return () => {
      if (searchTimeoutRef.current) {
        clearTimeout(searchTimeoutRef.current);
      }
    };
  }, []);

  return (
    <div style={{ position: 'relative' }}>
      <Input
        value={query}
        onChange={handleInputChange}
        placeholder={t('twitch.searchPlaceholder')}
        disabled={disabled}
        after={isSearching ? <Spinner size="s" /> : undefined}
      />

      {showResults && results.length > 0 && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            backgroundColor: 'var(--tgui--secondary_bg_color)',
            borderRadius: '8px',
            marginTop: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            zIndex: 10,
            maxHeight: '300px',
            overflowY: 'auto',
          }}
        >
          <List>
            {results.map(channel => (
              <Cell
                key={channel.id}
                before={
                  channel.avatarUrl ? (
                    <Avatar size={40} src={channel.avatarUrl} />
                  ) : (
                    <Avatar size={40}>{channel.displayName[0]}</Avatar>
                  )
                }
                onClick={() => handleSelect(channel.login)}
                subtitle={`@${channel.login}`}
                description={channel.isLive ? t('twitch.live') : t('twitch.offline')}
              >
                {channel.displayName}
              </Cell>
            ))}
          </List>
        </div>
      )}
    </div>
  );
}
