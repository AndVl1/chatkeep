import { useParams, Navigate, useNavigate } from 'react-router-dom';
import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Input, Select, Section } from '@telegram-apps/telegram-ui';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { useModeration } from '@/hooks/api/useModeration';
import { useNotification } from '@/hooks/ui/useNotification';
import { useConfirmDialog } from '@/hooks/ui/useConfirmDialog';
import type { PunishmentType } from '@/types';

export function ModerationPage() {
  const { t } = useTranslation();
  const { chatId } = useParams<{ chatId: string }>();
  const navigate = useNavigate();
  const numericChatId = Number(chatId);

  const { isExecuting, warn, mute, ban, kick } = useModeration();
  const { showSuccess, showError } = useNotification();
  const { confirm } = useConfirmDialog();

  const [userId, setUserId] = useState('');
  const [reason, setReason] = useState('');
  const [duration, setDuration] = useState('');
  const [actionType, setActionType] = useState<PunishmentType>('WARN');

  const handleExecute = useCallback(async () => {
    if (!userId || isNaN(Number(userId))) {
      showError(t('moderation.invalidUserId'));
      return;
    }

    const confirmed = await confirm(
      t('moderation.confirmAction', { action: t(`punishment.${actionType}`) }),
      t('moderation.confirmTitle')
    );

    if (!confirmed) return;

    const action = {
      userId: Number(userId),
      reason: reason || undefined,
      durationMinutes: duration ? Number(duration) : undefined,
    };

    try {
      let response;
      switch (actionType) {
        case 'WARN':
          response = await warn(numericChatId, action);
          break;
        case 'MUTE':
          response = await mute(numericChatId, action);
          break;
        case 'BAN':
          response = await ban(numericChatId, action);
          break;
        case 'KICK':
          response = await kick(numericChatId, action);
          break;
        default:
          throw new Error('Invalid action type');
      }

      showSuccess(response.message);
      setUserId('');
      setReason('');
      setDuration('');
    } catch (err) {
      showError((err as Error).message || t('moderation.actionFailed'));
    }
  }, [userId, reason, duration, actionType, numericChatId, warn, mute, ban, kick, showSuccess, showError, confirm, t]);

  if (!chatId || isNaN(numericChatId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <Button size="s" mode="plain" onClick={() => navigate('/')}>
          {t('common.back')}
        </Button>
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('moderation.title')}
        </h1>
      </div>

      <Section header={t('moderation.userInfo')}>
        <div style={{ padding: '12px' }}>
          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('moderation.userId')}
          </label>
          <Input
            type="number"
            placeholder={t('moderation.userIdPlaceholder')}
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            disabled={isExecuting}
          />
        </div>
      </Section>

      <Section header={t('moderation.actionDetails')}>
        <div style={{ padding: '12px' }}>
          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('moderation.action')}
          </label>
          <Select
            value={actionType}
            onChange={(e) => setActionType(e.target.value as PunishmentType)}
            disabled={isExecuting}
            style={{ marginBottom: '16px' }}
          >
            <option value="WARN">{t('punishment.WARN')}</option>
            <option value="MUTE">{t('punishment.MUTE')}</option>
            <option value="BAN">{t('punishment.BAN')}</option>
            <option value="KICK">{t('punishment.KICK')}</option>
          </Select>

          <label style={{ display: 'block', marginBottom: '8px' }}>
            {t('moderation.reason')}
          </label>
          <Input
            placeholder={t('moderation.reasonPlaceholder')}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            disabled={isExecuting}
            style={{ marginBottom: '16px' }}
          />

          {(actionType === 'MUTE' || actionType === 'BAN') && (
            <>
              <label style={{ display: 'block', marginBottom: '8px' }}>
                {t('settings.duration')}
              </label>
              <Input
                type="number"
                placeholder="0"
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
                disabled={isExecuting}
              />
              <p style={{ fontSize: '14px', color: 'var(--tg-theme-hint-color)', margin: '4px 0 0 0' }}>
                {t('moderation.durationHint')}
              </p>
            </>
          )}
        </div>
      </Section>

      <div style={{ padding: '16px 0' }}>
        {isExecuting ? (
          <LoadingSpinner />
        ) : (
          <Button
            size="l"
            stretched
            onClick={handleExecute}
            disabled={!userId || isNaN(Number(userId))}
          >
            {t('moderation.execute')}
          </Button>
        )}
      </div>
    </div>
  );
}
