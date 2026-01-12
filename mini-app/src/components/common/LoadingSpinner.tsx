import { Spinner } from '@telegram-apps/telegram-ui';

interface LoadingSpinnerProps {
  size?: 's' | 'm' | 'l';
}

export function LoadingSpinner({ size = 'l' }: LoadingSpinnerProps) {
  return (
    <div
      role="status"
      data-testid="loading-spinner"
      aria-label="Loading"
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '200px',
        width: '100%'
      }}
    >
      <Spinner size={size} />
    </div>
  );
}
