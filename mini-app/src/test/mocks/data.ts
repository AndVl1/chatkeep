/**
 * Mock data fixtures for UI testing
 * Provides consistent test data across all tests
 */

import type { Chat, ChatSettings, BlocklistPattern, LockSettings, LockType } from '@/types';

// === Chat Fixtures ===

export const mockChats: Chat[] = [
  { chatId: 100, chatTitle: 'Test Group 1', memberCount: 50, isBotAdmin: true },
  { chatId: 200, chatTitle: 'Test Group 2', memberCount: 150, isBotAdmin: false },
  { chatId: 300, chatTitle: 'Admin Chat', memberCount: 10, isBotAdmin: true },
];

export const mockEmptyChats: Chat[] = [];

// === Settings Fixtures ===

export const mockSettings: ChatSettings = {
  chatId: 100,
  chatTitle: 'Test Group 1',
  collectionEnabled: true,
  cleanServiceEnabled: false,
  maxWarnings: 3,
  warningTtlHours: 24,
  thresholdAction: 'WARN',
  thresholdDurationMinutes: null,
  defaultBlocklistAction: 'NOTHING',
  logChannelId: null,
  lockWarnsEnabled: false,
};

export const mockSettingsAllEnabled: ChatSettings = {
  chatId: 100,
  chatTitle: 'Test Group 1',
  collectionEnabled: true,
  cleanServiceEnabled: true,
  maxWarnings: 5,
  warningTtlHours: 48,
  thresholdAction: 'BAN',
  thresholdDurationMinutes: 60,
  defaultBlocklistAction: 'KICK',
  logChannelId: -1001234567890,
  lockWarnsEnabled: true,
};

export const mockSettingsDisabled: ChatSettings = {
  chatId: 100,
  chatTitle: 'Test Group 1',
  collectionEnabled: false,
  cleanServiceEnabled: false,
  maxWarnings: 1,
  warningTtlHours: 1,
  thresholdAction: 'NOTHING',
  thresholdDurationMinutes: null,
  defaultBlocklistAction: 'NOTHING',
  logChannelId: null,
  lockWarnsEnabled: false,
};

// === Blocklist Fixtures ===

export const mockBlocklistPatterns: BlocklistPattern[] = [
  {
    id: 1,
    pattern: 'spam',
    matchType: 'WILDCARD',
    action: 'WARN',
    actionDurationMinutes: null,
    severity: 1,
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 2,
    pattern: 'http://bad-link.com',
    matchType: 'EXACT',
    action: 'BAN',
    actionDurationMinutes: 60,
    severity: 5,
    createdAt: '2024-01-02T00:00:00Z',
  },
  {
    id: 3,
    pattern: 'offensive*word',
    matchType: 'WILDCARD',
    action: 'MUTE',
    actionDurationMinutes: 30,
    severity: 3,
    createdAt: '2024-01-03T00:00:00Z',
  },
];

export const mockEmptyBlocklist: BlocklistPattern[] = [];

export const mockNewPattern: BlocklistPattern = {
  id: 4,
  pattern: 'new-pattern',
  matchType: 'EXACT',
  action: 'WARN',
  actionDurationMinutes: null,
  severity: 1,
  createdAt: '2024-01-04T00:00:00Z',
};

// === Lock Fixtures ===

const createLocks = (lockedTypes: LockType[]): Record<string, { locked: boolean; reason?: string }> => {
  const allLockTypes: LockType[] = [
    'PHOTO', 'VIDEO', 'AUDIO', 'VOICE', 'DOCUMENT', 'STICKER',
    'GIF', 'VIDEONOTE', 'CONTACT', 'LOCATION', 'VENUE', 'DICE', 'POLL', 'GAME',
    'FORWARD', 'FORWARDUSER', 'FORWARDCHANNEL', 'FORWARDBOT', 'CHANNELPOST',
    'URL', 'BUTTON', 'INVITE', 'LINK', 'TEXTLINK', 'LINKPREVIEW',
    'TEXT', 'COMMANDS', 'EMAIL', 'PHONE', 'SPOILER', 'CAPTION',
    'MENTION', 'HASHTAG', 'CASHTAG', 'EMOJIGAME', 'EMOJI', 'INLINE',
    'RTLCHAR', 'ANONCHANNEL', 'COMMENT', 'ALBUM', 'TOPIC',
    'PREMIUM', 'SIGNATURE', 'EDIT', 'SERVICE', 'NEWMEMBERS', 'LEFTMEMBER', 'PINNED',
  ];

  return allLockTypes.reduce((acc, type) => {
    acc[type] = { locked: lockedTypes.includes(type), reason: undefined };
    return acc;
  }, {} as Record<string, { locked: boolean; reason?: string }>);
};

export const mockLocks: LockSettings = {
  chatId: 100,
  locks: createLocks(['PHOTO', 'VIDEO', 'FORWARD']),
  lockWarnsEnabled: false,
};

export const mockLocksAllUnlocked: LockSettings = {
  chatId: 100,
  locks: createLocks([]),
  lockWarnsEnabled: false,
};

export const mockLocksStrictMode: LockSettings = {
  chatId: 100,
  locks: createLocks([
    'PHOTO', 'VIDEO', 'AUDIO', 'VOICE', 'DOCUMENT', 'STICKER', 'GIF',
    'FORWARD', 'FORWARDUSER', 'FORWARDCHANNEL', 'FORWARDBOT',
    'URL', 'INVITE', 'LINK',
  ]),
  lockWarnsEnabled: true,
};

// === Error Scenarios ===

export const createApiError = (status: number, message: string) => ({
  status,
  message,
  error: message,
});

export const mockNetworkError = new Error('Network error');
export const mockUnauthorizedError = createApiError(401, 'Unauthorized');
export const mockNotFoundError = createApiError(404, 'Chat not found');
export const mockServerError = createApiError(500, 'Internal server error');

// === Factory Functions ===

export function createChat(overrides: Partial<Chat> = {}): Chat {
  return {
    chatId: Math.floor(Math.random() * 1000000),
    chatTitle: 'Generated Chat',
    memberCount: 100,
    isBotAdmin: true,
    ...overrides,
  };
}

export function createSettings(chatId: number, overrides: Partial<ChatSettings> = {}): ChatSettings {
  return {
    ...mockSettings,
    chatId,
    chatTitle: `Chat ${chatId}`,
    ...overrides,
  };
}

export function createBlocklistPattern(overrides: Partial<BlocklistPattern> = {}): BlocklistPattern {
  return {
    id: Math.floor(Math.random() * 1000000),
    pattern: 'test-pattern',
    matchType: 'EXACT',
    action: 'WARN',
    actionDurationMinutes: null,
    severity: 1,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

export function createLockSettings(chatId: number, lockedTypes: LockType[] = []): LockSettings {
  return {
    chatId,
    locks: createLocks(lockedTypes),
    lockWarnsEnabled: false,
  };
}
