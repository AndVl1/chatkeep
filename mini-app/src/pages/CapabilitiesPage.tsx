import { useTranslation } from 'react-i18next';
import { Section, Card } from '@telegram-apps/telegram-ui';
import { CustomBackButton } from '@/components/common/CustomBackButton';

const CAPABILITIES = [
  {
    category: 'MODERATION' as const,
    features: [
      {
        id: 'warnings',
        name: 'capabilities.features.warnings',
        description: 'capabilities.descriptions.warnings',
        commands: ['/warn', '/clearwarns'],
      },
      {
        id: 'moderation',
        name: 'capabilities.features.moderation',
        description: 'capabilities.descriptions.moderation',
        commands: ['/mute', '/unmute', '/ban', '/unban', '/kick'],
      },
      {
        id: 'locks',
        name: 'capabilities.features.locks',
        description: 'capabilities.descriptions.locks',
        commands: ['/lock', '/unlock', '/locks'],
      },
    ],
  },
  {
    category: 'AUTOMATION' as const,
    features: [
      {
        id: 'welcome',
        name: 'capabilities.features.welcome',
        description: 'capabilities.descriptions.welcome',
        commands: ['/welcome', '/setwelcome', '/goodbye', '/setgoodbye'],
      },
      {
        id: 'antiflood',
        name: 'capabilities.features.antiflood',
        description: 'capabilities.descriptions.antiflood',
        commands: ['/antiflood', '/setflood'],
      },
      {
        id: 'channelReply',
        name: 'capabilities.features.channelReply',
        description: 'capabilities.descriptions.channelReply',
        commands: ['/channelreply'],
      },
    ],
  },
  {
    category: 'CONTENT' as const,
    features: [
      {
        id: 'blocklist',
        name: 'capabilities.features.blocklist',
        description: 'capabilities.descriptions.blocklist',
        commands: ['/blocklist', '/addfilter', '/rmfilter'],
      },
      {
        id: 'notes',
        name: 'capabilities.features.notes',
        description: 'capabilities.descriptions.notes',
        commands: ['/notes', '/save', '/get', '/clear'],
      },
      {
        id: 'rules',
        name: 'capabilities.features.rules',
        description: 'capabilities.descriptions.rules',
        commands: ['/rules', '/setrules'],
      },
    ],
  },
  {
    category: 'SETTINGS' as const,
    features: [
      {
        id: 'collection',
        name: 'capabilities.features.collection',
        description: 'capabilities.descriptions.collection',
        commands: ['/collection'],
      },
      {
        id: 'cleanService',
        name: 'capabilities.features.cleanService',
        description: 'capabilities.descriptions.cleanService',
        commands: ['/cleanservice'],
      },
      {
        id: 'logs',
        name: 'capabilities.features.logs',
        description: 'capabilities.descriptions.logs',
        commands: ['/setlog', '/unsetlog', '/logs'],
      },
    ],
  },
];

export function CapabilitiesPage() {
  const { t } = useTranslation();

  return (
    <div style={{ padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '8px' }}>
        <CustomBackButton to="/" />
        <h1 style={{ margin: 0, fontSize: '20px', flex: 1 }}>
          {t('capabilities.title')}
        </h1>
      </div>

      {CAPABILITIES.map((category) => (
        <Section key={category.category} header={t(`capabilities.categories.${category.category}`)}>
          {category.features.map((feature) => (
            <Card key={feature.id} style={{ padding: '12px', marginBottom: '8px' }}>
              <div style={{ marginBottom: '8px' }}>
                <strong>{t(feature.name)}</strong>
              </div>
              <p style={{ margin: '0 0 12px 0', fontSize: '14px' }}>
                {t(feature.description)}
              </p>
              {feature.commands && feature.commands.length > 0 && (
                <>
                  <div style={{ fontSize: '14px', fontWeight: 'bold', marginBottom: '4px' }}>
                    {t('capabilities.commands')}:
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                    {feature.commands.map((cmd) => (
                      <code
                        key={cmd}
                        style={{
                          backgroundColor: 'var(--tg-theme-secondary-bg-color)',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          fontSize: '13px',
                        }}
                      >
                        {cmd}
                      </code>
                    ))}
                  </div>
                </>
              )}
            </Card>
          ))}
        </Section>
      ))}
    </div>
  );
}
