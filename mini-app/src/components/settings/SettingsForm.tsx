import { useState, useCallback } from 'react';
import { Section, Cell, Switch, Input, Select } from '@telegram-apps/telegram-ui';
import type { ChatSettings, PunishmentType } from '@/types';
import { PUNISHMENT_LABELS } from '@/utils/constants';

interface SettingsFormProps {
  settings: ChatSettings;
  onChange: (updates: Partial<ChatSettings>) => void;
}

export function SettingsForm({ settings, onChange }: SettingsFormProps) {
  const [localSettings, setLocalSettings] = useState(settings);

  const handleChange = useCallback(<K extends keyof ChatSettings>(
    key: K,
    value: ChatSettings[K]
  ) => {
    setLocalSettings(prev => ({ ...prev, [key]: value }));
    onChange({ [key]: value });
  }, [onChange]);

  return (
    <>
      <Section header="General Settings">
        <Cell
          Component="label"
          after={
            <Switch
              checked={localSettings.collectionEnabled}
              onChange={(e) => handleChange('collectionEnabled', e.target.checked)}
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
              checked={localSettings.cleanServiceEnabled}
              onChange={(e) => handleChange('cleanServiceEnabled', e.target.checked)}
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
              checked={localSettings.lockWarnsEnabled}
              onChange={(e) => handleChange('lockWarnsEnabled', e.target.checked)}
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
            value={localSettings.maxWarnings}
            onChange={(e) => {
              const val = parseInt(e.target.value);
              handleChange('maxWarnings', isNaN(val) ? 1 : Math.max(1, val));
            }}
            min={1}
            max={10}
          />
        </Cell>

        <Cell description="Warning expiry time (hours)">
          <Input
            type="number"
            value={localSettings.warningTtlHours}
            onChange={(e) => {
              const val = parseInt(e.target.value);
              handleChange('warningTtlHours', isNaN(val) ? 24 : Math.max(1, val));
            }}
            min={1}
            max={720}
          />
        </Cell>

        <Cell description="Action when threshold reached">
          <Select
            value={localSettings.thresholdAction}
            onChange={(e) => handleChange('thresholdAction', e.target.value as PunishmentType)}
          >
            {Object.entries(PUNISHMENT_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
        </Cell>

        {(localSettings.thresholdAction === 'MUTE' || localSettings.thresholdAction === 'BAN') && (
          <Cell description="Duration (minutes, 0 = permanent)">
            <Input
              type="number"
              value={localSettings.thresholdDurationMinutes ?? 0}
              onChange={(e) => {
                const val = parseInt(e.target.value);
                handleChange('thresholdDurationMinutes', isNaN(val) ? null : val);
              }}
              min={0}
            />
          </Cell>
        )}
      </Section>

      <Section header="Blocklist">
        <Cell description="Default action for blocked patterns">
          <Select
            value={localSettings.defaultBlocklistAction}
            onChange={(e) => handleChange('defaultBlocklistAction', e.target.value as PunishmentType)}
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
