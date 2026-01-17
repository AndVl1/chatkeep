import { Banner } from '@telegram-apps/telegram-ui';
import { useTranslation } from 'react-i18next';

interface AdminWarningBannerProps {
  className?: string;
}

export function AdminWarningBanner({ className }: AdminWarningBannerProps) {
  const { t } = useTranslation();

  return (
    <Banner
      type="section"
      className={className}
      header={t('warnings.botNotAdminShort')}
      description={t('warnings.botNotAdmin')}
      style={{
        backgroundColor: 'var(--tgui--destructive_text_color)',
        color: 'var(--tgui--white)',
        marginBottom: '16px',
      }}
    />
  );
}
