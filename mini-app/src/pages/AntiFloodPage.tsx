import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useCallback, useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Select, Section, Switch } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useAntiFlood } from '@/hooks/api/useAntiFlood';
import { useNotification } from '@/hooks/ui/useNotification';
import type { PunishmentType } from '@/types';

export function AntiFloodPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { data: antiflood, isLoading, isSaving, error, mutate, refetch } = useAntiFlood(numericChatId);
  const { showSuccess, showError } = useNotification();

  const [enabled, setEnabled] = useState(false);
  const [maxMessages, setMaxMessages] = useState('5');
  const [timeWindow, setTimeWindow] = useState('10');
  const [action, setAction] = useState<PunishmentType>('WARN');
  const [duration, setDuration] = useState('');

  useEffect(() => {
    if (antiflood) {
      setEnabled(antiflood.enabled);
      setMaxMessages(antiflood.maxMessages.toString());
      setTimeWindow(antiflood.timeWindowSeconds.toString());
      setAction(antiflood.action);
      setDuration(antiflood.actionDurationMinutes?.toString() || '');
    }
  }, [antiflood]);

  const handleSave = useCallback(async () => {
    try {
      await mutate({
        enabled,
        maxMessages: Number(maxMessages),
        timeWindowSeconds: Number(timeWindow),
        action,
        actionDurationMinutes: duration ? Number(duration) : null,
      });
      showSuccess(t('antiflood.saveSuccess'));
    } catch (err) {
      showError((err as Error).message || t('antiflood.saveError'));
    }
  }, [enabled, maxMessages, timeWindow, action, duration, mutate, showSuccess, showError, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error || !antiflood) {
    return <ErrorState error={error || new Error('Failed to load anti-flood settings')} onRetry={refetch} />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate('/')}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('antiflood.title')}
        </h1>
      </div>

      <Section>
        <div style={{ padding: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div>{t('antiflood.enabled')}</div>
            <div style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)' }}>
              {t('antiflood.enabledDescription')}
            </div>
          </div>
          <Switch checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        </div>
      </Section>

      <Section header={t('antiflood.configuration')}>
        <div style={{ padding: '12px' }}>
          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('antiflood.maxMessages')}
          </label>
          <Input
            type="number"
            value={maxMessages}
            onChange={(e) => setMaxMessages(e.target.value)}
            style={{ marginBottom: '16px' }}
          />

          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('antiflood.timeWindow')}
          </label>
          <Input
            type="number"
            value={timeWindow}
            onChange={(e) => setTimeWindow(e.target.value)}
            style={{ marginBottom: '16px' }}
          />
          <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '0 0 16px 0' }}>
            {t('antiflood.timeWindowHint')}
          </p>

          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('blocklist.action')}
          </label>
          <Select
            value={action}
            onChange={(e) => setAction(e.target.value as PunishmentType)}
            style={{ marginBottom: '16px' }}
          >
            <option value="WARN">{t('punishment.WARN')}</option>
            <option value="MUTE">{t('punishment.MUTE')}</option>
            <option value="BAN">{t('punishment.BAN')}</option>
            <option value="KICK">{t('punishment.KICK')}</option>
          </Select>

          {(action === 'MUTE' || action === 'BAN') && (
            <>
              <label style={{ display: 'block', marginBottom: '8px' }}>
                {t('settings.duration')}
              </label>
              <Input
                type="number"
                placeholder="0"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
              />
              <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '4px 0 0 0' }}>
                {t('antiflood.durationHint')}
              </p>
            </>
          )}
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
