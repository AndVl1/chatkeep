import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChatSelector } from '@/components/chats/ChatSelector';
import { useChats } from '@/hooks/api/useChats';
import { useChatStore } from '@/stores/chatStore';

export function HomePage() {
  const navigate = useNavigate();
  const { chats, isLoading, error, refetch } = useChats();
  const { selectedChatId, setSelectedChat } = useChatStore();

  const handleSelectChat = useCallback((chatId: number) => {
    setSelectedChat(chatId);
    navigate(`/chat/${chatId}/settings`);
  }, [setSelectedChat, navigate]);

  return (
    <div style={{ padding: '16px' }}>
      <h1 style={{ margin: '0 0 16px 0', fontSize: '24px' }}>
        Chatkeep Configuration
      </h1>
      <ChatSelector
        chats={chats}
        selectedChatId={selectedChatId}
        isLoading={isLoading}
        error={error}
        onSelectChat={handleSelectChat}
        onRetry={refetch}
      />
    </div>
  );
}
