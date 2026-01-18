import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea, Caption } from '@telegram-apps/telegram-ui';
import { useNotification } from '@/hooks/ui/useNotification';

interface TextEditorProps {
  value: string | null;
  onChange: (value: string | null) => void;
  disabled?: boolean;
  maxLength?: number;
}

export function TextEditor({
  value,
  onChange,
  disabled = false,
  maxLength = 4096,
}: TextEditorProps) {
  const { t } = useTranslation();
  const { showNotification } = useNotification();

  const currentLength = useMemo(() => value?.length || 0, [value]);

  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = e.target.value;
    if (newValue.length > maxLength) {
      showNotification(t('channelReply.textTruncated', { maxLength }));
      onChange(newValue.substring(0, maxLength));
    } else {
      onChange(newValue || null);
    }
  }, [onChange, maxLength, showNotification, t]);

  return (
    <div>
      <Textarea
        value={value || ''}
        onChange={handleChange}
        disabled={disabled}
        placeholder={t('channelReply.textPlaceholder')}
        rows={6}
      />
      <Caption
        level="1"
        weight="3"
        style={{
          marginTop: '8px',
          textAlign: 'right',
          color: currentLength > maxLength * 0.9 ? 'var(--tgui--destructive_text_color)' : 'var(--tgui--hint_color)',
        }}
      >
        {currentLength} / {maxLength}
      </Caption>
    </div>
  );
}
