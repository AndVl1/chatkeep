import { Placeholder, Button } from '@telegram-apps/telegram-ui';

interface ErrorStateProps {
  error: Error;
  onRetry?: () => void;
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  return (
    <Placeholder
      header="Error"
      description={error.message || 'Something went wrong'}
    >
      {onRetry && (
        <Button size="m" onClick={onRetry}>
          Retry
        </Button>
      )}
    </Placeholder>
  );
}
