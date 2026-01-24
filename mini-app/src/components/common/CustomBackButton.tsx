import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '@telegram-apps/telegram-ui';
import { useAuthMode } from '@/hooks/auth/useAuthMode';

interface CustomBackButtonProps {
  /** Override default navigation (-1) with custom path */
  to?: string;
  /** Override default translation key */
  label?: string;
}

/**
 * Custom back button that hides automatically in Mini App mode
 * (where native Telegram back button is used instead)
 */
export function CustomBackButton({ to, label }: CustomBackButtonProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isMiniApp } = useAuthMode();

  // In Mini App mode, native Telegram back button is used
  if (isMiniApp) {
    return null;
  }

  const handleClick = () => {
    if (to) {
      navigate(to);
    } else {
      navigate(-1);
    }
  };

  return (
    <Button size="s" mode="plain" onClick={handleClick}>
      {label || t('common.back')}
    </Button>
  );
}
