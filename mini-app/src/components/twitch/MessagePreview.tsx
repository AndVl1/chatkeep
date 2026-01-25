import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Caption } from '@telegram-apps/telegram-ui';

interface MessagePreviewProps {
  template: string;
}

export function MessagePreview({ template }: MessagePreviewProps) {
  const { t } = useTranslation();

  const preview = useMemo(() => {
    return template
      .replace('{streamer}', 'ExampleStreamer')
      .replace('{game}', 'Just Chatting')
      .replace('{title}', 'Talking with viewers | Come hang out!')
      .replace('{viewers}', '1,234')
      .replace('{duration}', '2:15');
  }, [template]);

  return (
    <Section header={t('twitch.preview')}>
      <div
        style={{
          padding: '12px',
          backgroundColor: 'var(--tgui--section_bg_color)',
          borderRadius: '8px',
          margin: '12px',
        }}
      >
        <Caption level="1" style={{ whiteSpace: 'pre-wrap' }}>
          {preview}
        </Caption>
        <div
          style={{
            marginTop: '12px',
            padding: '8px',
            backgroundColor: 'var(--tgui--secondary_bg_color)',
            borderRadius: '4px',
          }}
        >
          <Caption level="2" weight="3">
            {t('twitch.timelineAutoAdded')}
          </Caption>
        </div>
      </div>
    </Section>
  );
}
