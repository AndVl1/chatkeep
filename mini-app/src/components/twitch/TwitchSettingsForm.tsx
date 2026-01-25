import { useCallback, useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Section, Caption, Input } from '@telegram-apps/telegram-ui';
import { ChannelList } from './ChannelList';
import { ChannelSearchInput } from './ChannelSearchInput';
import { TemplateEditor } from './TemplateEditor';
import { MessagePreview } from './MessagePreview';
import { useConfirmDialog } from '@/hooks/ui/useConfirmDialog';
import { useNotification } from '@/hooks/ui/useNotification';
import type { TwitchChannel, TwitchSettings, AddTwitchChannelRequest } from '@/types';

interface TwitchSettingsFormProps {
  channels: TwitchChannel[];
  settings: TwitchSettings;
  onAddChannel: (data: AddTwitchChannelRequest) => Promise<void>;
  onRemoveChannel: (channelId: number) => Promise<void>;
  onUpdateSettings: (template: string, endedTemplate: string, buttonText: string) => Promise<void>;
  disabled?: boolean;
  maxChannels?: number;
}

export function TwitchSettingsForm({
  channels,
  settings,
  onAddChannel,
  onRemoveChannel,
  onUpdateSettings,
  disabled = false,
  maxChannels = 5,
}: TwitchSettingsFormProps) {
  const { t } = useTranslation();
  const { confirm } = useConfirmDialog();
  const { showSuccess, showError } = useNotification();
  const [isAdding, setIsAdding] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [template, setTemplate] = useState(settings.messageTemplate);
  const [endedTemplate, setEndedTemplate] = useState(settings.endedMessageTemplate);
  const [buttonText, setButtonText] = useState(settings.buttonText);
  const buttonTextDebounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Sync local state when settings prop changes (e.g., after refetch)
  useEffect(() => {
    setTemplate(settings.messageTemplate);
    setEndedTemplate(settings.endedMessageTemplate);
    setButtonText(settings.buttonText);
  }, [settings.messageTemplate, settings.endedMessageTemplate, settings.buttonText]);

  const handleAddChannel = useCallback(
    async (login: string) => {
      if (channels.length >= maxChannels) {
        showError(t('twitch.maxChannelsReached', { max: maxChannels }));
        return;
      }

      try {
        setIsAdding(true);
        await onAddChannel({ twitchLogin: login });
        showSuccess(t('twitch.channelAdded'));
      } catch (err) {
        showError((err as Error).message || t('twitch.addChannelError'));
      } finally {
        setIsAdding(false);
      }
    },
    [channels.length, maxChannels, onAddChannel, showSuccess, showError, t]
  );

  const handleRemoveChannel = useCallback(
    async (channelId: number) => {
      const confirmed = await confirm(
        t('twitch.confirmRemoveChannel'),
        t('common.delete')
      );
      if (!confirmed) return;

      try {
        setIsRemoving(true);
        await onRemoveChannel(channelId);
        showSuccess(t('twitch.channelRemoved'));
      } catch (err) {
        showError((err as Error).message || t('twitch.removeChannelError'));
      } finally {
        setIsRemoving(false);
      }
    },
    [confirm, onRemoveChannel, showSuccess, showError, t]
  );

  const handleTemplateChange = useCallback(
    async (newTemplate: string) => {
      setTemplate(newTemplate);
      try {
        await onUpdateSettings(newTemplate, endedTemplate, buttonText);
        showSuccess(t('settings.saveSuccess'));
      } catch (err) {
        showError((err as Error).message || t('twitch.updateSettingsError'));
      }
    },
    [onUpdateSettings, endedTemplate, buttonText, showSuccess, showError, t]
  );

  const handleEndedTemplateChange = useCallback(
    async (newEndedTemplate: string) => {
      setEndedTemplate(newEndedTemplate);
      try {
        await onUpdateSettings(template, newEndedTemplate, buttonText);
        showSuccess(t('settings.saveSuccess'));
      } catch (err) {
        showError((err as Error).message || t('twitch.updateSettingsError'));
      }
    },
    [onUpdateSettings, template, buttonText, showSuccess, showError, t]
  );

  const handleButtonTextChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newButtonText = e.target.value;
      if (newButtonText.length > 64) return;

      setButtonText(newButtonText);

      // Debounce the save
      if (buttonTextDebounceRef.current) {
        clearTimeout(buttonTextDebounceRef.current);
      }

      buttonTextDebounceRef.current = setTimeout(async () => {
        try {
          await onUpdateSettings(template, endedTemplate, newButtonText);
          showSuccess(t('settings.saveSuccess'));
        } catch (err) {
          showError((err as Error).message || t('twitch.updateSettingsError'));
        }
      }, 1000);
    },
    [onUpdateSettings, template, endedTemplate, showSuccess, showError, t]
  );

  // Cleanup debounce on unmount
  useEffect(() => {
    return () => {
      if (buttonTextDebounceRef.current) {
        clearTimeout(buttonTextDebounceRef.current);
      }
    };
  }, []);

  return (
    <div>
      <Section
        header={t('twitch.channels', { count: channels.length, max: maxChannels })}
      >
        <div style={{ padding: '12px' }}>
          <ChannelSearchInput
            onSelect={handleAddChannel}
            disabled={disabled || isAdding || channels.length >= maxChannels}
          />
        </div>

        <ChannelList
          channels={channels}
          onDelete={handleRemoveChannel}
          disabled={disabled || isRemoving}
        />

        {channels.length === 0 && (
          <div style={{ padding: '12px' }}>
            <Caption level="1" style={{ textAlign: 'center', color: 'var(--tgui--hint_color)' }}>
              {t('twitch.noChannels')}
            </Caption>
          </div>
        )}
      </Section>

      <TemplateEditor
        value={template}
        onChange={handleTemplateChange}
        disabled={disabled}
        title={t('twitch.startTemplate')}
      />

      <TemplateEditor
        value={endedTemplate}
        onChange={handleEndedTemplateChange}
        disabled={disabled}
        title={t('twitch.endedTemplate')}
      />

      <Section header={t('twitch.buttonSettings')}>
        <div style={{ padding: '12px' }}>
          <Input
            value={buttonText}
            onChange={handleButtonTextChange}
            disabled={disabled}
            placeholder={t('twitch.buttonTextPlaceholder')}
            maxLength={64}
          />
          <Caption
            level="2"
            weight="3"
            style={{ marginTop: '8px', color: 'var(--tgui--hint_color)' }}
          >
            {t('twitch.buttonTextHint')}
          </Caption>
        </div>
      </Section>

      <MessagePreview template={template} />
    </div>
  );
}
