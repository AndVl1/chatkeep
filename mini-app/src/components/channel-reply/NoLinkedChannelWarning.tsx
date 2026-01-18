import { useTranslation } from 'react-i18next';
import { Banner } from '@telegram-apps/telegram-ui';

export function NoLinkedChannelWarning() {
  const { t } = useTranslation();

  return (
    <Banner
      header={t('channelReply.noLinkedChannel')}
      type="section"
      style={{
        marginBottom: '16px',
        backgroundColor: 'var(--tgui--secondary_bg_color)',
        borderLeft: '4px solid var(--tgui--button_color)',
      }}
    >
      {t('channelReply.noLinkedChannelDescription')}
    </Banner>
  );
}
