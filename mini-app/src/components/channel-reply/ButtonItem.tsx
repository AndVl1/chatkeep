import { memo } from 'react';
import { Cell, IconButton } from '@telegram-apps/telegram-ui';
import { Icon24Cancel } from '@telegram-apps/telegram-ui/dist/icons/24/cancel';
import type { ReplyButton } from '@/types';

interface ButtonItemProps {
  button: ReplyButton;
  onDelete: () => void;
  disabled?: boolean;
}

export const ButtonItem = memo(function ButtonItem({
  button,
  onDelete,
  disabled = false,
}: ButtonItemProps) {
  return (
    <Cell
      subtitle={button.url}
      after={
        <IconButton
          mode="plain"
          size="s"
          onClick={onDelete}
          disabled={disabled}
        >
          <Icon24Cancel />
        </IconButton>
      }
    >
      {button.text}
    </Cell>
  );
});
