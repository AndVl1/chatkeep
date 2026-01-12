import { memo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Cell, IconButton } from '@telegram-apps/telegram-ui';
import type { BlocklistPattern } from '@/types';
import { formatDate, formatDuration } from '@/utils/formatters';

interface BlocklistItemProps {
  pattern: BlocklistPattern;
  onDelete: (patternId: number) => void;
}

export const BlocklistItem = memo(function BlocklistItem({
  pattern,
  onDelete,
}: BlocklistItemProps) {
  const { t } = useTranslation();

  const handleDelete = useCallback(() => {
    onDelete(pattern.id);
  }, [pattern.id, onDelete]);

  const subtitle = [
    t(`blocklist.${pattern.matchType.toLowerCase()}`),
    t(`punishment.${pattern.action}`),
    pattern.actionDurationMinutes !== null ? formatDuration(pattern.actionDurationMinutes) : null,
    `${t('blocklist.severity').split(' ')[0]}: ${pattern.severity}`,
  ].filter(Boolean).join(' • ');

  const description = `Added ${formatDate(pattern.createdAt)}`;

  return (
    <Cell
      subtitle={subtitle}
      description={description}
      after={
        <IconButton onClick={handleDelete} mode="plain" aria-label="Delete">
          <span style={{ color: 'var(--tgui--destructive_text_color)' }}>×</span>
        </IconButton>
      }
    >
      {pattern.pattern}
    </Cell>
  );
});
