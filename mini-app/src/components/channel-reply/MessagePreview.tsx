import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Button, Caption } from '@telegram-apps/telegram-ui';
import type { ReplyButton } from '@/types';

interface MessagePreviewProps {
  text: string | null;
  hasMedia?: boolean;
  mediaType: 'PHOTO' | 'VIDEO' | 'DOCUMENT' | 'ANIMATION' | null;
  buttons: ReplyButton[];
}

export function MessagePreview({
  text,
  hasMedia,
  mediaType,
  buttons,
}: MessagePreviewProps) {
  const { t } = useTranslation();

  const hasContent = useMemo(
    () => !!text || !!hasMedia,
    [text, hasMedia]
  );

  const isVideo = useMemo(
    () => mediaType === 'VIDEO' || mediaType === 'ANIMATION',
    [mediaType]
  );

  if (!hasContent) {
    return (
      <Section header={t('channelReply.preview')}>
        <div style={{ padding: '16px', textAlign: 'center', color: 'var(--tgui--hint_color)' }}>
          {t('channelReply.previewEmpty')}
        </div>
      </Section>
    );
  }

  return (
    <Section header={t('channelReply.preview')}>
      <div
        style={{
          padding: '16px',
          backgroundColor: 'var(--tgui--secondary_bg_color)',
          borderRadius: '12px',
          margin: '12px',
        }}
      >
        {/* Media preview */}
        {hasMedia && (
          <div
            style={{
              width: '100%',
              maxWidth: '300px',
              aspectRatio: '16/9',
              backgroundColor: 'var(--tgui--bg_color)',
              borderRadius: '8px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: text ? '12px' : '0',
            }}
          >
            <div style={{ fontSize: '48px' }}>
              {isVideo ? '🎬' : '🖼️'}
            </div>
          </div>
        )}

        {/* Text */}
        {text && (
          <div
            style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontSize: '14px',
              lineHeight: '1.5',
            }}
          >
            {text}
          </div>
        )}

        {/* Buttons */}
        {buttons.length > 0 && (
          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '8px',
              marginTop: '12px',
            }}
          >
            {buttons.map((button, index) => (
              <Button
                key={index}
                size="s"
                mode="outline"
                stretched
                style={{ pointerEvents: 'none' }}
              >
                {button.text}
              </Button>
            ))}
          </div>
        )}
      </div>
      <Caption
        level="1"
        weight="3"
        style={{
          margin: '0 12px 12px',
          color: 'var(--tgui--hint_color)',
          textAlign: 'center',
        }}
      >
        {t('channelReply.previewNote')}
      </Caption>
    </Section>
  );
}
