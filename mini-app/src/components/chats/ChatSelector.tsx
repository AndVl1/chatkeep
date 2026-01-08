import { useTranslation } from 'react-i18next';
import { Section } from '@telegram-apps/telegram-ui';
import { ChatCard } from './ChatCard';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';
import type { Chat } from '@/types';

interface ChatSelectorProps {
  chats: Chat[];
  selectedChatId: number | null;
  isLoading: boolean;
  error: Error | null;
  onSelectChat: (chatId: number) => void;
  onRetry?: () => void;
}

export function ChatSelector({
  chats,
  selectedChatId,
  isLoading,
  error,
  onSelectChat,
  onRetry,
}: ChatSelectorProps) {
  const { t } = useTranslation();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />;
  }

  if (chats.length === 0) {
    return (
      <EmptyState
        title={t('chats.noChats')}
        description={t('chats.noChatsDescription')}
      />
    );
  }

  return (
    <Section header={t('chats.selectChat')}>
      {chats.map((chat) => (
        <ChatCard
          key={chat.chatId}
          chat={chat}
          isActive={chat.chatId === selectedChatId}
          onSelect={onSelectChat}
        />
      ))}
    </Section>
  );
}
