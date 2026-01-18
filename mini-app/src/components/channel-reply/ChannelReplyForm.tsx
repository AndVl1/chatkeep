import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Cell, Switch } from '@telegram-apps/telegram-ui';
import { TextEditor } from './TextEditor';
import { MediaUploader } from './MediaUploader';
import { MediaPreview } from './MediaPreview';
import { ButtonManager } from './ButtonManager';
import { MessagePreview } from './MessagePreview';
import type { ChannelReply, ReplyButton } from '@/types';

interface ChannelReplyFormProps {
  data: ChannelReply;
  localText: string;
  onTextChange: (text: string) => void;
  onChange: (updates: Partial<ChannelReply>) => void;
  onMediaUpload: (file: File) => Promise<void>;
  onMediaDelete: () => Promise<void>;
  disabled?: boolean;
  isUploading?: boolean;
  isDeleting?: boolean;
}

export function ChannelReplyForm({
  data,
  localText,
  onTextChange,
  onChange,
  onMediaUpload,
  onMediaDelete,
  disabled = false,
  isUploading = false,
  isDeleting = false,
}: ChannelReplyFormProps) {
  const { t } = useTranslation();

  const handleTextChange = useCallback((text: string | null) => {
    onTextChange(text ?? '');
  }, [onTextChange]);

  const handleAddButton = useCallback((button: ReplyButton) => {
    onChange({ buttons: [...data.buttons, button] });
  }, [onChange, data.buttons]);

  const handleDeleteButton = useCallback((index: number) => {
    onChange({ buttons: data.buttons.filter((_, i) => i !== index) });
  }, [onChange, data.buttons]);

  return (
    <>
      {/* Enable/Disable Toggle */}
      <Section header={t('channelReply.title')}>
        <Cell
          Component="label"
          after={
            <Switch
              checked={data.enabled}
              onChange={(e) => onChange({ enabled: e.target.checked })}
              disabled={disabled}
            />
          }
          description={t('channelReply.enabledDescription')}
        >
          {t('channelReply.enabled')}
        </Cell>
      </Section>

      {/* Text Editor */}
      <Section header={t('channelReply.textSection')}>
        <div style={{ padding: '12px' }}>
          <TextEditor
            value={localText}
            onChange={handleTextChange}
            disabled={disabled || !data.enabled}
          />
        </div>
      </Section>

      {/* Media Upload/Preview */}
      <Section header={t('channelReply.mediaSection')}>
        <div style={{ padding: '12px' }}>
          {data.hasMedia && data.mediaType ? (
            <MediaPreview
              fileId={data.mediaFileId || data.mediaHash || 'media'}
              mediaType={data.mediaType}
              onDelete={onMediaDelete}
              disabled={disabled || !data.enabled}
              isDeleting={isDeleting}
            />
          ) : (
            <MediaUploader
              onUpload={onMediaUpload}
              disabled={disabled || !data.enabled}
              isUploading={isUploading}
            />
          )}
        </div>
      </Section>

      {/* Button Manager */}
      <ButtonManager
        buttons={data.buttons}
        onAdd={handleAddButton}
        onDelete={handleDeleteButton}
        disabled={disabled || !data.enabled}
      />

      {/* Message Preview */}
      {data.enabled && (
        <MessagePreview
          text={localText}
          hasMedia={data.hasMedia}
          mediaType={data.mediaType}
          buttons={data.buttons}
        />
      )}
    </>
  );
}
