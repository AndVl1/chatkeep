import { Placeholder } from '@telegram-apps/telegram-ui';

interface EmptyStateProps {
  title: string;
  description?: string;
  children?: React.ReactNode;
}

export function EmptyState({ title, description, children }: EmptyStateProps) {
  return (
    <Placeholder
      header={title}
      description={description}
    >
      {children}
    </Placeholder>
  );
}
