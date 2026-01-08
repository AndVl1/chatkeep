import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Cell, Input, Select, Button } from '@telegram-apps/telegram-ui';
import type { AddBlocklistPatternRequest, PunishmentType } from '@/types';

interface AddPatternFormProps {
  onAdd: (pattern: AddBlocklistPatternRequest) => Promise<void>;
  onCancel: () => void;
}

export function AddPatternForm({ onAdd, onCancel }: AddPatternFormProps) {
  const { t } = useTranslation();
  const [pattern, setPattern] = useState('');
  const [matchType, setMatchType] = useState<'EXACT' | 'WILDCARD'>('EXACT');
  const [action, setAction] = useState<PunishmentType>('WARN');
  const [duration, setDuration] = useState<number>(0);
  const [severity, setSeverity] = useState<number>(1);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const punishmentTypes: PunishmentType[] = ['NOTHING', 'WARN', 'MUTE', 'BAN', 'KICK'];

  const handleSubmit = useCallback(async () => {
    if (!pattern.trim()) return;

    setIsSubmitting(true);
    try {
      await onAdd({
        pattern: pattern.trim(),
        matchType,
        action,
        actionDurationMinutes: duration > 0 ? duration : null,
        severity,
      });
      // Reset form
      setPattern('');
      setMatchType('EXACT');
      setAction('WARN');
      setDuration(0);
      setSeverity(1);
    } finally {
      setIsSubmitting(false);
    }
  }, [pattern, matchType, action, duration, severity, onAdd]);

  return (
    <Section header={t('blocklist.addPattern')}>
      <Cell description={t('blocklist.patternLabel')}>
        <Input
          placeholder={t('blocklist.patternPlaceholder')}
          value={pattern}
          onChange={(e) => setPattern(e.target.value)}
        />
      </Cell>

      <Cell description={t('blocklist.matchType')}>
        <Select value={matchType} onChange={(e) => setMatchType(e.target.value as 'EXACT' | 'WILDCARD')}>
          <option value="EXACT">{t('blocklist.exact')}</option>
          <option value="WILDCARD">{t('blocklist.wildcard')}</option>
        </Select>
      </Cell>

      <Cell description={t('blocklist.action')}>
        <Select value={action} onChange={(e) => setAction(e.target.value as PunishmentType)}>
          {punishmentTypes.map((value) => (
            <option key={value} value={value}>
              {t(`punishment.${value}`)}
            </option>
          ))}
        </Select>
      </Cell>

      {(action === 'MUTE' || action === 'BAN') && (
        <Cell description={t('settings.duration')}>
          <Input
            type="number"
            value={duration}
            onChange={(e) => setDuration(parseInt(e.target.value) || 0)}
            min={0}
          />
        </Cell>
      )}

      <Cell description={t('blocklist.severity')}>
        <Input
          type="number"
          value={severity}
          onChange={(e) => setSeverity(parseInt(e.target.value) || 1)}
          min={1}
          max={10}
        />
      </Cell>

      <div style={{ padding: '16px', display: 'flex', gap: '8px' }}>
        <Button
          size="m"
          stretched
          onClick={handleSubmit}
          disabled={!pattern.trim() || isSubmitting}
        >
          {t('common.add')}
        </Button>
        <Button
          size="m"
          mode="outline"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          {t('common.cancel')}
        </Button>
      </div>
    </Section>
  );
}
