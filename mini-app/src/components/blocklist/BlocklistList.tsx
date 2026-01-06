import { Section } from '@telegram-apps/telegram-ui';
import { BlocklistItem } from './BlocklistItem';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';
import type { BlocklistPattern } from '@/types';

interface BlocklistListProps {
  patterns: BlocklistPattern[];
  isLoading: boolean;
  error: Error | null;
  onDelete: (patternId: number) => void;
  onRetry?: () => void;
}

export function BlocklistList({
  patterns,
  isLoading,
  error,
  onDelete,
  onRetry,
}: BlocklistListProps) {
  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={onRetry} />;
  }

  if (patterns.length === 0) {
    return (
      <EmptyState
        title="No Patterns"
        description="Add patterns to block specific words or phrases."
      />
    );
  }

  return (
    <Section header="Blocked Patterns">
      {patterns.map((pattern) => (
        <BlocklistItem
          key={pattern.id}
          pattern={pattern}
          onDelete={onDelete}
        />
      ))}
    </Section>
  );
}
