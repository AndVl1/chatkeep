import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Textarea, Section, Switch } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useWelcomeMessage } from '@/hooks/api/useWelcomeMessage';
import { useNotification } from '@/hooks/ui/useNotification';

export function WelcomePage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: welcome, isLoading, isSaving, error, mutate, refetch } = useWelcomeMessage(numericChatId);
  const { showSuccess, showError } = useNotification();

  const [enabled, setEnabled] = useState(false);
  const [welcomeText, setWelcomeText] = useState('');
  const [goodbyeText, setGoodbyeText] = useState('');
  const [deleteAfter, setDeleteAfter] = useState('');
  const [cleanPrevious, setCleanPrevious] = useState(false);

  useEffect(() => {
    if (welcome) {
      setEnabled(welcome.enabled);
      setWelcomeText(welcome.welcomeText || '');
      setGoodbyeText(welcome.goodbyeText || '');
      setDeleteAfter(welcome.deleteAfterMinutes?.toString() || '');
      setCleanPrevious(welcome.cleanPrevious);
    }
  }, [welcome]);

  const handleSave = useCallback(async () => {
    try {
      await mutate({
        enabled,
        welcomeText: welcomeText || null,
        goodbyeText: goodbyeText || null,
        deleteAfterMinutes: deleteAfter ? Number(deleteAfter) : null,
        cleanPrevious,
      });
      showSuccess(t('welcome.saveSuccess'));
    } catch (err) {
      showError((err as Error).message || t('welcome.saveError'));
    }
  }, [enabled, welcomeText, goodbyeText, deleteAfter, cleanPrevious, mutate, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !welcome) {
    return <ErrorState error={error || new Error('Failed to load welcome settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate('/')}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('welcome.title')}
        </h1>
      </div>

      <Section>
        <div style={{ padding: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>{t('welcome.enabled')}</span>
          <Switch checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        </div>
      </Section>

      <Section header={t('welcome.welcomeMessage')}>
        <div style={{ padding: '12px' }}>
          <Textarea
            placeholder={t('welcome.welcomePlaceholder')}
            value={welcomeText}
            onChange={(e) => setWelcomeText(e.target.value)}
            rows={4}
          />
        </div>
      </Section>

      <Section header={t('welcome.goodbyeMessage')}>
        <div style={{ padding: '12px' }}>
          <Textarea
            placeholder={t('welcome.goodbyePlaceholder')}
            value={goodbyeText}
            onChange={(e) => setGoodbyeText(e.target.value)}
            rows={4}
          />
        </div>
      </Section>

      <Section header={t('welcome.options')}>
        <div style={{ padding: '12px' }}>
          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('welcome.deleteAfter')}
          </label>
          <Input
            type="number"
            placeholder="0"
            value={deleteAfter}
            onChange={(e) => setDeleteAfter(e.target.value)}
            style={{ marginBottom: '16px' }}
          />
          <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '0 0 16px 0' }}>
            {t('welcome.deleteAfterHint')}
          </p>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>{t('welcome.cleanPrevious')}</span>
            <Switch checked={cleanPrevious} onChange={(e) => setCleanPrevious(e.target.checked)} />
          </div>
          <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '4px 0 0 0' }}>
            {t('welcome.cleanPreviousHint')}
          </p>
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
