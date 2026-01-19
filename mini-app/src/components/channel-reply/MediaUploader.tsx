import { useRef, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Spinner } from '@telegram-apps/telegram-ui';
import { useNotification } from '@/hooks/ui/useNotification';

interface MediaUploaderProps {
  onUpload: (file: File) => Promise<void>;
  disabled?: boolean;
  isUploading?: boolean;
}

const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
const ALLOWED_MIME_TYPES = [
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'video/mp4',
  'video/webm',
  'video/quicktime',
];

export function MediaUploader({
  onUpload,
  disabled = false,
  isUploading = false,
}: MediaUploaderProps) {
  const { t } = useTranslation();
  const { showError } = useNotification();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleButtonClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleFileChange = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate MIME type
    if (!ALLOWED_MIME_TYPES.includes(file.type)) {
      showError(t('channelReply.invalidFileType'));
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }

    // Validate size
    if (file.size > MAX_FILE_SIZE) {
      showError(t('channelReply.fileTooLarge'));
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }

    try {
      await onUpload(file);
      // Clear input to allow re-uploading the same file
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (error) {
      // Error is handled by parent
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  }, [onUpload, showError, t]);

  return (
    <div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*,video/*"
        style={{ display: 'none' }}
        onChange={handleFileChange}
        disabled={disabled || isUploading}
      />
      <Button
        size="m"
        mode="outline"
        onClick={handleButtonClick}
        disabled={disabled || isUploading}
        before={isUploading ? <Spinner size="s" /> : undefined}
      >
        {isUploading ? t('channelReply.uploading') : t('channelReply.uploadMedia')}
      </Button>
    </div>
  );
}
