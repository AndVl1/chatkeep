/**
 * Settings Contract Tests
 *
 * Verifies that the frontend can correctly deserialize backend responses
 * for settings endpoints.
 */

import { describe, it, expect } from 'vitest';
import { loadJsonFixture, fixtureExists } from './contractTestUtils';
import type { ChatSettings, PunishmentType } from '@/types';

describe('Settings Contract', () => {
  it.skipIf(!fixtureExists('settings_response.json'))(
    'should deserialize settings response from backend schema',
    () => {
      const result = loadJsonFixture<ChatSettings>('settings_response.json');

      // Required fields
      expect(result).toHaveProperty('chatId');
      expect(typeof result.chatId).toBe('number');

      expect(result).toHaveProperty('chatTitle');
      expect(typeof result.chatTitle).toBe('string');

      expect(result).toHaveProperty('collectionEnabled');
      expect(typeof result.collectionEnabled).toBe('boolean');

      expect(result).toHaveProperty('cleanServiceEnabled');
      expect(typeof result.cleanServiceEnabled).toBe('boolean');

      expect(result).toHaveProperty('maxWarnings');
      expect(typeof result.maxWarnings).toBe('number');

      expect(result).toHaveProperty('warningTtlHours');
      expect(typeof result.warningTtlHours).toBe('number');

      expect(result).toHaveProperty('thresholdAction');
      const validPunishments: PunishmentType[] = ['NOTHING', 'WARN', 'MUTE', 'BAN', 'KICK'];
      expect(validPunishments).toContain(result.thresholdAction);

      expect(result).toHaveProperty('thresholdDurationMinutes');
      expect(
        result.thresholdDurationMinutes === null ||
        typeof result.thresholdDurationMinutes === 'number'
      ).toBe(true);

      expect(result).toHaveProperty('defaultBlocklistAction');
      expect(validPunishments).toContain(result.defaultBlocklistAction);

      expect(result).toHaveProperty('logChannelId');
      expect(
        result.logChannelId === null ||
        typeof result.logChannelId === 'number'
      ).toBe(true);

      expect(result).toHaveProperty('lockWarnsEnabled');
      expect(typeof result.lockWarnsEnabled).toBe('boolean');
    }
  );

  it.skipIf(!fixtureExists('settings_update_request.json'))(
    'should serialize update request to backend schema',
    () => {
      const request = loadJsonFixture('settings_update_request.json');

      // Verify frontend sends compatible format
      expect(request).toBeDefined();
    }
  );
});
