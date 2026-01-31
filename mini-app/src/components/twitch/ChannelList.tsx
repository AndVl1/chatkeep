import { memo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { List, Cell, Avatar, Badge, IconButton } from '@telegram-apps/telegram-ui';
import type { TwitchChannel } from '@/types';

interface ChannelListProps {
  channels: TwitchChannel[];
  onDelete: (channelId: number) => void;
  onPin: (channelId: number, pinSilently: boolean) => void;
  onUnpin: (channelId: number) => void;
  disabled?: boolean;
}

export const ChannelList = memo(function ChannelList({
  channels,
  onDelete,
  onPin,
  onUnpin,
  disabled = false,
}: ChannelListProps) {
  const { t } = useTranslation();

  const handleDelete = useCallback(
    (channelId: number) => {
      onDelete(channelId);
    },
    [onDelete]
  );

  const handlePinToggle = useCallback(
    (channel: TwitchChannel) => {
      if (channel.isPinned) {
        onUnpin(channel.id);
      } else {
        onPin(channel.id, true); // Default: silent
      }
    },
    [onPin, onUnpin]
  );

  const handleSilentToggle = useCallback(
    (channel: TwitchChannel) => {
      if (channel.isPinned) {
        onPin(channel.id, !channel.pinSilently);
      }
    },
    [onPin]
  );

  if (channels.length === 0) {
    return null;
  }

  return (
    <List>
      {channels.map(channel => (
        <Cell
          key={channel.id}
          before={
            channel.avatarUrl ? (
              <Avatar size={40} src={channel.avatarUrl} />
            ) : (
              <Avatar size={40}>{channel.displayName[0]}</Avatar>
            )
          }
          after={
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              {/* Pin toggle */}
              <IconButton
                mode="plain"
                size="s"
                onClick={() => handlePinToggle(channel)}
                disabled={disabled}
                aria-label={channel.isPinned ? t('twitch.unpin') : t('twitch.pin')}
              >
                {channel.isPinned ? '📌' : '📍'}
              </IconButton>

              {/* Silent notification toggle (only if pinned) */}
              {channel.isPinned && (
                <IconButton
                  mode="plain"
                  size="s"
                  onClick={() => handleSilentToggle(channel)}
                  disabled={disabled}
                  aria-label={
                    channel.pinSilently
                      ? t('twitch.enableNotification')
                      : t('twitch.disableNotification')
                  }
                >
                  {channel.pinSilently ? '🔕' : '🔔'}
                </IconButton>
              )}

              {/* Delete button */}
              <IconButton
                mode="plain"
                size="s"
                onClick={() => handleDelete(channel.id)}
                disabled={disabled}
              >
                ×
              </IconButton>
            </div>
          }
          subtitle={channel.isLive ? t('twitch.live') : t('twitch.offline')}
          description={`@${channel.twitchLogin}`}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {channel.displayName}
            {channel.isLive && (
              <Badge type="number" style={{ backgroundColor: 'var(--tgui--destructive_text_color)' }}>
                LIVE
              </Badge>
            )}
          </div>
        </Cell>
      ))}
    </List>
  );
});
