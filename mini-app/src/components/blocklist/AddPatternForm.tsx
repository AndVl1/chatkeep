import { useState, useCallback } from 'react';
import { Section, Cell, Input, Select, Button } from '@telegram-apps/telegram-ui';
import type { AddBlocklistPatternRequest, PunishmentType } from '@/types';
import { PUNISHMENT_LABELS } from '@/utils/constants';

interface AddPatternFormProps {
  onAdd: (pattern: AddBlocklistPatternRequest) => Promise<void>;
  onCancel: () => void;
}

export function AddPatternForm({ onAdd, onCancel }: AddPatternFormProps) {
  const [pattern, setPattern] = useState('');
  const [matchType, setMatchType] = useState<'EXACT' | 'WILDCARD'>('EXACT');
  const [action, setAction] = useState<PunishmentType>('WARN');
  const [duration, setDuration] = useState<number>(0);
  const [severity, setSeverity] = useState<number>(1);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
    <Section header="Add Pattern">
      <Cell description="Pattern to match">
        <Input
          placeholder="e.g., spam, advertisement"
          value={pattern}
          onChange={(e) => setPattern(e.target.value)}
        />
      </Cell>

      <Cell description="Match type">
        <Select value={matchType} onChange={(e) => setMatchType(e.target.value as 'EXACT' | 'WILDCARD')}>
          <option value="EXACT">Exact</option>
          <option value="WILDCARD">Wildcard</option>
        </Select>
      </Cell>

      <Cell description="Action">
        <Select value={action} onChange={(e) => setAction(e.target.value as PunishmentType)}>
          {Object.entries(PUNISHMENT_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </Select>
      </Cell>

      {(action === 'MUTE' || action === 'BAN') && (
        <Cell description="Duration (minutes, 0 = permanent)">
          <Input
            type="number"
            value={duration}
            onChange={(e) => setDuration(parseInt(e.target.value) || 0)}
            min={0}
          />
        </Cell>
      )}

      <Cell description="Severity (1-10)">
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
          Add Pattern
        </Button>
        <Button
          size="m"
          mode="outline"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          Cancel
        </Button>
      </div>
    </Section>
  );
}
