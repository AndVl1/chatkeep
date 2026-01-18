import { useTranslation } from 'react-i18next';
import { IconButton, Spinner } from '@telegram-apps/telegram-ui';
import { Icon24Cancel } from '@telegram-apps/telegram-ui/dist/icons/24/cancel';

interface MediaPreviewProps {
  fileId: string;
  mediaType: 'PHOTO' | 'VIDEO' | 'DOCUMENT' | 'ANIMATION';
  onDelete: () => void;
  disabled?: boolean;
  isDeleting?: boolean;
}

export function MediaPreview({
  fileId,
  mediaType,
  onDelete,
  disabled = false,
  isDeleting = false,
}: MediaPreviewProps) {
  const { t } = useTranslation();

  // For now, we show a placeholder since we can't fetch the actual file URL
  // without backend support. In production, backend should provide a preview URL.
  const isVideo = mediaType === 'VIDEO' || mediaType === 'ANIMATION';

  return (
    <div
      style={{
        position: 'relative',
        display: 'inline-block',
        maxWidth: '100%',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '300px',
          aspectRatio: '16/9',
          backgroundColor: 'var(--tgui--secondary_bg_color)',
          borderRadius: '12px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '16px',
          textAlign: 'center',
        }}
      >
        <div>
          <div style={{ fontSize: '48px', marginBottom: '8px' }}>
            {isVideo ? '🎬' : '🖼️'}
          </div>
          <div style={{ fontSize: '14px', color: 'var(--tgui--hint_color)' }}>
            {t(`channelReply.mediaType.${mediaType}`)}
          </div>
          <div
            style={{
              fontSize: '12px',
              color: 'var(--tgui--hint_color)',
              marginTop: '4px',
              wordBreak: 'break-all',
            }}
          >
            {fileId.substring(0, 20)}...
          </div>
        </div>
      </div>
      <IconButton
        mode="plain"
        size="s"
        style={{
          position: 'absolute',
          top: '8px',
          right: '8px',
          backgroundColor: 'var(--tgui--secondary_bg_color)',
          borderRadius: '50%',
        }}
        onClick={onDelete}
        disabled={disabled || isDeleting}
      >
        {isDeleting ? <Spinner size="s" /> : <Icon24Cancel />}
      </IconButton>
    </div>
  );
}
