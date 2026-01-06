import { useCallback } from 'react';
import { Section, Cell, Switch, Input, Select } from '@telegram-apps/telegram-ui';
import type { ChatSettings, PunishmentType } from '@/types';
import { PUNISHMENT_LABELS } from '@/utils/constants';

interface SettingsFormProps {
  settings: ChatSettings;
  onChange: (updates: Partial<ChatSettings>) => void;
  disabled?: boolean;
}

export function SettingsForm({ settings, onChange, disabled }: SettingsFormProps) {
  const handleChange = useCallback(<K extends keyof ChatSettings>(
    key: K,
    value: ChatSettings[K]
  ) => {
    onChange({ [key]: value });
  }, [onChange]);

  return (
    <>
      <Section header="General Settings">
        <Cell
          Component="label"
          after={
            <Switch
              checked={settings.collectionEnabled}
              onChange={(e) => handleChange('collectionEnabled', e.target.checked)}
              disabled={disabled}
            />
          }
          description="Enable message collection for this chat"
        >
          Collection Enabled
        </Cell>

        <Cell
          Component="label"
          after={
            <Switch
              checked={settings.cleanServiceEnabled}
              onChange={(e) => handleChange('cleanServiceEnabled', e.target.checked)}
              disabled={disabled}
            />
          }
          description="Automatically delete service messages"
        >
          Clean Service Messages
        </Cell>

        <Cell
          Component="label"
          after={
            <Switch
              checked={settings.lockWarnsEnabled}
              onChange={(e) => handleChange('lockWarnsEnabled', e.target.checked)}
              disabled={disabled}
            />
          }
          description="Warn users when they violate locks"
        >
          Lock Warnings
        </Cell>
      </Section>

      <Section header="Warning Configuration">
        <Cell description="Maximum warnings before threshold action">
          <Input
            type="number"
            value={settings.maxWarnings}
            onChange={(e) => {
              const val = parseInt(e.target.value, 10);
              handleChange('maxWarnings', isNaN(val) ? 1 : Math.max(1, Math.min(20, val)));
            }}
            min={1}
            max={20}
            disabled={disabled}
          />
        </Cell>

        <Cell description="Warning expiry time (hours)">
          <Input
            type="number"
            value={settings.warningTtlHours}
            onChange={(e) => {
              const val = parseInt(e.target.value, 10);
              handleChange('warningTtlHours', isNaN(val) ? 24 : Math.max(1, Math.min(168, val)));
            }}
            min={1}
            max={168}
            disabled={disabled}
          />
        </Cell>

        <Cell description="Action when threshold reached">
          <Select
            value={settings.thresholdAction}
            onChange={(e) => handleChange('thresholdAction', e.target.value as PunishmentType)}
            disabled={disabled}
          >
            {Object.entries(PUNISHMENT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </Cell>

        {(settings.thresholdAction === 'MUTE' || settings.thresholdAction === 'BAN') && (
          <Cell description="Duration (minutes, 0 = permanent)">
            <Input
              type="number"
              value={settings.thresholdDurationMinutes ?? 0}
              onChange={(e) => {
                const val = parseInt(e.target.value, 10);
                handleChange('thresholdDurationMinutes', isNaN(val) ? null : val);
              }}
              min={0}
              disabled={disabled}
            />
          </Cell>
        )}
      </Section>

      <Section header="Blocklist">
        <Cell description="Default action for blocked patterns">
          <Select
            value={settings.defaultBlocklistAction}
            onChange={(e) => handleChange('defaultBlocklistAction', e.target.value as PunishmentType)}
            disabled={disabled}
          >
            {Object.entries(PUNISHMENT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </Cell>
      </Section>
    </>
  );
}
