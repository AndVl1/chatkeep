import type { LockCategory, LockType } from '@/types';

/**
 * Lock categories and their associated lock types.
 * Display labels are defined in i18n locales.
 */
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
