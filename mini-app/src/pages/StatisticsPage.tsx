import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button, Card, Section } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useStatistics } from '@/hooks/api/useStatistics';

export function StatisticsPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: stats, isLoading, error, refetch } = useStatistics(numericChatId);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !stats) {
    return <ErrorState error={error || new Error('Failed to load statistics')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('statistics.title')}
        </h1>
      </div>

      <Section header={t('statistics.messages')}>
        <Card style={{ padding: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.totalMessages')}</span>
            <strong>{(stats?.totalMessages ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.last24h')}</span>
            <strong>{(stats?.messagesLast24h ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.last7d')}</span>
            <strong>{(stats?.messagesLast7d ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>{t('statistics.last30d')}</span>
            <strong>{(stats?.messagesLast30d ?? 0).toLocaleString()}</strong>
          </div>
        </Card>
      </Section>

      <Section header={t('statistics.users')}>
        <Card style={{ padding: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>{t('statistics.totalUsers')}</span>
            <strong>{(stats?.totalUsers ?? 0).toLocaleString()}</strong>
          </div>
        </Card>
      </Section>

      <Section header={t('statistics.moderation')}>
        <Card style={{ padding: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.activeWarnings')}</span>
            <strong>{(stats?.activeWarnings ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.totalBans')}</span>
            <strong>{(stats?.totalBans ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span>{t('statistics.totalMutes')}</span>
            <strong>{(stats?.totalMutes ?? 0).toLocaleString()}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span>{t('statistics.totalKicks')}</span>
            <strong>{(stats?.totalKicks ?? 0).toLocaleString()}</strong>
          </div>
        </Card>
      </Section>
    </div>
  );
}
