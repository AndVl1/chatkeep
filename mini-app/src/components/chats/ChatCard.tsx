import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { Cell, Badge } from '@telegram-apps/telegram-ui';
import type { Chat } from '@/types';

interface ChatCardProps {
  chat: Chat;
  isActive?: boolean;
  onSelect: (chatId: number) => void;
}

export const ChatCard = memo(function ChatCard({
  chat,
  isActive = false,
  onSelect,
}: ChatCardProps) {
  const { t } = useTranslation();
  const subtitle = chat.memberCount
    ? t('chats.members', { count: chat.memberCount })
    : undefined;

  return (
    <Cell
      onClick={() => onSelect(chat.chatId)}
      subtitle={subtitle}
      after={
        !chat.isBotAdmin ? (
          <Badge type="number" style={{ backgroundColor: 'var(--tgui--destructive_text_color)' }}>
            !
          </Badge>
        ) : undefined
      }
      style={isActive ? { backgroundColor: 'var(--tgui--secondary_bg_color)' } : undefined}
    >
      {chat.chatTitle}
    </Cell>
  );
});
