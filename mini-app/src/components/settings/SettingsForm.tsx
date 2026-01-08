import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Cell, Switch, Select } from '@telegram-apps/telegram-ui';
import type { ChatSettings, PunishmentType } from '@/types';
import { DebouncedNumberInput } from './DebouncedNumberInput';
import { LocaleSelector } from './LocaleSelector';

interface SettingsFormProps {
  settings: ChatSettings;
  onChange: (updates: Partial<ChatSettings>) => void;
  disabled?: boolean;
}

export function SettingsForm({ settings, onChange, disabled }: SettingsFormProps) {
  const { t } = useTranslation();

  const handleChange = useCallback(<K extends keyof ChatSettings>(
    key: K,
    value: ChatSettings[K]
  ) => {
    onChange({ [key]: value });
  }, [onChange]);

  const punishmentTypes: PunishmentType[] = ['NOTHING', 'WARN', 'MUTE', 'BAN', 'KICK'];

  return (
    <>
      <Section header={t('settings.general')}>
        <Cell
          Component="label"
          after={
            <Switch
              checked={settings.collectionEnabled}
              onChange={(e) => handleChange('collectionEnabled', e.target.checked)}
              disabled={disabled}
            />
          }
          description={t('settings.collectionDescription')}
        >
          {t('settings.collectionEnabled')}
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
          description={t('settings.cleanServiceDescription')}
        >
          {t('settings.cleanService')}
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
          description={t('settings.lockWarningsDescription')}
        >
          {t('settings.lockWarnings')}
        </Cell>
      </Section>

      <Section header={t('settings.warningConfig')}>
        <Cell description={t('settings.maxWarnings')}>
          <DebouncedNumberInput
            value={settings.maxWarnings}
            onChange={(val) => handleChange('maxWarnings', val)}
            min={1}
            max={20}
            disabled={disabled}
          />
        </Cell>

        <Cell description={t('settings.warningExpiry')}>
          <DebouncedNumberInput
            value={settings.warningTtlHours}
            onChange={(val) => handleChange('warningTtlHours', val)}
            min={1}
            max={168}
            disabled={disabled}
          />
        </Cell>

        <Cell description={t('settings.thresholdAction')}>
          <Select
            value={settings.thresholdAction}
            onChange={(e) => handleChange('thresholdAction', e.target.value as PunishmentType)}
            disabled={disabled}
          >
            {punishmentTypes.map((value) => (
              <option key={value} value={value}>
                {t(`punishment.${value}`)}
              </option>
            ))}
          </Select>
        </Cell>

        {(settings.thresholdAction === 'MUTE' || settings.thresholdAction === 'BAN') && (
          <Cell description={t('settings.duration')}>
            <DebouncedNumberInput
              value={settings.thresholdDurationMinutes ?? 0}
              onChange={(val) => handleChange('thresholdDurationMinutes', val)}
              min={0}
              disabled={disabled}
            />
          </Cell>
        )}
      </Section>

      <Section header={t('settings.blocklist')}>
        <Cell description={t('settings.blocklistAction')}>
          <Select
            value={settings.defaultBlocklistAction}
            onChange={(e) => handleChange('defaultBlocklistAction', e.target.value as PunishmentType)}
            disabled={disabled}
          >
            {punishmentTypes.map((value) => (
              <option key={value} value={value}>
                {t(`punishment.${value}`)}
              </option>
            ))}
          </Select>
        </Cell>
      </Section>

      <Section header={t('settings.language')}>
        <LocaleSelector />
      </Section>
    </>
  );
}
