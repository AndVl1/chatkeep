import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { ChatSelector } from '@/components/chats/ChatSelector';
import { useChats } from '@/hooks/api/useChats';
import { useChatStore } from '@/stores/chatStore';
import { useAuthMode } from '@/hooks/auth/useAuthMode';
import { useAuthStore } from '@/stores/authStore';

export function HomePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { chats, isLoading, error, refetch } = useChats();
  const { selectedChatId, setSelectedChat } = useChatStore();
  const { isWeb } = useAuthMode();
  const { logout } = useAuthStore();

  const handleSelectChat = useCallback((chatId: number) => {
    setSelectedChat(chatId);
    navigate(`/chat/${chatId}/settings`);
  }, [setSelectedChat, navigate]);

  const handleLogout = useCallback(() => {
    logout();
  }, [logout]);

  return (
    <div style={{ padding: '16px' }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '16px'
      }}>
        <h1 style={{ margin: 0, fontSize: '24px' }}>
          {t('home.title')}
        </h1>
        {isWeb && (
          <Button
            mode="outline"
            size="s"
            onClick={handleLogout}
          >
            {t('common.logout')}
          </Button>
        )}
      </div>
      <div style={{ marginBottom: '16px' }}>
        <Button
          size="l"
          stretched
          mode="outline"
          onClick={() => navigate('/capabilities')}
        >
          {t('home.viewCapabilities')}
        </Button>
      </div>
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
