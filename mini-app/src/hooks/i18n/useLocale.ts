import { useTranslation } from 'react-i18next';
import { useCallback } from 'react';

export type SupportedLocale = 'en' | 'ru';

export function useLocale() {
  const { i18n } = useTranslation();

  const currentLocale = i18n.language as SupportedLocale;

  const setLocale = useCallback((locale: SupportedLocale) => {
    i18n.changeLanguage(locale);
    // TODO: Sync with backend when API is ready
    // This would persist the user's language preference
  }, [i18n]);

  return { currentLocale, setLocale };
}
