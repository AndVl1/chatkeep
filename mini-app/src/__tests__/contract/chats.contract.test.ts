/**
 * Chats Contract Tests
 *
 * Verifies that the frontend can correctly deserialize backend responses
 * for chat-related endpoints.
 */

import { describe, it, expect } from 'vitest';
import { loadJsonFixture, fixtureExists } from './contractTestUtils';
import type { Chat } from '@/types';

describe('Chats Contract', () => {
  it.skipIf(!fixtureExists('chats_list_response.json'))(
    'should deserialize chats list response from backend schema',
    () => {
      const result = loadJsonFixture<Chat[]>('chats_list_response.json');

      expect(Array.isArray(result)).toBe(true);

      if (result.length > 0) {
        const chat = result[0];

        expect(chat).toHaveProperty('chatId');
        expect(typeof chat.chatId).toBe('number');

        expect(chat).toHaveProperty('chatTitle');
        expect(typeof chat.chatTitle).toBe('string');

        expect(chat).toHaveProperty('isBotAdmin');
        expect(typeof chat.isBotAdmin).toBe('boolean');

        // Optional field
        if (chat.memberCount !== undefined) {
          expect(typeof chat.memberCount).toBe('number');
        }
      }
    }
  );

  it.skipIf(!fixtureExists('chat_detail_response.json'))(
    'should deserialize single chat response from backend schema',
    () => {
      const result = loadJsonFixture<Chat>('chat_detail_response.json');

      expect(result).toHaveProperty('chatId');
      expect(typeof result.chatId).toBe('number');

      expect(result).toHaveProperty('chatTitle');
      expect(typeof result.chatTitle).toBe('string');

      expect(result).toHaveProperty('isBotAdmin');
      expect(typeof result.isBotAdmin).toBe('boolean');
    }
  );
});
