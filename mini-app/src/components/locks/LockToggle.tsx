import { memo } from 'react';
import { Cell, Switch } from '@telegram-apps/telegram-ui';
import type { LockType } from '@/types';
import { LOCK_TYPE_LABELS } from '@/utils/constants';

interface LockToggleProps {
  lockType: LockType;
  locked: boolean;
  onToggle: (lockType: LockType, locked: boolean) => void;
  disabled?: boolean;
}

export const LockToggle = memo(function LockToggle({
  lockType,
  locked,
  onToggle,
  disabled = false,
}: LockToggleProps) {
  const label = LOCK_TYPE_LABELS[lockType] || lockType;

  return (
    <Cell
      Component="label"
      after={
        <Switch
          checked={locked}
          onChange={(e) => onToggle(lockType, e.target.checked)}
          disabled={disabled}
        />
      }
    >
      {label}
    </Cell>
  );
});
