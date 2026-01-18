import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Button, Placeholder } from '@telegram-apps/telegram-ui';
import { ButtonItem } from './ButtonItem';
import { AddButtonForm } from './AddButtonForm';
import type { ReplyButton } from '@/types';

interface ButtonManagerProps {
  buttons: ReplyButton[];
  onAdd: (button: ReplyButton) => void;
  onDelete: (index: number) => void;
  disabled?: boolean;
  maxButtons?: number;
}

export function ButtonManager({
  buttons,
  onAdd,
  onDelete,
  disabled = false,
  maxButtons = 10,
}: ButtonManagerProps) {
  const { t } = useTranslation();
  const [isAddFormOpen, setIsAddFormOpen] = useState(false);

  const handleAdd = useCallback((button: ReplyButton) => {
    onAdd(button);
    setIsAddFormOpen(false);
  }, [onAdd]);

  const handleDelete = useCallback((index: number) => {
    onDelete(index);
  }, [onDelete]);

  return (
    <>
      <Section header={t('channelReply.buttonsSection')}>
        {buttons.length === 0 ? (
          <Placeholder description={t('channelReply.noButtons')} />
        ) : (
          buttons.map((button, index) => (
            <ButtonItem
              key={index}
              button={button}
              onDelete={() => handleDelete(index)}
              disabled={disabled}
            />
          ))
        )}
        <div style={{ padding: '12px' }}>
          <Button
            size="m"
            mode="outline"
            stretched
            onClick={() => setIsAddFormOpen(true)}
            disabled={disabled || buttons.length >= maxButtons}
          >
            {t('channelReply.addButton')}
          </Button>
          {buttons.length >= maxButtons && (
            <div style={{ textAlign: 'center', marginTop: '8px', fontSize: '12px', color: 'var(--tgui--hint_color)' }}>
              {t('channelReply.maxButtonsInfo', { max: maxButtons })}
            </div>
          )}
        </div>
      </Section>

      {isAddFormOpen && (
        <AddButtonForm
          onAdd={handleAdd}
          onCancel={() => setIsAddFormOpen(false)}
          maxButtons={maxButtons}
          currentButtonCount={buttons.length}
        />
      )}
    </>
  );
}
