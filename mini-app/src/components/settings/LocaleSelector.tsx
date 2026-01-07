import { useTranslation } from 'react-i18next';
import { Cell, Select } from '@telegram-apps/telegram-ui';
import { useLocale } from '@/hooks/i18n/useLocale';

export function LocaleSelector() {
  const { t } = useTranslation();
  const { currentLocale, setLocale } = useLocale();

  return (
    <Cell description={t('settings.languageDescription')}>
      <Select
        value={currentLocale}
        onChange={(e) => setLocale(e.target.value as 'en' | 'ru')}
      >
        <option value="en">English</option>
        <option value="ru">Русский</option>
      </Select>
    </Cell>
  );
}
