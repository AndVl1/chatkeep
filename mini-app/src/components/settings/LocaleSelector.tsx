import { useTranslation } from 'react-i18next';
import { Select } from '@telegram-apps/telegram-ui';
import { useLocale } from '@/hooks/i18n/useLocale';

export function LocaleSelector() {
  const { t } = useTranslation();
  const { currentLocale, setLocale } = useLocale();

  return (
    <div style={{ marginBottom: '16px' }}>
      <label
        style={{
          display: 'block',
          marginBottom: '8px',
          fontWeight: '500',
          color: 'var(--tg-theme-text-color)',
        }}
      >
        {t('settings.language')}
      </label>
      <p
        style={{
          marginBottom: '8px',
          fontSize: '14px',
          color: 'var(--tg-theme-hint-color)',
        }}
      >
        {t('settings.languageDescription')}
      </p>
      <Select
        value={currentLocale}
        onChange={(e) => setLocale(e.target.value as 'en' | 'ru')}
        style={{ width: '100%' }}
      >
        <option value="en">English</option>
        <option value="ru">Русский</option>
      </Select>
    </div>
  );
}
