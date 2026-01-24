import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Textarea, Section } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useRules } from '@/hooks/api/useRules';
import { useNotification } from '@/hooks/ui/useNotification';

export function RulesPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: rules, isLoading, isSaving, error, mutate, refetch } = useRules(numericChatId);
  const { showSuccess, showError } = useNotification();

  const [rulesText, setRulesText] = useState('');

  useEffect(() => {
    if (rules) {
      setRulesText(rules.rulesText || '');
    }
  }, [rules]);

  const handleSave = useCallback(async () => {
    try {
      await mutate({
        rulesText: rulesText || null,
      });
      showSuccess(t('rules.saveSuccess'));
    } catch (err) {
      showError((err as Error).message || t('rules.saveError'));
    }
  }, [rulesText, mutate, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={refetch} />;
  }

  if (!rules) {
    return <LoadingSpinner />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate(`/chat/${chatId}/settings`)}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('rules.title')}
        </h1>
      </div>

      <Section header={t('rules.rulesText')}>
        <div style={{ padding: '12px' }}>
          <Textarea
            placeholder={t('rules.placeholder')}
            value={rulesText}
            onChange={(e) => setRulesText(e.target.value)}
            rows={10}
          />
        </div>
      </Section>

      <div style={{ padding: '16px 0' }}>
        {isSaving ? (
          <LoadingSpinner />
        ) : (
          <Button size="l" stretched onClick={handleSave}>
            {t('common.save')}
          </Button>
        )}
      </div>
    </div>
  );
}
