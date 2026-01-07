import { useTranslation } from 'react-i18next';
import { Placeholder, Button } from '@telegram-apps/telegram-ui';

interface ErrorStateProps {
  error: Error;
  onRetry?: () => void;
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const { t } = useTranslation();

  return (
    <Placeholder
      header={t('common.error')}
      description={error.message || t('errors.generic')}
    >
      {onRetry && (
        <Button size="m" onClick={onRetry}>
          {t('common.retry')}
        </Button>
      )}
    </Placeholder>
  );
}
