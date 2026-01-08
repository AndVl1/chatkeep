import { memo } from 'react';
import { useTranslation } from 'react-i18next';
import { Cell, Switch } from '@telegram-apps/telegram-ui';
import type { LockType } from '@/types';

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
  const { t } = useTranslation();
  const label = t(`lockTypes.${lockType}`, lockType);

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
