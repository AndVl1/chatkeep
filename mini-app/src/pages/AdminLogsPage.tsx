import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Section, Placeholder } from '@telegram-apps/telegram-ui';
import { exportAdminLogs } from '@/api';
import { useNotification } from '@/hooks/ui/useNotification';

export function AdminLogsPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);
  const { showSuccess, showError } = useNotification();
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = useCallback(async () => {
    try {
      setIsExporting(true);
      const blob = await exportAdminLogs(numericChatId);

      // Download file
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `admin-logs-${numericChatId}-${new Date().toISOString()}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      showSuccess(t('adminLogs.exportSuccess'));
    } catch (err) {
      showError((err as Error).message || t('adminLogs.exportFailed'));
    } finally {
      setIsExporting(false);
    }
  }, [numericChatId, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('adminLogs.title')}
        </h1>
      </div>

      <Section>
        <Placeholder
          description={t('adminLogs.exportDescription')}
          header={t('adminLogs.exportHeader')}
        >
          <img
            alt="Export"
            src="https://xelene.me/telegram.gif"
            style={{ display: 'block', width: '144px', height: '144px' }}
          />
        </Placeholder>

        <div style={{ padding: '16px', textAlign: 'center' }}>
          <Button
            size="l"
            onClick={handleExport}
            disabled={isExporting}
            style={{ minWidth: '200px' }}
          >
            {isExporting ? t('common.loading') : t('adminLogs.export')}
          </Button>
        </div>
      </Section>
    </div>
  );
}
