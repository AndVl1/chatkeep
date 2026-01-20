import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Card, Section } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { useAdminLogs } from '@/hooks/api/useAdminLogs';
import { useNotification } from '@/hooks/ui/useNotification';

export function AdminLogsPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: logs, isLoading, isExporting, error, refetch, exportLogs } = useAdminLogs(numericChatId, {
    page: 0,
    pageSize: 50,
  });
  const { showSuccess, showError } = useNotification();
  const [currentPage, setCurrentPage] = useState(0);

  const handleExport = useCallback(async () => {
    try {
      await exportLogs();
      showSuccess(t('adminLogs.exportSuccess'));
    } catch (err) {
      showError((err as Error).message || t('adminLogs.exportFailed'));
    }
  }, [exportLogs, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !logs) {
    return <ErrorState error={error || new Error('Failed to load logs')} onRetry={refetch} />;
  }

  const totalPages = Math.ceil(logs.totalCount / 50);

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate('/')}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('adminLogs.title')}
        </h1>
        <Button size="s" onClick={handleExport} disabled={isExporting}>
          {isExporting ? t('common.loading') : t('adminLogs.export')}
        </Button>
      </div>

      {logs.logs.length === 0 ? (
        <EmptyState
          title={t('adminLogs.noLogs')}
          description={t('adminLogs.noLogsDescription')}
        />
      ) : (
        <>
          <Section header={t('adminLogs.recentActions')}>
            {logs.logs.map((log) => (
              <Card key={log.id} style={{ padding: '12px', marginBottom: '8px' }}>
                <div style={{ marginBottom: '4px' }}>
                  <strong>{log.action}</strong>
                </div>
                <div style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', marginBottom: '4px' }}>
                  {t('adminLogs.performedBy')}: {log.performedByUsername || log.performedBy}
                </div>
                {log.targetUsername && (
                  <div style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', marginBottom: '4px' }}>
                    {t('adminLogs.target')}: {log.targetUsername || log.targetUserId}
                  </div>
                )}
                {log.details && (
                  <div style={{ fontSize: '14px', marginBottom: '4px' }}>
                    {log.details}
                  </div>
                )}
                <div style={{ fontSize: '12px', color: 'var(--tg-theme-hint-color)' }}>
                  {new Date(log.timestamp).toLocaleString()}
                </div>
              </Card>
            ))}
          </Section>

          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', padding: '16px 0' }}>
              <Button
                size="s"
                disabled={currentPage === 0}
                onClick={() => setCurrentPage(p => p - 1)}
              >
                {t('adminLogs.previous')}
              </Button>
              <span style={{ alignSelf: 'center' }}>
                {t('adminLogs.pageInfo', { current: currentPage + 1, total: totalPages })}
              </span>
              <Button
                size="s"
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage(p => p + 1)}
              >
                {t('adminLogs.next')}
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
