// === Domain Types ===

export interface Chat {
  chatId: number;
  chatTitle: string;
  memberCount?: number;
}

export interface ChatSettings {
  chatId: number;
  chatTitle: string;
  collectionEnabled: boolean;
  cleanServiceEnabled: boolean;
  maxWarnings: number;
  warningTtlHours: number;
  thresholdAction: PunishmentType;
  thresholdDurationMinutes: number | null;
  defaultBlocklistAction: PunishmentType;
  logChannelId: number | null;
  lockWarnsEnabled: boolean;
}

export type PunishmentType = 'NOTHING' | 'WARN' | 'MUTE' | 'BAN' | 'KICK';

export interface LockInfo {
  locked: boolean;
  reason?: string;
}

export type LockCategory = 'CONTENT' | 'FORWARD' | 'URL' | 'TEXT' | 'ENTITY' | 'OTHER';

export type LockType =
  // CONTENT
  | 'PHOTO' | 'VIDEO' | 'AUDIO' | 'VOICE' | 'DOCUMENT' | 'STICKER'
  | 'GIF' | 'VIDEONOTE' | 'CONTACT' | 'LOCATION' | 'VENUE' | 'DICE' | 'POLL' | 'GAME'
  // FORWARD
  | 'FORWARD' | 'FORWARDUSER' | 'FORWARDCHANNEL' | 'FORWARDBOT' | 'CHANNELPOST'
  // URL
  | 'URL' | 'BUTTON' | 'INVITE' | 'LINK' | 'TEXTLINK' | 'LINKPREVIEW'
  // TEXT
  | 'TEXT' | 'COMMANDS' | 'EMAIL' | 'PHONE' | 'SPOILER' | 'CAPTION'
  // ENTITY
  | 'MENTION' | 'HASHTAG' | 'CASHTAG' | 'EMOJIGAME' | 'EMOJI' | 'INLINE'
  // OTHER
  | 'RTLCHAR' | 'ANONCHANNEL' | 'COMMENT' | 'ALBUM' | 'TOPIC'
  | 'PREMIUM' | 'SIGNATURE' | 'EDIT' | 'SERVICE' | 'NEWMEMBERS' | 'LEFTMEMBER' | 'PINNED';

export interface BlocklistPattern {
  id: number;
  pattern: string;
  matchType: 'EXACT' | 'WILDCARD';
  action: PunishmentType;
  actionDurationMinutes: number | null;
  severity: number;
  createdAt: string;
}

export interface AddBlocklistPatternRequest {
  pattern: string;
  matchType: 'EXACT' | 'WILDCARD';
  action: PunishmentType;
  actionDurationMinutes: number | null;
  severity: number;
}

export interface LockSettings {
  chatId: number;
  locks: Record<string, LockInfo>;
  lockWarnsEnabled: boolean;
}

export interface UpdateLocksRequest {
  locks: Record<string, LockInfo>;
  lockWarnsEnabled?: boolean;
}

// === Telegram Types ===

export interface TelegramUser {
  id: number;
  firstName: string;
  lastName?: string;
  username?: string;
  isPremium: boolean;
  languageCode?: string;
}

export interface TelegramAuthData {
  id: number;
  first_name: string;
  last_name?: string;
  username?: string;
  photo_url?: string;
  auth_date: number;
  hash: string;
}

// Telegram WebApp type declarations
export interface TelegramWebApp {
  initData: string;
  initDataUnsafe: {
    user?: {
      id: number;
      first_name: string;
      last_name?: string;
      username?: string;
      language_code?: string;
      is_premium?: boolean;
    };
    [key: string]: any;
  };
  ready: () => void;
  expand: () => void;
  close: () => void;
  [key: string]: any;
}

declare global {
  interface Window {
    Telegram?: {
      WebApp: TelegramWebApp;
    };
  }
}
