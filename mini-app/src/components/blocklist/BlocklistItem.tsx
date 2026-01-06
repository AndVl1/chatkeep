import { memo, useCallback } from 'react';
import { Cell, IconButton } from '@telegram-apps/telegram-ui';
import type { BlocklistPattern } from '@/types';
import { PUNISHMENT_LABELS } from '@/utils/constants';
import { formatDate, formatDuration } from '@/utils/formatters';

interface BlocklistItemProps {
  pattern: BlocklistPattern;
  onDelete: (patternId: number) => void;
}

export const BlocklistItem = memo(function BlocklistItem({
  pattern,
  onDelete,
}: BlocklistItemProps) {
  const handleDelete = useCallback(() => {
    onDelete(pattern.id);
  }, [pattern.id, onDelete]);

  const subtitle = [
    `${pattern.matchType}`,
    PUNISHMENT_LABELS[pattern.action] || pattern.action,
    pattern.actionDurationMinutes !== null ? formatDuration(pattern.actionDurationMinutes) : null,
    `Severity: ${pattern.severity}`,
  ].filter(Boolean).join(' • ');

  const description = `Added ${formatDate(pattern.createdAt)}`;

  return (
    <Cell
      subtitle={subtitle}
      description={description}
      after={
        <IconButton onClick={handleDelete} mode="plain">
          <span style={{ color: 'var(--tgui--destructive_text_color)' }}>×</span>
        </IconButton>
      }
    >
      {pattern.pattern}
    </Cell>
  );
});
