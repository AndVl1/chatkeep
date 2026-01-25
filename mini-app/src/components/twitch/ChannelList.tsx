import { memo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { List, Cell, Avatar, Badge, IconButton } from '@telegram-apps/telegram-ui';
import type { TwitchChannel } from '@/types';

interface ChannelListProps {
  channels: TwitchChannel[];
  onDelete: (channelId: number) => void;
  disabled?: boolean;
}

export const ChannelList = memo(function ChannelList({
  channels,
  onDelete,
  disabled = false,
}: ChannelListProps) {
  const { t } = useTranslation();

  const handleDelete = useCallback(
    (channelId: number) => {
      onDelete(channelId);
    },
    [onDelete]
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
            <IconButton
              mode="plain"
              size="s"
              onClick={() => handleDelete(channel.id)}
              disabled={disabled}
            >
              ×
            </IconButton>
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
