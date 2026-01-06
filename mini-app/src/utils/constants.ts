import type { LockCategory, LockType } from '@/types';

export const LOCK_CATEGORIES: Record<LockCategory, LockType[]> = {
  CONTENT: [
    'PHOTO', 'VIDEO', 'AUDIO', 'VOICE', 'DOCUMENT', 'STICKER',
    'GIF', 'VIDEONOTE', 'CONTACT', 'LOCATION', 'VENUE', 'DICE', 'POLL', 'GAME'
  ],
  FORWARD: [
    'FORWARD', 'FORWARDUSER', 'FORWARDCHANNEL', 'FORWARDBOT', 'CHANNELPOST'
  ],
  URL: [
    'URL', 'BUTTON', 'INVITE', 'LINK', 'TEXTLINK', 'LINKPREVIEW'
  ],
  TEXT: [
    'TEXT', 'COMMANDS', 'EMAIL', 'PHONE', 'SPOILER', 'CAPTION'
  ],
  ENTITY: [
    'MENTION', 'HASHTAG', 'CASHTAG', 'EMOJIGAME', 'EMOJI', 'INLINE'
  ],
  OTHER: [
    'RTLCHAR', 'ANONCHANNEL', 'COMMENT', 'ALBUM', 'TOPIC',
    'PREMIUM', 'SIGNATURE', 'EDIT', 'SERVICE', 'NEWMEMBERS', 'LEFTMEMBER', 'PINNED'
  ],
};

export const LOCK_TYPE_LABELS: Record<LockType, string> = {
  // CONTENT
  PHOTO: 'Photos',
  VIDEO: 'Videos',
  AUDIO: 'Audio files',
  VOICE: 'Voice messages',
  DOCUMENT: 'Documents',
  STICKER: 'Stickers',
  GIF: 'GIFs',
  VIDEONOTE: 'Video notes',
  CONTACT: 'Contacts',
  LOCATION: 'Locations',
  VENUE: 'Venues',
  DICE: 'Dice',
  POLL: 'Polls',
  GAME: 'Games',

  // FORWARD
  FORWARD: 'All forwards',
  FORWARDUSER: 'Forwards from users',
  FORWARDCHANNEL: 'Forwards from channels',
  FORWARDBOT: 'Forwards from bots',
  CHANNELPOST: 'Channel posts',

  // URL
  URL: 'URLs',
  BUTTON: 'Inline buttons',
  INVITE: 'Invite links',
  LINK: 'Links',
  TEXTLINK: 'Text links',
  LINKPREVIEW: 'Link previews',

  // TEXT
  TEXT: 'Text messages',
  COMMANDS: 'Bot commands',
  EMAIL: 'Email addresses',
  PHONE: 'Phone numbers',
  SPOILER: 'Spoilers',
  CAPTION: 'Captions',

  // ENTITY
  MENTION: 'Mentions',
  HASHTAG: 'Hashtags',
  CASHTAG: 'Cashtags',
  EMOJIGAME: 'Emoji games',
  EMOJI: 'Custom emojis',
  INLINE: 'Inline bots',

  // OTHER
  RTLCHAR: 'RTL characters',
  ANONCHANNEL: 'Anonymous channels',
  COMMENT: 'Comments',
  ALBUM: 'Media albums',
  TOPIC: 'Topics',
  PREMIUM: 'Premium content',
  SIGNATURE: 'Signatures',
  EDIT: 'Edited messages',
  SERVICE: 'Service messages',
  NEWMEMBERS: 'New members',
  LEFTMEMBER: 'Left members',
  PINNED: 'Pinned messages',
};

export const PUNISHMENT_LABELS: Record<string, string> = {
  NOTHING: 'Do nothing',
  WARN: 'Warn',
  MUTE: 'Mute',
  BAN: 'Ban',
  KICK: 'Kick',
};
