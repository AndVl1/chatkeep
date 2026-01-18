import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Cell, Input, Button, Caption } from '@telegram-apps/telegram-ui';
import type { ReplyButton } from '@/types';

interface AddButtonFormProps {
  onAdd: (button: ReplyButton) => void;
  onCancel: () => void;
  maxButtons?: number;
  currentButtonCount?: number;
}

export function AddButtonForm({
  onAdd,
  onCancel,
  maxButtons = 10,
  currentButtonCount = 0,
}: AddButtonFormProps) {
  const { t } = useTranslation();
  const [text, setText] = useState('');
  const [url, setUrl] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<{ text?: string; url?: string }>({});

  const validate = useCallback(() => {
    const newErrors: { text?: string; url?: string } = {};

    if (!text.trim()) {
      newErrors.text = t('channelReply.buttonTextRequired');
    } else if (text.length > 64) {
      newErrors.text = t('channelReply.buttonTextTooLong');
    }

    if (!url.trim()) {
      newErrors.url = t('channelReply.buttonUrlRequired');
    } else if (!/^https?:\/\/.+/.test(url) && !/^tg:\/\/.+/.test(url)) {
      newErrors.url = t('channelReply.buttonUrlInvalid');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [text, url, t]);

  const handleSubmit = useCallback(async () => {
    if (!validate()) return;

    if (currentButtonCount >= maxButtons) {
      setErrors({ text: t('channelReply.maxButtonsReached') });
      return;
    }

    setIsSubmitting(true);
    try {
      onAdd({ text: text.trim(), url: url.trim() });
      // Reset form
      setText('');
      setUrl('');
      setErrors({});
    } catch (error) {
      // Error is handled by parent
    } finally {
      setIsSubmitting(false);
    }
  }, [validate, onAdd, text, url, currentButtonCount, maxButtons, t]);

  return (
    <Section header={t('channelReply.addButton')}>
      <Cell description={t('channelReply.buttonText')}>
        <Input
          placeholder={t('channelReply.buttonTextPlaceholder')}
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={64}
          disabled={isSubmitting}
        />
      </Cell>
      {errors.text && (
        <div style={{ padding: '0 16px', marginTop: '-8px', marginBottom: '8px' }}>
          <Caption level="1" weight="3" style={{ color: 'var(--tgui--destructive_text_color)' }}>
            {errors.text}
          </Caption>
        </div>
      )}
      <div style={{ padding: '0 16px', marginBottom: '8px' }}>
        <Caption level="1" weight="3" style={{ color: 'var(--tgui--hint_color)' }}>
          {text.length} / 64
        </Caption>
      </div>

      <Cell description={t('channelReply.buttonUrl')}>
        <Input
          placeholder="https://example.com or tg://..."
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          disabled={isSubmitting}
        />
      </Cell>
      {errors.url && (
        <div style={{ padding: '0 16px', marginTop: '-8px', marginBottom: '8px' }}>
          <Caption level="1" weight="3" style={{ color: 'var(--tgui--destructive_text_color)' }}>
            {errors.url}
          </Caption>
        </div>
      )}

      <div style={{ padding: '16px', display: 'flex', gap: '8px' }}>
        <Button
          size="m"
          stretched
          onClick={handleSubmit}
          disabled={!text.trim() || !url.trim() || isSubmitting}
        >
          {t('common.add')}
        </Button>
        <Button
          size="m"
          mode="outline"
          onClick={onCancel}
          disabled={isSubmitting}
        >
          {t('common.cancel')}
        </Button>
      </div>
    </Section>
  );
}
