import { useTranslation } from 'react-i18next';
import { useCallback } from 'react';
import client from '../../api/client';

export type SupportedLocale = 'en' | 'ru';

export function useLocale() {
  const { i18n } = useTranslation();

  const currentLocale = i18n.language as SupportedLocale;

  const setLocale = useCallback(async (locale: SupportedLocale) => {
    i18n.changeLanguage(locale);
    try {
      await client.put('preferences', { json: { locale } });
    } catch (error) {
      console.error('Failed to save locale preference:', error);
    }
  }, [i18n]);

  return { currentLocale, setLocale };
}
