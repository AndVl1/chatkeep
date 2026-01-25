import { useCallback, useMemo, useRef, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Textarea, Caption, Section } from '@telegram-apps/telegram-ui';
import { useNotification } from '@/hooks/ui/useNotification';

interface TemplateEditorProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  title?: string;
  placeholder?: string;
}

const VARIABLES = [
  '{streamer}',
  '{game}',
  '{title}',
  '{viewers}',
  '{duration}',
];

export function TemplateEditor({
  value,
  onChange,
  disabled = false,
  title,
  placeholder,
}: TemplateEditorProps) {
  const { t } = useTranslation();
  const sectionTitle = title ?? t('twitch.messageTemplate');
  const inputPlaceholder = placeholder ?? t('twitch.templatePlaceholder');
  const { showNotification } = useNotification();
  const maxLength = 2048;
  const debounceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Local state for immediate UI updates (no lag)
  const [localValue, setLocalValue] = useState(value);

  // Sync local value when prop changes (e.g., from server)
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  const currentLength = useMemo(() => localValue?.length || 0, [localValue]);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const newValue = e.target.value;
      if (newValue.length > maxLength) {
        showNotification(t('twitch.templateTruncated', { maxLength }));
        return;
      }

      // Update local state immediately (no lag)
      setLocalValue(newValue);

      // Debounce the API call
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }

      debounceTimeoutRef.current = setTimeout(() => {
        setIsSaving(true);
        onChange(newValue);
        // Reset saving indicator after a short delay
        setTimeout(() => setIsSaving(false), 500);
      }, 1000);
    },
    [onChange, maxLength, showNotification, t]
  );

  useEffect(() => {
    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, []);

  return (
    <Section header={sectionTitle}>
      <div style={{ padding: '12px' }}>
        <Textarea
          value={localValue}
          onChange={handleChange}
          disabled={disabled}
          placeholder={inputPlaceholder}
          rows={6}
        />
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '8px' }}>
          <Caption
            level="1"
            weight="3"
            style={{
              color: isSaving ? 'var(--tgui--link_color)' : 'transparent',
              transition: 'color 0.2s',
            }}
          >
            {isSaving ? t('common.save') + '...' : ''}
          </Caption>
          <Caption
            level="1"
            weight="3"
            style={{
              color:
                currentLength > maxLength * 0.9
                  ? 'var(--tgui--destructive_text_color)'
                  : 'var(--tgui--hint_color)',
            }}
          >
            {currentLength} / {maxLength}
          </Caption>
        </div>

        <Caption
          level="2"
          weight="3"
          style={{ marginTop: '12px', display: 'block' }}
        >
          {t('twitch.availableVariables')}
        </Caption>
        <div
          style={{
            marginTop: '8px',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '8px',
          }}
        >
          {VARIABLES.map(variable => (
            <code
              key={variable}
              style={{
                padding: '4px 8px',
                backgroundColor: 'var(--tgui--section_bg_color)',
                borderRadius: '4px',
                fontSize: '12px',
                fontFamily: 'monospace',
              }}
            >
              {variable}
            </code>
          ))}
        </div>
      </div>
    </Section>
  );
}
