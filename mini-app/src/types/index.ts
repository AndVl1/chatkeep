// === Domain Types ===

export interface Chat {
  chatId: number;
  chatTitle: string;
  memberCount?: number;
  isBotAdmin: boolean;
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

export interface LinkedChannel {
  id: number;
  title: string;
}

export interface ChannelReply {
  enabled: boolean;
  replyText: string | null;
  mediaFileId: string | null;
  mediaType: 'PHOTO' | 'VIDEO' | 'DOCUMENT' | 'ANIMATION' | null;
  buttons: ReplyButton[];
  mediaHash?: string | null;
  hasMedia?: boolean;
  linkedChannel?: LinkedChannel | null;
}

export interface ReplyButton {
  text: string;
  url: string;
}

export interface UpdateChannelReplyRequest {
  enabled?: boolean;
  replyText?: string | null;
  buttons?: ReplyButton[];
}

export interface MediaUploadResponse {
  fileId: string;
  mediaType: 'PHOTO' | 'VIDEO' | 'DOCUMENT' | 'ANIMATION';
}

// === Session Types ===

export interface AdminSession {
  chatId: number;
  isConnected: boolean;
  connectedAt?: string;
  lastActivity?: string;
}

// === Admin Logs Types ===

export interface AdminLog {
  id: number;
  chatId: number;
  action: string;
  performedBy: number;
  performedByUsername?: string;
  targetUserId?: number;
  targetUsername?: string;
  details?: string;
  timestamp: string;
}

export interface AdminLogsResponse {
  logs: AdminLog[];
  totalCount: number;
  page: number;
  pageSize: number;
}

export interface AdminLogsFilter {
  action?: string;
  performedBy?: number;
  targetUserId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

// === Welcome Message Types ===

export interface WelcomeMessage {
  enabled: boolean;
  messageText: string | null;
  sendToChat: boolean;
  deleteAfterSeconds: number | null;
}

export interface UpdateWelcomeMessageRequest {
  enabled?: boolean;
  messageText?: string | null;
  sendToChat?: boolean;
  deleteAfterSeconds?: number | null;
}

// === Rules Types ===

export interface ChatRules {
  chatId: number;
  rulesText: string | null;
}

export interface UpdateRulesRequest {
  rulesText?: string | null;
}

// === Notes Types ===

export interface Note {
  id: number;
  noteName: string;
  content: string;
  chatId: number;
  createdBy: number;
  createdAt: string;
  updatedAt: string;
}

export interface AddNoteRequest {
  noteName: string;
  content: string;
}

export interface UpdateNoteRequest {
  noteName?: string;
  content?: string;
}

// === Anti-Flood Types ===

export interface AntiFloodSettings {
  chatId: number;
  enabled: boolean;
  maxMessages: number;
  timeWindowSeconds: number;
  action: PunishmentType;
  actionDurationMinutes: number | null;
}

export interface UpdateAntiFloodRequest {
  enabled?: boolean;
  maxMessages?: number;
  timeWindowSeconds?: number;
  action?: PunishmentType;
  actionDurationMinutes?: number | null;
}

// === Feature Capability Types ===

export interface FeatureCapability {
  id: string;
  category: 'MODERATION' | 'AUTOMATION' | 'SETTINGS' | 'CONTENT';
  name: string;
  description: string;
  commands?: string[];
}

// === Gated Features Types ===

export interface GatedFeature {
  key: string;
  enabled: boolean;
  name: string;
  description: string;
}

// === Twitch Integration Types ===

export interface TwitchChannel {
  id: number;
  twitchLogin: string;
  displayName: string;
  avatarUrl: string | null;
  isLive: boolean;
  isPinned: boolean;
  pinSilently: boolean;
}

export interface TwitchSearchResult {
  id: string;
  login: string;
  displayName: string;
  avatarUrl: string | null;
  isLive: boolean;
}

export interface TwitchSettings {
  messageTemplate: string;
  endedMessageTemplate: string;
  buttonText: string;
}

export interface AddTwitchChannelRequest {
  twitchLogin: string;
}

export interface UpdateTwitchSettingsRequest {
  messageTemplate: string;
  endedMessageTemplate: string;
  buttonText: string;
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
