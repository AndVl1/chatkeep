import { useState, useMemo } from 'react';
import { Section, Tabbar } from '@telegram-apps/telegram-ui';
import { LockToggle } from './LockToggle';
import type { LockType, LockCategory } from '@/types';
import { LOCK_CATEGORIES } from '@/utils/constants';

interface LocksGridProps {
  locks: Record<string, { locked: boolean }>;
  onToggle: (lockType: LockType, locked: boolean) => void;
  disabled?: boolean;
}

export function LocksGrid({ locks, onToggle, disabled = false }: LocksGridProps) {
  const [activeCategory, setActiveCategory] = useState<LockCategory>('CONTENT');

  const categories = useMemo(() => Object.keys(LOCK_CATEGORIES) as LockCategory[], []);

  const currentLocks = useMemo(() => {
    return LOCK_CATEGORIES[activeCategory] || [];
  }, [activeCategory]);

  return (
    <div>
      <Tabbar>
        {categories.map((category) => (
          <Tabbar.Item
            key={category}
            selected={activeCategory === category}
            onClick={() => setActiveCategory(category)}
          >
            {category}
          </Tabbar.Item>
        ))}
      </Tabbar>

      <Section header={`${activeCategory} Locks`} style={{ paddingBottom: '70px' }}>
        {currentLocks.map((lockType) => (
          <LockToggle
            key={lockType}
            lockType={lockType}
            locked={locks[lockType]?.locked ?? false}
            onToggle={onToggle}
            disabled={disabled}
          />
        ))}
      </Section>
    </div>
  );
}
